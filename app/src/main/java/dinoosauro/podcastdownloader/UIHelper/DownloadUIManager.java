package dinoosauro.podcastdownloader.UIHelper;


import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;

import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.R;

public class DownloadUIManager {
    /**
     * The LinearLayout where the items will be added, if no other layout is provided
     */
    private static LinearLayout linearLayout;

    /**
     * Change the default LinearLayout for metadata information
     * @param layout the LinearLayout
     */
    public static void setLinearLayout(LinearLayout layout) {
        linearLayout = layout;
    }

    /**
     * Create a LinearLayout with an icon and the text
     * @param text the text
     * @param asset the name of the asset in the "drawable" folder
     * @param context the Context used for creating the layout
     * @param underline if the text should be underlined
     * @return a LinearLayout, with the image and the text added.
     */
    private static LinearLayout createDataInformation(String text, String asset, Context context, boolean underline) {
        LinearLayout container = new LinearLayout(context);
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
        // Adding the text:
        TextView textView = new TextView(context);
        if (underline) textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        textView.setText(text);
        container.addView(imageView);
        container.addView(textView);
        return container;
    }

    /**
     * Create a LinearLayout with an icon and the text
     * @param text the text
     * @param asset the name of the asset in the "drawable" folder
     * @param context the Context used for creating the layout
     * @return a LinearLayout, with the image and the text added.
     */
    private static LinearLayout createDataInformation(String text, String asset, Context context) {
        return createDataInformation(text, asset, context, false);
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
        innerLayout.addView(createDataInformation(downloader.items.get(0).title, "baseline_keyboard_voice_24", linearLayout.getContext()));
        innerLayout.addView(createDataInformation(downloader.title, "baseline_podcasts_24", innerLayout.getContext()));
        innerLayout.addView(createDataInformation(downloader.items.get(0).author, "baseline_person_24", linearLayout.getContext()));
        innerLayout.addView(createDataInformation(downloader.items.get(0).description, "baseline_short_text_24", linearLayout.getContext()));
        innerLayout.addView(createDataInformation(downloader.items.get(0).episodeNumber + " [" + downloader.items.get(0).publishedDate + "]", "baseline_numbers_24", linearLayout.getContext()));
        LinearLayout linkView = createDataInformation(downloader.items.get(0).url, "baseline_link_24", linearLayout.getContext(), true);
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
        addPodcastConversion(downloader, operationId, linearLayout);
    }
}
