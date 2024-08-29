package dinoosauro.podcastdownloader;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import org.jsoup.Jsoup;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import dinoosauro.podcastdownloader.PodcastClasses.CheckDuplicates;
import dinoosauro.podcastdownloader.PodcastClasses.GetAlbumArt;
import dinoosauro.podcastdownloader.PodcastClasses.GetPodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastDownloadInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.UrlStorage;
import dinoosauro.podcastdownloader.UIHelper.CheckUpdates;
import dinoosauro.podcastdownloader.UIHelper.ColorMenuIcons;
import dinoosauro.podcastdownloader.UIHelper.DownloadUIManager;
import dinoosauro.podcastdownloader.UIHelper.LoadingDialog;
import dinoosauro.podcastdownloader.UIHelper.PodcastProgress;

public class MainActivity extends AppCompatActivity {
    private static AlertDialog downloadDialog = null;

    /**
     * From a DownloadManager's URI, get the real path
     * @param context an Android context
     * @param uri the Uri fetched from the DownloadManager's event
     * @return a String with a valid path
     */
    public String getRealPathFromURI(Context context, Uri uri) {
        if (uri == null) return null;
        String result = null;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                if (idx != -1) {
                    result = cursor.getString(idx);
                }
            }
            cursor.close();
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Setup toolbar: set it as support, and change its text
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getResources().getString(R.string.downloader));
        PodcastDownloader.DownloadQueue.setContext(getApplicationContext()); // Add the global context to the PodcastDownloader class, so that it can be used when downloading podcasts
        DownloadUIManager.setLinearLayout(findViewById(R.id.downloadItemsContainer)); // Set that the default LinearLayout where the metadata information cards will be appended is the one in MainActivity
        PodcastProgress.setProgressBar(findViewById(R.id.progressBar)); // Set what progress bar should be used when an episode is added in the queue (or has finished downloading)
        if (Build.VERSION.SDK_INT <= 28) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
        findViewById(R.id.downloadNewEpisodes).setOnClickListener(view -> { // Show the Dialog where the user can choose how to download the new episodes of their favorite podcasts
            SharedPreferences preferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View layout = inflater.inflate(R.layout.download_multiple_files, null);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));
            layout.setVisibility(View.VISIBLE);
            AtomicBoolean stopAtFirst = new AtomicBoolean(false); // If true, the script will break when a duplicate is found
            ((MaterialSwitch) layout.findViewById(R.id.breakAtFirst)).setOnCheckedChangeListener((buttonView, checked) -> {
                stopAtFirst.set(checked);
            });
            layout.findViewById(R.id.stopFileName).setOnClickListener(v -> { // Check if a file with the same name already exists to get if a file is a duplicate
                new Thread(() -> {
                    CheckDuplicates.UsingFileName(preferences, stopAtFirst.get(), new CheckDuplicates.EnqueueHandler() {
                        @Override
                        public void enqueue(PodcastInformation information) { // Use this to run the enqueueItem function in the main thread
                            runOnUiThread(() -> PodcastDownloader.DownloadQueue.enqueueItem(information));
                        }
                    });
                }).start();
                downloadDialog.dismiss();
            });
            layout.findViewById(R.id.stopUrl).setOnClickListener(v -> { // Check if the URL has already been downloaded to get if a file is a duplicate
                new Thread(() -> {
                    CheckDuplicates.UsingURL(preferences, stopAtFirst.get(), getApplicationContext(), new CheckDuplicates.EnqueueHandler() {
                        @Override
                        public void enqueue(PodcastInformation information) {
                            runOnUiThread(() -> PodcastDownloader.DownloadQueue.enqueueItem(information));
                        }
                    });
                }).start();
                downloadDialog.dismiss();
            });
            downloadDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                    .setView(layout)
                    .show();
        });
        findViewById(R.id.downloadButton).setOnClickListener(view -> {
            // Create a MaterialDialog that blocks user interaction until the RSS feed hasn't been fetched
            MaterialAlertDialogBuilder dialog = LoadingDialog.build(MainActivity.this);
            AlertDialog dialogShown = dialog.show();
            Thread thread = new Thread(() -> {
                Editable urlText = ((TextInputEditText) findViewById(R.id.downloadUrl)).getText(); // Get the URL
                if (urlText == null) return;
                PodcastInformation information = GetPodcastInformation.FromUrl(urlText.toString(), getSharedPreferences(getPackageName(), Context.MODE_PRIVATE), view);
                if (information != null) {
                    PodcastDownloader.setPodcastInformation(information); // We'll store the new items in the PodcastDownloader class.
                    Intent intent = new Intent(MainActivity.this, PodcastsItemsDownloader.class);
                    startActivity(intent);
                }
                runOnUiThread(dialogShown::dismiss);
            });
            thread.start();
        });
        // Register a BroadcastReceiver to handle download completion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED); else registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        // Get if the user has shared an URL with the application, and start the RSS fetching
        for (Map.Entry<Long, PodcastDownloadInformation> item : PodcastDownloader.DownloadQueue.currentOperations.entrySet()) DownloadUIManager.addPodcastConversion(item.getValue(), item.getKey()); // Restore the metadata cards of the currently downloaded items. This might happen if the user changes theme.
        if (PodcastDownloader.DownloadQueue.currentOperations.size() > 0) {
            findViewById(R.id.downloadItemsContainer).setVisibility(View.VISIBLE); // Make the container visible if there are new elements
            ((ProgressBar) findViewById(R.id.progressBar)).setMax(PodcastDownloader.DownloadQueue.getInfoLength());
        }
        CheckUpdates.checkAndDisplay(MainActivity.this); // Look if there are updates available
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        String intentAction = intent.getAction();
        if (intentAction != null && intentAction.equalsIgnoreCase(Intent.ACTION_SEND)) {
            String extraText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (extraText != null) {
                String url = extraText;
                url = url.substring(url.indexOf("http")).replace("\n", "").trim();
                ((TextInputEditText) findViewById(R.id.downloadUrl)).setText(url, TextView.BufferType.EDITABLE);
                findViewById(R.id.downloadButton).performClick();
            }
        }
    }
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != -1) {
                    new Thread(() -> {
                        PodcastDownloadInformation currentPodcastInformation = PodcastDownloader.DownloadQueue.currentOperations.get(downloadId);
                        PodcastDownloader.DownloadQueue.currentOperations.remove(downloadId); // Make sure the next item can be downloaded
                        // And start its download. *THIS MUST BE DONE IN THE MAIN THREAD*: otherwise, the Thread won't be the same which initialized the View, causing in an Exception
                        runOnUiThread(PodcastDownloader.DownloadQueue::startDownload);
                        String realPath = getRealPathFromURI(context, ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).getUriForDownloadedFile(downloadId));
                        if (realPath == null && currentPodcastInformation != null) {
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), currentPodcastInformation.downloadPath);
                            if (file.exists()) realPath = file.getAbsolutePath();
                        }
                        if (realPath != null) {
                            File file = new File(realPath);
                            if (currentPodcastInformation != null) UrlStorage.addDownloadedUrl(getApplicationContext(), currentPodcastInformation.items.get(0).url); // Add the URL in the history, so that further downloads can be avoided
                            if (currentPodcastInformation != null && file.getAbsolutePath().endsWith(".mp3") && getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getBoolean("MP3Metadata", true)) { // Add metadata to MP3 file
                                try {
                                    Mp3File mp3File = new Mp3File(file);
                                    ID3v2 tag = mp3File.getId3v2Tag();
                                    tag.setAlbum(currentPodcastInformation.title);
                                    tag.setAlbumArtist(currentPodcastInformation.author);
                                    try {
                                        tag.setAlbumImage(GetAlbumArt.downloadAlbum(currentPodcastInformation.image, getApplicationContext()), "image/jpeg");
                                    } catch (IOException e) {
                                        runOnUiThread(() -> Snackbar.make(findViewById(R.id.downloadItemsContainer), getResources().getString(R.string.failed_album_art_download) + " " + currentPodcastInformation.title, BaseTransientBottomBar.LENGTH_LONG).show());
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
                                    if (description != null && getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getBoolean("DecodeHTML", true)) { // Parse the HTML string
                                        try {
                                            description = Jsoup.parse(description).wholeText();
                                        } catch (Exception ex) {
                                            Snackbar.make(findViewById(R.id.downloadItemsContainer), R.string.failed_html_parsing, Snackbar.LENGTH_LONG).show();
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
                                    MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, new String[]{"audio/mpeg"}, null); // Scan the new file
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        Snackbar.make(findViewById(R.id.downloadItemsContainer), getResources().getString(R.string.failed_metadata_add) + " " + currentPodcastInformation.items.get(0).title, BaseTransientBottomBar.LENGTH_LONG).show();
                                    });
                                }
                            } else  {
                                MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, new String[]{"audio/*"}, null); // Scan the downloaded file
                            }
                        } else {
                            runOnUiThread(() -> {
                                String fileName = currentPodcastInformation != null ? currentPodcastInformation.items.get(0).title : "Unknown";
                                Snackbar.make(findViewById(R.id.downloadItemsContainer), getResources().getString(R.string.failed_file_fetching) + " " + fileName + " " + getResources().getString(R.string.file_mediaprovider_error), BaseTransientBottomBar.LENGTH_LONG).show();
                            });
                        }
                            ViewGroup[] destinationLayout = DownloadUIManager.operationContainer.get(downloadId);
                            if (destinationLayout != null) {
                                runOnUiThread(() -> { // Start a scaling animation for deleting the element
                                    Animation anim = new ScaleAnimation(
                                            1f, 1f,
                                            1f, 0f,
                                            Animation.RELATIVE_TO_SELF, 0f,
                                            Animation.RELATIVE_TO_SELF, 0f);
                                    anim.setFillAfter(true);
                                    anim.setDuration(500);
                                    for (ViewGroup view : destinationLayout) view.startAnimation(anim);
                                });
                                try {
                                    Thread.sleep(500);
                                    runOnUiThread(() -> {
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
                                runOnUiThread(() -> PodcastProgress.updateValue(1));
                            }

                    }).start();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        ColorMenuIcons.color(menu.findItem(R.id.action_settings), this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}