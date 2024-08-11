package dinoosauro.podcastdownloader.PodcastClasses;

import java.util.List;

public class PodcastDownloadInformation extends PodcastInformation {
    public String downloadPath;
    /**
     * The PodcastInformation class contains all the useful metadata for a Podcast.
     *
     * @param title  the title of the podcast show
     * @param image  an URL that can be used for downloading the album art
     * @param author who created this podcast
     * @param items  a list of the episodes of this podcast. Note that, in case the content is being downloaded, this list will contain only one element (the downloaded file)
     * @param downloadPath the suggested path for downloading, in case it isn't possible to obtain it from the Cursor
     */
    public PodcastDownloadInformation(String title, String image, String author, List<ShowItems> items, String downloadPath) {
        super(title, image, author, items);
        this.downloadPath = downloadPath;
    }
    public PodcastDownloadInformation(PodcastInformation information, String downloadPath) {
        super(information.title, information.image, information.author, information.items);
        this.downloadPath = downloadPath;
    }
}
