package a.gautham.statusdownloader.Adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.List;
import java.util.Objects;

import a.gautham.statusdownloader.Models.Status;
import a.gautham.statusdownloader.R;
import a.gautham.statusdownloader.Utils.Common;

public class ImageAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private final List<Status> imagesList;
    private Context context;
    private final RelativeLayout container;

    // Rewarded Ad (SAVE)
    private RewardedAd mRewardedAd;

    // Interstitial Ad (SHARE)
    private InterstitialAd mInterstitialAd;

    public ImageAdapter(List<Status> imagesList, RelativeLayout container) {
        this.imagesList = imagesList;
        this.container = container;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        context = parent.getContext();

        MobileAds.initialize(context);

        loadRewardedAd();
        loadInterstitialAd();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_status, parent, false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {

        final Status status = imagesList.get(position);

        // Load Image
        if (status.isApi30()) {
            Glide.with(context)
                    .load(status.getDocumentFile().getUri())
                    .into(holder.imageView);
        } else {
            Glide.with(context)
                    .load(status.getFile())
                    .into(holder.imageView);
        }

        // ==============================
        // 🔥 SAVE BUTTON (REWARDED AD)
        // ==============================
        holder.save.setOnClickListener(v -> {

            if (mRewardedAd != null) {

                mRewardedAd.setFullScreenContentCallback(
                        new FullScreenContentCallback() {

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mRewardedAd = null;
                                loadRewardedAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(
                                    @NonNull com.google.android.gms.ads.AdError adError) {
                                mRewardedAd = null;
                                loadRewardedAd();
                            }
                        });

                mRewardedAd.show((Activity) context,
                        rewardItem -> {
                            // User earned reward → SAVE FILE
                            Common.copyFile(status, context, container);
                        });

            } else {
                // If ad not loaded → save directly
                Common.copyFile(status, context, container);
            }
        });

        // ==============================
        // 🔥 SHARE BUTTON (INTERSTITIAL AD)
        // ==============================
        holder.share.setOnClickListener(v -> {

            if (mInterstitialAd != null) {

                mInterstitialAd.setFullScreenContentCallback(
                        new FullScreenContentCallback() {

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mInterstitialAd = null;
                                loadInterstitialAd();
                                shareStatus(status);
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(
                                    @NonNull com.google.android.gms.ads.AdError adError) {
                                mInterstitialAd = null;
                                loadInterstitialAd();
                                shareStatus(status);
                            }
                        });

                mInterstitialAd.show((Activity) context);

            } else {
                // If ad not loaded → directly share
                shareStatus(status);
            }
        });

        // ==============================
        // FULL SCREEN VIEW
        // ==============================
        holder.imageView.setOnClickListener(v -> {

            final AlertDialog.Builder alertD =
                    new AlertDialog.Builder(context);

            LayoutInflater inflater =
                    LayoutInflater.from(context);

            View view = inflater.inflate(
                    R.layout.view_image_full_screen, null);

            alertD.setView(view);

            ImageView imageView =
                    view.findViewById(R.id.img);

            if (status.isApi30()) {
                Glide.with(context)
                        .load(status.getDocumentFile().getUri())
                        .into(imageView);
            } else {
                Glide.with(context)
                        .load(status.getFile())
                        .into(imageView);
            }

            AlertDialog alert = alertD.create();

            Objects.requireNonNull(alert.getWindow())
                    .getAttributes().windowAnimations =
                    R.style.SlidingDialogAnimation;

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alert.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));

            alert.show();
        });
    }

    // ==============================
    // SHARE FUNCTION
    // ==============================
    private void shareStatus(Status status) {

        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        if (status.isVideo()) {
            shareIntent.setType("video/mp4");
        } else {
            shareIntent.setType("image/jpg");
        }

        if (status.isApi30()) {
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    status.getDocumentFile().getUri());
        } else {
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    Uri.fromFile(status.getFile()));
        }

        context.startActivity(
                Intent.createChooser(shareIntent, "Share")
        );
    }

    // ==============================
    // LOAD REWARDED AD
    // ==============================
    private void loadRewardedAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(
                context,
                "ca-app-pub-3940256099942544/5224354917", // TEST Rewarded ID
                adRequest,
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(
                            @NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                    }

                    @Override
                    public void onAdFailedToLoad(
                            @NonNull LoadAdError loadAdError) {
                        mRewardedAd = null;
                    }
                });
    }

    // ==============================
    // LOAD INTERSTITIAL AD
    // ==============================
    private void loadInterstitialAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(
                context,
                "ca-app-pub-3940256099942544/1033173712", // TEST Interstitial ID
                adRequest,
                new InterstitialAdLoadCallback() {

                    @Override
                    public void onAdLoaded(
                            @NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(
                            @NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    @Override
    public int getItemCount() {
        return imagesList.size();
    }
}

























//package a.gautham.statusdownloader.Adapter;
//
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.net.Uri;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.Window;
//import android.widget.ImageView;
//import android.widget.RelativeLayout;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.FullScreenContentCallback;
//import com.google.android.gms.ads.LoadAdError;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.interstitial.InterstitialAd;
//import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
//
//import java.util.List;
//import java.util.Objects;
//
//import a.gautham.statusdownloader.Models.Status;
//import a.gautham.statusdownloader.R;
//import a.gautham.statusdownloader.Utils.Common;
//
//public class ImageAdapter extends RecyclerView.Adapter<ItemViewHolder> {
//
//    private final List<Status> imagesList;
//    private Context context;
//    private final RelativeLayout container;
//
//    // Interstitial Ad
//    private InterstitialAd mInterstitialAd;
//
//    public ImageAdapter(List<Status> imagesList, RelativeLayout container) {
//        this.imagesList = imagesList;
//        this.container = container;
//    }
//
//    @NonNull
//    @Override
//    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//
//        context = parent.getContext();
//
//        // Initialize AdMob
//        MobileAds.initialize(context);
//
//        loadInterstitialAd();
//
//        View view = LayoutInflater.from(context).inflate(R.layout.item_status, parent, false);
//        return new ItemViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
//
//        final Status status = imagesList.get(position);
//
//        if (status.isApi30()) {
//            Glide.with(context).load(status.getDocumentFile().getUri()).into(holder.imageView);
//        } else {
//            Glide.with(context).load(status.getFile()).into(holder.imageView);
//        }
//
//        holder.save.setOnClickListener(v ->
//                Common.copyFile(status, context, container)
//        );
//
//        holder.share.setOnClickListener(v -> {
//
//            Intent shareIntent = new Intent(Intent.ACTION_SEND);
//            shareIntent.setType("image/jpg");
//
//            if (status.isApi30()) {
//                shareIntent.putExtra(Intent.EXTRA_STREAM,
//                        status.getDocumentFile().getUri());
//            } else {
//                shareIntent.putExtra(Intent.EXTRA_STREAM,
//                        Uri.parse("file://" + status.getFile().getAbsolutePath()));
//            }
//
//            // Show Interstitial Ad First
//            if (mInterstitialAd != null) {
//
//                mInterstitialAd.setFullScreenContentCallback(
//                        new FullScreenContentCallback() {
//
//                            @Override
//                            public void onAdDismissedFullScreenContent() {
//                                // After ad closed → open share
//                                context.startActivity(
//                                        Intent.createChooser(shareIntent, "Share image")
//                                );
//
//                                mInterstitialAd = null;
//                                loadInterstitialAd(); // Load next ad
//                            }
//
//                            @Override
//                            public void onAdFailedToShowFullScreenContent(
//                                    com.google.android.gms.ads.AdError adError) {
//
//                                context.startActivity(
//                                        Intent.createChooser(shareIntent, "Share image")
//                                );
//                            }
//                        });
//
//                mInterstitialAd.show((android.app.Activity) context);
//
//            } else {
//                // If ad not loaded → directly share
//                context.startActivity(
//                        Intent.createChooser(shareIntent, "Share image")
//                );
//            }
//
//        });
//
//        holder.imageView.setOnClickListener(v -> {
//
//            final AlertDialog.Builder alertD = new AlertDialog.Builder(context);
//            LayoutInflater inflater = LayoutInflater.from(context);
//            View view = inflater.inflate(R.layout.view_image_full_screen, null);
//            alertD.setView(view);
//
//            ImageView imageView = view.findViewById(R.id.img);
//
//            if (status.isApi30()) {
//                Glide.with(context)
//                        .load(status.getDocumentFile().getUri())
//                        .into(imageView);
//            } else {
//                Glide.with(context)
//                        .load(status.getFile())
//                        .into(imageView);
//            }
//
//            AlertDialog alert = alertD.create();
//            Objects.requireNonNull(alert.getWindow()).getAttributes().windowAnimations =
//                    R.style.SlidingDialogAnimation;
//
//            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            alert.getWindow().setBackgroundDrawable(
//                    new ColorDrawable(Color.TRANSPARENT)
//            );
//
//            alert.show();
//        });
//    }
//
//    private void loadInterstitialAd() {
//
//        AdRequest adRequest = new AdRequest.Builder().build();
//
//        InterstitialAd.load(
//                context,
//                "ca-app-pub-3940256099942544/1033173712", // TEST Interstitial Ad ID
//                adRequest,
//                new InterstitialAdLoadCallback() {
//
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                        mInterstitialAd = interstitialAd;
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        mInterstitialAd = null;
//                    }
//                });
//    }
//
//    @Override
//    public int getItemCount() {
//        return imagesList.size();
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
