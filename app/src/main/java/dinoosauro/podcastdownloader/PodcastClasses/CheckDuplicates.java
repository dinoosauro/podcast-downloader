package dinoosauro.podcastdownloader.PodcastClasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import dinoosauro.podcastdownloader.PodcastDownloader;
import dinoosauro.podcastdownloader.R;

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
        String folderString = preferences.getString("DownloadFolder", null);
        if (availableSources != null && folderString != null) {
            try {
                Uri folderUri = Uri.parse(folderString);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(context, folderUri);
                // Let's start by getting all the files in the directory. We'll look both for files outside and inside the show-specific directory.
                HashMap<String, List<String>> availableSubfolderFiles = new HashMap<String, List<String>>();
                List<String> filesNotInSubfolders = new ArrayList<>();
                for (DocumentFile subdir: pickedDir.listFiles()) {
                    if (subdir.isDirectory()) {
                        List<String> output = new ArrayList<>();
                        for (DocumentFile singlePodcasts: subdir.listFiles()) {
                            output.add(singlePodcasts.getName());
                        }
                        availableSubfolderFiles.put(subdir.getName(), output);
                    } else filesNotInSubfolders.add(subdir.getName());
                }
                String source;
                while ((source = availableSources.readLine()) != null) { // Iterate over all the podcast RSS feeds
                    String[] splitSpace = source.split(" ");
                    PodcastInformation information = preferences.getBoolean("UsePreviousTrackSettings", true) ? GetPodcastInformation.FromUrl(String.join(" ", Arrays.copyOf(splitSpace, splitSpace.length - 3)), context, null, splitSpace[splitSpace.length - 3].equals("1"), Integer.parseInt(splitSpace[splitSpace.length - 2]), Integer.parseInt(splitSpace[splitSpace.length - 1])) : GetPodcastInformation.FromUrl(String.join(" ", Arrays.copyOf(splitSpace, splitSpace.length - 3)), context, null);
                    if (information != null) {
                        List<String> downloadedPodcasts = availableSubfolderFiles.get(PodcastDownloader.DownloadQueue.nameSanitizer(information.title));
                        if (downloadedPodcasts != null) {
                            for (ShowItems items : information.items) {
                                String fileName = PodcastDownloader.DownloadQueue.nameSanitizer(items.title + PodcastDownloader.DownloadQueue.getExtensionFromUrl(items.url));
                                if (downloadedPodcasts.contains(fileName) || filesNotInSubfolders.contains(fileName)) {
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
