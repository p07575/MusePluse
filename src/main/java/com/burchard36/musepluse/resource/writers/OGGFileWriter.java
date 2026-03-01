package com.burchard36.musepluse.resource.writers;

import com.google.gson.Gson;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static com.burchard36.libs.utils.StringUtils.convert;

public abstract class OGGFileWriter extends SoundsJsonWriter{
    public OGGFileWriter(Gson gson) {
        super(gson);
    }

    /**
     * Copies OGG files from /media/ogg into the resource pack temp directory.
     * Uses Files.copy instead of renameTo — renameTo silently fails cross-filesystem.
     */
    public void flashOGGFilesToTempDirectory() {
        File[] files = this.getOggDirectory().listFiles();
        if (files == null || files.length == 0)
            throw new RuntimeException("No OGG files found in /media/ogg — conversion may have failed.");
        final File musicDirectory = new File(this.getResourcePackTempFilesDirectory(), "/assets/assets/musepluse/sounds/music");
        if (!musicDirectory.exists()) musicDirectory.mkdirs();
        for (File file : files) {
            if (!file.getName().endsWith(".ogg")) continue;
            File dest = new File(musicDirectory, file.getName());
            try {
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(convert("&fCopied OGG: &b" + file.getName() + " &f(" + dest.length() + " bytes)"));
            } catch (IOException e) {
                Bukkit.getLogger().severe("Failed to copy OGG file " + file.getName() + ": " + e.getMessage());
            }
        }
    }
}
