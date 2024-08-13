package dinoosauro.podcastdownloader.UIHelper;

import android.os.Build;
import android.widget.ProgressBar;

/**
 * Update the Progress Bar for the download progress
 */
public class PodcastProgress {
    private static ProgressBar progressBar;

    /**
     * Set the ProgressBar that will track the downloaded podcasts
     * @param progress the ProgressBar
     */
    public static void setProgressBar(ProgressBar progress) {
        progressBar = progress;
    }

    /**
     * Update the maximum of the ProgressBar
     * @param addItems the number to add to the current maximum
     */
    public static void updateMaximum(int addItems) {
        progressBar.setMax(progressBar.getMax() + addItems);
    }

    /**
     * Update the value of the ProgressBar
     * @param addValue the value to add to the current value
     */
    public static void updateValue(int addValue) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) progressBar.setProgress(progressBar.getProgress() + addValue, true); else progressBar.setProgress(progressBar.getProgress() + addValue);
    }
}
