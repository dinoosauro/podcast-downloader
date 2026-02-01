package dinoosauro.podcastdownloader.PodcastClasses;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dinoosauro.podcastdownloader.R;

/**
 * Download a new file. Note that you must initialize a DownloadContent class each time you want to download a file, since you can't reuse the `downloadWebpage`method.
 */
public class DownloadContent {
    /**
     * The Channel ID used for the download progress notification
     */
    private static final String CHANNEL_ID = "DownloaderNotification";
    /**
     * The Context used to build the notification
     */
    private Context context;
    /**
     * The NotificationManager that'll update the notification
     */
    private NotificationManagerCompat notificationManagerCompat;
    /**
     * The DownloadCallback class that'll be called after the file has been downloaded
     */
    private DownloadCallback callback;
    /**
     * The text that should be displayed in the notification
     */
    private String notificationText = "";
    /**
     * A number, between 0 and 100, that indicates the progress of the download in percentage.
     */
    private int progress = 0;

    /**
     * Initialize the class
     * @param context the Context used to create the download notification
     * @param callback a DownloadCallback that'll be called after the download
     */

    public DownloadContent(Context context, DownloadCallback callback) {
        this.context = context;
        this.notificationManagerCompat = NotificationManagerCompat.from(context);
        this.callback = callback;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Download progress", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows the download progress of the single podcast");
            channel.setSound(null, null);
            channel.enableVibration(false);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Download a webpage (in this case, audio files) and save it in a File
     * @param url the webpage to download
     * @param file the File where the webpage will be saved
     * @param userAgent the User Agent header used to send the request to the server
     * @param outputFile the DocumentFile of the output file in the external storage. After the file has been downloaded, and metadata have been added, the output file will be copied in this DocumentFile (and the source file will be deleted)
     */
    public void downloadWebpage(String url, final File file, String userAgent, DocumentFile outputFile) {

        int notificationId = new Random().nextInt();
        new Thread(() -> {
            String fileName = outputFile.getName();
            File output = file;
            String path = output.getAbsolutePath();
            String filePath = path.substring(0, path.lastIndexOf(".")); // Path of the file, without the extension
            String extension = path.substring(path.lastIndexOf(("."))); // The extension, with the "." before
            try { // Connect to the URL
                URL parsedUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) parsedUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(60000);
                connection.setInstanceFollowRedirects(true);
                if (!userAgent.trim().isEmpty()) connection.setRequestProperty("User-Agent", userAgent); // Set custom user agent
                int fileSize = connection.getContentLength();
                boolean fileCreated = output.createNewFile();
                int i = 0;
                while (!fileCreated) { // Make sure the file is created. Usually this fails if the file already exists, so we'll try to add a number between brackets to create a dupliacte one.
                    i++;
                    output = new File(String.format("%s (%s)%s", filePath, i, extension));
                    fileCreated = output.createNewFile();
                }
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(output);
                     BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                     BufferedOutputStream bufferedOutput = new BufferedOutputStream(outputStream)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    int totalBytesRead = 0;
                    progress = fileSize;
                    String translatedDownloaded = context.getString(R.string.downloaded);
                    ScheduledExecutorService service = null;
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).getBoolean("SendNotificationWhileDownloading", true)) {
                        // Add an interval that updates the download notification with its progress at each second
                    service = Executors.newScheduledThreadPool(1);
                    service.scheduleWithFixedDelay(() -> {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setContentTitle(fileName)
                                .setPriority(NotificationCompat.PRIORITY_LOW)
                                .setStyle(new NotificationCompat.BigTextStyle().setSummaryText(notificationText))
                                .setOngoing(true)
                                .setSilent(true);
                        if (progress >= 0) {
                            builder.setProgress(100, progress, false);
                        } else { // If -1, we don't know the file size, so it must be indeterminate
                            builder.setProgress(0, 0, true);
                        }

                        notificationManagerCompat.notify(notificationId, builder.build());
                        }, 0, 1, TimeUnit.SECONDS);
                    }
                    while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                        bufferedOutput.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (fileSize > 0) { // Update the notification values (progress and description)
                            progress = (int) ((totalBytesRead * 100L) / fileSize);
                            notificationText = String.format("%s %s (%d%%)", translatedDownloaded, formatBytes(totalBytesRead), progress);
                        } else {
                            notificationText = String.format("%s %s", translatedDownloaded, formatBytes(totalBytesRead));
                        }
                    }
                    if (service != null) service.shutdown(); // Stop updating the notification
                }
                connection.disconnect();
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).getBoolean("SendNotificationWhileDownloading", true)) { // Update the notification. The description text will tell the user that the item is being post-processed.
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_menu_save)
                            .setContentTitle(fileName)
                            .setContentText(context.getString(R.string.postProcessingNotification))
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setAutoCancel(true)
                            .setSilent(true);
                    notificationManagerCompat.notify(notificationId, builder.build());
                    callback.RunCallback(output, notificationId, notificationManagerCompat, true, context.getContentResolver().openOutputStream(outputFile.getUri())); // Run the post-processing script
                }
            } catch (IOException e) { // Show the error notification
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).getBoolean("SendNotificationWhileDownloading", true)) {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            .setContentTitle(fileName)
                            .setContentText(context.getString(R.string.downloadFailed))
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setAutoCancel(true)
                            .setSilent(true);
                    notificationManagerCompat.notify(notificationId, builder.build());
                    try {
                        callback.RunCallback(output, notificationId, notificationManagerCompat, false, context.getContentResolver().openOutputStream(outputFile.getUri())); // Run the post-processing script
                    } catch (FileNotFoundException ignored) {

                    }
                }
            }
        }).start();
    }
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

}
