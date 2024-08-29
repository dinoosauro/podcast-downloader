package dinoosauro.podcastdownloader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;
import dinoosauro.podcastdownloader.PodcastClasses.UrlStorage;
import dinoosauro.podcastdownloader.UIHelper.ColorMenuIcons;
import dinoosauro.podcastdownloader.UIHelper.DownloadUIManager;

public class PodcastsItemsDownloader extends AppCompatActivity {
    /**
     * All the useful information about the shown podcast show
     */
    PodcastInformation information;
    /**
     * The list of items to download
     */
    List<ShowItems> downloadItems = new ArrayList<>();

    /**
     * Send the selected items to download and close the activity
     */
    private void callback() {
        for (ShowItems downloadItem : downloadItems) {
            ArrayList<ShowItems> items = new ArrayList<>();
            items.add(downloadItem); // A new PodcastInformation object must be created for each podcast episode. The ShowItems list will include only one item (the current episode)
            PodcastDownloader.DownloadQueue.enqueueItem(new PodcastInformation(information.title, information.image, information.author, items));
        }
        finish();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcasts_items_downloader);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        LinearLayout linearLayout = findViewById(R.id.podcastItemsContainer);
        information = PodcastDownloader.getPodcastInformation();
        findViewById(R.id.downloadItems).setOnClickListener(v -> callback());
        List<CheckBox> checkBoxes = new ArrayList<>();
        List<String> downloadedUrls = UrlStorage.getDownloadedUrl(PodcastsItemsDownloader.this);
        if (information != null) {
            collapsingToolbar.setTitle(information.title);
            findViewById(R.id.toggleCheckbox).setOnClickListener(view -> {
                boolean shouldBeChecked = downloadItems.size() != information.items.size();
                    for (CheckBox check : checkBoxes) check.setChecked(shouldBeChecked);
                Snackbar.make(linearLayout, shouldBeChecked ? R.string.selected_everything : R.string.selected_nothing, Snackbar.LENGTH_LONG).show();
            });
            for (ShowItems item : information.items) {
                LinearLayout container = new LinearLayout(this);
                container.setOrientation(LinearLayout.HORIZONTAL);
                // Create checkbox
                CheckBox checked = new CheckBox(this);
                checked.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) downloadItems.add(item); else downloadItems.remove(item);
                });
                checkBoxes.add(checked);
                // Create title text
                TextView textView = new TextView(this);
                textView.setText(item.title);
                textView.setPadding(10, 0, 0, 8); // Add padding to the bottom so that the last line can be underlined (if the item hasn't been downloaded)
                if (downloadedUrls == null || !downloadedUrls.contains(item.url)) textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                textView.setOnClickListener(view -> { // Show all the podcast metadata
                    ScrollView dialogContainer = new ScrollView(this);
                    LinearLayout layout = new LinearLayout(this);
                    layout.setPadding(ColorMenuIcons.getScalablePixels(15, getApplicationContext()), 0, ColorMenuIcons.getScalablePixels(15, getApplicationContext()), 0);
                    dialogContainer.addView(layout);
                    ArrayList<ShowItems> currentItem = new ArrayList<>();
                    currentItem.add(item);
                    DownloadUIManager.addPodcastConversion(new PodcastInformation(information.title, information.image, information.author, currentItem), -1, layout);
                    new MaterialAlertDialogBuilder(this)
                            .setView(dialogContainer)
                            .show();
                });
                container.addView(checked);
                container.addView(textView);
                container.setPadding(0, 0, 0, ColorMenuIcons.getScalablePixels(15, getApplicationContext()));
                container.setGravity(Gravity.CENTER_VERTICAL);
                linearLayout.addView(container);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.podcast_selection_menu, menu);
        ColorMenuIcons.color(menu.findItem(R.id.action_playlistChecked), this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_playlistChecked) {
            callback();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}