package dev.darkblade.croppy.core.resourcepack;

import dev.darkblade.croppy.api.resourcepack.PackBuildResult;
import dev.darkblade.croppy.api.resourcepack.ResourcePackCompiler;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Pure Java 21 implementation of {@link ResourcePackCompiler} that merges base companion pack assets
 * and discovered addon assets into an optimized, checksummed ZIP archive.
 */
public class DefaultResourcePackCompiler implements ResourcePackCompiler {

    private static final Logger LOGGER = Logger.getLogger(DefaultResourcePackCompiler.class.getName());
    private static final Pattern VALID_PATH_SEGMENT = Pattern.compile("^[a-z0-9_.-]+$");

    @Override
    public PackBuildResult compile(Path basePackPath, Collection<Path> addonAssetSources, Path outputZipPath) throws IOException {
        Objects.requireNonNull(basePackPath, "basePackPath cannot be null");
        Objects.requireNonNull(addonAssetSources, "addonAssetSources cannot be null");
        Objects.requireNonNull(outputZipPath, "outputZipPath cannot be null");

        long startTime = System.currentTimeMillis();

        Path parentDir = outputZipPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Path tempFile = (parentDir != null)
                ? Files.createTempFile(parentDir, "pack-", ".tmp")
                : Files.createTempFile("pack-", ".tmp");

        MessageDigest sha1Digest;
        try {
            sha1Digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 digest algorithm not available in current JVM", e);
        }

        Set<String> addedEntries = new HashSet<>();
        int assetCount = 0;

        try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DigestOutputStream dos = new DigestOutputStream(bos, sha1Digest);
             ZipOutputStream zos = new ZipOutputStream(dos)) {

            zos.setLevel(Deflater.DEFAULT_COMPRESSION);

            // 1. Merge Base Companion Pack
            if (Files.exists(basePackPath)) {
                assetCount += mergeDirectorySource(basePackPath, "", zos, addedEntries);
            }

            // 2. Merge Addon Asset Sources (Directories or JAR archives)
            for (Path addonSource : addonAssetSources) {
                if (!Files.exists(addonSource)) {
                    continue;
                }

                if (Files.isDirectory(addonSource)) {
                    Path assetsSubdir = addonSource.resolve("assets");
                    if (Files.isDirectory(assetsSubdir)) {
                        assetCount += mergeDirectorySource(assetsSubdir, "assets/", zos, addedEntries);
                    } else if (addonSource.getFileName() != null && addonSource.getFileName().toString().equals("assets")) {
                        assetCount += mergeDirectorySource(addonSource, "assets/", zos, addedEntries);
                    } else {
                        assetCount += mergeDirectorySource(addonSource, "", zos, addedEntries);
                    }
                } else if (Files.isRegularFile(addonSource) && isArchiveFile(addonSource)) {
                    assetCount += mergeArchiveSource(addonSource, zos, addedEntries);
                }
            }
        }

        // Calculate 40-character hex string of SHA-1
        byte[] digestBytes = sha1Digest.digest();
        StringBuilder hexBuilder = new StringBuilder(40);
        for (byte b : digestBytes) {
            hexBuilder.append(String.format("%02x", b));
        }
        String sha1Hex = hexBuilder.toString();

        // Atomically replace target destination file
        try {
            Files.move(tempFile, outputZipPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Fallback for filesystems that do not support atomic moves across volumes
            Files.move(tempFile, outputZipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long durationMs = Math.max(0, System.currentTimeMillis() - startTime);
        long sizeBytes = Files.size(outputZipPath);

        return new PackBuildResult(outputZipPath, sha1Hex, digestBytes, sizeBytes, assetCount, durationMs);
    }

    private int mergeDirectorySource(Path rootDir, String prefix, ZipOutputStream zos, Set<String> addedEntries) throws IOException {
        int[] count = new int[]{0};

        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = rootDir.relativize(file).toString().replace('\\', '/');
                String entryPath = prefix.isEmpty() ? relative : (prefix.endsWith("/") ? prefix + relative : prefix + "/" + relative);

                if (!isValidMinecraftPath(entryPath)) {
                    LOGGER.log(Level.WARNING, "Skipping invalid resource pack asset path: {0}", entryPath);
                    return FileVisitResult.CONTINUE;
                }

                if (addedEntries.add(entryPath)) {
                    ZipEntry entry = new ZipEntry(entryPath);
                    entry.setTime(attrs.lastModifiedTime().toMillis());
                    zos.putNextEntry(entry);
                    try (InputStream fis = Files.newInputStream(file)) {
                        fis.transferTo(zos);
                    }
                    zos.closeEntry();
                    count[0]++;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return count[0];
    }

    private int mergeArchiveSource(Path archivePath, ZipOutputStream zos, Set<String> addedEntries) throws IOException {
        int count = 0;

        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName().replace('\\', '/');

                if (entry.isDirectory() || !entryName.startsWith("assets/")) {
                    continue;
                }

                if (!isValidMinecraftPath(entryName)) {
                    LOGGER.log(Level.WARNING, "Skipping invalid asset path in archive {0}: {1}", new Object[]{archivePath, entryName});
                    continue;
                }

                if (addedEntries.add(entryName)) {
                    ZipEntry newEntry = new ZipEntry(entryName);
                    newEntry.setTime(entry.getTime() > 0 ? entry.getTime() : System.currentTimeMillis());
                    zos.putNextEntry(newEntry);
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        is.transferTo(zos);
                    }
                    zos.closeEntry();
                    count++;
                }
            }
        }

        return count;
    }

    private boolean isArchiveFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip");
    }

    /**
     * Validates that each path segment inside `assets/<namespace>/...` matches `^[a-z0-9_.-]+$`.
     */
    public static boolean isValidMinecraftPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        // pack.mcmeta and pack.png in root are valid
        if (path.equals("pack.mcmeta") || path.equals("pack.png")) {
            return true;
        }

        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.isEmpty() || !VALID_PATH_SEGMENT.matcher(segment).matches()) {
                return false;
            }
        }
        return true;
    }
}
