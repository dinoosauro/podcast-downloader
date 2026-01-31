package dinoosauro.podcastdownloader.ui.search;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import dinoosauro.podcastdownloader.MainDownloader;
import dinoosauro.podcastdownloader.PodcastClasses.iTunesClass;
import dinoosauro.podcastdownloader.PodcastClasses.iTunesResponse;
import dinoosauro.podcastdownloader.R;
import dinoosauro.podcastdownloader.UIHelper.ColorMenuIcons;
import dinoosauro.podcastdownloader.databinding.FragmentSearchBinding;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SearchViewModel searchViewModel =
                new ViewModelProvider(this).get(SearchViewModel.class);

        binding = FragmentSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        CollapsingToolbarLayout collapsingToolbar = root.findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getResources().getString(R.string.search));
        /**
         * The LinearLayout that'll contain all the search results
         */
        LinearLayout resultsContainer = root.findViewById(R.id.resultsContainer);
        FloatingActionButton searchBtn = root.findViewById(R.id.searchBtn);
        EditText searchContent = root.findViewById(R.id.searchQuery);
        searchContent.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) { // User has clicked the new line button. Let's search.
                    searchBtn.performClick();
                    try { // Hide the keyboard
                        searchContent.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(searchContent.getWindowToken(), 0);
                    } catch (Exception ignored) {

                    }
                    return true;
                }
                return false;
            }
        });
        searchBtn.setOnClickListener(view -> {
            root.findViewById(R.id.searchingItemsStatus).setVisibility(View.VISIBLE); // Show that the application is doing something
            new Thread(() -> {
                try {
                    // Call the iTunes API
                    URL url = new URL("https://itunes.apple.com/search?media=podcast&term=" + URLEncoder.encode(searchContent.getText().toString()));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(1000);
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        getActivity().runOnUiThread(() -> {
                            Snackbar.make(root, getString(R.string.search_error), Snackbar.LENGTH_LONG).show();
                        });
                    } else {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            StringBuilder json = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) json.append(line);
                            Gson gson = new Gson();
                            iTunesResponse response = gson.fromJson(json.toString(), iTunesResponse.class);
                            getActivity().runOnUiThread(resultsContainer::removeAllViews);
                            for (iTunesClass entry : response.results) {
                                MaterialCardView layout = null;
                                String imagePath = entry.artworkUrl600 != null ? entry.artworkUrl600 : entry.artworkUrl100 != null ? entry.artworkUrl100 : entry.artworkUrl60 != null ? entry.artworkUrl60 : entry.artworkUrl30 != null ? entry.artworkUrl30 : null;
                                if (imagePath != null) { // Try downloading the podcast album art so that it can be displayed alongside the information.
                                    URL imageUrl = new URL(imagePath);
                                    HttpURLConnection imageConnection = (HttpURLConnection) imageUrl.openConnection();
                                    imageConnection.connect();
                                    try (InputStream imageInput = imageConnection.getInputStream()) {
                                        Bitmap imageOutput = BitmapFactory.decodeStream(imageInput);
                                        layout = addSearchItem(entry.collectionName, entry.artistName, entry.primaryGenreName, imageOutput);
                                    } catch (Exception ignored) {
                                        layout = addSearchItem(entry.collectionName, entry.artistName, entry.primaryGenreName, null);
                                    }
                                } else layout = addSearchItem(entry.collectionName, entry.artistName, entry.primaryGenreName, null);
                                layout.setOnClickListener(view2 -> {
                                    ((MainDownloader) (getActivity())).newDownloadFromUrl(entry.feedUrl);
                                });
                                MaterialCardView finalLayout = layout;
                                getActivity().runOnUiThread(() -> {
                                    resultsContainer.addView(finalLayout);
                                    LinearLayout marginLayout = new LinearLayout(getContext());
                                    marginLayout.setPadding(0, 0, 0, ColorMenuIcons.getScalablePixels(15, getContext()));
                                    resultsContainer.addView(marginLayout);
                                });
                            }
                        }
                    }
                } catch (IOException e) {
                    getActivity().runOnUiThread(() -> {
                        Snackbar.make(root, getString(R.string.search_error), Snackbar.LENGTH_LONG).show();
                    });
                }
                getActivity().runOnUiThread(() -> root.findViewById(R.id.searchingItemsStatus).setVisibility(View.GONE));
            }).start();
        });
        return root;
    }
    /**
     * Create the MaterialCardView that'll contain information about the fetched podcast
     * @param showName the name of the show
     * @param author the author name
     * @param genre the suggested genre 
     * @param outputImage the Bitmap of the podcast album art. If it couldn't be fetched, put `null`.
     * @return
     */
    private MaterialCardView addSearchItem(String showName, String author, String genre, Bitmap outputImage) {
        Context context = getContext();
        MaterialCardView cardView = new MaterialCardView(context);
        cardView.setPadding(ColorMenuIcons.getScalablePixels(10, context), ColorMenuIcons.getScalablePixels(5, context), ColorMenuIcons.getScalablePixels(10, context), ColorMenuIcons.getScalablePixels(5, context));
        // Layout inside the card
        LinearLayout layout = new LinearLayout(context);
        // Layout with the information, at the right
        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        imgParams.gravity = Gravity.CENTER_VERTICAL;
        params.gravity = Gravity.CENTER;
        infoLayout.setLayoutParams(params);
        TextView showText = new TextView(context);
        showText.setText(showName);
        showText.setPadding(0, 0, ColorMenuIcons.getScalablePixels(20, context), 0);
        TextView authorText = new TextView(context);
        authorText.setText(String.format("%s – %s", author, genre));
        infoLayout.addView(showText);
        infoLayout.addView(authorText);
        if (outputImage != null) { // Create the image that'll be displayed at the left of the layout
            ImageView imageView = new ImageView(context);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            imageView.setAdjustViewBounds(true);
            imageView.setLayoutParams(imgParams);
            imageView.setMaxWidth(displayMetrics.widthPixels * 30 / 100);
            imageView.setMinimumWidth(displayMetrics.widthPixels * 30 / 100);
            imageView.setMaxHeight(displayMetrics.heightPixels * 30 / 100);
            imageView.setMinimumHeight(displayMetrics.heightPixels * 30 / 100);
            imageView.setImageBitmap(outputImage);
            imageView.setPadding(ColorMenuIcons.getScalablePixels(20, context), 0, 0, 0);
            layout.addView(imageView);
            infoLayout.setPadding(ColorMenuIcons.getScalablePixels(20, context), 0, ColorMenuIcons.getScalablePixels(20, context), 0);
        }
        layout.addView(infoLayout);
        cardView.addView(layout);
        return cardView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}