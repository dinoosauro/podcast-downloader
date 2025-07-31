package dinoosauro.podcastdownloader;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import dinoosauro.podcastdownloader.PodcastClasses.CheckDuplicates;
import dinoosauro.podcastdownloader.PodcastClasses.GetPodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastDownloadInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;
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
        CheckUpdates.migrate(getApplicationContext());
        findViewById(R.id.downloadNewEpisodes).setOnClickListener(view -> { // Show the Dialog where the user can choose how to download the new episodes of their favorite podcasts
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
                    CheckDuplicates.UsingFileName(getApplicationContext(), stopAtFirst.get(), new CheckDuplicates.EnqueueHandler() {
                        @Override
                        public void enqueue(PodcastInformation information) { // Use this to run the enqueueItem function in the main thread
                            runOnUiThread(() -> PodcastDownloader.DownloadQueue.enqueueItem(information, MainActivity.this));
                        }
                    });
                }).start();
                downloadDialog.dismiss();
            });
            layout.findViewById(R.id.stopUrl).setOnClickListener(v -> { // Check if the URL has already been downloaded to get if a file is a duplicate
                new Thread(() -> {
                    CheckDuplicates.UsingURL(stopAtFirst.get(), getApplicationContext(), new CheckDuplicates.EnqueueHandler() {
                        @Override
                        public void enqueue(PodcastInformation information) {
                            runOnUiThread(() -> PodcastDownloader.DownloadQueue.enqueueItem(information, MainActivity.this));
                        }
                    });
                }).start();
                downloadDialog.dismiss();
            });
            downloadDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                    .setView(layout)
                    .show();
        });
        // The ActivityResult used ot get which elements should be downloaded
        ActivityResultLauncher<Intent> getFetchedRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    ArrayList<Integer> jsonExtra = data.getIntegerArrayListExtra("chosenPodcasts"); // The items to download are stored in an Integer array, where each integer is the position in the PodcastInformation.items ArrayList
                    if (jsonExtra != null) {
                        List<PodcastInformation> info = new ArrayList<>();
                        PodcastInformation information = PodcastDownloader.getPodcastInformation();
                        PodcastDownloader.clearPodcastInformation();
                        for (int i: jsonExtra) {
                            ArrayList<ShowItems> items = new ArrayList<>();
                            items.add(information.items.get(i)); // A new PodcastInformation object must be created for each podcast episode. The ShowItems list will include only one item (the current episode)
                            info.add(new PodcastInformation(information.title, information.image, information.author, items));
                        }
                        runOnUiThread(() -> { // Now let's add to the queue the podcast entries
                            for (PodcastInformation entry : info) {
                                PodcastDownloader.DownloadQueue.enqueueItem(entry, MainActivity.this);
                            }
                        });
                    }
                }
            }
        });
        findViewById(R.id.downloadButton).setOnClickListener(view -> {
            // Create a MaterialDialog that blocks user interaction until the RSS feed hasn't been fetched
            MaterialAlertDialogBuilder dialog = LoadingDialog.build(MainActivity.this);
            AlertDialog dialogShown = dialog.show();
            Thread thread = new Thread(() -> {
                Editable urlText = ((TextInputEditText) findViewById(R.id.downloadUrl)).getText(); // Get the URL
                if (urlText == null) return;
                PodcastInformation information = GetPodcastInformation.FromUrl(urlText.toString(), getApplicationContext(), view);
                if (information != null) {
                    PodcastDownloader.setPodcastInformation(information); // We'll store the new items in the PodcastDownloader class, since there's a risk that passing them as a extra intent string causes a TransactionTooLargeException
                    Intent intent = new Intent(MainActivity.this, PodcastsItemsDownloader.class);
                    getFetchedRequest.launch(intent);
                }
                runOnUiThread(dialogShown::dismiss);
            });
            thread.start();
        });
        // Get if the user has shared an URL with the application, and start the RSS fetching
        for (Map.Entry<Long, PodcastDownloadInformation> item : PodcastDownloader.DownloadQueue.currentOperations.entrySet()) DownloadUIManager.addPodcastConversion(item.getValue(), item.getKey()); // Restore the metadata cards of the currently downloaded items. This might happen if the user changes theme.
        if (!PodcastDownloader.DownloadQueue.currentOperations.isEmpty()) {
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