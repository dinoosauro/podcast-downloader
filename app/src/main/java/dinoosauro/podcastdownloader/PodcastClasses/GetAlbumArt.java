package dinoosauro.podcastdownloader.PodcastClasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GetAlbumArt {
    private static class AlbumArtReference {
        /**
         * The Byte[] with the downloaded content from the provided webpage, without re-encoding
         */
        public byte[] originalBytes;
        /**
         * The maximum width that was set when this image was re-encoded.
         * This is kept so that, if the user later changes re-encoding settings, the image will be re-encoded with the new quality.
         */
        public int maxWidthOfElaborated;
        /**
         * The maximum height that was set when this image was re-encoded.
         * This is kept so that, if the user later changes re-encoding settings, the image will be re-encoded with the new quality.
         */
        public int maxHeightOfElaborated;
        /**
         * The JPEG quality that was set when this image was re-encoded.
         * This is kept so that, if the user later changes re-encoding settings, the image will be re-encoded with the new quality.
         */
        public int quality;
        /**
         * The Byte[] of the re-encoded image, in JPEG format.
         */
        public byte[] elaboratedImage;
        public AlbumArtReference(byte[] originalBytes, int maxWidthOfElaborated, int maxHeightOfElaborated, int quality, byte[] elaboratedImage) {
            this.originalBytes = originalBytes;
            this.maxWidthOfElaborated = maxWidthOfElaborated;
            this.maxHeightOfElaborated = maxHeightOfElaborated;
            this.quality = quality;
            this.elaboratedImage = elaboratedImage;
        }
    }

    /**
     * From the download image URL, get the downloaded image, the re-encoded image and width/height/quality information
     */
    private static Map<String, AlbumArtReference> albumArtContainer = new HashMap<>();

    /**
     * Download an Album Art. Caching is done for the entirety of the session.
     * @param imageURL the URL to connect for downloading the image
     * @param context Android Context that'll be used for getting user's preferences for re-encoding quality
     * @return a Byte[] with a JPEG image
     * @throws IOException The URL is not valid
     */
    public static byte[] downloadAlbum(String imageURL, Context context) throws IOException {
        AlbumArtReference cachedContent = albumArtContainer.get(imageURL);
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        int width = preferences.getInt("MaximumAlbumWidth", 800);
        int height = preferences.getInt("MaximumAlbumHeight", 800);
        int quality = preferences.getInt("JpegQuality", 80);
        if (cachedContent != null) {
            if (cachedContent.maxHeightOfElaborated == height && cachedContent.maxWidthOfElaborated == width && cachedContent.quality == quality) { // Check that all the re-encoding options are the same. In this case, the image does not need re-encoding and the output byte[] can be returned
                return cachedContent.elaboratedImage;
            };
            byte[] newReference = convertImage(cachedContent.originalBytes, width, height, quality);
            albumArtContainer.put(imageURL, new AlbumArtReference(cachedContent.originalBytes, width, height, quality, newReference));
            return newReference;
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(imageURL).openConnection();
        connection.setRequestMethod("GET");
        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byte[] output = byteArrayOutputStream.toByteArray();
            byte[] imageEncoded = convertImage(output, width, height, quality);
            albumArtContainer.put(imageURL, new AlbumArtReference(output, width, height, quality, imageEncoded));
            return output;

        }

    /**
     * Convert an image to a JPEG
     * @param image a Byte[] with the image content
     * @param maxWidth the maximum permitted width
     * @param maxHeight the maximum permitted height
     * @param quality the quality of the output JPEG
     * @return a Byte[] with the encoded image
     */
        private static byte[] convertImage(byte[] image, int maxWidth, int maxHeight, int quality) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxWidth || height > maxHeight) {
                float aspectRatio = (float) width / (float) height;
                if (width > height) {
                    width = maxWidth;
                    height = Math.round(maxWidth / aspectRatio);
                } else {
                    height = maxHeight;
                    width = Math.round(maxHeight * aspectRatio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
}
