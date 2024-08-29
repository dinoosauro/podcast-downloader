package dinoosauro.podcastdownloader.UIHelper;

import android.content.Context;
import android.widget.ProgressBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dinoosauro.podcastdownloader.R;

/**
 * Create a Dialog that tells the user a request is being done
 */
public class LoadingDialog {
    /**
     * Get the AlertDialogBuilder of the waiting dialog
     * @param context the Context used for creating this dialog
     * @return the AlertDialogBuilder of this dialog
     */
    public static MaterialAlertDialogBuilder build(Context context) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.podcast_info);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(ColorMenuIcons.getScalablePixels(15, context), 0, ColorMenuIcons.getScalablePixels(15, context), 0);
        dialog.setView(progressBar);
        dialog.setMessage(R.string.podcast_info_desc);
        dialog.setCancelable(false);
        return dialog;
    }
}
