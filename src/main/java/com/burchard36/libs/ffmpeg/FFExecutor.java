package com.burchard36.libs.ffmpeg;

import com.burchard36.musepluse.MusePlusePlugin;
import com.burchard36.musepluse.resource.SongQuality;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FFExecutor {
    public static Executor FFMPEG_THREAD_POOL = Executors.newFixedThreadPool(2);

    protected final File ffmpeg;
    protected final Runtime runtime = Runtime.getRuntime();
    protected final MusePlusePlugin pluginInstance;

    public FFExecutor(final File ffmpeg) {
        this.ffmpeg = ffmpeg;
        this.pluginInstance = MusePlusePlugin.INSTANCE;
    }

    public void doFuckingSomething() {
        Bukkit.getLogger().info("What????");
    }

    public void convertToOgg(final File from, final File to, Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                final int songQuality = SongQuality.getQualityNumber(this.pluginInstance.getMusePluseSettings().getSongGenerationQuality());
                // Minecraft requires OGG Vorbis at 44100 Hz sample rate
                // -c:a libvorbis: correct encoder for Minecraft compatibility
                // -q:a: VBR quality mode (avoids libvorbis minimum bitrate errors)
                // -ar 44100: sample rate required by Minecraft
                ProcessBuilder pb = new ProcessBuilder(
                        ffmpeg.getPath(), "-y", "-v", "error",
                        "-i", from.getPath(),
                        "-c:a", "libvorbis", "-q:a", String.valueOf(songQuality), "-ar", "44100",
                        to.getPath()
                );
                pb.redirectErrorStream(false);
                Process process = pb.start();

                // Consume stdout
                try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    while (stdInput.readLine() != null) {}
                }
                // Capture stderr for error logging
                StringBuilder errOutput = new StringBuilder();
                try (BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = stdError.readLine()) != null) { errOutput.append(line).append("\n"); }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0 || !to.exists() || to.length() == 0) {
                    Bukkit.getLogger().severe("FFmpeg conversion failed: " + errOutput.toString().trim());
                }

                onComplete.run();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, FFMPEG_THREAD_POOL);
    }

}
