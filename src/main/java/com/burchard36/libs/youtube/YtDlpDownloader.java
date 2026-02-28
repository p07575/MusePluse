package com.burchard36.libs.youtube;

import com.burchard36.musepluse.MusePlusePlugin;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.burchard36.libs.utils.StringUtils.convert;

public class YtDlpDownloader {

    protected final URL windowsDownloadLink;
    protected final URL linuxDownloadLink;
    protected final URL linuxArm64DownloadLink;
    protected final URL macDownloadLink;
    protected final File ytDlpInstallationDirectory;
    @Getter
    protected final AtomicBoolean downloading = new AtomicBoolean(false);

    public YtDlpDownloader(final MusePlusePlugin plugin) {
        try {
            this.windowsDownloadLink = new URL("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe");
            this.linuxDownloadLink = new URL("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux");
            this.linuxArm64DownloadLink = new URL("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64");
            this.macDownloadLink = new URL("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos");
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }

        this.ytDlpInstallationDirectory = new File(plugin.getDataFolder(), "/yt-dlp");
        if (!this.isInstalled()) {
            this.ytDlpInstallationDirectory.mkdirs();
            Bukkit.getConsoleSender().sendMessage(convert("&cyt-dlp was detected as not installed on this server"));
            Bukkit.getConsoleSender().sendMessage(convert("&cAsynchronous installation will now begin..."));
            Bukkit.getConsoleSender().sendMessage(convert("&cThis will be the only time your server will need to do this!"));
            this.downloading.set(true);
        }
    }

    public final void installYtDlp() {
        if (this.isInstalled()) {
            this.downloading.set(false);
            Bukkit.getConsoleSender().sendMessage(convert("&fyt-dlp was detected as installed on this server!"));
            return;
        }
        this.downloading.set(true);
        Bukkit.getConsoleSender().sendMessage(convert("&fAttempting to download yt-dlp..."));
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) this.getURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);

                final String fileName = MusePlusePlugin.IS_WINDOWS ? "yt-dlp.exe" : "yt-dlp";
                final File outputFile = new File(this.ytDlpInstallationDirectory, fileName);

                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                if (!MusePlusePlugin.IS_WINDOWS) {
                    outputFile.setExecutable(true, false);
                }

                Bukkit.getConsoleSender().sendMessage(convert("&ayt-dlp has been successfully installed!"));
                this.downloading.set(false);
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(convert("&cFailed to download yt-dlp: " + e.getMessage()));
                this.downloading.set(false);
                throw new RuntimeException(e);
            }
        }, MusePlusePlugin.MAIN_THREAD_POOL);
    }

    public boolean isDownloading() {
        return this.downloading.get();
    }

    public URL getURL() {
        if (MusePlusePlugin.IS_WINDOWS) return this.windowsDownloadLink;
        if (MusePlusePlugin.IS_MAC) return this.macDownloadLink;
        if (MusePlusePlugin.IS_AARCH_64 || MusePlusePlugin.IS_ARM_64) {
            return this.linuxArm64DownloadLink;
        }
        return this.linuxDownloadLink;
    }

    public final boolean isInstalled() {
        return this.getYtDlpFile().exists();
    }

    public final File getYtDlpFile() {
        if (MusePlusePlugin.IS_WINDOWS) {
            return new File(this.ytDlpInstallationDirectory, "yt-dlp.exe");
        } else {
            return new File(this.ytDlpInstallationDirectory, "yt-dlp");
        }
    }
}
