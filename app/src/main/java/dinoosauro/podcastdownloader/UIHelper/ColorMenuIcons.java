package dinoosauro.podcastdownloader.UIHelper;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.MenuItem;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import dinoosauro.podcastdownloader.R;

public class ColorMenuIcons {
    /**
     * Change the color of the icons, so that they follow Monet
     * @param item the MenuItem that contains the icon
     * @param context the Context that'll be used for getting the color
     */
    public static void color(MenuItem item, Context context) {
        Drawable icon = item.getIcon();
        if (icon != null) {
            icon = DrawableCompat.wrap(icon);
            try {
                DrawableCompat.setTint(icon, ContextCompat.getColor(context, com.google.android.material.R.color.material_dynamic_primary50));
            } catch (Exception ex) {
                DrawableCompat.setTint(icon, ContextCompat.getColor(context, R.color.fallbackAccent));
            }
            item.setIcon(icon);
        }
    }

    /**
     * Get the "sp" (scalable pixels) from a numebr
     * @param dimension the number before "sp"
     * @param context An Android context
     * @return an Integer, with the number of pixels that would correspond to `dimension`px
     */
    public static int getScalablePixels(float dimension, Context context) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                dimension,
                context.getResources().getDisplayMetrics()
        );
    }
}
