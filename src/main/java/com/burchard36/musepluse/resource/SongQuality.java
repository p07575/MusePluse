package com.burchard36.musepluse.resource;

/**
 * Enum for SongQuality for converting videos to ogg files with ffmpeg
 *
 * HORRIBLE = 16k
 * LOW = 32k
 * MEDIUM = 64k
 * HIGH = 96k
 * ULTRA = 128k
 */
public enum SongQuality {

    HORRIBLE,
    LOW,
    MEDIUM,
    HIGH,
    ULTRA;

    /**
     * Returns libvorbis VBR quality value (-1 to 10).
     * Scale: 1=~45kbps, 3=~112kbps, 5=~160kbps, 7=~224kbps, 9=~320kbps
     */
    public static int getQualityNumber(final SongQuality quality) {
        switch (quality) {
            case HORRIBLE -> { return 1; }
            case LOW      -> { return 2; }
            case MEDIUM   -> { return 4; }
            case HIGH     -> { return 6; }
            case ULTRA    -> { return 8; }
        }
        return 4;
    }
}
