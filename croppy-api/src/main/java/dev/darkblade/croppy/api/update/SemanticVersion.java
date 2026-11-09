package dev.darkblade.croppy.api.update;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable Semantic Version representation adhering to SemVer 2.0.0 principles.
 * Supports comparison, parsing of {@code vX.Y.Z[-prerelease]} formats, and precedence checks.
 */
public record SemanticVersion(int major, int minor, int patch, Optional<String> preRelease) implements Comparable<SemanticVersion> {

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^[vV]?(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)(?:\\.(?<patch>0|[1-9]\\d*))?(?:-(?<prerelease>[0-9A-Za-z.-]+))?$"
    );

    public SemanticVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version numbers cannot be negative: " + major + "." + minor + "." + patch);
        }
        Objects.requireNonNull(preRelease, "preRelease optional cannot be null");
    }

    public static SemanticVersion of(int major, int minor, int patch) {
        return new SemanticVersion(major, minor, patch, Optional.empty());
    }

    public static SemanticVersion of(int major, int minor, int patch, String preRelease) {
        return new SemanticVersion(major, minor, patch, Optional.ofNullable(preRelease).filter(s -> !s.isBlank()));
    }

    /**
     * Parses a semantic version string.
     *
     * @param rawVersion the raw version string, optionally prefixed with 'v' or 'V'
     * @return the parsed SemanticVersion
     * @throws IllegalArgumentException if the format is invalid
     */
    public static SemanticVersion parse(String rawVersion) {
        Objects.requireNonNull(rawVersion, "rawVersion cannot be null");
        String trimmed = rawVersion.trim();
        Matcher matcher = SEMVER_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version format: '" + rawVersion + "'");
        }

        int major = Integer.parseInt(matcher.group("major"));
        int minor = Integer.parseInt(matcher.group("minor"));
        String patchGroup = matcher.group("patch");
        int patch = patchGroup != null ? Integer.parseInt(patchGroup) : 0;
        String pre = matcher.group("prerelease");

        return of(major, minor, patch, pre);
    }

    /**
     * Checks if this version is a pre-release build.
     *
     * @return true if pre-release identifier is present
     */
    public boolean isPreRelease() {
        return preRelease.isPresent();
    }

    /**
     * Evaluates if this version is strictly newer than the given version.
     *
     * @param other the version to compare against
     * @return true if this version has higher precedence than other
     */
    public boolean isNewerThan(SemanticVersion other) {
        Objects.requireNonNull(other, "other version cannot be null");
        return this.compareTo(other) > 0;
    }

    /**
     * Formats this version as a clean version string without leading 'v'.
     *
     * @return version string (e.g., "1.2.3" or "1.2.3-beta.1")
     */
    public String asString() {
        String base = major + "." + minor + "." + patch;
        return preRelease.map(s -> base + "-" + s).orElse(base);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        Objects.requireNonNull(other, "other version cannot be null");

        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        if (this.patch != other.patch) {
            return Integer.compare(this.patch, other.patch);
        }

        // SemVer 2.0.0 precedence: A normal version has higher precedence than a pre-release version
        // 1.0.0 > 1.0.0-alpha
        if (this.preRelease.isEmpty() && other.preRelease.isPresent()) {
            return 1;
        }
        if (this.preRelease.isPresent() && other.preRelease.isEmpty()) {
            return -1;
        }
        if (this.preRelease.isPresent() && other.preRelease.isPresent()) {
            return comparePreReleaseIdentifiers(this.preRelease.get(), other.preRelease.get());
        }

        return 0;
    }

    private static int comparePreReleaseIdentifiers(String preA, String preB) {
        if (preA.equals(preB)) {
            return 0;
        }
        String[] partsA = preA.split("\\.");
        String[] partsB = preB.split("\\.");
        int minLength = Math.min(partsA.length, partsB.length);

        for (int i = 0; i < minLength; i++) {
            String partA = partsA[i];
            String partB = partsB[i];

            boolean aIsNumeric = isNumeric(partA);
            boolean bIsNumeric = isNumeric(partB);

            if (aIsNumeric && bIsNumeric) {
                int cmp = Integer.compare(Integer.parseInt(partA), Integer.parseInt(partB));
                if (cmp != 0) return cmp;
            } else if (aIsNumeric) {
                // Numeric identifiers always have lower precedence than non-numeric identifiers
                return -1;
            } else if (bIsNumeric) {
                return 1;
            } else {
                int cmp = partA.compareTo(partB);
                if (cmp != 0) return cmp;
            }
        }

        // A larger set of pre-release fields has a higher precedence than a smaller set
        return Integer.compare(partsA.length, partsB.length);
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return asString();
    }
}
