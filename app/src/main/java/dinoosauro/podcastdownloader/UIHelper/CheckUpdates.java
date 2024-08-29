package dinoosauro.podcastdownloader.UIHelper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import dinoosauro.podcastdownloader.R;

public class CheckUpdates {
    /**
     * The current version of PodcastDownloader
     */
    public static final String VERSION_NUMBER = "1.1.0";
    /**
     * The latest version, fetched from GitHub
     */
    private static String suggestedVersion = "";

    /**
     * Connect to GitHub and check if there's a new version available
     * 
     * @param callback the Runnable that'll be started if there's a new version
     *                 available
     */
    public static void check(Runnable callback) {
        new Thread(() -> {
            try {
                URL webpageUrl = new URL(
                        "https://raw.githubusercontent.com/Dinoosauro/podcast-downloader/main/updateCode");
                HttpURLConnection connection = (HttpURLConnection) webpageUrl.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();
                suggestedVersion = content.toString().replace("\n", "").trim();
                if (!suggestedVersion.equals(VERSION_NUMBER))
                    callback.run();
            } catch (Exception ignored) {
            }

        }).start();
    }

    /**
     * Check if there's a new update, and shows a MaterialDialog if true
     * 
     * @param context the Context used for creating the dialog
     */
    public static void checkAndDisplay(Context context) {
        check(() -> {
            if (context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE)
                    .getString("AvoidVersionUpdate", "").equals(suggestedVersion))
                return;
            new Handler(context.getMainLooper()).post(() -> new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.update_available)
                    .setMessage(R.string.update_desc)
                    .setPositiveButton(R.string.update, (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Dinoosauro/podcast-downloader/releases"));
                        context.startActivity(intent);
                    })
                    .setNeutralButton(R.string.postpone_update, null)
                    .setNegativeButton(R.string.skip_update,
                            ((dialog, which) -> context
                                    .getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).edit()
                                    .putString("AvoidVersionUpdate", suggestedVersion).apply()))
                    .show());
        });
    }
}
