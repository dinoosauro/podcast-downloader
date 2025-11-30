package dinoosauro.podcastdownloader;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.jsoup.internal.StringUtil;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import dinoosauro.podcastdownloader.PodcastClasses.GetPodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.UrlStorage;
import dinoosauro.podcastdownloader.UIHelper.LoadingDialog;
import dinoosauro.podcastdownloader.UIHelper.PickPodcastFolder;

public class Settings extends AppCompatActivity {
    public enum SettingsSave {
        SAVE_AS_INT,
        SAVE_AS_BOOLEAN,
        SAVE_AS_STRING
    }
    private class updateFields {
        public SettingsSave type;
        public String key;
        public String defaultVal;
        public updateFields(String key, SettingsSave type, String defaultVal) {
            this.type = type;
            this.key = key;
            this.defaultVal = defaultVal;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getResources().getString(R.string.settingsTitle));
        Map<View, updateFields> updateValue = new HashMap<View, updateFields>(){{
            put((View) findViewById(R.id.concurrentDownloads), new updateFields("ConcurrentDownloads", SettingsSave.SAVE_AS_INT, "3"));
            put((View) findViewById(R.id.maxAlbumHeight), new updateFields("MaximumAlbumHeight", SettingsSave.SAVE_AS_INT, "800"));
            put((View) findViewById(R.id.maxAlbumWidth), new updateFields("MaximumAlbumWidth", SettingsSave.SAVE_AS_INT, "800"));
            put((View) findViewById(R.id.jpegQuality), new updateFields("JpegQuality", SettingsSave.SAVE_AS_INT, "80"));
            put((View) findViewById(R.id.mp3Metadata), new updateFields("MP3Metadata", SettingsSave.SAVE_AS_BOOLEAN, "1"));
            put((View) findViewById(R.id.htmlParsing), new updateFields("DecodeHTML", SettingsSave.SAVE_AS_BOOLEAN, "1"));
            put((View) findViewById(R.id.saveXmlFile), new updateFields("WriteOutputXML", SettingsSave.SAVE_AS_BOOLEAN, "0"));
            put((View) findViewById(R.id.keepIndentation), new updateFields("KeepIndentation", SettingsSave.SAVE_AS_BOOLEAN, "0"));
            put((View) findViewById(R.id.keepLineBreak), new updateFields("KeepLineBreak", SettingsSave.SAVE_AS_BOOLEAN, "0"));
            put((View) findViewById(R.id.saveRssFeed), new updateFields("SaveRSSFeedUrl", SettingsSave.SAVE_AS_BOOLEAN, "1"));
            put((View) findViewById(R.id.userAgent), new updateFields("UserAgent", SettingsSave.SAVE_AS_STRING, ""));
            put((View) findViewById(R.id.saveInPodcastTitleDirectory), new updateFields("CreateShowSubdirectory", SettingsSave.SAVE_AS_BOOLEAN, "1"));
            put((View) findViewById(R.id.saveJsonFile), new updateFields("CreateJsonFile", SettingsSave.SAVE_AS_BOOLEAN, "1"));
        }};
        SharedPreferences preferences = this.getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        for (Map.Entry<View, updateFields> entry : updateValue.entrySet()) {
            if (entry.getValue().type == SettingsSave.SAVE_AS_BOOLEAN) {
                MaterialSwitch materialSwitch = (MaterialSwitch) entry.getKey();
                materialSwitch.setChecked(preferences.getBoolean(entry.getValue().key, Objects.equals(entry.getValue().defaultVal, "1")));
                materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    preferences.edit().putBoolean(entry.getValue().key, isChecked).apply();
                });
            } else {
                TextInputEditText editText = (TextInputEditText) entry.getKey();
                editText.setText(entry.getValue().type == SettingsSave.SAVE_AS_INT ? String.valueOf(preferences.getInt(entry.getValue().key, Integer.parseInt(entry.getValue().defaultVal))) : preferences.getString(entry.getValue().key, entry.getValue().defaultVal));
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (s != null) {
                                if (entry.getValue().type == SettingsSave.SAVE_AS_INT && !s.toString().equals("")) preferences.edit().putInt(entry.getValue().key, Integer.parseInt(s.toString())).apply(); else if (entry.getValue().type == SettingsSave.SAVE_AS_STRING) preferences.edit().putString(entry.getValue().key, s.toString()).apply();
                            }
                        }
                    });
            }
        }
        findViewById(R.id.openSource).setOnClickListener(view -> {
            WebView openSourceView = new WebView(this);
            openSourceView.loadUrl("file:///android_res/raw/open_source_licenses.html");
            openSourceView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    view.getContext().startActivity(intent);
                    return true;
                }
            });
            WebSettings webSettings = openSourceView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            new MaterialAlertDialogBuilder(this)
                    .setView(openSourceView)
                    .setTitle(getResources().getString(R.string.open_source_licenses))
                    .show();
        });
        findViewById(R.id.github).setOnClickListener(view -> {
            Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dinoosauro/podcast-downloader"));
            startActivity(open);
        });
        findViewById(R.id.podcastTrackNumber).setOnClickListener(view -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View layout = inflater.inflate(R.layout.podcast_track_settings, null);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            layout.setVisibility(View.VISIBLE);
            MaterialSwitch providedTrack = layout.findViewById(R.id.useProvidedTrack);
            providedTrack.setChecked(preferences.getBoolean("UseSuggestedTrack", true));
            providedTrack.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.edit().putBoolean("UseSuggestedTrack", isChecked).apply());
            MaterialSwitch keepSpecificTrackNumberSettings = layout.findViewById(R.id.keepPodcastSpecificTrack);
            keepSpecificTrackNumberSettings.setChecked(preferences.getBoolean("UsePreviousTrackSettings", true));
            keepSpecificTrackNumberSettings.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.edit().putBoolean("UsePreviousTrackSettings", isChecked).apply());
            RadioGroup trackFallback = layout.findViewById(R.id.customTrackContainer);
            ((RadioButton) trackFallback.getChildAt(preferences.getInt("SuggestedTrackFallback", 0))).setChecked(true);
            trackFallback.setOnCheckedChangeListener((group, checkedId) -> preferences.edit().putInt("SuggestedTrackFallback", trackFallback.indexOfChild(layout.findViewById(checkedId))).apply());
            TextInputEditText startFrom = layout.findViewById(R.id.trackStartFrom);
            startFrom.setText(String.valueOf(preferences.getInt("SuggestedTrackStartFrom", 1)));
            startFrom.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s != null) {
                        try {
                            preferences.edit().putInt("SuggestedTrackStartFrom", Integer.parseInt(s.toString())).apply();
                        } catch (Exception ignored) {

                        }
                    }
                }
            });
            new MaterialAlertDialogBuilder(this)
                    .setView(layout)
                    .show();
        });
        // Create a new chip with the URL by getting all the Podcast sources
        BufferedReader xmlSources = UrlStorage.getDownloadBuffer(getApplicationContext(), true);
        if (xmlSources != null) {
            String sourceUrl;
            try {
                while ((sourceUrl = xmlSources.readLine()) != null) { 
                    String[] source = sourceUrl.split(" ");
                    if (source.length > 3)
                        CreateChip(String.join(" ", Arrays.copyOf(source, source.length - 3)), preferences, findViewById(R.id.sourcesContainer));
                }
            } catch (Exception ignored) {

            }
        }
        findViewById(R.id.addSource).setOnClickListener(v -> { // Add the text in the textbox as a source
            Editable url = ((TextInputEditText) findViewById(R.id.sourceUrl)).getText();
            if (url == null) return;
            UrlStorage.addDownloadedUrl(getApplicationContext(), String.format("%s %s %s %s", url, preferences.getBoolean("UseSuggestedTrack", true) ? 1 : 0, preferences.getInt("SuggestedTrackFallback", 0), preferences.getInt("SuggestedTrackStartFrom", 1)), true, true); // We need to add also the current podcast track number options, since, if files are automatically downloaded, the user can choose to download the new podcasts by keeping the previous track number settings.
            CreateChip(url.toString(), preferences, findViewById(R.id.sourcesContainer));
        });
        findViewById(R.id.deleteUrls).setOnClickListener(view -> UrlStorage.removeUrls(this)); // Remove every URL from history
        /**
         * The ActivityResult called after the user has chosen where the file with the URL history should be written.
         */
        ActivityResultLauncher<Intent> writeFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == RESULT_OK) {
                Intent data = o.getData();
                if (data != null) WriteHistoryFile(getApplicationContext(), data.getData(), findViewById(R.id.exportUrls));
            }
        });
        findViewById(R.id.exportUrls).setOnClickListener(view -> { // Export the history to a file (the location will be asked to the user)
            writeFile.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE, "HistoryLinks.txt"));
        });

        /**
         * The ActivityResult that'll read the URL history file, and import them.
         */
        ActivityResultLauncher<Intent> readFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == RESULT_OK) {
                Intent data = o.getData();
                if (data != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!UrlStorage.checkEntry(getApplicationContext(), false, line)) UrlStorage.addDownloadedUrl(Settings.this, line); // Check that the link isn't already saved
                        }
                    } catch(IOException e) {
                        Log.d("ReadError", e.toString());
                    }
                }
            }
        });

        findViewById(R.id.importUrls).setOnClickListener(view -> {
            readFile.launch(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"));
        });

        // ActivityResult that permits to pick a custom download folder
        ActivityResultLauncher<Intent> getOutputFolder = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> PickPodcastFolder.updateStorage(o, getApplicationContext()));
        findViewById(R.id.changeOutputFolder).setOnClickListener(view -> getOutputFolder.launch(PickPodcastFolder.getIntent()) );

        // Empty cache button
        findViewById(R.id.emptyCache).setOnClickListener(view -> {
            File folder = new File(getFilesDir(), "TempFiles");
            if (folder.exists()) {
                for (File file: folder.listFiles()) {
                    if (file.isFile()) file.delete();
                }
            }
        });
    }


    private static void WriteHistoryFile(Context context, Uri uri, View view) {
        try { // Get the URLS and, if there are some, write them to the selected file
            BufferedReader urls = UrlStorage.getDownloadBuffer(context, false);
            if (urls == null) return;
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            String write;
            while ((write = urls.readLine()) != null) outputStream.write((write + "\n").getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(view, R.string.failed_url_export, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Create a new Chip with the Source URL. If clicked, a dialog with its title is shown
     * @param sourceUrl the URL of the source
     * @param preferences the SharedPreferences to use to delete the chip (if the user clicks on the close button)
     * @param container the container where the Chip should be appended
     */
    private void CreateChip(String sourceUrl, SharedPreferences preferences, ViewGroup container) {
        Chip chip = new Chip(this);
        chip.setText(sourceUrl);
        chip.setCloseIconVisible(true);
        chip.setEllipsize(TextUtils.TruncateAt.START);
        chip.setOnCloseIconClickListener(v -> { // Remove the source from the list
            Set<String> sources = new HashSet<>(preferences.getStringSet("PodcastSources", new HashSet<>()));
            sources.remove(sourceUrl);
            preferences.edit().putStringSet("PodcastSources", sources).apply();
            container.removeView(chip);
        });
        chip.setOnClickListener(v -> { // Create a new Dialog that shows the title name (and URL)
            AlertDialog dialog = LoadingDialog.build(Settings.this).show();
            new Thread(() -> {
                try {
                    Document document = GetPodcastInformation.getDocumentFromUrl(GetPodcastInformation.getCorrectUrl(sourceUrl), preferences.getString("UserAgent", ""));
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        if (document != null) {
                            new MaterialAlertDialogBuilder(Settings.this)
                                    .setTitle(GetPodcastInformation.nullPlaceholder(document.getElementsByTagName("title").item(0), preferences.getBoolean("KeepIndentation", false), preferences.getBoolean("KeepLineBreak", false)))
                                    .setMessage(sourceUrl)
                                    .setPositiveButton(R.string.open_source_rss, (dialog1, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl))))
                                    .setNegativeButton(R.string.delete, (dialog1, which) -> chip.performCloseIconClick())
                                    .show();
                        }
                    });
                } catch (Exception ignored) {
                    dialog.dismiss();
                }
            }).start();
        });
        container.addView(chip);
    }
}