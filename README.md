# podcast-downloader

An Android application that can download podcasts from a RSS feed.

## How it works

Find the URL of a podcast's RSS feed. Now, add it in the "Downloader" textbox.
The application will fetch all the available podcasts, and you'll be able to
select the items to download. The application will download them and, if they
are a MP3 file, add metadata. You can find screenshots for this process below.

### Download screenshots:

![Image of the main UI](./readme_assets/downloader_ui.jpg)

After you click the "Start download" button, the podcasts items will be fetched.
You'll be able to choose which elements to download:

![Podcast episode picker UI](./readme_assets/selection_ui.jpg)

After you've selected all the files you want to download, click on the "Download
selected items" button (or the download icon at the top).

![Download episode UI](./readme_assets/download_ui.jpg)

You'll go back to the Downloader UI. Now, at the bottom you'll find the episodes
that are being downloaded. They'll disappear when the download ends.

## Settings

![Settings UI](./readme_assets/settings_ui.jpg)

In the settings, you can customize:

### Concurrent downloads

- The number of concurrent downloads (how many downloads can happen at the same
  time)
- If you want to save the XML file in the PodcastsDownloader directory

### Metadata

- If you want to add metadata to MP3 files
- And if the description should be parsed from HTML

#### Track number

![Track settings UI](./readme_assets/track_settings_ui.jpg)

Additional settings are available for track number customization. Here you can
choose if the suggested track number from the XML should be used, and what the
app should do if the podcast track number is unknown.

### Album Art

If possible, podcast-downloader autiomatically downloads the album art, and
it'll re-encode it to a JPEG. You can customize:

- The maximum width and the maximum height
- The JPEG quality

## Disclaimer

This application is licensed under the MIT license. The user is responsable for
the usage of this tool.
