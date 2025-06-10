package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Version;

import java.util.*;

public class VersionUtils {

    private VersionUtils() {
        throw new IllegalStateException("Utility class");
    }


    /**
     * Determines if the first version is newer than the second version.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return true if version1 is newer than version2, false otherwise
     */
    public static boolean isVersionNewer(Version version1, Version version2) {
        if (version1 == null || version2 == null) {
            return false;
        }

        // Compare by release date
        return version1.getReleaseDate().after(version2.getReleaseDate());
    }

    /**
     * Finds the latest (newest) version from a list of versions.
     *
     * @param versions the list of versions to search
     * @return the latest version
     */
    public static Version findLatestVersion(List<Version> versions) {
        return versions.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Version::getReleaseDate))
                .orElse(null);
    }

    public static Version getLatestVersion(List<Version> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        Version latest = versions.getFirst();
        for (Version version : versions) {
            if (version.getReleaseDate() != null &&
                    (latest.getReleaseDate() == null ||
                            version.getReleaseDate().after(latest.getReleaseDate()))) {
                latest = version;
            }
        }

        return latest;
    }

}
