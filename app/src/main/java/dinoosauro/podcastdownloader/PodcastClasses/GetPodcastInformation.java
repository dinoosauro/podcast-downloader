package dinoosauro.podcastdownloader.PodcastClasses;

import android.content.SharedPreferences;
import android.os.Environment;
import android.view.View;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dinoosauro.podcastdownloader.MainActivity;
import dinoosauro.podcastdownloader.PodcastDownloader;
import dinoosauro.podcastdownloader.PodcastsItemsDownloader;
import dinoosauro.podcastdownloader.R;

/**
 * Get information about a Podcast from a RSS feed
 */
public class GetPodcastInformation {
    /**
     * From an Element, get the text content of an item, returning null if non-existent
     * @param item the item to get the TextContent
     * @param keepIndentation if false, each line will be trimmed.
     * @param keepLineBreak if false, the "\n" will be cut from the item.
     * @return a String with the TextContent
     */
    public static String nullPlaceholder(Node item, boolean keepIndentation, boolean keepLineBreak) {
        if (item == null) return null;
        String content = item.getTextContent();
        if (!keepIndentation) { // Trim spaces
            if (content.contains("\n")) {
                StringBuilder builder = new StringBuilder();
                String[] split = content.split("\n");
                for (int i = 0; i < split.length; i++) builder.append(split[i].trim() + (i == split.length - 1? "" : !keepLineBreak ? " " : "\n")); // If it's the last line, don't add anything. Else, if if the user doesn't want to keep the line breaks, put a space; otherwise, add a new line.
                content = builder.toString();
            }
        }
        return content;
    }

    /**
     * Get the Document of the parsed XML item
     * @param urlText the URL where the RSS feed is located
     * @return the parsed Document
     */
    public static Document getDocumentFromUrl(String urlText) throws ParserConfigurationException, IOException, SAXException {
        // Parse the XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(urlText);
    }

    /**
     * Convert the AntennaPod URLs, or return the original URL by trimming it.
     * @param urlText the original URL
     * @return the URL to use for the RSS feed fetching
     * @throws UnsupportedEncodingException if UTF-8 isn't supported as a encoding option.
     */
    public static String getCorrectUrl(String urlText) throws UnsupportedEncodingException {
        String finalUrl = urlText.toString().trim();
        if (finalUrl.contains("https://antennapod.org") && finalUrl.contains("url=")) { // Parse AntennaPod URLs to get the RSS feed
            finalUrl = finalUrl.substring(finalUrl.indexOf("url=") + 4);
            if (finalUrl.contains("&")) finalUrl = finalUrl.substring(0, finalUrl.indexOf("&"));
            finalUrl = URLDecoder.decode(finalUrl, "utf-8");
        }
        return finalUrl;
    }

    /**
     * Get the PodcastInformation from a RSS feed
     * @param urlText the original URL of the RSS feed
     * @param preferences the SharedPreferences used to get, well, the preferences of the user
     * @param view the View where Snackbars will be added
     * @return the PodcastInformation object of the current RSS feed
     */
    public static PodcastInformation FromUrl(String urlText, SharedPreferences preferences, View view) {
        try {
            boolean keepIndentation = preferences.getBoolean("KeepIndentation", false);
            boolean shouldKeepLineBreak = preferences.getBoolean("KeepLineBreak", false);
            String finalUrl = getCorrectUrl(urlText);
            Document document = getDocumentFromUrl(finalUrl);
            List<ShowItems> optionList = new ArrayList<ShowItems>(); // The container of all the podcast items
            if (preferences.getBoolean("WriteOutputXML", true)) {
                try {
                    // Create folder and file
                    File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PodcastDownloader");
                    folder.mkdir();
                    String getPodcastTitle = nullPlaceholder(document.getElementsByTagName("title").item(0), keepIndentation, shouldKeepLineBreak);
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
                    if (view != null) Snackbar.make(view, R.string.failed_xml_writing, BaseTransientBottomBar.LENGTH_LONG).show();
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
                String author = nullPlaceholder(element.getElementsByTagName("itunes:author").item(0), keepIndentation, shouldKeepLineBreak); // We'll start by looking if there's an author field in the specific episode.
                if (author == null || author.trim().equals("")) author = nullPlaceholder(document.getElementsByTagName("itunes:author").item(0), keepIndentation, shouldKeepLineBreak); // If there's no author field in the specific episode, we'll use the one that's generally in the podcast.
                String podcastNumber = shouldUseSuggestedTrack ? nullPlaceholder(element.getElementsByTagName("itunes:episode").item(0), keepIndentation, shouldKeepLineBreak) : null;
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
                        nullPlaceholder(element.getElementsByTagName("title").item(0), keepIndentation, shouldKeepLineBreak),
                        nullPlaceholder(element.getElementsByTagName("description").item(0), keepIndentation, shouldKeepLineBreak),
                        nullPlaceholder(element.getElementsByTagName("pubDate").item(0), keepIndentation, shouldKeepLineBreak),
                        podcastNumber,
                        author,
                        element.getElementsByTagName("enclosure").item(0).getAttributes().getNamedItem("url").getNodeValue()
                ));
            }
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
            Set<String> podcastSources = new HashSet<>(preferences.getStringSet("PodcastSources", new HashSet<>()));
            podcastSources.add(finalUrl);
            preferences.edit().putStringSet("PodcastSources", podcastSources).apply();
            return new PodcastInformation(nullPlaceholder(document.getElementsByTagName("title").item(0), keepIndentation, shouldKeepLineBreak), image, nullPlaceholder(document.getElementsByTagName("itunes:author").item(0), keepIndentation, shouldKeepLineBreak), optionList);
        } catch (MalformedURLException e) {
            if (view != null) Snackbar.make(view, R.string.malformedUrl, BaseTransientBottomBar.LENGTH_LONG).show();
        } catch (IOException e) {
            if (view != null) Snackbar.make(view, R.string.urlIOException, BaseTransientBottomBar.LENGTH_LONG).show();
        } catch (ParserConfigurationException e) {
            if (view != null) Snackbar.make(view, R.string.invalidXml, BaseTransientBottomBar.LENGTH_LONG).show();
        } catch (SAXException e) {
            if (view != null) Snackbar.make(view, R.string.invalidXml, BaseTransientBottomBar.LENGTH_LONG).show();
        }
        return null;
    }
}
