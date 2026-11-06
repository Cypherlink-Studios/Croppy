package dev.darkblade.croppy.api.addon;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable metadata description of an addon parsed from its addon.yml manifest.
 *
 * @param id unique identifier of the addon (lowercase alphanumeric, dashes, underscores)
 * @param name human-readable display name
 * @param version semantic version string
 * @param namespace primary resource namespace owned by this addon
 * @param main optional fully-qualified class name of the CroppyAddon entrypoint (empty for declarative packs)
 * @param authors list of author names
 * @param description brief summary of the addon's purpose
 * @param dependencies list of required addon IDs that must be loaded and enabled before this addon
 * @param softDependencies list of optional addon IDs loaded before this addon if present
 */
public record AddonDescription(
        String id,
        String name,
        String version,
        String namespace,
        Optional<String> main,
        List<String> authors,
        String description,
        List<String> dependencies,
        List<String> softDependencies
) {
    public AddonDescription {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(main, "main cannot be null");
        authors = authors == null ? List.of() : List.copyOf(authors);
        description = description == null ? "" : description;
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        softDependencies = softDependencies == null ? List.of() : List.copyOf(softDependencies);
    }

    /**
     * @return true if this addon contains a Java code entrypoint, false if it is a declarative content pack
     */
    public boolean isCodeAddon() {
        return main.isPresent() && !main.get().isBlank();
    }
}
