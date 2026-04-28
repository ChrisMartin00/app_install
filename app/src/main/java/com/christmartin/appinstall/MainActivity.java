package com.christmartin.appinstall;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String OWNER = "ChrisMartin00";
    private static final String REPO = "app_install";
    private static final String RELEASE_URL = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ReleaseAsset> assets = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView statusView;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        statusView = new TextView(this);
        statusView.setPadding(32, 32, 32, 32);
        statusView.setTextSize(18f);
        statusView.setText(getString(R.string.loading));
        root.addView(statusView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        listView = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> installAsset(assets.get(position)));
        root.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        setContentView(root);
        loadLatestAssets();
    }

    private void loadLatestAssets() {
        new Thread(() -> {
            try {
                List<ReleaseAsset> apkAssets = fetchApkAssets();
                assets.clear();
                assets.addAll(apkAssets);
                List<String> labels = new ArrayList<>();
                for (ReleaseAsset asset : apkAssets) {
                    labels.add(asset.name);
                }
                mainHandler.post(() -> {
                    adapter.clear();
                    adapter.addAll(labels);
                    adapter.notifyDataSetChanged();
                    if (labels.isEmpty()) {
                        statusView.setText(getString(R.string.empty_state));
                    } else {
                        statusView.setText("Tap an APK to install it from the latest GitHub release.");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    statusView.setText("Failed to load release assets: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Release fetch failed", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private List<ReleaseAsset> fetchApkAssets() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(RELEASE_URL).openConnection();
        connection.setRequestProperty("User-Agent", "AppInstall/1.0");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             Reader reader = new InputStreamReader(in)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }

            JSONObject root = new JSONObject(sb.toString());
            JSONArray assetsJson = root.getJSONArray("assets");
            List<ReleaseAsset> result = new ArrayList<>();
            for (int i = 0; i < assetsJson.length(); i++) {
                JSONObject item = assetsJson.getJSONObject(i);
                String name = item.optString("name", "");
                String url = item.optString("browser_download_url", "");
                if (name.toLowerCase().endsWith(".apk") && !url.isEmpty()) {
                    result.add(new ReleaseAsset(name, url));
                }
            }
            return result;
        } finally {
            connection.disconnect();
        }
    }

    private void installAsset(ReleaseAsset asset) {
        statusView.setText("Preparing " + asset.name + "...");
        new Thread(() -> {
            try {
                File outFile = new File(getCacheDir(), asset.name);
                downloadFile(asset.downloadUrl, outFile);

                if (trySilentInstall(outFile)) {
                    mainHandler.post(() -> {
                        statusView.setText(asset.name + " installed silently.");
                        Toast.makeText(MainActivity.this, "Installed silently", Toast.LENGTH_LONG).show();
                    });
                } else {
                    mainHandler.post(() -> {
                        statusView.setText("Opening installer for " + asset.name);
                        openSystemInstaller(outFile);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    statusView.setText("Install failed: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Install failed", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void downloadFile(String downloadUrl, File outFile) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setRequestProperty("User-Agent", "AppInstall/1.0");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
        } finally {
            connection.disconnect();
        }
    }

    private boolean trySilentInstall(File apkFile) {
        try {
            Process process = new ProcessBuilder("su", "-c", "pm install -r \"" + apkFile.getAbsolutePath() + "\"")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void openSystemInstaller(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private static final class ReleaseAsset {
        final String name;
        final String downloadUrl;

        ReleaseAsset(String name, String downloadUrl) {
            this.name = name;
            this.downloadUrl = downloadUrl;
        }
    }
}
