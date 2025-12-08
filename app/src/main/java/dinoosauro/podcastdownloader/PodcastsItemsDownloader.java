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
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dinoosauro.podcastdownloader.PodcastClasses.PodcastDownloadInformation;
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
    List<PodcastInformation> informationArr = new ArrayList<>();
    /**
     * A list of the items to download. Their position in the `information` array is stored.
     */
    ArrayList<String> downloadItems = new ArrayList<>();
    List<CheckBox> checkBoxes = new ArrayList<>();

    /**
     * Send the selected items to the MainActivity and close the activity
     */
    private void callback() {
        Intent result = new Intent();
        result.putStringArrayListExtra("chosenPodcasts", downloadItems);
        setResult(RESULT_OK, result);
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
        informationArr = PodcastDownloader.getPodcastInformationArr();
        findViewById(R.id.downloadItems).setOnClickListener(v -> callback());
        if (information != null) { // The user needs to choose the episodes from a single podcast, so the standard view will be used.
            collapsingToolbar.setTitle(information.title);
            findViewById(R.id.toggleCheckbox).setOnClickListener(view -> {
                boolean shouldBeChecked = downloadItems.size() != information.items.size();
                    for (CheckBox check : checkBoxes) check.setChecked(shouldBeChecked);
                Snackbar.make(linearLayout, shouldBeChecked ? R.string.selected_everything : R.string.selected_nothing, Snackbar.LENGTH_LONG).show();
            });
            for (int i = 0; i < information.items.size(); i++) {
                ShowItems item = information.items.get(i);
                ArrayList<ShowItems> currentItem = new ArrayList<>();
                currentItem.add(item);
                AddItemToList(new PodcastInformation(information.title, information.image, information.author, currentItem), linearLayout);
            }
        }
        if (informationArr != null) { // The user needs to show which episodes to download between multiple shows, so we'll need to create a new card for each show.
            collapsingToolbar.setTitle("New episodes");
            findViewById(R.id.toggleCheckbox).setOnClickListener(view -> {
                boolean shouldBeChecked = downloadItems.size() != informationArr.size();
                for (CheckBox check : checkBoxes) check.setChecked(shouldBeChecked);
                Snackbar.make(linearLayout, shouldBeChecked ? R.string.selected_everything : R.string.selected_nothing, Snackbar.LENGTH_LONG).show();
            });
            List<MaterialCardView> cardList = new ArrayList<>(); // A list of all the cards that need to be appended at the end
            HashMap<String, LinearLayout> linearContainer = new HashMap<>(); // A map that links the podcast show name with the LinearLayout container of the checkboxes
            for (PodcastInformation info: informationArr) {
                LinearLayout viewToAdd = linearContainer.get(info.title);
                if (viewToAdd == null) { // We need to create the new LinearLayout (and therefore also the new Card and the title label) for the podcast show
                    MaterialCardView cardToAdd = new MaterialCardView(this);
                    cardToAdd.setPadding(ColorMenuIcons.getScalablePixels(10, cardToAdd.getContext()), ColorMenuIcons.getScalablePixels(20, cardToAdd.getContext()), ColorMenuIcons.getScalablePixels(10, cardToAdd.getContext()), ColorMenuIcons.getScalablePixels(20, cardToAdd.getContext()));
                    viewToAdd = new LinearLayout(cardToAdd.getContext());
                    viewToAdd.setOrientation(LinearLayout.VERTICAL);
                    TextView titleName = new TextView(cardToAdd.getContext());
                    titleName.setText(info.title);
                    titleName.setPadding(20, 25, 20, 25);
                    titleName.setTextSize(24);
                    viewToAdd.addView(titleName);
                    linearContainer.put(info.title, viewToAdd);
                    cardToAdd.addView(viewToAdd);
                    cardList.add(cardToAdd);
                }
                AddItemToList(info, viewToAdd);
            }
            // Now we'll append the Card, along with some margin, to the activity
            for (MaterialCardView view : cardList) {
                linearLayout.addView(view);
                LinearLayout marginLayout = new LinearLayout(view.getContext());
                marginLayout.setPadding(0, 0, 0, ColorMenuIcons.getScalablePixels(15, linearLayout.getContext()));
                linearLayout.addView(marginLayout);
            }
        }
    }

    /**
     * Create a new checkbox with the text name
     * @param information the PodcastInformation object that contains the data of the checkbox
     * @param linearLayout the layout where the item will be appended
     */
    private void AddItemToList(PodcastInformation information, LinearLayout linearLayout) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        ShowItems item = information.items.get(0);
        // Create checkbox
        CheckBox checked = new CheckBox(this);
        checked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) downloadItems.add(item.uuid); else downloadItems.remove(item.uuid);
        });
        checkBoxes.add(checked);
        // Create title text
        TextView textView = new TextView(this);
        textView.setText(item.title);
        textView.setPadding(10, 0, 0, 8); // Add padding to the bottom so that the last line can be underlined (if the item hasn't been downloaded)
        if (!UrlStorage.checkEntry(getApplicationContext(), false, item.url)) textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        textView.setOnClickListener(view -> { // Show all the podcast metadata
            ScrollView dialogContainer = new ScrollView(this);
            LinearLayout layout = new LinearLayout(this);
            layout.setPadding(ColorMenuIcons.getScalablePixels(15, getApplicationContext()), 0, ColorMenuIcons.getScalablePixels(15, getApplicationContext()), 0);
            dialogContainer.addView(layout);
            DownloadUIManager.addPodcastConversion(information, -1, layout);
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