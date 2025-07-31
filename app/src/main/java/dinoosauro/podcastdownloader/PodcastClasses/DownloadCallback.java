package dinoosauro.podcastdownloader.PodcastClasses;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;

import dinoosauro.podcastdownloader.MainActivity;
import dinoosauro.podcastdownloader.PodcastDownloader;
import dinoosauro.podcastdownloader.R;
import dinoosauro.podcastdownloader.UIHelper.DownloadUIManager;
import dinoosauro.podcastdownloader.UIHelper.PodcastProgress;

/**
 * Create the Class that'll handle the post-processing of the MP3 files
 */
public class DownloadCallback {
    /**
     * The Activity that needs to be updated (ex: remove the card after the download, etc.)
     */
    private final Activity context;
    /**
     * The random long ID for this download operation
     */
    private final long downloadId;

    /**
     * Create the Class that'll handle the post-processing of the MP3 files
     * @param context the Activity that needs to be updated (ex: remove the card after the download, etc.)
     * @param downloadId the random long ID for this download operation
     */
    public DownloadCallback(Activity context, long downloadId) {
        this.context = context;
        this.downloadId = downloadId;
    }

    /**
     * The void to call after the file has been downloaded (or not)
     * @param file the File where the file has been saved
     * @param notificationId the ID used to inform the user of the download
     * @param notificationManagerCompat the NotificationManagerCompat used to create the notification
     * @param isSuccessful if the download was successful or not
     */
    public void RunCallback(File file, int notificationId, NotificationManagerCompat notificationManagerCompat, boolean isSuccessful) {
        Thread thread = new Thread(() -> {
            PodcastDownloadInformation currentPodcastInformation = PodcastDownloader.DownloadQueue.currentOperations.get(downloadId);
            PodcastDownloader.DownloadQueue.currentOperations.remove(downloadId); // Make sure the next item can be downloaded
            // And start its download. *THIS MUST BE DONE IN THE MAIN THREAD*: otherwise, the Thread won't be the same which initialized the View, causing in an Exception
            context.runOnUiThread(() -> PodcastDownloader.DownloadQueue.startDownload(context));
            if (isSuccessful && file.exists()) { // The file exists and the download was completed
                if (currentPodcastInformation != null)
                    UrlStorage.addDownloadedUrl(context.getApplicationContext(), currentPodcastInformation.items.get(0).url); // Add the URL in the history, so that further downloads can be avoided
                if (currentPodcastInformation != null && file.getAbsolutePath().endsWith(".mp3") && context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).getBoolean("MP3Metadata", true)) { // Add metadata to MP3 file
                    try {
                        Mp3File mp3File = new Mp3File(file);
                        ID3v2 tag = mp3File.getId3v2Tag();
                        tag.setAlbum(currentPodcastInformation.title);
                        tag.setAlbumArtist(currentPodcastInformation.author);
                        try {
                            tag.setAlbumImage(GetAlbumArt.downloadAlbum(currentPodcastInformation.image, context), "image/jpeg");
                        } catch (IOException e) {
                            context.runOnUiThread(() -> Snackbar.make(context.findViewById(R.id.downloadItemsContainer), context.getResources().getString(R.string.failed_album_art_download) + " " + currentPodcastInformation.title, BaseTransientBottomBar.LENGTH_LONG).show());
                        }
                        tag.setArtist(currentPodcastInformation.author);
                        tag.setDate(currentPodcastInformation.items.get(0).publishedDate);
                        if (currentPodcastInformation.items.get(0).publishedDate != null) { // Add year
                            String number = currentPodcastInformation.items.get(0).publishedDate.length() > 12 ? currentPodcastInformation.items.get(0).publishedDate.substring(12, currentPodcastInformation.items.get(0).publishedDate.indexOf(' ', 12)).trim() : currentPodcastInformation.items.get(0).publishedDate.trim(); // In the Podcast standard syntax, the year is from the twelfth char. If there aren't enough characters, we'll try to use the entire string as the year.
                            if (number.matches("^\\d+$")) { // Check that the string is composed of numbers
                                tag.setYear(number);
                            }
                        }
                        tag.setTitle(currentPodcastInformation.items.get(0).title);
                        String description = currentPodcastInformation.items.get(0).description;
                        if (description != null && context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).getBoolean("DecodeHTML", true)) { // Parse the HTML string
                            try {
                                description = Jsoup.parse(description).wholeText();
                            } catch (Exception ex) {
                                Snackbar.make(context.findViewById(R.id.downloadItemsContainer), R.string.failed_html_parsing, Snackbar.LENGTH_LONG).show();
                            }
                        }
                        tag.setComment(description);
                        tag.setTrack(currentPodcastInformation.items.get(0).episodeNumber);
                        mp3File.setId3v2Tag(tag);
                        // Create a new file, that it'll contain the new metadata
                        String newFileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + "-Metadata" + Math.random() + ".mp3";
                        File metadataFile = new File(newFileName);
                        if (metadataFile.exists()) metadataFile.delete();
                        mp3File.save(newFileName);
                        file.delete(); // Delete the old file
                        metadataFile.renameTo(file); // And move the metadata file to the location
                        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, new String[]{"audio/mpeg"}, null); // Scan the new file
                    } catch (Exception e) {
                        context.runOnUiThread(() -> {
                            Snackbar.make(context.findViewById(R.id.downloadItemsContainer), context.getResources().getString(R.string.failed_metadata_add) + " " + currentPodcastInformation.items.get(0).title, BaseTransientBottomBar.LENGTH_LONG).show();
                        });
                    }
                } else {
                    MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, new String[]{"audio/*"}, null); // Scan the downloaded file
                }
            }
            ViewGroup[] destinationLayout = DownloadUIManager.operationContainer.get(downloadId);
            if (destinationLayout != null) {
                context.runOnUiThread(() -> { // Start a scaling animation for deleting the element
                    Animation anim = new ScaleAnimation(
                            1f, 1f,
                            1f, 0f,
                            Animation.RELATIVE_TO_SELF, 0f,
                            Animation.RELATIVE_TO_SELF, 0f);
                    anim.setFillAfter(true);
                    anim.setDuration(500);
                    for (ViewGroup view : destinationLayout) view.startAnimation(anim);
                });
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && isSuccessful) { // Delete the notification since the file is ready
                    notificationManagerCompat.cancel(notificationId);
                }
                    try {
                    Thread.sleep(500);
                    context.runOnUiThread(() -> {
                        PodcastDownloader.DownloadQueue.disableServiceIfNecessary(); // Check if the Foreground Service should be disabled (and disable it if necessary)
                        for (ViewGroup view : destinationLayout) {
                            if (view == null) continue;
                            ViewGroup parentElement = ((ViewGroup) view.getParent());
                            if (parentElement != null) parentElement.removeView(view);
                        }
                        PodcastProgress.updateValue(1);
                    });
                } catch (InterruptedException e) {
                    Log.e("Failed removal", String.valueOf(downloadId));
                }
            } else {
                context.runOnUiThread(() -> PodcastProgress.updateValue(1));
            }

        });
        thread.start();
    }
}
