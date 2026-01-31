package dinoosauro.podcastdownloader;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.List;

import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;
import dinoosauro.podcastdownloader.UIHelper.CheckUpdates;
import dinoosauro.podcastdownloader.databinding.ActivityMainDownloaderBinding;
import dinoosauro.podcastdownloader.ui.podcast_download.PodcastDownloadFragment;

public class MainDownloader extends AppCompatActivity {

    private ActivityMainDownloaderBinding binding;

    private NavController navController;
    /**
     * The URL that has been shared from a third-party application and that should be downloaded.
     * The URL is stored in this variable only if the application is in a different section than the "Downloader" one, and is cleared immediately after the RSS fetch process starts.
     * 
     * The only goal of this variable is to store the URL while the application changes the selected tab to the "Downloader" one, since otherwise the download can't be started.
     */

    private String urlToDownload = null;

    /**
     * The code called after the user has selected the podcast episodes to download. It's public since it's also called from the Downloader fragment (and we can't keep it in the Downloader fragment since otherwise it wouldn't always be triggered).
     */

    public static ActivityResultLauncher<Intent> getFetchedRequest = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainDownloaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // The ActivityResult used to get which elements should be downloaded
        getFetchedRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    ArrayList<String> jsonExtra = data.getStringArrayListExtra("chosenPodcasts"); // The items to download are stored in an Integer array, where each integer is the position in the PodcastInformation.items ArrayList
                    if (jsonExtra != null) {
                        List<PodcastInformation> info = new ArrayList<>();
                        // We'll now get both the podcast information array (in case multiple podcasts shows are queued) and the single podcast information object (in case the user is manually downloading a single show)
                        PodcastInformation information = PodcastDownloader.getPodcastInformation();
                        List<PodcastInformation> informationArr = PodcastDownloader.getPodcastInformationArr();
                        PodcastDownloader.clearPodcastInformation();
                        if (information != null) { // User is downloading a single show
                            for (ShowItems i: information.items) {
                                if (!jsonExtra.contains(i.uuid)) continue;
                                ArrayList<ShowItems> items = new ArrayList<>();
                                items.add(i); // A new PodcastInformation object must be created for each podcast episode. The ShowItems list will include only one item (the current episode)
                                info.add(new PodcastInformation(information.title, information.image, information.author, items));
                            }
                        }
                        if (informationArr != null) { // User is downloading multiple podcasts shows
                            for (PodcastInformation i: informationArr) {
                                if (!jsonExtra.contains(i.items.get(0).uuid)) continue; // When downloading multiple podcasts shows, each episode has its own PodcastInformation object, so the length of the ShowItems list will always be 1.
                                info.add(i);
                            }
                        }
                        runOnUiThread(() -> { // Now let's add to the queue the podcast entries
                            for (PodcastInformation entry : info) {
                                PodcastDownloader.DownloadQueue.enqueueItem(entry, this);
                            }
                        });
                    }
                }
            }
        });

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main_downloader);
        NavigationUI.setupWithNavController(binding.navView, navController);
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
        CheckUpdates.migrate(getApplicationContext());
        CheckUpdates.checkAndDisplay(MainDownloader.this); // Look if there are updates available
        // We'll now set up a callback so that we can be informed when the app section has been successfully changed to the "Download" section. We need this so that we can download items if they're shared from another app.
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        super.onFragmentResumed(fm, f);
                        if (f instanceof PodcastDownloadFragment && urlToDownload != null) {
                            ((PodcastDownloadFragment) f).startDownload(urlToDownload);
                            urlToDownload = null;
                        }
                    }
                }, true
        );
        checkIntent(getIntent());
    }

    /**
     * Start the RSS fetching process
     * @param url the URL of the RSS feed.
     */
    public void newDownloadFromUrl(String url) {
        // Change section to the "Downloader" one
        navController.navigate(R.id.navigation_podcasts_download);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main_downloader);
        Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
        if (currentFragment instanceof PodcastDownloadFragment) { // We can start the download
            ((PodcastDownloadFragment) currentFragment).startDownload(url);
        } else urlToDownload = url; // Let's store it in a temp variable. When the section will be successfully changed, the download will be started.
    }
    /**
     * Check if the provided intent contains a link to download
     * @param intent the Intent to check
     */
    private void checkIntent(Intent intent) {
        if (intent != null) {
            String intentAction = intent.getAction();
            if (intentAction != null && intentAction.equalsIgnoreCase(Intent.ACTION_SEND)) {
                String extraText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                if (extraText != null) {
                    String url = extraText;
                    url = url.substring(url.indexOf("http")).replace("\n", "").trim();
                    newDownloadFromUrl(url);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        checkIntent(intent);
    }

}