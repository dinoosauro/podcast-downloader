package dinoosauro.podcastdownloader.UIHelper;


import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastDownloader;
import dinoosauro.podcastdownloader.R;

public class DownloadUIManager {
    /**
     * The LinearLayout where the items will be added, if no other layout is provided
     */
    private static LinearLayout mainLinearLayout;

    /**
     * Change the default LinearLayout for metadata information
     * @param layout the LinearLayout
     */
    public static void setLinearLayout(LinearLayout layout) {
        mainLinearLayout = layout;
    }

    /**
     * Create a LinearLayout with an icon and the text
     * @param text the text
     * @param asset the name of the asset in the "drawable" folder
     * @param context the Context used for creating the layout
     * @param underline if the text should be underlined
     * @param updateText The void to call when the EditText is edited. Pass null to make the text non-editable
     * @param contentDescription the hint that'll be displayed in the EditText
     * @return a LinearLayout, with the image and the text added.
     */
    private static LinearLayout createDataInformation(String text, String asset, Context context, boolean underline, UpdateContent updateText, String contentDescription) {
        LinearLayout container = new LinearLayout(context);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(ColorMenuIcons.getScalablePixels(10, context), ColorMenuIcons.getScalablePixels(5, context), ColorMenuIcons.getScalablePixels(10, context), ColorMenuIcons.getScalablePixels(5, context));
        container.setOrientation(LinearLayout.HORIZONTAL);
        // Adding the icon:
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(context.getResources().getIdentifier(asset, "drawable", context.getPackageName()));
        try {
            imageView.setColorFilter(ContextCompat.getColor(context, com.google.android.material.R.color.material_dynamic_primary50), PorterDuff.Mode.SRC_IN);
        } catch (Exception ex) {
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.fallbackAccent), PorterDuff.Mode.SRC_IN);
        }
        imageView.setMaxWidth(ColorMenuIcons.getScalablePixels(24, context));
        imageView.setMaxHeight(ColorMenuIcons.getScalablePixels(24, context));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ColorMenuIcons.getScalablePixels(24, context),ColorMenuIcons.getScalablePixels(24, context));
        params.weight = 0f;
        params.rightMargin = 10;
        imageView.setLayoutParams(params);
        container.addView(imageView);
        // Adding the text:
        if (updateText != null) { // Add an EditText, since the field can be edited
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); // Make sure the textbox occupies full width
            TextInputLayout textInputLayout = new TextInputLayout(context);
            textInputLayout.setLayoutParams(params1);
            TextInputEditText inputEditText = new TextInputEditText(context);
            inputEditText.setLayoutParams(params1);
            inputEditText.setHint(contentDescription);
            inputEditText.setText(text);
            inputEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    updateText.updateLogic(s.toString());
                }
            });
            textInputLayout.addView(inputEditText);
            container.addView(textInputLayout);
        } else {
            TextView textView = new TextView(context);
            if (underline) textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
            textView.setText(text);
            container.addView(textView);
        }
        return container;
    }

    /**
     * Create a LinearLayout with an icon and the text
     * @param text the text
     * @param asset the name of the asset in the "drawable" folder
     * @param context the Context used for creating the layout
     * @param updateContent The void to call when the EditText is edited. Pass null to make the text non-editable
     * @param contentDescription the hint that'll be displayed in the EditText
     * @return a LinearLayout, with the image and the text added.
     */
    private static LinearLayout createDataInformation(String text, String asset, Context context, UpdateContent updateContent, String contentDescription) {
        return createDataInformation(text, asset, context, false, updateContent, contentDescription);
    }

    /**
     * A map, that ties the download operation ID to the CardView that contains all the information
     */
    public static Map<Long, ViewGroup[]> operationContainer = new HashMap<>();

    /**
     * Create a new MaterialCardView with all the metadata of the downloaded file
     * @param downloader the metadata information for this operation
     * @param operationId the long that identifies this download
     * @param linearLayout the Layout where the item will be added
     */
    public static void addPodcastConversion(PodcastInformation downloader, long operationId, LinearLayout linearLayout) {
        MaterialCardView cardView = new MaterialCardView(linearLayout.getContext());
        cardView.setPadding(ColorMenuIcons.getScalablePixels(10, linearLayout.getContext()), ColorMenuIcons.getScalablePixels(20, linearLayout.getContext()), ColorMenuIcons.getScalablePixels(10, linearLayout.getContext()), ColorMenuIcons.getScalablePixels(20, linearLayout.getContext()));
        LinearLayout innerLayout = new LinearLayout(cardView.getContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.addView(createDataInformation(downloader.items.get(0).title, "baseline_keyboard_voice_24", linearLayout.getContext(), linearLayout == mainLinearLayout ? null : new UpdateContent() {
            @Override
            public void updateLogic(String newText) {
                PodcastInformation temp = operationId == -1 ? downloader : PodcastDownloader.DownloadQueue.currentOperations.get(operationId);
                if (temp != null) temp.items.get(0).title = newText;
            }
        }, innerLayout.getContext().getString(R.string.episode_title)));
        innerLayout.addView(createDataInformation(downloader.title, "baseline_podcasts_24", innerLayout.getContext(), new UpdateContent() {
            @Override
            public void updateLogic(String newText) {
                PodcastInformation temp = operationId == -1 ? downloader : PodcastDownloader.DownloadQueue.currentOperations.get(operationId);
                if (temp != null) temp.title = newText;
            }
        }, innerLayout.getContext().getString(R.string.show_title)));
        innerLayout.addView(createDataInformation(downloader.items.get(0).author, "baseline_person_24", linearLayout.getContext(), new UpdateContent() {
            @Override
            public void updateLogic(String newText) {
                PodcastInformation temp = operationId == -1 ? downloader : PodcastDownloader.DownloadQueue.currentOperations.get(operationId);
                if (temp != null) temp.items.get(0).author = newText;
            }
        }, innerLayout.getContext().getString(R.string.podcast_author)));
        innerLayout.addView(createDataInformation(downloader.items.get(0).description, "baseline_short_text_24", linearLayout.getContext(), new UpdateContent() {
            @Override
            public void updateLogic(String newText)  {
                PodcastInformation temp = operationId == -1 ? downloader : PodcastDownloader.DownloadQueue.currentOperations.get(operationId);
                if (temp != null) temp.items.get(0).description = newText;
            }
        }, innerLayout.getContext().getString(R.string.podcast_description)));
        innerLayout.addView(createDataInformation(downloader.items.get(0).episodeNumber, "baseline_numbers_24", linearLayout.getContext(), new UpdateContent() {
            @Override
            public void updateLogic(String newText) {
                PodcastInformation temp = operationId == -1 ? downloader : PodcastDownloader.DownloadQueue.currentOperations.get(operationId);
                if (temp != null) temp.items.get(0).episodeNumber = newText;
            }
        }, innerLayout.getContext().getString(R.string.episode_number)));
        innerLayout.addView(createDataInformation(downloader.items.get(0).publishedDate, "baseline_calendar_month_24", linearLayout.getContext(), new UpdateContent() {
            @Override
            public void updateLogic(String newText) {
                PodcastInformation temp = operationId == -1 ? downloader : PodcastDownloader.DownloadQueue.currentOperations.get(operationId);
                if (temp != null) temp.items.get(0).publishedDate = newText;
            }
        }, innerLayout.getContext().getString(R.string.published_date)));
        LinearLayout linkView = createDataInformation(downloader.items.get(0).url, "baseline_link_24", linearLayout.getContext(), true, null, "URL");
        linkView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloader.items.get(0).url));
            linearLayout.getContext().startActivity(intent);
        });
        innerLayout.addView(linkView);
        cardView.addView(innerLayout);
        linearLayout.addView(cardView);
        LinearLayout marginLayout = new LinearLayout(cardView.getContext());
        marginLayout.setPadding(0, 0, 0, ColorMenuIcons.getScalablePixels(15, linearLayout.getContext()));
        linearLayout.addView(marginLayout);
        if (operationId != -1) operationContainer.put(operationId, new ViewGroup[]{cardView, marginLayout});
    }
    /**
     * Create a new MaterialCardView with all the metadata of the downloaded file
     * @param downloader the metadata information for this operation
     * @param operationId the long that identifies this download
     */
    public static void addPodcastConversion(PodcastInformation downloader, long operationId) {
        mainLinearLayout.setVisibility(View.VISIBLE); // On the first conversion, the LinearLayout visibility will be set to GONE
        addPodcastConversion(downloader, operationId, mainLinearLayout);
    }
}
