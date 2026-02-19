package com.smartstatus.downloader.Utils;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import com.smartstatus.downloader.Models.Status;
import com.smartstatus.downloader.R;

public class Common {
    public static final int GRID_COUNT = 2;

    private static final String CHANNEL_NAME = "GAUTHAM";

    public static final File STATUS_DIRECTORY = new File(Environment.getExternalStorageDirectory() +
            File.separator + "WhatsApp/Media/.Statuses");

    public static String APP_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/status_saver";

    public static void copyFile(Status status, Context context, RelativeLayout container) {

        File appDir = new File(APP_DIR);
        if (!appDir.exists()) {
            if (!appDir.mkdirs()) {
                Snackbar.make(container, "Something went wrong", Snackbar.LENGTH_SHORT).show();
            }
        }

        String fileName;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        if (status.isVideo()) {
            fileName = "VID_" + currentDateTime + ".mp4";
        } else {
            fileName = "IMG_" + currentDateTime + ".jpg";
        }

        File destFile = new File(appDir + File.separator + fileName);

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(status.isApi30() ? status.getDocumentFile().getUri() : Uri.fromFile(status.getFile()));
            OutputStream outputStream = context.getContentResolver().openOutputStream(registerNewFile(destFile, status.isVideo(), context));
            IOUtils.copy(inputStream, outputStream);

            showNotification(context, container, status, fileName, Uri.fromFile(destFile));

        } catch (IOException e) {
            e.printStackTrace();
            Snackbar.make(container, "Failed to save: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    private static Uri registerNewFile(File file, boolean isVideo, Context context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/status_saver");

        Uri collectionUri;
        if (isVideo) {
            values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            collectionUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            collectionUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }

        return context.getContentResolver().insert(collectionUri, values);
    }


    private static void showNotification(Context context, RelativeLayout container, Status status,
                                         String fileName, Uri data) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            makeNotificationChannel(context);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (status.isVideo()) {
            intent.setDataAndType(data, "video/*");
        } else {
            intent.setDataAndType(data, "image/*");
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(context, CHANNEL_NAME);

        notification.setSmallIcon(R.drawable.ic_file_download_black)
                .setContentTitle(fileName)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notification.setContentText("File Saved to " +
                Environment.DIRECTORY_DCIM + "/status_saver");

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        assert notificationManager != null;
        notificationManager.notify(new Random().nextInt(), notification.build());

        Snackbar.make(container, "Saved to " + APP_DIR, Snackbar.LENGTH_LONG).show();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void makeNotificationChannel(Context context) {

        NotificationChannel channel = new NotificationChannel(Common.CHANNEL_NAME, "Saved", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        assert notificationManager != null;
        notificationManager.createNotificationChannel(channel);
    }

}
