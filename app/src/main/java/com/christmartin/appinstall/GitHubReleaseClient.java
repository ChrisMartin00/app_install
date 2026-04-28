package com.christmartin.appinstall;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

final class GitHubReleaseClient {
    private static final String OWNER = "ChrisMartin00";
    private static final String REPO = "app_install";
    private static final String RELEASE_URL = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";

    private GitHubReleaseClient() {}

    static List<ReleaseAsset> fetchApkAssets() throws Exception {
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
}
