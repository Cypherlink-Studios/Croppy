package dev.darkblade.croppy.api.resourcepack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Domain engine contract for compiling and merging resource pack assets.
 */
public interface ResourcePackCompiler {

    /**
     * Compiles and merges base companion pack assets with addon asset directories into a single ZIP archive.
     *
     * @param basePackPath root directory of the base companion resource pack
     * @param addonAssetSources collection of addon asset directory or archive paths
     * @param outputZipPath destination path of the output ZIP file
     * @return the build result containing file metadata, SHA-1 checksum, and timing
     * @throws IOException if any I/O error occurs during scanning or compression
     */
    PackBuildResult compile(Path basePackPath, Collection<Path> addonAssetSources, Path outputZipPath) throws IOException;
}
