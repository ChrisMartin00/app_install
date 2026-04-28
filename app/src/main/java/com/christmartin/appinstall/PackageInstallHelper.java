package com.christmartin.appinstall;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class PackageInstallHelper {
    private PackageInstallHelper() {}

    static void downloadFile(String downloadUrl, File outFile) throws Exception {
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

    static boolean trySilentInstall(File apkFile) {
        try {
            Process process = new ProcessBuilder("su", "-c", "pm install -r \"" + apkFile.getAbsolutePath() + "\"")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    static void openSystemInstaller(Context context, File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
