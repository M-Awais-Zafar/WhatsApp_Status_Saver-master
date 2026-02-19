package com.smartstatus.downloader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;
import java.util.concurrent.Executors;

import a.gautham.library.AppUpdater;
import a.gautham.library.helper.Display;
import com.smartstatus.downloader.Adapter.PageAdapter;
import com.smartstatus.downloader.Utils.Common;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private long back_pressed;

    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Context context;
    private AdView adView;   // Banner Ad

    ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {

                            Intent data = result.getData();
                            assert data != null;

                            getContentResolver().takePersistableUriPermission(
                                    Objects.requireNonNull(data.getData()),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                            Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
                        }
                    });

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        // ✅ Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});

        // ✅ Load Banner Ad
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        checkForUpdate();

        MaterialToolbar toolbar = findViewById(R.id.toolbarMainActivity);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        setSupportActionBar(toolbar);

        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.images)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.videos)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.saved_files)));

        PagerAdapter adapter =
                new PageAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.menu_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey check out my app at \n\nhttps://github.com/GauthamAsir/WhatsApp_Status_Saver/releases");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (arePermissionDenied()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissionQ();
            } else {
                requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            }
        }

        if (Common.APP_DIR == null || Common.APP_DIR.isEmpty()) {
            Common.APP_DIR = Objects.requireNonNull(getExternalFilesDir("StatusDownloader")).getPath();
        }
    }

    private boolean arePermissionDenied() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getContentResolver().getPersistedUriPermissions().isEmpty();
        }

        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void requestPermissionQ() {
        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        Intent intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
        String startDir = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses";

        Uri uri = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");

        if (uri != null) {
            String scheme = uri.toString();
            scheme = scheme.replace("/root/", "/document/");
            scheme += "%3A" + startDir;
            uri = Uri.parse(scheme);
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        activityResultLauncher.launch(intent);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            finish();
        } else {
            Snackbar.make(viewPager,
                    "Press Again to Exit",
                    Snackbar.LENGTH_LONG).show();
            back_pressed = System.currentTimeMillis();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    private void checkForUpdate() {
        Executors.newSingleThreadExecutor().execute(() -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                AppUpdater appUpdater = new AppUpdater(MainActivity.this);
                appUpdater.setUpGithub("M Awais Zafar", "WhatsApp_Status_Saver");
                appUpdater.setDisplay(Display.DIALOG);
                appUpdater.start();
            });
        });
    }
}
