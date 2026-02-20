package it.unisa.skiscore.model;

/**
 * Represents a single webcam at a ski resort.
 */
public class WebcamModel {

    public enum StreamType { IMAGE, YOUTUBE, VIDEO_MP4 }

    private final String name;
    private final String streamUrl;   // URL of the stream or YouTube link
    private final String thumbnailUrl; // Explicit thumbnail (null = auto-derive)
    private final StreamType streamType;

    public WebcamModel(String name, String streamUrl, StreamType streamType) {
        this.name        = name;
        this.streamUrl   = streamUrl;
        this.streamType  = streamType;
        this.thumbnailUrl = deriveThumbnail(streamUrl, streamType);
    }

    /** For plain image sources */
    public WebcamModel(String name, String imageUrl) {
        this(name, imageUrl, StreamType.IMAGE);
    }

    private static String deriveThumbnail(String url, StreamType type) {
        if (type == StreamType.YOUTUBE && url != null) {
            // Extract video ID from youtu.be or youtube.com URLs
            String videoId = null;
            if (url.contains("youtu.be/")) {
                videoId = url.substring(url.lastIndexOf('/') + 1);
                // Strip query params if any
                int q = videoId.indexOf('?');
                if (q != -1) videoId = videoId.substring(0, q);
            } else if (url.contains("v=")) {
                videoId = url.substring(url.indexOf("v=") + 2);
                int q = videoId.indexOf('&');
                if (q != -1) videoId = videoId.substring(0, q);
            }
            if (videoId != null) {
                return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            }
        }
        return null; // Will use placeholder
    }

    public String getName()         { return name; }
    public String getStreamUrl()    { return streamUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public StreamType getStreamType() { return streamType; }
    // Backward-compat alias
    public String getImageUrl()     { return thumbnailUrl != null ? thumbnailUrl : streamUrl; }
}
