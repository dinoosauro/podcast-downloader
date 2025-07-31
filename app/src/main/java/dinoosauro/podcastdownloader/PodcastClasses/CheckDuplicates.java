package dinoosauro.podcastdownloader.PodcastClasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import dinoosauro.podcastdownloader.PodcastDownloader;

/**
 * Get if the file has already been downloaded
 */
public class CheckDuplicates {
    /**
     * This class is used so that this class can run the "enqueue" script in the main thread
     */
    public abstract static class EnqueueHandler {
        /**
         * The void that'll be called when an item should be enqueued in the downloads
         *
         * @param information the PodcastInformation to download
         */
        public abstract void enqueue(PodcastInformation information);
    }

    /**
     * Download if there isn't a file with the same name
     *
     * @param context      the Context used to get the SharedPreferences
     * @param breakAtFirst if the download should stop at the first item that has not been downloaded
     * @param enqueue      the EnqueueHandler for downloading files
     */
    public static void UsingFileName(Context context, boolean breakAtFirst, EnqueueHandler enqueue) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        BufferedReader availableSources = UrlStorage.getDownloadBuffer(context, true);
        if (availableSources != null) {
            try {
                String source;
                while ((source = availableSources.readLine()) != null) { // Iterate over all the podcast RSS feeds
                    String[] splitSpace = source.split(" ");
                    PodcastInformation information = preferences.getBoolean("UsePreviousTrackSettings", true) ? GetPodcastInformation.FromUrl(String.join(" ", Arrays.copyOf(splitSpace, splitSpace.length - 3)), context, null, splitSpace[splitSpace.length - 3].equals("1"), Integer.parseInt(splitSpace[splitSpace.length - 2]), Integer.parseInt(splitSpace[splitSpace.length - 1])) : GetPodcastInformation.FromUrl(String.join(" ", Arrays.copyOf(splitSpace, splitSpace.length - 3)), context, null);
                    if (information != null) {
                        for (ShowItems items : information.items) {
                            File check = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PodcastDownloader/" + PodcastDownloader.DownloadQueue.nameSanitizer(information.title) + "/" + PodcastDownloader.DownloadQueue.nameSanitizer(items.title + PodcastDownloader.DownloadQueue.getExtensionFromUrl(items.url)));
                            if (check.exists()) {
                                if (breakAtFirst) break;
                                continue;
                            }
                            List<ShowItems> newItems = new ArrayList<ShowItems>() {{
                                add(items);
                            }};
                            enqueue.enqueue(new PodcastInformation(information.title, information.image, information.author, newItems));
                        }
                    }
                }
            } catch (IOException ignored) {

            }
        }
    }

    /**
     * Download if the URL hasn't been downloaded previously
     *
     * @param breakAtFirst if the download should stop at the first item that has not been downloaded
     * @param context      the Context of the application
     * @param enqueue      the EnqueueHandler for downloading files
     */

    public static void UsingURL(boolean breakAtFirst, Context context, EnqueueHandler enqueue) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        BufferedReader availableSources = UrlStorage.getDownloadBuffer(context, true);
        if (availableSources != null) {
            try {
                String source;
                while ((source = availableSources.readLine()) != null) { // Iterate over all the podcast RSS feeds
                    String[] splitSpace = source.split(" ");
                    PodcastInformation information = preferences.getBoolean("UsePreviousTrackSettings", true) ? GetPodcastInformation.FromUrl(String.join(" ", Arrays.copyOf(splitSpace, splitSpace.length - 3)), context, null, splitSpace[splitSpace.length - 3].equals("1"), Integer.parseInt(splitSpace[splitSpace.length - 2]), Integer.parseInt(splitSpace[splitSpace.length - 1])) : GetPodcastInformation.FromUrl(String.join(" ", Arrays.copyOf(splitSpace, splitSpace.length - 3)), context, null);
                    if (information != null) {
                        for (ShowItems items : information.items) {
                            if (UrlStorage.checkEntry(context, false, items.url)) {
                                if (breakAtFirst) break;
                                continue;
                            }
                            List<ShowItems> newItems = new ArrayList<ShowItems>() {{
                                add(items);
                            }};
                            enqueue.enqueue(new PodcastInformation(information.title, information.image, information.author, newItems));
                        }
                    }
                }
            } catch (Exception ignored) {

            }
        }
    }


}
