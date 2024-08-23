package dinoosauro.podcastdownloader;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dinoosauro.podcastdownloader.PodcastClasses.GetAlbumArt;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastDownloadInformation;
import dinoosauro.podcastdownloader.PodcastClasses.PodcastInformation;
import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;
import dinoosauro.podcastdownloader.UIHelper.CheckUpdates;
import dinoosauro.podcastdownloader.UIHelper.ColorMenuIcons;
import dinoosauro.podcastdownloader.UIHelper.DownloadUIManager;
import dinoosauro.podcastdownloader.UIHelper.PodcastProgress;

public class MainActivity extends AppCompatActivity {
    /**
     * From an Element, get the text content of an item, returning null if non-existent
     * @param item the item to get the TextContent
     * @return a String with the TextContent
     */
    String nullPlaceholder(Node item) {
        if (item == null) return null;
        return item.getTextContent();
    }

    /**
     * From a DownloadMangaer's URI, get the real path
     * @param context an Android context
     * @param uri the Uri fetched from the DownloadManager's event
     * @return a String with a valid path
     */
    public String getRealPathFromURI(Context context, Uri uri) {
        if (uri == null) return null;
        String result = null;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                if (idx != -1) {
                    result = cursor.getString(idx);
                }
            }
            cursor.close();
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Setup toolbar: set it as support, and change its text
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getResources().getString(R.string.downloader));
        DownloadUIManager.setLinearLayout(findViewById(R.id.downloadItemsContainer)); // Set that the default LinearLayout where the metadata information cards will be appended is the one in MainActivity
        PodcastProgress.setProgressBar(findViewById(R.id.progressBar)); // Set what progress bar should be used when an episode is added in the queue (or has finished downloading)
        if (Build.VERSION.SDK_INT <= 28) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
        findViewById(R.id.downloadButton).setOnClickListener(view -> {
            // Create a MaterialDialog that blocks user interaction until the RSS feed hasn't been fetched
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this);
            dialog.setTitle(R.string.podcast_info);
            ProgressBar progressBar = new ProgressBar(MainActivity.this);
            progressBar.setIndeterminate(true);
            progressBar.setPadding(ColorMenuIcons.getScalablePixels(15, getApplicationContext()), 0, ColorMenuIcons.getScalablePixels(15, getApplicationContext()), 0);
            dialog.setView(progressBar);
            dialog.setMessage(R.string.podcast_info_desc);
            dialog.setCancelable(false);
            AlertDialog dialogShown = dialog.show();
            Thread thread = new Thread(() -> {
                try {
                Editable urlText = ((TextInputEditText) findViewById(R.id.downloadUrl)).getText(); // Get the URL
                if (urlText == null) return;
                String finalUrl = urlText.toString().trim();
                if (finalUrl.contains("https://antennapod.org") && finalUrl.contains("url=")) { // Parse AntennaPod URLs to get the RSS feed
                    finalUrl = finalUrl.substring(finalUrl.indexOf("url=") + 4);
                    if (finalUrl.contains("&")) finalUrl = finalUrl.substring(0, finalUrl.indexOf("&"));
                    finalUrl = URLDecoder.decode(finalUrl, "utf-8");
                }
                List<ShowItems> optionList = new ArrayList<ShowItems>(); // The container of all the podcast items
                    // Parse the XML
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.parse(finalUrl);
                    SharedPreferences preferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                    if (preferences.getBoolean("WriteOutputXML", true)) {
                        try {
                            // Create folder and file
                            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PodcastDownloader");
                            folder.mkdir();
                            String getPodcastTitle = nullPlaceholder(document.getElementsByTagName("title").item(0));
                            File xmlFile = new File(folder, PodcastDownloader.DownloadQueue.nameSanitizer(getPodcastTitle != null ? getPodcastTitle : finalUrl) + " [" + PodcastDownloader.DownloadQueue.nameSanitizer((new Date()).toString()) + "].xml");
                            xmlFile.createNewFile();
                            FileOutputStream fos = new FileOutputStream(xmlFile);
                            // Convert the Document to a String using the TransformerFactory. This might be rewritten later, since it would be more convenient to directly fetch the XML as a string and write it.
                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                            Transformer transformer = transformerFactory.newTransformer();
                            StringWriter stringWriter = new StringWriter();
                            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
                            fos.write(stringWriter.toString().getBytes());
                            fos.close();
                        } catch (Exception ex) {
                            Snackbar.make(view, R.string.failed_xml_writing, BaseTransientBottomBar.LENGTH_LONG).show();
                        }
                    }
                    document.getDocumentElement().normalize();
                    NodeList items = document.getElementsByTagName("item");
                    boolean shouldUseSuggestedTrack = preferences.getBoolean("UseSuggestedTrack", true); // If false, the suggested track from the RSS feed will be ignored
                    int trackFallbackType = preferences.getInt("SuggestedTrackFallback", 0); // What the application should do if there isn't a suggested track from the RSS feed
                    int trackFallbackStartFrom = preferences.getInt("SuggestedTrackStartFrom", 1); // From what number the numeration should tart
                    int podcastCount = items.getLength();
                    for (int i = 0; i < podcastCount; i++) {
                        Element element = (Element) items.item(i);
                        String author = nullPlaceholder(element.getElementsByTagName("itunes:author").item(0)); // We'll start by looking if there's an author field in the specific episode.
                        if (author == null || author.trim().equals("")) author = nullPlaceholder(document.getElementsByTagName("itunes:author").item(0)); // If there's no author field in the specific episode, we'll use the one that's generally in the podcast.
                        String podcastNumber = shouldUseSuggestedTrack ? nullPlaceholder(element.getElementsByTagName("itunes:episode").item(0)) : null;
                        if (podcastNumber == null || podcastNumber.trim().equals("")) {
                            switch(trackFallbackType) {
                                case 1: // Start from the newest podcast
                                    podcastNumber = String.valueOf(i + trackFallbackStartFrom);
                                    break;
                                case 2: // Start from the oldest podcast
                                    podcastNumber = String.valueOf(podcastCount - i + trackFallbackStartFrom - 1);
                                    break;
                            }
                        }
                        optionList.add(new ShowItems(
                                nullPlaceholder(element.getElementsByTagName("title").item(0)),
                                nullPlaceholder(element.getElementsByTagName("description").item(0)),
                                nullPlaceholder(element.getElementsByTagName("pubDate").item(0)),
                                podcastNumber,
                                author,
                                element.getElementsByTagName("enclosure").item(0).getAttributes().getNamedItem("url").getNodeValue()
                        ));
                    }
                    runOnUiThread(dialogShown::dismiss);
                String image = null;
                Element imgElement = ((Element) document.getElementsByTagName("image").item(0)); // Some podcasts (Megaphone) have the <image><url></url></image> tag for the album image. We'll try to get those tags.
                if (imgElement != null) {
                        imgElement = ((Element) document.getElementsByTagName("url").item(0));
                        if (imgElement != null) image = imgElement.getTextContent();
                } else { // We'll try using the standard itunes:image tag
                    imgElement = ((Element) document.getElementsByTagName("itunes:image").item(0));
                    if (imgElement != null) image = imgElement.getAttribute("href");
                }
                if (image != null) {
                    image = image.replace("\n", "").trim(); // Remove unnecessary characters that might be in the URL
                    // We complete the URL structure if necessary
                    if (image.startsWith("/")) image = finalUrl.substring(0, finalUrl.indexOf("/", finalUrl.indexOf("://") + 3)) + image;
                    if (image.startsWith("./")) image = finalUrl.substring(0, finalUrl.lastIndexOf("/")) + image.substring(1);
                }
                PodcastDownloader.setPodcastInformation(new PodcastInformation(nullPlaceholder(document.getElementsByTagName("title").item(0)), image, nullPlaceholder(document.getElementsByTagName("itunes:author").item(0)), optionList)); // We'll store the new items in the PodcastDownloader class.
                    Intent intent = new Intent(MainActivity.this, PodcastsItemsDownloader.class);
                    startActivity(intent);

            } catch (MalformedURLException e) {
                    Snackbar.make(view, R.string.malformedUrl, BaseTransientBottomBar.LENGTH_LONG).show();
                    dialogShown.dismiss();
            } catch (IOException e) {
                    Snackbar.make(view, R.string.urlIOException, BaseTransientBottomBar.LENGTH_LONG).show();
                    dialogShown.dismiss();
            } catch (ParserConfigurationException e) {
                    Snackbar.make(view, R.string.invalidXml, BaseTransientBottomBar.LENGTH_LONG).show();
                    dialogShown.dismiss();
                } catch (SAXException e) {
                    Snackbar.make(view, R.string.invalidXml, BaseTransientBottomBar.LENGTH_LONG).show();
                    dialogShown.dismiss();
                }
            });
            thread.start();
        });
        // Register a BroadcastReceiver to handle download completion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED); else registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        // Get if the user has shared an URL with the application, and start the RSS fetching
        for (Map.Entry<Long, PodcastDownloadInformation> item : PodcastDownloader.DownloadQueue.currentOperations.entrySet()) DownloadUIManager.addPodcastConversion(item.getValue(), item.getKey()); // Restore the metadata cards of the currently downloaded items. This might happen if the user changes theme.
        if (PodcastDownloader.DownloadQueue.currentOperations.size() > 0) {
            findViewById(R.id.downloadItemsContainer).setVisibility(View.VISIBLE); // Make the container visible if there are new elements
            ((ProgressBar) findViewById(R.id.progressBar)).setMax(PodcastDownloader.DownloadQueue.getInfoLength());
        }
        CheckUpdates.checkAndDisplay(MainActivity.this); // Look if there are updates available
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        String intentAction = intent.getAction();
        if (intentAction != null && intentAction.equalsIgnoreCase(Intent.ACTION_SEND)) {
            String extraText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (extraText != null) {
                String url = extraText;
                url = url.substring(url.indexOf("http")).replace("\n", "").trim();
                ((TextInputEditText) findViewById(R.id.downloadUrl)).setText(url, TextView.BufferType.EDITABLE);
                findViewById(R.id.downloadButton).performClick();
            }
        }
    }
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != -1) {
                    new Thread(() -> {
                        PodcastDownloadInformation currentPodcastInformation = PodcastDownloader.DownloadQueue.currentOperations.get(downloadId);
                        PodcastDownloader.DownloadQueue.currentOperations.remove(downloadId); // Make sure the next item can be downloaded
                        // And start its download. *THIS MUST BE DONE IN THE MAIN THREAD*: otherwise, the Thread won't be the same which initialized the View, causing in an Exception
                        runOnUiThread(PodcastDownloader.DownloadQueue::startDownload);
                        String realPath = getRealPathFromURI(context, ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).getUriForDownloadedFile(downloadId));
                        if (realPath == null && currentPodcastInformation != null) {
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), currentPodcastInformation.downloadPath);
                            if (file.exists()) realPath = file.getAbsolutePath();
                        }
                        if (realPath != null) {
                            File file = new File(realPath);
                            if (currentPodcastInformation != null && file.getAbsolutePath().endsWith(".mp3") && getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getBoolean("MP3Metadata", true)) { // Add metadata to MP3 file
                                try {
                                    Mp3File mp3File = new Mp3File(file);
                                    ID3v2 tag = mp3File.getId3v2Tag();
                                    tag.setAlbum(currentPodcastInformation.title);
                                    tag.setAlbumArtist(currentPodcastInformation.author);
                                    try {
                                        tag.setAlbumImage(GetAlbumArt.downloadAlbum(currentPodcastInformation.image, getApplicationContext()), "image/jpeg");
                                    } catch (IOException e) {
                                        runOnUiThread(() -> Snackbar.make(findViewById(R.id.downloadItemsContainer), getResources().getString(R.string.failed_album_art_download) + " " + currentPodcastInformation.title, BaseTransientBottomBar.LENGTH_LONG).show());
                                    }
                                    tag.setArtist(currentPodcastInformation.author);
                                    tag.setDate(currentPodcastInformation.items.get(0).publishedDate);
                                    if (currentPodcastInformation.items.get(0).publishedDate != null) { // Add year
                                        String number = currentPodcastInformation.items.get(0).publishedDate.length() > 12 ? currentPodcastInformation.items.get(0).publishedDate.substring(12, currentPodcastInformation.items.get(0).publishedDate.indexOf(' ', 12)).trim() : currentPodcastInformation.items.get(0).publishedDate.trim(); // In the Podcast standard syntax, the year is from the twelfth char. If there aren't enough characters, we'll try to use the entire string as the year.
                                        if (number.matches("^\\d+$")) { // Check that the string is composed of numbers
                                            tag.setYear(number);
                                        }
                                    }
                                    tag.setTitle(currentPodcastInformation.items.get(0).title);
                                    String description = currentPodcastInformation.items.get(0).description;
                                    if (description != null && getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getBoolean("DecodeHTML", true)) { // Parse the HTML string
                                        try {
                                            description = Jsoup.parse(description).wholeText();
                                        } catch (Exception ex) {
                                            Snackbar.make(findViewById(R.id.downloadItemsContainer), R.string.failed_html_parsing, Snackbar.LENGTH_LONG).show();
                                        }
                                    }
                                    tag.setComment(description);
                                    tag.setTrack(currentPodcastInformation.items.get(0).episodeNumber);
                                    mp3File.setId3v2Tag(tag);
                                    // Create a new file, that it'll contain the new metadata
                                    String newFileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + "-Metadata" + Math.random() + ".mp3";
                                    File metadataFile = new File(newFileName);
                                    if (metadataFile.exists()) metadataFile.delete();
                                    mp3File.save(newFileName);
                                    file.delete(); // Delete the old file
                                    metadataFile.renameTo(file); // And move the metadata file to the location
                                    MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, new String[]{"audio/mpeg"}, null); // Scan the new file
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        Snackbar.make(findViewById(R.id.downloadItemsContainer), getResources().getString(R.string.failed_metadata_add) + " " + currentPodcastInformation.items.get(0).title, BaseTransientBottomBar.LENGTH_LONG).show();
                                    });
                                }
                            } else  {
                                MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, new String[]{"audio/*"}, null); // Scan the downloaded file
                            }
                        } else {
                            runOnUiThread(() -> {
                                String fileName = currentPodcastInformation != null ? currentPodcastInformation.items.get(0).title : "Unknown";
                                Snackbar.make(findViewById(R.id.downloadItemsContainer), getResources().getString(R.string.failed_file_fetching) + " " + fileName + " " + getResources().getString(R.string.file_mediaprovider_error), BaseTransientBottomBar.LENGTH_LONG).show();
                            });
                        }
                            ViewGroup[] destinationLayout = DownloadUIManager.operationContainer.get(downloadId);
                            if (destinationLayout != null) {
                                runOnUiThread(() -> { // Start a scaling animation for deleting the element
                                    Animation anim = new ScaleAnimation(
                                            1f, 1f,
                                            1f, 0f,
                                            Animation.RELATIVE_TO_SELF, 0f,
                                            Animation.RELATIVE_TO_SELF, 0f);
                                    anim.setFillAfter(true);
                                    anim.setDuration(500);
                                    for (ViewGroup view : destinationLayout) view.startAnimation(anim);
                                });
                                try {
                                    Thread.sleep(500);
                                    runOnUiThread(() -> {
                                        PodcastDownloader.DownloadQueue.disableServiceIfNecessary(); // Check if the Foreground Service should be disabled (and disable it if necessary)
                                        for (ViewGroup view : destinationLayout) {
                                            if (view == null) continue;
                                            ViewGroup parentElement = ((ViewGroup) view.getParent());
                                            if (parentElement != null) parentElement.removeView(view);
                                        }
                                        PodcastProgress.updateValue(1);
                                    });
                                } catch (InterruptedException e) {
                                    Log.e("Failed removal", String.valueOf(downloadId));
                                }
                            } else {
                                runOnUiThread(() -> PodcastProgress.updateValue(1));
                            }

                    }).start();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        ColorMenuIcons.color(menu.findItem(R.id.action_settings), this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}