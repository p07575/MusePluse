package com.burchard36.musepluse.resource.writers;

import com.burchard36.musepluse.resource.ResourcePackFiles;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.io.FileWriter;
import java.io.IOException;

import static com.burchard36.libs.utils.StringUtils.convert;

public abstract class McMetaWriter extends ResourcePackFiles {
    public final Gson gson;

    public McMetaWriter(final Gson gson) {
        this.gson = gson;
    }

    /**
     * Writes the mc meta file to the temp file location
     */
    public void writeMcMeta() {
        if (this.getMcMetaFile().exists()) {
            if (this.getMcMetaFile().delete()) Bukkit.getConsoleSender().sendMessage(convert("&fOld pack.mcmeta was found, &cdeleting&f..."));
            try {
                if (this.getMcMetaFile().createNewFile()) Bukkit.getConsoleSender().sendMessage(convert("&aSuccessfully&f created new &bpack.mcmeta"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        JsonObject object = new JsonObject();
        JsonObject subObject = new JsonObject();
        object.add("pack", subObject);
        subObject.addProperty("pack_format", 75);
        subObject.addProperty("description", "Muse Pluse Resource Pack!");
        // supported_formats (array format) for MC 1.20.2 - 1.21.8 compatibility
        JsonArray supportedFormats = new JsonArray();
        supportedFormats.add(13);
        supportedFormats.add(75);
        subObject.add("supported_formats", supportedFormats);
        // min_format / max_format required for MC 1.21.9+ (replaces supported_formats)
        subObject.addProperty("min_format", 13);
        subObject.addProperty("max_format", 75);
        try (final FileWriter writer = new FileWriter(this.getMcMetaFile())) {
            this.gson.toJson(object, writer);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
