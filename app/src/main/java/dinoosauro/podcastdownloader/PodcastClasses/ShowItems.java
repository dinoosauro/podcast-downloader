package dinoosauro.podcastdownloader.PodcastClasses;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.UUID;

public class ShowItems {
    public String title;
    public String description;
    public String publishedDate;
    public String episodeNumber;
    public String author;
    public String url;
    public String uuid = UUID.randomUUID().toString();

    /**
     * ShowItems contains the specific metadata for each podcast episode
     * @param title the title of the episode
     * @param description the description of the episode
     * @param publishedDate the date when the episode was published
     * @param episodeNumber the episode number
     * @param author who made this episode
     * @param url the URL of the audio file
     */
    public ShowItems(String title, String description, String publishedDate, String episodeNumber, String author, String url) {
    this.title = title;
    this.description = description;
    this.publishedDate = publishedDate;
    this.episodeNumber = episodeNumber;
    this.author = author;
    this.url = url;
    }

}
