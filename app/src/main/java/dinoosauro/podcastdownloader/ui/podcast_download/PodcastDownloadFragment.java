package dinoosauro.podcastdownloader.ui.podcast_download;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import dinoosauro.podcastdownloader.MainDownloader;
import dinoosauro.podcastdownloader.PodcastClasses.CheckDuplicates;
import dinoosauro.podcastdownloader.PodcastClasses.GetPodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastDownloadInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;
import dinoosauro.podcastdownloader.PodcastDownloader;
import dinoosauro.podcastdownloader.PodcastsItemsDownloader;
import dinoosauro.podcastdownloader.R;
import dinoosauro.podcastdownloader.UIHelper.DownloadUIManager;
import dinoosauro.podcastdownloader.UIHelper.LoadingDialog;
import dinoosauro.podcastdownloader.UIHelper.PickPodcastFolder;
import dinoosauro.podcastdownloader.UIHelper.PodcastProgress;
import dinoosauro.podcastdownloader.databinding.FragmentPodcastDownloadBinding;

public class PodcastDownloadFragment extends Fragment {

    private static AlertDialog downloadDialog = null;
    private FragmentPodcastDownloadBinding binding;
    View root = null;

    public View onCreateView(@NonNull LayoutInflater inflaterL,
                             ViewGroup container, Bundle savedInstanceState) {
        PodcastDownloadViewModel homeViewModel =
                new ViewModelProvider(this).get(PodcastDownloadViewModel.class);

        binding = FragmentPodcastDownloadBinding.inflate(inflaterL, container, false);
        root = binding.getRoot();
        CollapsingToolbarLayout collapsingToolbar = root.findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getResources().getString(R.string.downloader));
        PodcastDownloader.DownloadQueue.setContext(root.getContext().getApplicationContext()); // Add the global context to the PodcastDownloader class, so that it can be used when downloading podcasts
        DownloadUIManager.setLinearLayout(root.findViewById(R.id.downloadItemsContainer)); // Set that the default LinearLayout where the metadata information cards will be appended is the one in MainActivity
        PodcastProgress.setProgressBar(root.findViewById(R.id.progressBar)); // Set what progress bar should be used when an episode is added in the queue (or has finished downloading)

        root.findViewById(R.id.downloadNewEpisodes).setOnClickListener(view -> { // Show the Dialog where the user can choose how to download the new episodes of their favorite podcasts
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View layout = inflater.inflate(R.layout.download_multiple_files, null);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));
            layout.setVisibility(View.VISIBLE);
            // The list that'll contain all the information of the fetched podcasts
            List<PodcastInformation> fetchedPodcasts = new ArrayList<>();
            AtomicBoolean stopAtFirst = new AtomicBoolean(false); // If true, the script will break when a duplicate is found
            AtomicBoolean showDownloadPicker = new AtomicBoolean(false); // If true, the user will be able to pick which podcasts to download before downloading them
            ((MaterialSwitch) layout.findViewById(R.id.breakAtFirst)).setOnCheckedChangeListener((buttonView, checked) -> {
                stopAtFirst.set(checked);
            });
            ((MaterialSwitch) layout.findViewById(R.id.showItemsToDownload)).setOnCheckedChangeListener((buttonView, checked) -> {
                showDownloadPicker.set(checked);
            });
            layout.findViewById(R.id.stopFileName).setOnClickListener(v -> { // Check if a file with the same name already exists to get if a file is a duplicate
                new Thread(() -> {
                    CheckDuplicates.UsingFileName(requireActivity().getApplicationContext(), stopAtFirst.get(), new CheckDuplicates.EnqueueHandler() {
                        @Override
                        public void enqueue(PodcastInformation information) { // Use this to run the enqueueItem function in the main thread
                            requireActivity().runOnUiThread(() -> {
                                if (showDownloadPicker.get()) fetchedPodcasts.add(information); else PodcastDownloader.DownloadQueue.enqueueItem(information, getActivity());
                            });
                        }
                        @Override
                        public void fetchedAllItems() {
                            requireActivity().runOnUiThread(() -> { // Hide circle progress bar, and notify the user that all the items have been enqueued
                                root.findViewById(R.id.fetchingPodcastsStatus).setVisibility(View.GONE);
                                Snackbar.make(root.findViewById(R.id.main), getResources().getString(R.string.fetched_data), Snackbar.LENGTH_LONG).show();
                            });
                            if (showDownloadPicker.get()) {
                                PodcastDownloader.setPodcastInformation(fetchedPodcasts); // We'll store the new items in the PodcastDownloader class, since there's a risk that passing them as a extra intent string causes a TransactionTooLargeException
                                Intent intent = new Intent(getActivity(), PodcastsItemsDownloader.class);
                                MainDownloader.getFetchedRequest.launch(intent);
                            }
                        }
                    });
                }).start();
                downloadDialog.dismiss();
                getActivity().runOnUiThread(() -> root.findViewById(R.id.fetchingPodcastsStatus).setVisibility(View.VISIBLE));
            });
            layout.findViewById(R.id.stopUrl).setOnClickListener(v -> { // Check if the URL has already been downloaded to get if a file is a duplicate
                new Thread(() -> {
                    CheckDuplicates.UsingURL(stopAtFirst.get(), getContext().getApplicationContext(), new CheckDuplicates.EnqueueHandler() {
                        @Override
                        public void enqueue(PodcastInformation information) {
                            getActivity().runOnUiThread(() -> {
                                if (showDownloadPicker.get()) fetchedPodcasts.add(information); else PodcastDownloader.DownloadQueue.enqueueItem(information, getActivity());
                            });
                        }
                        @Override
                        public void fetchedAllItems() {
                            getActivity().runOnUiThread(() -> { // Hide circle progress bar, and notify the user that all the items have been enqueued
                                root.findViewById(R.id.fetchingPodcastsStatus).setVisibility(View.GONE);
                                Snackbar.make(root.findViewById(R.id.main), getResources().getString(R.string.fetched_data), Snackbar.LENGTH_LONG).show();
                            });
                            if (showDownloadPicker.get()) {
                                PodcastDownloader.setPodcastInformation(fetchedPodcasts); // We'll store the new items in the PodcastDownloader class, since there's a risk that passing them as a extra intent string causes a TransactionTooLargeException
                                Intent intent = new Intent(getActivity(), PodcastsItemsDownloader.class);
                                MainDownloader.getFetchedRequest.launch(intent);
                            }
                        }
                    });
                }).start();
                downloadDialog.dismiss();
                getActivity().runOnUiThread(() -> root.findViewById(R.id.fetchingPodcastsStatus).setVisibility(View.VISIBLE));
            });
            downloadDialog = new MaterialAlertDialogBuilder(getContext())
                    .setView(layout)
                    .show();
        });
        ActivityResultLauncher<Intent> getOutputFolder = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (PickPodcastFolder.updateStorage(o, getContext().getApplicationContext()) != null) { // Successful storage update
                root.findViewById(R.id.downloadButton).performClick();
            }
        });
        root.findViewById(R.id.downloadButton).setOnClickListener(view -> {
            String uri = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getString("DownloadFolder", null);
            boolean folderHasBeenDeleted = false;
            if (uri != null) {
                DocumentFile file = DocumentFile.fromTreeUri(getContext(), Uri.parse(uri));
                folderHasBeenDeleted = file == null || !file.isDirectory() || !file.exists() || !file.canWrite();
            }
            if (uri == null || folderHasBeenDeleted) { // Ask the user to pick the output folder for the files
                Toast.makeText(getContext().getApplicationContext(), getResources().getString(R.string.pick_dir_prompt), Toast.LENGTH_LONG).show();
                getOutputFolder.launch(PickPodcastFolder.getIntent());
                return;
            }
            // Create a MaterialDialog that blocks user interaction until the RSS feed hasn't been fetched
            MaterialAlertDialogBuilder dialog = LoadingDialog.build(getContext());
            AlertDialog dialogShown = dialog.show();
            Thread thread = new Thread(() -> {
                Editable urlText = ((TextInputEditText) root.findViewById(R.id.downloadUrl)).getText(); // Get the URL
                if (urlText == null) return;
                PodcastInformation information = GetPodcastInformation.FromUrl(urlText.toString(), getContext(), view);
                if (information != null) {
                    PodcastDownloader.setPodcastInformation(information); // We'll store the new items in the PodcastDownloader class, since there's a risk that passing them as a extra intent string causes a TransactionTooLargeException
                    Intent intent = new Intent(getContext(), PodcastsItemsDownloader.class);
                    MainDownloader.getFetchedRequest.launch(intent);
                }
                getActivity().runOnUiThread(dialogShown::dismiss);
            });
            thread.start();
        });
        // Get if the user has shared an URL with the application, and start the RSS fetching
        for (Map.Entry<Long, PodcastDownloadInformation> item : PodcastDownloader.DownloadQueue.currentOperations.entrySet()) DownloadUIManager.addPodcastConversion(item.getValue(), item.getKey()); // Restore the metadata cards of the currently downloaded items. This might happen if the user changes theme.
        if (!PodcastDownloader.DownloadQueue.currentOperations.isEmpty()) {
            root.findViewById(R.id.downloadItemsContainer).setVisibility(View.VISIBLE); // Make the container visible if there are new elements
            ((ProgressBar) root.findViewById(R.id.progressBar)).setMax(PodcastDownloader.DownloadQueue.getInfoLength());
        }
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Start the RSS feed fetching. This must be called only if the user is already in the "PodcastDownload" class.
     * @param url the URL of the RSS feed
     */
    public void startDownload(String url) {
        ((TextInputEditText) root.findViewById(R.id.downloadUrl)).setText(url, TextView.BufferType.EDITABLE);
        root.findViewById(R.id.downloadButton).performClick();
    }

}