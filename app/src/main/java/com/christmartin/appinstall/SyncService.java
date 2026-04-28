package com.christmartin.appinstall;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.util.List;

public class SyncService extends IntentService {
    private static final String TAG = "AppInstallSync";

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            List<ReleaseAsset> assets = GitHubReleaseClient.fetchApkAssets();
            for (ReleaseAsset asset : assets) {
                File outFile = new File(getCacheDir(), asset.name);
                PackageInstallHelper.downloadFile(asset.downloadUrl, outFile);
                boolean installed = PackageInstallHelper.trySilentInstall(outFile);
                Log.i(TAG, asset.name + " installed=" + installed);
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
        }
    }
}
