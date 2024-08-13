package dinoosauro.podcastdownloader;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dinoosauro.podcastdownloader.databinding.PodcastTrackSettingsBinding;

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
            put((View) findViewById(R.id.saveXmlFile), new updateFields("WriteOutputXML", SettingsSave.SAVE_AS_BOOLEAN, "1"));
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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                        view.getContext().startActivity(intent);
                    }
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
    }
}