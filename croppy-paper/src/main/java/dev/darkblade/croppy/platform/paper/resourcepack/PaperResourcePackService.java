package dev.darkblade.croppy.platform.paper.resourcepack;

import dev.darkblade.croppy.api.resourcepack.PackBuildResult;
import dev.darkblade.croppy.api.resourcepack.ResourcePackCompiler;
import dev.darkblade.croppy.api.resourcepack.ResourcePackService;
import dev.darkblade.croppy.core.resourcepack.DefaultResourcePackCompiler;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper platform implementation of {@link ResourcePackService} that manages asynchronous
 * compilation, local directory exports, the embedded HTTP server, and player delivery.
 */
public class PaperResourcePackService implements ResourcePackService, Listener {

    private final CroppyPlugin plugin;
    private final PluginConfig config;
    private final ResourcePackCompiler compiler;
    private final Logger logger;

    private final Path exportsDir;
    private final Path outputZipFile;
    private final Path basePackDir;
    private final Path addonsDir;

    private final AtomicReference<PackBuildResult> activePack = new AtomicReference<>();
    private EmbeddedPackHttpServer httpServer;

    public PaperResourcePackService(CroppyPlugin plugin, PluginConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.compiler = new DefaultResourcePackCompiler();
        this.logger = plugin.getLogger();

        Path dataPath = plugin.getDataFolder().toPath();
        this.exportsDir = dataPath.resolve("exports");
        this.outputZipFile = exportsDir.resolve("croppy-unified-pack.zip");
        this.addonsDir = dataPath.resolve("addons");

        // Primary base pack path in data folder, with fallback to root repository path if in development
        Path customBase = dataPath.resolve("resourcepack");
        if (Files.exists(customBase)) {
            this.basePackDir = customBase;
        } else {
            Path rootDevBase = Path.of("resourcepack");
            this.basePackDir = Files.exists(rootDevBase) ? rootDevBase : customBase;
        }
    }

    /**
     * Initializes the service, starting the embedded HTTP server and loading existing pack metadata.
     */
    public void start() {
        if (!Files.exists(exportsDir)) {
            try {
                Files.createDirectories(exportsDir);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create exports directory: " + e.getMessage(), e);
            }
        }

        // Check if an existing compiled pack is on disk
        if (Files.exists(outputZipFile)) {
            try {
                PackBuildResult loaded = computeExistingPackResult(outputZipFile);
                activePack.set(loaded);
                logger.info(String.format("Loaded existing companion resource pack (Size: %.2f MB, SHA-1: %s)",
                        loaded.sizeBytes() / (1024.0 * 1024.0), loaded.sha1Hex()));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to inspect existing resource pack archive: " + e.getMessage());
            }
        }

        // Start embedded HTTP server if enabled
        if (config.isResourcePackServerEnabled()) {
            this.httpServer = new EmbeddedPackHttpServer(
                    config.getResourcePackServerHost(),
                    config.getResourcePackServerPort(),
                    outputZipFile,
                    activePack::get,
                    logger
            );
            httpServer.start();
        }

        // Register join and status listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Gracefully stops the service and embedded HTTP server.
     */
    public void shutdown() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
    }

    @Override
    public CompletableFuture<PackBuildResult> buildPackAsync() {
        CompletableFuture<PackBuildResult> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<Path> addonSources = discoverAddonAssetSources();
                PackBuildResult result = compiler.compile(basePackDir, addonSources, outputZipFile);
                activePack.set(result);
                future.complete(result);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Failed to build unified companion resource pack: " + t.getMessage(), t);
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    @Override
    public Optional<PackBuildResult> getActivePack() {
        return Optional.ofNullable(activePack.get());
    }

    @Override
    public boolean sendPack(UUID playerUniqueId) {
        Player player = Bukkit.getPlayer(playerUniqueId);
        if (player == null || !player.isOnline()) {
            return false;
        }

        PackBuildResult pack = activePack.get();
        if (pack == null) {
            return false;
        }

        String url = resolvePublicUrl();
        if (url == null || url.isBlank()) {
            return false;
        }

        Component promptComponent = MiniMessage.miniMessage().deserialize(config.getResourcePackPrompt());
        boolean required = config.isResourcePackRequired();

        player.setResourcePack(url, pack.sha1Hex(), required, promptComponent);
        return true;
    }

    @Override
    public int sendPackToAll() {
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (sendPack(online.getUniqueId())) {
                count++;
            }
        }
        return count;
    }

    public String resolvePublicUrl() {
        String configuredUrl = config.getResourcePackPublicUrl();
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl.trim();
        }

        if (httpServer != null && httpServer.isRunning()) {
            String host = httpServer.getHost();
            if (host.equals("0.0.0.0") || host.isBlank()) {
                try {
                    host = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception e) {
                    host = "127.0.0.1";
                }
            }
            return "http://" + host + ":" + httpServer.getPort() + "/pack/croppy-unified-pack.zip";
        }

        return null;
    }

    private List<Path> discoverAddonAssetSources() {
        List<Path> sources = new ArrayList<>();
        if (!Files.exists(addonsDir) || !Files.isDirectory(addonsDir)) {
            return sources;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(addonsDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    sources.add(entry);
                } else if (Files.isRegularFile(entry)) {
                    String name = entry.getFileName().toString().toLowerCase();
                    if (name.endsWith(".jar") || name.endsWith(".zip")) {
                        sources.add(entry);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error scanning addons directory for assets: " + e.getMessage());
        }

        return sources;
    }

    private PackBuildResult computeExistingPackResult(Path zipPath) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        long size = Files.size(zipPath);

        byte[] buffer = new byte[8192];
        try (InputStream fis = Files.newInputStream(zipPath);
             DigestInputStream dis = new DigestInputStream(fis, sha1)) {
            while (dis.read(buffer) != -1) {
                // stream through to compute digest
            }
        }

        byte[] digest = sha1.digest();
        StringBuilder sb = new StringBuilder(40);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }

        return new PackBuildResult(zipPath, sb.toString(), digest, size, 0, 0L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isResourcePackAutoSendOnJoin()) {
            return;
        }

        // Delay prompt by 20 ticks (1 second) to allow world chunk sync
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                sendPack(event.getPlayer().getUniqueId());
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        if (status == PlayerResourcePackStatusEvent.Status.DECLINED) {
            logger.info(String.format("Player %s declined companion resource pack download.", player.getName()));
        } else if (status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            logger.warning(String.format("Player %s failed to download companion resource pack.", player.getName()));
        } else if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            logger.fine(String.format("Player %s successfully applied companion resource pack.", player.getName()));
        }
    }
}
