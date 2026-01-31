package dinoosauro.podcastdownloader;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
     * The multiple podcasts shows that the user might want to download
     */
    private static List<PodcastInformation> currentPodcastInfoArr;

    /**
     * Set the podcast information that will be selected by the user
     * @param info the PodcastInformation to set
     */
    public static void setPodcastInformation(PodcastInformation info) {
        currentPodcastInfo = info;
        currentPodcastInfoArr = null;
    }
    /**
     * Set the multiple PodcastInformation objects that the user might want to download
     * @param info the PodcastInformation list to set
     */
    public static void setPodcastInformation(List<PodcastInformation> info) {
        currentPodcastInfo = null;
        currentPodcastInfoArr = info;
    }

    /**
     * Get the PodcastInformation the user needs to choose
     * @return the PodcastInformation to choose
     */
    public static PodcastInformation getPodcastInformation() {
        return currentPodcastInfo;
    }
    /**
     * Get the PodcastInformation list the user needs to choose
     * @return the PodcastInformation list to choose
     */
    public static List<PodcastInformation> getPodcastInformationArr() {
        return currentPodcastInfoArr;
    }

    /**
     * Delete the PodcastInformation ArrayList
     */
    public static void clearPodcastInformation() {
        currentPodcastInfo = null;
        currentPodcastInfoArr = null;
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
                SharedPreferences preferences = appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE);
                String folderString = preferences.getString("DownloadFolder", null);
                if (folderString == null) {
                    Toast.makeText(context, context.getResources().getString(R.string.pick_dir_prompt), Toast.LENGTH_LONG).show();
                    return;
                }
                Uri folderUri = Uri.parse(folderString);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(context, folderUri);
                if (pickedDir == null || !pickedDir.isDirectory() || !pickedDir.exists() || !pickedDir.canWrite()) {
                    Toast.makeText(context, context.getResources().getString(R.string.pick_dir_prompt), Toast.LENGTH_LONG).show();
                    return;
                }
                if (preferences.getBoolean("CreateShowSubdirectory", true)) {
                    String podcastName = nameSanitizer(podcastInformation.title);
                    // We'll now iterate over all files/directories in the folder to find if a folder with the same name exists
                    boolean foundFolder = false;
                    for (DocumentFile file : pickedDir.listFiles()) {
                        if (file.isDirectory() && file.getName() != null && file.getName().equals(podcastName)) {
                            pickedDir = file;
                            foundFolder = true;
                            break;
                        }
                    }
                    // Otherwise, we'll create the new show folder.
                    if (!foundFolder) pickedDir = pickedDir.createDirectory(nameSanitizer(podcastInformation.title));
                }
                if (preferences.getBoolean("CreateJsonFile", true)) {
                    try {
                        DocumentFile outputFile = pickedDir.createFile("application/json", nameSanitizer(currentPodcastInformation.title));
                        OutputStream fos = appContext.getContentResolver().openOutputStream(outputFile.getUri());
                        fos.write(new Gson().toJson(podcastInformation).getBytes());
                        fos.close();
                    } catch (IOException e) {
                        Toast.makeText(appContext, appContext.getResources().getString(R.string.failed_json_creation) + " " + currentPodcastInformation.title, Toast.LENGTH_LONG).show();
                    }
                }
                String fileExtension = getExtensionFromUrl(currentPodcastInformation.url);
                String subPath = "PodcastDownloader/" + nameSanitizer(podcastInformation.title) + "/" + nameSanitizer(currentPodcastInformation.title + fileExtension);
                long id = new Random().nextLong();
                currentOperations.put(id, new PodcastDownloadInformation(podcastInformation, subPath));
                // Start the download process
                DownloadContent content = new DownloadContent(appContext, new DownloadCallback(context, id));
                // We'll now create a private file where the audio file will be downloaded. We'll later move this file in a public DocumentFile, when everything else has been completed. We unfortunately need to do this since the library used for MP3 files allows editing files only by providing their file path.
                File tempFolder = new File(context.getFilesDir(), "TempFiles");
                if (!tempFolder.exists()) tempFolder.mkdir();
                File file = new File(tempFolder, UUID.randomUUID().toString() + fileExtension);
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.substring(1));
                content.downloadWebpage(currentPodcastInformation.url, file, appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE).getString("UserAgent", ""), pickedDir.createFile(mimeType == null ? "application/octet-stream" : mimeType, nameSanitizer(currentPodcastInformation.title) + (mimeType == null ? fileExtension : "")));
                DownloadUIManager.addPodcastConversion(podcastInformation, id);
            } else if (currentOperations.isEmpty() && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) { // Send a notification to the user that all the podcasts have been downloaded
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("DownloadsResult", "Download result", NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("Notifies the user when the download process has been completed.");
                    channel.enableVibration(true);
                    context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
                }
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "DownloadsResult")
                        .setSmallIcon(R.drawable.baseline_cloud_done_24)
                        .setContentTitle(context.getResources().getString(R.string.files_downloaded))
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
                notificationManagerCompat.notify(1, builder.build());
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
