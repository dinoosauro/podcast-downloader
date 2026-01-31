package dinoosauro.podcastdownloader.ForegroundService;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import dinoosauro.podcastdownloader.ForegroundService.ForegroundService;
import dinoosauro.podcastdownloader.MainDownloader;
import dinoosauro.podcastdownloader.R;

/**
 * Manage things for the Foreground Service
 */
public class ForegroundNotificationHelper {
    public static final String CHANNEL_ID = "DownloadsChannel";

    /**
     * Create a Notification channel for the Foreground Service
     * @param context the Android app Context
     */

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Downloading Information",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * Create a custom notification for the Foreground service
     * @param context the Android app Context
     * @return a Notification
     */

    public static Notification createNotification(Context context) {
        Intent notificationIntent = new Intent(context, MainDownloader.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.monochrome_icon)
                .setContentTitle(context.getResources().getString(R.string.downloading_podcasts))
                .setContentText(context.getResources().getString(R.string.downloading_podcasts_desc))
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .build();
    }

    /**
     * Get if the Foreground Service is enabled or not
     * @param context the Android app Context
     * @return a boolean, true if the Foreground Service is running
     */
    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
