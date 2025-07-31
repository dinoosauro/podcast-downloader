package dinoosauro.podcastdownloader;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.color.DynamicColors;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import dinoosauro.podcastdownloader.ForegroundService.ForegroundService;
import dinoosauro.podcastdownloader.PodcastClasses.DownloadCallback;
import dinoosauro.podcastdownloader.PodcastClasses.DownloadContent;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastDownloadInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;
import dinoosauro.podcastdownloader.UIHelper.DownloadUIManager;
import dinoosauro.podcastdownloader.ForegroundService.ForegroundNotificationHelper;
import dinoosauro.podcastdownloader.UIHelper.PodcastProgress;

public class PodcastDownloader extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }

    /**
     * The podcast information that are being selected by the user
     */
    private static PodcastInformation currentPodcastInfo;

    /**
     * Set the podcast information that will be selected by the user
     * @param info the PodcastInformation to set
     */
    public static void setPodcastInformation(PodcastInformation info) {
        currentPodcastInfo = info;
    }

    /**
     * Get the PodcastInformation the user needs to choose
     * @return the PodcastInformation to choose
     */
    public static PodcastInformation getPodcastInformation() {
        return currentPodcastInfo;
    }

    /**
     * Delete the PodcastInformation ArrayList
     */
    public static void clearPodcastInformation() {
        currentPodcastInfo = null;
    }

    /**
     * Handle podcast download process
     */
    public static class DownloadQueue {

        private static Context appContext;

        /**
         * Set the application context for downloading the file
         * @param context the application Context
         */
        public static void setContext(Context context) {
            appContext = context;
        }

        /**
         * The items to download
         */
        private static List<PodcastInformation> downloadPodcastInfoList = new ArrayList<>();
        /**
         * A Map that, for each DownloaderManager ID (long), associates its PodcastInformation
         */
        public static Map<Long, PodcastDownloadInformation> currentOperations = new HashMap<>();

        /**
         * Get the next element to download, and deletes it from the array
         * @return the PodcastInformation element to download
         */
        public static PodcastInformation shift() {
            try {
                PodcastInformation firstEntry = downloadPodcastInfoList.get(0);
                downloadPodcastInfoList.remove(0);
                return firstEntry;
            } catch (IndexOutOfBoundsException ex) {
                return null;
            }
        }

        /**
         * Get the size of the remaining items to download
         * @return an int, with the number of items that are in the queue.
         */
        public static int getInfoLength() {
            return downloadPodcastInfoList.size();
        }

        /**
         * Sanitize the name so that it's valid on every OS
         * @param str the string to sanitize
         * @return the sanitized string
         */
        public static String nameSanitizer(String str) {
            if (str.contains("\n")) { // Delete new lines, since they aren't allowed in file names. We'll also trim each line, so that we can delete XML indentation
                String[] split = str.split("\n");
                StringBuilder output = new StringBuilder();
                for (int i = 0; i < split.length; i++) output.append(split[i].trim()).append(split.length - 1 == i ? "" : " "); // Add the space for each line, excluding the last one
                str = output.toString();
            }
            return str.replace("<", "‹").replace(">", "›").replace(":", "∶").replace("\"", "″").replace("/", "∕").replace("\\", "∖").replace("|", "¦").replace("?", "¿").replace("*", "");
        }

        /**
         * If no elements are in the queue, the Foreground service will be disabled.
         */
        public static void disableServiceIfNecessary() {
            try {
                if (currentOperations.isEmpty() && downloadPodcastInfoList.isEmpty() && ForegroundNotificationHelper.isServiceRunning(appContext)) {
                Intent stopIntent = new Intent(appContext, ForegroundService.class);
                stopIntent.setAction("STOP_FOREGROUND_SERVICE");
                appContext.startService(stopIntent);
            }
            } catch (Exception ex) {
                Toast.makeText(appContext, "Failed to stop Foreground Service", Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Get the suggested file extension from the passed URL
         * @param url the URL to download
         * @return the extension of the file of that URL
         */
        public static String getExtensionFromUrl(String url) {
            String fileExtension = url.substring(url.lastIndexOf('/') + 1);
            if (fileExtension.contains("?")) fileExtension = fileExtension.substring(0, fileExtension.indexOf("?"));
            return fileExtension.substring(fileExtension.lastIndexOf("."));

        }


        /**
         * Start the download of the next file
         * @param context the Activity context where the card with all the details of the download will be appended
         */
        public static void startDownload(Activity context) {
            PodcastInformation podcastInformation = shift();
            if (podcastInformation != null) {
                try {
                    if (!ForegroundNotificationHelper.isServiceRunning(appContext)) {
                        Intent serviceIntent = new Intent(appContext, ForegroundService.class);
                        appContext.startService(serviceIntent);
                    }
                } catch (Exception ex) {
                    Toast.makeText(appContext, "Failed to start Foreground Service. Download experience might be unreliable.", Toast.LENGTH_LONG).show();
                }
                ShowItems currentPodcastInformation = podcastInformation.items.get(0);
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File podcastsGeneralDir = new File(downloadDir, "PodcastDownloader");
                if (!podcastsGeneralDir.exists()) podcastsGeneralDir.mkdir();
                File singlePodcastDir = new File (podcastsGeneralDir, nameSanitizer(podcastInformation.title));
                if (!singlePodcastDir.exists()) singlePodcastDir.mkdir();
                File outputFile = new File(singlePodcastDir, nameSanitizer(currentPodcastInformation.title + ".json"));
                try {
                    if (outputFile.exists()) outputFile.delete();
                    outputFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    fos.write(new Gson().toJson(podcastInformation).getBytes());
                    fos.close();
                } catch (IOException e) {
                    Toast.makeText(appContext, appContext.getResources().getString(R.string.failed_json_creation) + " " + currentPodcastInformation.title, Toast.LENGTH_LONG).show();
                }
                String fileExtension = getExtensionFromUrl(currentPodcastInformation.url);
                String subPath = "PodcastDownloader/" + nameSanitizer(podcastInformation.title) + "/" + nameSanitizer(currentPodcastInformation.title + fileExtension);
                long id = new Random().nextLong();
                currentOperations.put(id, new PodcastDownloadInformation(podcastInformation, subPath));
                // Start the download process
                DownloadContent content = new DownloadContent(appContext, new DownloadCallback(context, id));
                content.downloadWebpage(currentPodcastInformation.url, new File(singlePodcastDir, nameSanitizer(currentPodcastInformation.title + fileExtension)), appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE).getString("UserAgent", ""));
                DownloadUIManager.addPodcastConversion(podcastInformation, id);
            }
        }

        /**
         * Enqueue an item for downloading
         * @param podcastInformation the PodcastInformation of that item
         * @param activity the Activity that'll be used to update the "DOM" after the conversion
         */
        public static void enqueueItem(PodcastInformation podcastInformation, Activity activity) {
            downloadPodcastInfoList.add(podcastInformation);
            PodcastProgress.updateMaximum(1);
            if (currentOperations.size() < appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE).getInt("ConcurrentDownloads", 3)) startDownload(activity);
        }
    }
}
