package com.smartstatus.downloader.Adapter;

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
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

import com.smartstatus.downloader.Models.Status;
import com.smartstatus.downloader.R;

public class FilesAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private final List<Status> imagesList;
    private Context context;

    private RewardedAd mRewardedAd;
    private InterstitialAd mInterstitialAd;

    public FilesAdapter(List<Status> imagesList) {
        this.imagesList = imagesList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        context = parent.getContext();
        MobileAds.initialize(context);

        loadRewardedAd();
        loadInterstitialAd();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_saved_files, parent, false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {

        holder.save.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24));

        holder.share.setVisibility(View.VISIBLE);
        holder.save.setVisibility(View.VISIBLE);

        final Status status = imagesList.get(position);

        if (status.isApi30()) {
            Glide.with(context)
                    .load(status.getDocumentFile().getUri())
                    .into(holder.imageView);
        } else {
            Glide.with(context)
                    .load(status.getFile())
                    .into(holder.imageView);
        }

        // ===================================
        // 🎁 SAVE (DELETE) - REWARDED AD
        // ===================================
        holder.save.setOnClickListener(view -> {

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
                                    com.google.android.gms.ads.AdError adError) {
                                mRewardedAd = null;
                                loadRewardedAd();
                            }
                        });

                mRewardedAd.show((Activity) context,
                        rewardItem -> {

                            // User watched ad → Delete file
                            if (status.getFile().delete()) {
                                imagesList.remove(holder.getAdapterPosition());
                                notifyItemRemoved(holder.getAdapterPosition());
                                Toast.makeText(context,
                                        "File Deleted",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context,
                                        "Unable to Delete File",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

            } else {
                // If ad not loaded → delete directly
                if (status.getFile().delete()) {
                    imagesList.remove(holder.getAdapterPosition());
                    notifyItemRemoved(holder.getAdapterPosition());
                    Toast.makeText(context,
                            "File Deleted",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context,
                            "Unable to Delete File",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ===================================
        // 📺 SHARE - INTERSTITIAL AD
        // ===================================
        holder.share.setOnClickListener(v -> {

            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            if (status.isVideo())
                shareIntent.setType("video/mp4");
            else
                shareIntent.setType("image/jpg");

            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    Uri.parse("file://" + status.getFile().getAbsolutePath()));

            if (mInterstitialAd != null) {

                mInterstitialAd.setFullScreenContentCallback(
                        new FullScreenContentCallback() {

                            @Override
                            public void onAdDismissedFullScreenContent() {

                                context.startActivity(
                                        Intent.createChooser(shareIntent, "Share file")
                                );

                                mInterstitialAd = null;
                                loadInterstitialAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(
                                    com.google.android.gms.ads.AdError adError) {

                                context.startActivity(
                                        Intent.createChooser(shareIntent, "Share file")
                                );
                            }
                        });

                mInterstitialAd.show((Activity) context);

            } else {
                context.startActivity(
                        Intent.createChooser(shareIntent, "Share file")
                );
            }
        });

        // ===================================
        // FULL SCREEN VIEW (UNCHANGED)
        // ===================================
        holder.imageView.setOnClickListener(v -> {

            if (status.isVideo()) {

                LayoutInflater inflater = LayoutInflater.from(context);
                View view1 = inflater.inflate(
                        R.layout.view_video_full_screen, null);

                AlertDialog.Builder alertDg =
                        new AlertDialog.Builder(context);

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
                videoView.setVideoURI(Uri.fromFile(status.getFile()));
                videoView.requestFocus();

                if (mediaController.getParent() != null) {
                    ((ViewGroup) mediaController.getParent())
                            .removeView(mediaController);
                }

                mediaControls.addView(mediaController);

                AlertDialog alert2 = alertDg.create();
                alert2.getWindow().getAttributes().windowAnimations =
                        R.style.SlidingDialogAnimation;
                alert2.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alert2.getWindow().setBackgroundDrawable(
                        new ColorDrawable(Color.TRANSPARENT));
                alert2.show();

            } else {

                AlertDialog.Builder alertD =
                        new AlertDialog.Builder(context);

                View view = LayoutInflater.from(context)
                        .inflate(R.layout.view_image_full_screen, null);

                alertD.setView(view);

                ImageView imageView = view.findViewById(R.id.img);

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
                alert.getWindow().getAttributes().windowAnimations =
                        R.style.SlidingDialogAnimation;
                alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alert.getWindow().setBackgroundDrawable(
                        new ColorDrawable(Color.TRANSPARENT));
                alert.show();
            }
        });
    }

    // Load Rewarded
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context,
                context.getString(R.string.rewarded_id),
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

    // Load Interstitial
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context,
                context.getString(R.string.interstitial_id),
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
















//package com.smartstatus.downloader.Adapter;
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
//import android.widget.ImageView;
//import android.widget.MediaController;
//import android.widget.Toast;
//import android.widget.VideoView;
//
//import androidx.annotation.NonNull;
//import androidx.core.content.ContextCompat;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//
//import java.util.List;
//
//import a.Models.statusdownloader.status.Status;
//import com.smartstatus.downloader.R;
//
//public class FilesAdapter extends RecyclerView.Adapter<ItemViewHolder> {
//
//    private final List<Status> imagesList;
//    private Context context;
//
//    public FilesAdapter(List<Status> imagesList) {
//        this.imagesList = imagesList;
//    }
//
//    @NonNull
//    @Override
//    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//
//        context = parent.getContext();
//        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_files, parent, false);
//        return new ItemViewHolder(view);
//
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
//
//        holder.save.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24));
//        holder.share.setVisibility(View.VISIBLE);
//        holder.save.setVisibility(View.VISIBLE);
//
//        final Status status = imagesList.get(position);
//
//        if (status.isApi30()) {
//            Glide.with(context).load(status.getDocumentFile().getUri()).into(holder.imageView);
//        } else {
//            Glide.with(context).load(status.getFile()).into(holder.imageView);
//        }
//
////        if (status.isVideo())
////            Glide.with(context).asBitmap().load(status.getFile()).into(holder.imageView);
//////            holder.imageView.setImageBitmap(status.getThumbnail());
////        else {
////            if(status.isApi30()) {
////                Glide.with(context).load(status.getDocumentFile().getUri()).into(holder.imageView);
////            } else  {
////                Glide.with(context).load(status.getFile()).into(holder.imageView);
////            }
////        }
//
//        holder.save.setOnClickListener(view -> {
//            if (status.getFile().delete()) {
//                imagesList.remove(position);
//                notifyDataSetChanged();
//                Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
//            } else
//                Toast.makeText(context, "Unable to Delete File", Toast.LENGTH_SHORT).show();
//        });
//
//        holder.share.setOnClickListener(v -> {
//
//            Intent shareIntent = new Intent(Intent.ACTION_SEND);
//
//            if (status.isVideo())
//                shareIntent.setType("image/mp4");
//            else
//                shareIntent.setType("image/jpg");
//
//            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + status.getFile().getAbsolutePath()));
//            context.startActivity(Intent.createChooser(shareIntent, "Share image"));
//
//        });
//
//        LayoutInflater inflater = LayoutInflater.from(context);
//        final View view1 = inflater.inflate(R.layout.view_video_full_screen, null);
//
//        holder.imageView.setOnClickListener(v -> {
//
//            if (status.isVideo()) {
//
//                final AlertDialog.Builder alertDg = new AlertDialog.Builder(context);
//
//                FrameLayout mediaControls = view1.findViewById(R.id.videoViewWrapper);
//
//                if (view1.getParent() != null) {
//                    ((ViewGroup) view1.getParent()).removeView(view1);
//                }
//
//                alertDg.setView(view1);
//
//                final VideoView videoView = view1.findViewById(R.id.video_full);
//
//                final MediaController mediaController = new MediaController(context, false);
//
//                videoView.setOnPreparedListener(mp -> {
//
//                    mp.start();
//                    mediaController.show(0);
//                    mp.setLooping(true);
//                });
//
//                videoView.setMediaController(mediaController);
//                mediaController.setMediaPlayer(videoView);
//                videoView.setVideoURI(Uri.fromFile(status.getFile()));
//                videoView.requestFocus();
//
//                ((ViewGroup) mediaController.getParent()).removeView(mediaController);
//
//                if (mediaControls.getParent() != null) {
//                    mediaControls.removeView(mediaController);
//                }
//
//                mediaControls.addView(mediaController);
//
//                final AlertDialog alert2 = alertDg.create();
//
//                alert2.getWindow().getAttributes().windowAnimations = R.style.SlidingDialogAnimation;
//                alert2.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                alert2.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//                alert2.show();
//
//            } else {
//
//                final AlertDialog.Builder alertD = new AlertDialog.Builder(context);
//                LayoutInflater inflater1 = LayoutInflater.from(context);
//                View view = inflater1.inflate(R.layout.view_image_full_screen, null);
//                alertD.setView(view);
//
//                ImageView imageView = view.findViewById(R.id.img);
//                if (status.isApi30()) {
//                    Glide.with(context).load(status.getDocumentFile().getUri()).into(imageView);
//                } else {
//                    Glide.with(context).load(status.getFile()).into(imageView);
//                }
//
//                AlertDialog alert = alertD.create();
//                alert.getWindow().getAttributes().windowAnimations = R.style.SlidingDialogAnimation;
//                alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                alert.show();
//
//            }
//
//        });
//
//    }
//
//    @Override
//    public int getItemCount() {
//        return imagesList.size();
//    }
//
//}
