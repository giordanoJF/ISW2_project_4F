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

    /**
     * Finds the oldest version with a release date.
     *
     * @param versions The list of versions to search
     * @return The oldest version with a release date, or null if none found
     */
    public static Version findOldestVersionWithReleaseDate(List<Version> versions) {
        Version oldestVersion = null;

        // Find the first version with a release date
        for (Version version : versions) {
            if (version != null && version.getReleaseDate() != null) {
                oldestVersion = version;
                break;
            }
        }

        // Find the oldest version
        if (oldestVersion != null) {
            for (Version version : versions) {
                if (version != null && version.getReleaseDate() != null &&
                        version.getReleaseDate().before(oldestVersion.getReleaseDate())) {
                    oldestVersion = version;
                }
            }
        }

        return oldestVersion;
    }

    /**
     * Crea una mappa di nomi di versione su oggetti Version.
     *
     * @param versions Lista di oggetti Version da mappare
     * @return Mappa con nomi di versione come chiavi e oggetti Version come valori
     */
    public static Map<String, Version> createVersionMap(List<Version> versions) {
        Map<String, Version> versionMap = new HashMap<>();

        if (versions == null || versions.isEmpty()) {
            return versionMap;
        }

        for (Version version : versions) {
            if (version != null && version.getName() != null && !version.getName().isEmpty()) {
                versionMap.put(version.getName(), version);
            }
        }

        return versionMap;
    }
}
