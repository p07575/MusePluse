package com.burchard36.libs.youtube;

import lombok.Getter;

/**
 * Simple data holder for video information retrieved via yt-dlp.
 * Replaces the old {@code com.github.kiulian.downloader.model.videos.VideoInfo} from java-youtube-downloader.
 */
public class YtDlpVideoInfo {

    @Getter
    private final int durationSeconds;
    @Getter
    private final String title;
    @Getter
    private final String youtubeUrl;

    public YtDlpVideoInfo(int durationSeconds, String title, String youtubeUrl) {
        this.durationSeconds = durationSeconds;
        this.title = title;
        this.youtubeUrl = youtubeUrl;
    }
}
