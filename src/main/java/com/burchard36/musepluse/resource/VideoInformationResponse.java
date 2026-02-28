package com.burchard36.musepluse.resource;

import com.burchard36.libs.config.SongData;
import com.burchard36.libs.youtube.YtDlpVideoInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A class used for passing VideoInformation between tasks, threads & classes
 * @param videoInfo the {@link YtDlpVideoInfo} provided
 * @param songData the {@link SongData} of the song
 */
public record VideoInformationResponse(@NonNull YtDlpVideoInfo videoInfo, @NonNull SongData songData) {
}
