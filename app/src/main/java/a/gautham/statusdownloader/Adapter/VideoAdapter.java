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
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.List;
import java.util.Objects;

import a.gautham.statusdownloader.Models.Status;
import a.gautham.statusdownloader.R;
import a.gautham.statusdownloader.Utils.Common;

public class VideoAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private final List<Status> videoList;
    private Context context;
    private final RelativeLayout container;

    // Ads
    private RewardedAd mRewardedAd;
    private InterstitialAd mInterstitialAd;

    public VideoAdapter(List<Status> videoList, RelativeLayout container) {
        this.videoList = videoList;
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

        final Status status = videoList.get(position);

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
        // 🎁 SAVE BUTTON (Rewarded Ad)
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
                            // User earned reward → Save video
                            Common.copyFile(status, context, container);
                        });

            } else {
                // If ad not loaded → save directly
                Common.copyFile(status, context, container);
            }
        });

        // ==============================
        // 📺 SHARE BUTTON (Interstitial)
        // ==============================
        holder.share.setOnClickListener(v -> {

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/mp4");

            if (status.isApi30()) {
                shareIntent.putExtra(Intent.EXTRA_STREAM,
                        status.getDocumentFile().getUri());
            } else {
                shareIntent.putExtra(Intent.EXTRA_STREAM,
                        Uri.parse("file://" + status.getFile().getAbsolutePath()));
            }

            if (mInterstitialAd != null) {

                mInterstitialAd.setFullScreenContentCallback(
                        new FullScreenContentCallback() {

                            @Override
                            public void onAdDismissedFullScreenContent() {

                                context.startActivity(
                                        Intent.createChooser(shareIntent, "Share video")
                                );

                                mInterstitialAd = null;
                                loadInterstitialAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(
                                    @NonNull com.google.android.gms.ads.AdError adError) {

                                context.startActivity(
                                        Intent.createChooser(shareIntent, "Share video")
                                );
                            }
                        });

                mInterstitialAd.show((Activity) context);

            } else {
                context.startActivity(
                        Intent.createChooser(shareIntent, "Share video")
                );
            }
        });

        // ==============================
        // FULL SCREEN VIDEO VIEW
        // ==============================
        holder.imageView.setOnClickListener(v -> {

            LayoutInflater inflater = LayoutInflater.from(context);
            View view1 = inflater.inflate(R.layout.view_video_full_screen, null);

            AlertDialog.Builder alertDg = new AlertDialog.Builder(context);

            FrameLayout mediaControls =
                    view1.findViewById(R.id.videoViewWrapper);

            alertDg.setView(view1);

            VideoView videoView =
                    view1.findViewById(R.id.video_full);

            MediaController mediaController =
                    new MediaController(context, false);

            videoView.setOnPreparedListener(mp -> {
                mp.start();
                mp.setLooping(true);
                mediaController.show(0);
            });

            videoView.setMediaController(mediaController);
            mediaController.setMediaPlayer(videoView);

            if (status.isApi30()) {
                videoView.setVideoURI(status.getDocumentFile().getUri());
            } else {
                videoView.setVideoURI(Uri.fromFile(status.getFile()));
            }

            videoView.requestFocus();

            if (mediaController.getParent() != null) {
                ((ViewGroup) mediaController.getParent())
                        .removeView(mediaController);
            }

            mediaControls.addView(mediaController);

            AlertDialog alert2 = alertDg.create();

            Objects.requireNonNull(alert2.getWindow()).getAttributes().windowAnimations =
                    R.style.SlidingDialogAnimation;

            alert2.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alert2.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));

            alert2.show();
        });
    }

    // ==============================
    // Load Rewarded Ad
    // ==============================
    private void loadRewardedAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(
                context,
                "ca-app-pub-3940256099942544/5224354917",
                adRequest,
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
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
    // Load Interstitial Ad
    // ==============================
    private void loadInterstitialAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(
                context,
                "ca-app-pub-3940256099942544/1033173712",
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
        return videoList.size();
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
//import android.widget.FrameLayout;
//import android.widget.MediaController;
//import android.widget.RelativeLayout;
//import android.widget.VideoView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//
//import java.util.List;
//
//import a.gautham.statusdownloader.Models.Status;
//import a.gautham.statusdownloader.R;
//import a.gautham.statusdownloader.Utils.Common;
//
//public class VideoAdapter extends RecyclerView.Adapter<ItemViewHolder> {
//
//    private final List<Status> videoList;
//    private Context context;
//    private final RelativeLayout container;
//
//    public VideoAdapter(List<Status> videoList, RelativeLayout container) {
//        this.videoList = videoList;
//        this.container = container;
//    }
//
//    @NonNull
//    @Override
//    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//
//        context = parent.getContext();
//        View view = LayoutInflater.from(context).inflate(R.layout.item_status, parent, false);
//        return new ItemViewHolder(view);
//
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
//
//        final Status status = videoList.get(position);
//
//        if (status.isApi30()) {
////            holder.save.setVisibility(View.GONE);
//            Glide.with(context).load(status.getDocumentFile().getUri()).into(holder.imageView);
//        } else {
////            holder.save.setVisibility(View.VISIBLE);
//            Glide.with(context).load(status.getFile()).into(holder.imageView);
//        }
//
//        holder.share.setOnClickListener(v -> {
//
//            Intent shareIntent = new Intent(Intent.ACTION_SEND);
//
//            shareIntent.setType("image/mp4");
//            if (status.isApi30()) {
//                shareIntent.putExtra(Intent.EXTRA_STREAM, status.getDocumentFile().getUri());
//            } else {
//                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + status.getFile().getAbsolutePath()));
//            }
//            context.startActivity(Intent.createChooser(shareIntent, "Share image"));
//
//        });
//
//        LayoutInflater inflater = LayoutInflater.from(context);
//        final View view1 = inflater.inflate(R.layout.view_video_full_screen, null);
//
//        holder.imageView.setOnClickListener(v -> {
//
//            final AlertDialog.Builder alertDg = new AlertDialog.Builder(context);
//
//            FrameLayout mediaControls = view1.findViewById(R.id.videoViewWrapper);
//
//            if (view1.getParent() != null) {
//                ((ViewGroup) view1.getParent()).removeView(view1);
//            }
//
//            alertDg.setView(view1);
//
//            final VideoView videoView = view1.findViewById(R.id.video_full);
//
//            final MediaController mediaController = new MediaController(context, false);
//
//            videoView.setOnPreparedListener(mp -> {
//
//                mp.start();
//                mediaController.show(0);
//                mp.setLooping(true);
//            });
//
//            videoView.setMediaController(mediaController);
//            mediaController.setMediaPlayer(videoView);
//
//            if (status.isApi30()) {
//                videoView.setVideoURI(status.getDocumentFile().getUri());
//            } else {
//                videoView.setVideoURI(Uri.fromFile(status.getFile()));
//            }
//            videoView.requestFocus();
//
//            ((ViewGroup) mediaController.getParent()).removeView(mediaController);
//
//            if (mediaControls.getParent() != null) {
//                mediaControls.removeView(mediaController);
//            }
//
//            mediaControls.addView(mediaController);
//
//            final AlertDialog alert2 = alertDg.create();
//
//            alert2.getWindow().getAttributes().windowAnimations = R.style.SlidingDialogAnimation;
//            alert2.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            alert2.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//            alert2.show();
//
//        });
//
//        holder.save.setOnClickListener(v -> Common.copyFile(status, context, container));
//
//    }
//
//    @Override
//    public int getItemCount() {
//        return videoList.size();
//    }
//
//}
