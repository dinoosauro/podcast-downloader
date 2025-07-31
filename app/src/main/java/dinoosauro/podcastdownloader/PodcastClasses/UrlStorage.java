package dinoosauro.podcastdownloader.PodcastClasses;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.util.Arrays;
import java.util.List;

/**
 * Edit the URL history or, if "true" is passed as the last argument, the list of XML links
 */
public class UrlStorage {
    /**
     * Get a list of the URLs that have been saved as a file.
     * Deprecated: to avoid excessive RAM usage, especially when the downloaded files list is big, use the `checkEntry` method.
     * @param context the Application context
     * @param getPodcastXmlList if true, the list of the XML links the user has downloaded a podcast from will be returned (with their track number settings at the end). If false, the list of links the user downloaded will be returned.
     * @return the list of downloaded URLs
     */
    @Deprecated
    public static List<String> getDownloadedUrl(Context context, boolean getPodcastXmlList) {
        File file = new File(context.getFilesDir(), getPodcastXmlList ? "PodcastList.txt" : "DownloadedLinks.txt");
        if (!file.exists()) return null;
        try {
            FileInputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            List<String> outputUrls = new ArrayList<>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                outputUrls.add(line);
            }
            return outputUrls;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Check that either in the Podcast XML links or in the downloaded links history an entry is there
     * @param context the application Context
     * @param getPodcastXmlList if true, the XML list of the URLs the user has downloaded a podcast from will be checked. If false, the list of downloaded URLs will be checked.
     * @param entry the link to compare.
     * @return true if the item exists in the file, false otherwise.
     * @implNote This function currently doesn't automatically extract the link from the Podcast XML file, so also the track number settings must be passed.
     */
    public static boolean checkEntry(Context context, boolean getPodcastXmlList, String entry) {
        File file = new File(context.getFilesDir(), getPodcastXmlList ? "PodcastList.txt" : "DownloadedLinks.txt");
        if (!file.exists()) return false;
        try {
            FileInputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.equals(entry)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;

    }
    /**
     * Get the list of the URLs that have already been downloaded.
     * Deprecated: to avoid excessive RAM usage, especially when the downloaded files list is big, use the `checkEntry` method.
     * @param context the Application context
     * @return the list of downloaded URLs
     */

    @Deprecated
    public static List<String> getDownloadedUrl(Context context) {
        return getDownloadedUrl(context, false);
    }

    /**
     * Get the BufferedReader of either the XML link list or the download link history list
     * @param context the application Context
     * @param getPodcastXmlList if true, the XML list of the URLs the user has downloaded a podcast from will be opened. If false, the list of downloaded URLs will be opened.
     * @return the BufferedReader, where each line is a new link
     * @implNote This function doesn't extract the link from the Podcast XML file, so there'll be in the last part of the string the track number settings. To remove them, split the string by a space, and then remove the last three entries.
     */
    public static BufferedReader getDownloadBuffer(Context context, boolean getPodcastXmlList) {
        File file = new File(context.getFilesDir(), getPodcastXmlList ? "PodcastList.txt" : "DownloadedLinks.txt");
        if (!file.exists()) return null;
        try {
            FileInputStream stream = new FileInputStream(file);
            return new BufferedReader(new InputStreamReader(stream));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Remove every URL from the history
     * @param context the Application context
     * @param getPodcastXmlList if true, the XML list of the URLs the user has downloaded a podcast from will be deleted. If false, the list of downloaded URLs will be deleted.
     */
    public static void removeUrls(Context context, boolean getPodcastXmlList) {
        try {
            new File(context.getFilesDir(), getPodcastXmlList ? "PodcastList.txt" : "DownloadedLinks.txt").delete();
        } catch (Exception ignored) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(context, "Failed to delete the history JSON file.", Toast.LENGTH_LONG).show());
        }
    }
     /**
     * Remove every downloaded audio URL from the history
     * @param context the Application context
     */
    public static void removeUrls(Context context) {
        removeUrls(context, false);
    }

    /**
     * Add an URL to either the downloaded URL history or the downloaded podcast XML history.
     * @param context the Application context
     * @param url the URL to add
     * @param getPodcastXmlList if true, the URL will be added to the XML list of the URLs the user has downloaded a podcast from. If false, the URL will be added to the list of downloaded URLs will be opened.
     * @param overwritePrevious if the `url` is already in the file, overwrite the URL instead of adding it. Note that this function automatically extracts the URL from the Podcast XML list file.
     */
    public static void addDownloadedUrl(Context context, String url, boolean getPodcastXmlList, boolean overwritePrevious) {
        try {
            File file = new File(context.getFilesDir(), getPodcastXmlList ? "PodcastList.txt" : "DownloadedLinks.txt");
            if (!file.exists()) file.createNewFile();
            StringBuilder thingsToWrite = new StringBuilder(); // The items that'll be added in the output string, in case the file must be re-written
            if (overwritePrevious) {
                String textToLook = url; // The string with the real URL to look. This is done since we need to remove the podcast track number settings from the Podcast XML list, so in that case the `textToLook` string won't be the same as the `url` one.
                if (getPodcastXmlList) { 
                    String[] arr = textToLook.split(" ");
                    textToLook = String.join(" ", Arrays.copyOf(arr, arr.length - 3));
                }
                // Read the file per each line, and look if there's a same entry. If so, don't add it to the output file.
                FileInputStream stream = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String lineToLook = line;
                    if (getPodcastXmlList) {
                        String[] arr = line.split(" ");
                        if (arr.length > 3) lineToLook = String.join(" ", Arrays.copyOf(arr, arr.length - 3));
                    }
                    if (!lineToLook.equals(textToLook)) thingsToWrite.append(line).append("\n");
                };
            }
            FileOutputStream fos = new FileOutputStream(file, !overwritePrevious);
            if (!thingsToWrite.toString().trim().isEmpty()) fos.write(thingsToWrite.toString().getBytes());
            fos.write((url + "\n").getBytes());
            fos.close();
        } catch (Exception ignored) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(context, "Failed to add the podcast URL in history.", Toast.LENGTH_LONG).show());
        }
    }
    /**
     * Add an URL to the downloaded URL history
     * @param context the Application context
     * @param url the URL to add
     */
    public static void addDownloadedUrl(Context context, String url) {
        addDownloadedUrl(context, url, false, false);
    }
}
