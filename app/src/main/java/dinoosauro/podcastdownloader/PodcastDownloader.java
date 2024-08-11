package dinoosauro.podcastdownloader;

import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
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

import dinoosauro.podcastdownloader.ForegroundService.ForegroundService;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastDownloadInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;
import dinoosauro.podcastdownloader.UIHelper.DownloadUIManager;
import dinoosauro.podcastdownloader.ForegroundService.ForegroundNotificationHelper;

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
         * Sanitize the name so that it's valid on every OS
         * @param str the string to sanitize
         * @return the sanitized string
         */
        private static String nameSanitizer(String str) {
            return str.replace("<", "‹").replace(">", "›").replace(":", "∶").replace("\"", "″").replace("/", "∕").replace("\\", "∖").replace("|", "¦").replace("?", "¿").replace("*", "");
        }

        /**
         * If no elements are in the queue, the Foreground service will be disabled.
         */
        public static void disableServiceIfNecessary() {
            if (currentOperations.size() == 0 && downloadPodcastInfoList.size() == 0 && ForegroundNotificationHelper.isServiceRunning(appContext)) {
                Intent stopIntent = new Intent(appContext, ForegroundService.class);
                stopIntent.setAction("STOP_FOREGROUND_SERVICE");
                appContext.startService(stopIntent);
            }
        }


        /**
         * Start the download of the next file
         */
        public static void startDownload() {
            PodcastInformation podcastInformation = shift();
            if (podcastInformation != null) {
                if (!ForegroundNotificationHelper.isServiceRunning(appContext)) {
                    Intent serviceIntent = new Intent(appContext, ForegroundService.class);
                    appContext.startService(serviceIntent);
                }
                ShowItems currentPodcastInformation = podcastInformation.items.get(0);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(currentPodcastInformation.url));
                request.setDescription(currentPodcastInformation.description);
                request.setTitle(currentPodcastInformation.title);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                String fileExtension = currentPodcastInformation.url.substring(currentPodcastInformation.url.lastIndexOf('/') + 1);
                if (fileExtension.contains("?")) fileExtension = fileExtension.substring(0, fileExtension.indexOf("?"));
                fileExtension = fileExtension.substring(fileExtension.lastIndexOf("."));
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File podcastsGeneralDir = new File(downloadDir, "PodcastDownloader");
                if (!podcastsGeneralDir.exists()) podcastsGeneralDir.mkdir();
                File singlePodcastDir = new File (podcastsGeneralDir, nameSanitizer(podcastInformation.title));
                if (!podcastsGeneralDir.exists()) singlePodcastDir.mkdir();
                File outputFile = new File(singlePodcastDir, nameSanitizer(currentPodcastInformation.title + ".json"));
                try {
                    if (outputFile.exists()) outputFile.delete();
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    fos.write(new Gson().toJson(podcastInformation).getBytes());
                    fos.close();
                } catch (IOException e) {
                    Toast.makeText(appContext, appContext.getResources().getString(R.string.failed_json_creation) + " " + currentPodcastInformation.title, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                String subPath = "PodcastDownloader/" + nameSanitizer(podcastInformation.title) + "/" + nameSanitizer(currentPodcastInformation.title + fileExtension);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, subPath);
                DownloadManager manager = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
                long id = manager.enqueue(request);
                currentOperations.put(id, new PodcastDownloadInformation(podcastInformation, subPath));
                DownloadUIManager.addPodcastConversion(podcastInformation, id);
            }
        }

        /**
         * Enqueue an item for downloading
         * @param podcastInformation the PodcastInformation of that item
         */
        public static void enqueueItem(PodcastInformation podcastInformation) {
            downloadPodcastInfoList.add(podcastInformation);
            if (currentOperations.size() < appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE).getInt("ConcurrentDownloads", 3)) startDownload();
        }
    }
}
