package dinoosauro.podcastdownloader.PodcastClasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
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
         * @param information the PodcastInformation to download
         */
        public abstract void enqueue(PodcastInformation information);
    }

    /**
     * Download if there isn't a file with the same name
     * @param preferences the SharedPreferences, used to get the podcast sources
     * @param breakAtFirst if the download should stop at the first item that has not been downloaded
     * @param enqueue the EnqueueHandler for downloading files
     */
    public static void UsingFileName(SharedPreferences preferences, boolean breakAtFirst, EnqueueHandler enqueue) {
        for (String source : preferences.getStringSet("PodcastSources", new HashSet<>())) { // Iterate over all the podcast RSS feeds
            PodcastInformation information = GetPodcastInformation.FromUrl(source, preferences, null);
            if (information != null) {
                for (ShowItems items : information.items) {
                    File check = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PodcastDownloader/" + PodcastDownloader.DownloadQueue.nameSanitizer(information.title) + "/" + PodcastDownloader.DownloadQueue.nameSanitizer(items.title + PodcastDownloader.DownloadQueue.getExtensionFromUrl(items.url) ));
                    if (check.exists()) {
                        if (breakAtFirst) break;
                        continue;
                    }
                    List<ShowItems> newItems = new ArrayList<ShowItems>(){{add(items);}};
                    enqueue.enqueue(new PodcastInformation(information.title, information.image, information.author, newItems));
                }
            }
        }
    }
    /**
     * Download if the URL hasn't been downloaded previously
     * @param preferences the SharedPreferences, used to get the podcast sources
     * @param breakAtFirst if the download should stop at the first item that has not been downloaded
     * @param context the Context of the application
     * @param enqueue the EnqueueHandler for downloading files
     */

    public static void UsingURL(SharedPreferences preferences, boolean breakAtFirst, Context context, EnqueueHandler enqueue) {
        List<String> downloadedUrls = UrlStorage.getDownloadedUrl(context);
        if (downloadedUrls == null) return; // No file has been downloaded
        for (String source : preferences.getStringSet("PodcastSources", new HashSet<>())) { // Iterate over all the podcast RSS feeds
            PodcastInformation information = GetPodcastInformation.FromUrl(source, preferences, null);
            if (information != null) {
                for (ShowItems items : information.items) {
                    if (downloadedUrls.contains(items.url)) {
                        if (breakAtFirst) break;
                        continue;
                    }
                    List<ShowItems> newItems = new ArrayList<ShowItems>(){{add(items);}};
                    enqueue.enqueue(new PodcastInformation(information.title, information.image, information.author, newItems));
                }
            }
        }
    }


}
