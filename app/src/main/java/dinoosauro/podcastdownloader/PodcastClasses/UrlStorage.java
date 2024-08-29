package dinoosauro.podcastdownloader.PodcastClasses;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Edit the URL history
 */
public class UrlStorage {
    /**
     * Get the list of the URLs that have already been downloaded
     * @param context the Application context
     * @return the list of downloaded URLs
     */
    public static List<String> getDownloadedUrl(Context context) {
        File file = new File(context.getFilesDir(), "DownloadedLinks.txt");
        if (!file.exists()) return null;
        try {
            FileInputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            List<String> outputUrls = new ArrayList<>();
            String line = null;
            while ((line = reader.readLine()) != null) outputUrls.add(line);
            return outputUrls;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Remove every URL from the history
     * @param context the Application context
     */
    public static void removeUrls(Context context) {
        try {
            new File(context.getFilesDir(), "DownloadedLinks.txt").delete();
        } catch (Exception ignored) {
            Toast.makeText(context, "Failed to delete the history JSON file", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Add an URL to the downloaded URL history
     * @param context the Application context
     * @param url the URL to add
     */
    public static void addDownloadedUrl(Context context, String url) {
        try {
            File file = new File(context.getFilesDir(), "DownloadedLinks.txt");
            if (!file.exists()) file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write((url + "\n").getBytes());
            fos.close();
        } catch (Exception ignored) {
            Toast.makeText(context, "Failed to add the podcast URL in history.", Toast.LENGTH_LONG).show();
        }
    }
}
