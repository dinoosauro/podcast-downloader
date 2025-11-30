package dinoosauro.podcastdownloader.UIHelper;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResult;

/**
 * Functions that permit the user to pick a custom output folder
 */
public class PickPodcastFolder {
    /**
     * Get an Intent that'll ask the user to pick a folder.
     */
    public static Intent getIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return intent;
    }

    /**
     * Update the SharedPreferences by saving the selected folder Uri (if available)
     * @param o the ActivityResult of the Intent
     * @param context a valid Context object
     * @return the Uri path of the selected folder
     */
    public static String updateStorage(ActivityResult o, Context context) {
        if (o.getResultCode() == RESULT_OK) {
            Intent data = o.getData();
            context.getContentResolver().takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            String path = data.getData().toString();
            context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).edit().putString("DownloadFolder", path).apply();
            return path;
        }
        return null;
    }
}
