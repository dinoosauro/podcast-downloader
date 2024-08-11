package dinoosauro.podcastdownloader.PodcastClasses;

import java.util.List;

import dinoosauro.podcastdownloader.PodcastClasses.ShowItems;

public class PodcastInformation {
    /**
     * The title of the podcast show
     */
    public String title;
    /**
     * An URL that can be used for downloading the album art
     */
    public String image;
    /**
     * Who created this podcast
     */
    public String author;
    /**
     * A list of the episodes of this podcast.
     * Note that, in case the content is being downloaded, this list will contain only one element (the downloaded file)
     */
    public List<ShowItems> items;

    /**
     * The PodcastInformation class contains all the useful metadata for a Podcast.
     * @param title the title of the podcast show
     * @param image an URL that can be used for downloading the album art
     * @param author who created this podcast
     * @param items a list of the episodes of this podcast. Note that, in case the content is being downloaded, this list will contain only one element (the downloaded file)
     */
    public PodcastInformation(String title, String image, String author, List<ShowItems> items) {
        this.title = title;
        this.image = image;
        this.author = author;
        this.items = items;
    }

}
