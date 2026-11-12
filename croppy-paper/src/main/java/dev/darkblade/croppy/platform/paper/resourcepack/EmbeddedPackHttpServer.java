package dev.darkblade.croppy.platform.paper.resourcepack;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.resourcepack.PackBuildResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Lightweight embedded HTTP server using standard {@link com.sun.net.httpserver.HttpServer}
 * to serve the compiled unified companion resource pack directly to Minecraft clients.
 */
public class EmbeddedPackHttpServer {

    private final String host;
    private final int port;
    private final Path packFilePath;
    private final Supplier<PackBuildResult> activePackSupplier;
    private final CroppyLogger logger;

    private HttpServer server;
    private ExecutorService executor;
    private boolean isRunning = false;

    public EmbeddedPackHttpServer(
            String host,
            int port,
            Path packFilePath,
            Supplier<PackBuildResult> activePackSupplier,
            Logger logger
    ) {
        this(
                host,
                port,
                packFilePath,
                activePackSupplier,
                new JulCroppyLogger(Objects.requireNonNull(logger, "logger cannot be null"))
        );
    }

    public EmbeddedPackHttpServer(
            String host,
            int port,
            Path packFilePath,
            Supplier<PackBuildResult> activePackSupplier,
            CroppyLogger logger
    ) {
        this.host = (host != null && !host.isBlank()) ? host.trim() : "0.0.0.0";
        this.port = port;
        this.packFilePath = Objects.requireNonNull(packFilePath, "packFilePath cannot be null");
        this.activePackSupplier = Objects.requireNonNull(activePackSupplier, "activePackSupplier cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    /**
     * Starts the embedded HTTP server.
     */
    public synchronized boolean start() {
        if (isRunning) {
            return true;
        }

        try {
            InetSocketAddress address = (host.equals("0.0.0.0"))
                    ? new InetSocketAddress(port)
                    : new InetSocketAddress(host, port);

            server = HttpServer.create(address, 0);
            PackHandler handler = new PackHandler();
            server.createContext("/pack/croppy-unified-pack.zip", handler);
            server.createContext("/pack", handler);

            executor = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "croppy-pack-http-worker");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.start();
            isRunning = true;

            logger.debug(LogChannel.PACK, () ->
                    String.format("Croppy embedded Resource Pack HTTP server started on %s:%d", address.getHostString(), port));
            return true;
        } catch (BindException e) {
            logger.warn(LogChannel.PACK, String.format("Failed to bind embedded resource pack HTTP server to port %d (Address already in use). You can configure a different port in config.yml.", port));
            stop();
            return false;
        } catch (IOException e) {
            logger.warn(LogChannel.PACK, "Failed to start embedded resource pack HTTP server: " + e.getMessage(), e);
            stop();
            return false;
        }
    }

    /**
     * Stops the embedded HTTP server and releases worker threads.
     */
    public synchronized void stop() {
        isRunning = false;
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception ignored) {
            }
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    private class PackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            if (!Files.exists(packFilePath)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "application/zip");
            responseHeaders.set("Cache-Control", "no-cache, must-revalidate");

            PackBuildResult currentPack = activePackSupplier.get();
            if (currentPack != null && currentPack.sha1Hex() != null) {
                responseHeaders.set("ETag", "\"" + currentPack.sha1Hex() + "\"");
            }

            long fileLength = Files.size(packFilePath);

            if ("HEAD".equalsIgnoreCase(method)) {
                responseHeaders.set("Content-Length", String.valueOf(fileLength));
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            exchange.sendResponseHeaders(200, fileLength);
            try (InputStream inputStream = Files.newInputStream(packFilePath);
                 OutputStream outputStream = exchange.getResponseBody()) {
                inputStream.transferTo(outputStream);
            }
            exchange.close();
        }
    }
}
