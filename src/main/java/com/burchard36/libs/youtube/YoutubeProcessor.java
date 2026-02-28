package com.burchard36.libs.youtube;

import com.burchard36.musepluse.MusePlusePlugin;
import com.burchard36.libs.ffmpeg.FFExecutor;
import com.burchard36.libs.ffmpeg.FFMPEGDownloader;
import com.burchard36.libs.ffmpeg.FFTask;
import com.burchard36.libs.ffmpeg.events.FFMPEGInitializedEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.burchard36.musepluse.MusePlusePlugin.*;
import static com.burchard36.libs.utils.StringUtils.convert;

public class YoutubeProcessor implements Listener {

    protected final YtDlpDownloader ytDlpDownloader;
    protected final FFMPEGDownloader ffmpegDownloader;
    protected FFExecutor ffExecutor;
    protected final List<FFTask> queuedOGGConversions;
    protected final File mediaOutput;
    protected final File oggOutput;
    protected final File m4aOutput;
    protected File ffmpegFile;

    public YoutubeProcessor(final MusePlusePlugin pluginInstance) {
        MusePlusePlugin.registerEvent(this);
        this.queuedOGGConversions = new ArrayList<>();
        this.ytDlpDownloader = pluginInstance.getYtDlpDownloader();
        this.ffmpegDownloader = pluginInstance.getFfmpegDownloader();
        this.mediaOutput = new File(pluginInstance.getDataFolder(), "/media");
        this.oggOutput = new File(this.mediaOutput, "/ogg");
        this.m4aOutput = new File(this.mediaOutput, "/m4a");

        if (this.ffmpegDownloader.ffmpegIsInstalled()) this.initializeFFMPEG();
    }

    /**
     * Downloads a YouTube video's audio via yt-dlp and converts it to OGG using FFmpeg.
     *
     * @param youtubeUrl  the full YouTube video URL
     * @param newFileName the file name to save (without extension)
     * @param callback    called when conversion is finished (or null on failure)
     */
    public final void downloadYouTubeAudioAsOGG(final String youtubeUrl, String newFileName, final Consumer<File> callback) {
        final String finalNewFileName = newFileName;

        CompletableFuture.runAsync(() -> {
            try {
                if (this.ytDlpDownloader.isDownloading()) {
                    Bukkit.getConsoleSender().sendMessage(convert("&cyt-dlp is still installing, cannot download &b%s&f yet. Will retry automatically.".formatted(finalNewFileName)));
                    callback.accept(null);
                    return;
                }

                if (!this.m4aOutput.exists()) this.m4aOutput.mkdirs();
                if (!this.oggOutput.exists()) this.oggOutput.mkdirs();

                final File ytDlpFile = this.ytDlpDownloader.getYtDlpFile();
                // Use %(ext)s so yt-dlp determines the correct extension after download
                final String outputTemplate = new File(this.m4aOutput, finalNewFileName + ".%(ext)s").getPath();
                final File oggFile = new File(this.oggOutput, finalNewFileName + ".ogg");

                // Build yt-dlp command: download best audio stream only (no post-processing needed)
                List<String> command = new ArrayList<>();
                command.add(ytDlpFile.getPath());
                command.add("-f");
                command.add("bestaudio");
                command.add("-o");
                command.add(outputTemplate);
                command.add("--no-part");
                command.add("--no-playlist");
                command.add("--no-mtime");
                command.add(youtubeUrl);

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                // Consume process output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // yt-dlp progress output — can be logged if needed
                    }
                }

                int exitCode = process.waitFor();

                // Find the downloaded file (extension may vary: m4a, webm, opus, etc.)
                File downloadedFile = findDownloadedFile(finalNewFileName);

                if (exitCode != 0 || downloadedFile == null) {
                    Bukkit.getConsoleSender().sendMessage(convert("&cERROR downloading %s via yt-dlp (exit code: %d)".formatted(finalNewFileName, exitCode)));
                    callback.accept(null);
                    return;
                }

                Bukkit.getConsoleSender().sendMessage(convert("&fYouTube video &b%s&f has finished downloading!").formatted(finalNewFileName));
                Bukkit.getConsoleSender().sendMessage(convert("&fAttempting to convert &b%s&f to OGG file format...").formatted(downloadedFile.getPath()));

                if (ffmpegDownloader.isDownloading()) {
                    Bukkit.getConsoleSender().sendMessage(convert("&fPausing conversion of file &b%s&f as FFMPEG is not initialized! (is it still installing?)\nThis task will automatically resume! This is not an error!"));
                    queuedOGGConversions.add(new FFTask(downloadedFile, oggFile, callback));
                } else {
                    ffExecutor.convertToOgg(downloadedFile, oggFile, () -> {
                        Bukkit.getConsoleSender().sendMessage(convert("&aSuccessfully &fconverted file &b%s&f! Cleaning up...").formatted(finalNewFileName));
                        callback.accept(downloadedFile);
                    });
                }

            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(convert("&cERROR WITH %s".formatted(finalNewFileName)));
                e.printStackTrace();
                callback.accept(null);
            }
        }, MAIN_THREAD_POOL);
    }

    /**
     * Gets information about a YouTube video using yt-dlp's JSON output.
     *
     * @param youtubeLink a full YouTube video link
     * @param callback    a callback to accept the video info (or null on error)
     */
    public final void getVideoInformation(final String youtubeLink, final Consumer<YtDlpVideoInfo> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                if (this.ytDlpDownloader.isDownloading()) {
                    Bukkit.getConsoleSender().sendMessage(convert("&cyt-dlp is still installing, cannot fetch info for &b%s&f yet.".formatted(youtubeLink)));
                    callback.accept(null);
                    return;
                }

                final File ytDlpFile = this.ytDlpDownloader.getYtDlpFile();

                List<String> command = new ArrayList<>();
                command.add(ytDlpFile.getPath());
                command.add("-j");
                command.add("--no-download");
                command.add("--no-playlist");
                command.add(youtubeLink);

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(false);
                Process process = processBuilder.start();

                // Read stdout (JSON metadata)
                StringBuilder jsonOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonOutput.append(line);
                    }
                }

                // Read stderr for error messages
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode != 0 || jsonOutput.length() == 0) {
                    Bukkit.getConsoleSender().sendMessage(convert("&cERROR WITH &b%s".formatted(youtubeLink)));
                    Bukkit.getConsoleSender().sendMessage(convert("&cThe plugin will attempt to skip this song and continue loading!"));
                    if (errorOutput.length() > 0) {
                        Bukkit.getConsoleSender().sendMessage(convert("&cyt-dlp error: %s".formatted(errorOutput.toString().trim())));
                    }
                    Bukkit.getConsoleSender().sendMessage(convert("&cIf you encounter any issues, please try removing this song from songs.yml first!"));
                    callback.accept(null);
                    return;
                }

                JsonObject json = JsonParser.parseString(jsonOutput.toString()).getAsJsonObject();
                int duration = json.has("duration") ? json.get("duration").getAsInt() : 0;
                String title = json.has("title") ? json.get("title").getAsString() : "Unknown";

                Bukkit.getConsoleSender().sendMessage(convert("&fVideo information for song &b%s&f received!").formatted(youtubeLink));
                callback.accept(new YtDlpVideoInfo(duration, title, youtubeLink));

            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(convert("&cERROR WITH &b%s".formatted(youtubeLink)));
                Bukkit.getConsoleSender().sendMessage(convert("&cThe plugin will attempt to skip this song and continue loading!"));
                Bukkit.getConsoleSender().sendMessage(convert("&cIf you encounter any issues, please try removing this song from songs.yml first!"));
                e.printStackTrace();
                callback.accept(null);
            }
        }, MAIN_THREAD_POOL);
    }

    /**
     * Finds the downloaded audio file by name prefix in the m4a output directory.
     * yt-dlp may save as .webm, .m4a, .opus, etc. depending on the source.
     */
    private File findDownloadedFile(final String fileNamePrefix) {
        File[] files = this.m4aOutput.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().startsWith(fileNamePrefix + ".")) {
                return f;
            }
        }
        return null;
    }

    /**
     * Deletes all the files inside /media
     */
    public final void cleanupOutputs() {
        if (this.mediaOutput.delete()) Bukkit.getConsoleSender().sendMessage(convert("&aSuccessfully&f deleted /media outputs!"));
    }

    @EventHandler
    public void onFFMPEGInitialization(final FFMPEGInitializedEvent initializedEvent) {
        this.initializeFFMPEG();
    }

    protected void initializeFFMPEG() {
        if (IS_WINDOWS) {
            this.ffmpegFile = new File(MusePlusePlugin.INSTANCE.getDataFolder().getPath() + "\\ffmpeg\\bin\\ffmpeg.exe");
        } else {
            final File ffmpegForLinux = new File(MusePlusePlugin.INSTANCE.getDataFolder().getPath() + "/ffmpeg/ffmpeg");
            final File ffprobeForLinux = new File(MusePlusePlugin.INSTANCE.getDataFolder().getPath() + "/ffmpeg/ffprobe");
            ffmpegForLinux.setExecutable(true, false);
            ffprobeForLinux.setExecutable(true, false);
            this.ffmpegFile = ffmpegForLinux;
        }

        this.ffExecutor = new FFExecutor(this.ffmpegFile);
        Bukkit.getLogger().info(convert("&cFFMPEG Instance successfully set"));
        this.queuedOGGConversions.forEach((entry) -> {
            Bukkit.getConsoleSender().sendMessage(convert("Resuming OGG File conversion for &b%s&f".formatted(entry.to().getPath())));
            ffExecutor.convertToOgg(entry.from(), entry.to(), () -> {
                Bukkit.getConsoleSender().sendMessage(convert("&fSuccessfully converted file &b%s&f! Cleaning up...").formatted(entry.to().getPath()));
                entry.callback().accept(entry.to());
            });
        });
    }
}
