package it.giordano.isw_project.utils;

import it.giordano.isw_project.models.Version;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.logging.Logger;

public final class VersionUtils {

    @Nonnull private static final Logger LOGGER = Objects.requireNonNull(Logger.getLogger(VersionUtils.class.getName()));

    private VersionUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Finds the most recent version from a list of versions.
     *
     * <p>Used to determine the most recent fixed version for a ticket.</p>
     *
     * @param versions the list of versions to search
     * @return the most recent Version object, or null if the list is empty
     */
    @Nullable
    public static Version findNewestVersionFromVersions(@Nullable List<Version> versions) {
        if (versions == null){
            throw new IllegalArgumentException("versions cannot be null");
        }

        if (versions.isEmpty()) {
            LOGGER.warning("The list of versions is empty. Returning null.");
            return null;
        }

        Version mostRecent = null;
        for (Version version : versions) {
            if (version == null || version.getReleaseDate() == null) {
                continue; // Skip null versions or versions without a release date
            }
            if (mostRecent == null || (version.getReleaseDate() != null &&
                    (mostRecent.getReleaseDate() == null || version.getReleaseDate().after(mostRecent.getReleaseDate())))) {
                mostRecent = version;
            }
        }
        return mostRecent;
    }

    /**
     * Finds the oldest version with a release date from a list of versions.
     *
     * <p>This method is used to determine the injected version for tickets,
     * which is typically the oldest affected version that has a release date.</p>
     *
     * @param versions the list of versions to search
     * @return the oldest version with a release date, or null if none found
     * @throws IllegalArgumentException if versions is null
     *
     * TODO: Could be splitted into two methods.
     */
    @Nullable
    public static Version findOldestVersionFromVersions(@Nullable List<Version> versions) {
        if (versions == null) {
            throw new IllegalArgumentException("Versions list cannot be null or empty");
        }
        if (versions.isEmpty()) {
            LOGGER.warning("The list of versions is empty. Returning null.");
            return null;
        }

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
     * Checks if a version is valid for being an opening version.
     *
     * <p>A version is valid for opening if it has a release date and
     * was released before or on the ticket creation date.</p>
     *
     * @param version the version to check
     * @param createdDate the ticket creation date
     * @return true if the version is valid for opening, false otherwise
     * @throws IllegalArgumentException if createdDate is null
     */
    public static boolean isValidVersionForOpening(@Nullable Version version, @Nullable Date createdDate) {
        if (createdDate == null) {
            throw new IllegalArgumentException("Created date cannot be null");
        }
        return version != null &&
                version.getReleaseDate() != null &&
                !version.getReleaseDate().after(createdDate);
    }

    /**
     * Checks if a version is more recent than the current latest version.
     *
     * <p>Used to find the most recent valid opening version for a ticket.</p>
     *
     * @param version the version to check
     * @param currentLatest the current latest version to compare against
     * @return true if the version is more recent, false otherwise
     * @throws IllegalArgumentException if version is null
     */
    public static boolean isMoreRecentThanCurrent(@Nullable Version version, @Nullable Version currentLatest) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        if (version.getReleaseDate() == null) {
            LOGGER.warning("Version has no release date, cannot be more recent than current latest.");
            return false;
        }
        return currentLatest == null ||
                (currentLatest.getReleaseDate() != null &&
                        version.getReleaseDate().after(currentLatest.getReleaseDate()));
    }

    /**
     * Creates a map of versions indexed by their names.
     *
     * <p>This utility method creates a lookup map for quick version retrieval
     * by name during ticket processing.</p>
     *
     * @param versions the list of versions to map
     * @return a map with version names as keys and Version objects as values
     * @throws IllegalArgumentException if versions is null
     */
    @Nonnull
    public static Map<String, Version> createVersionMap(@Nullable List<Version> versions) {
        Map<String, Version> versionMap = new HashMap<>();
        if (versions == null) {
            throw new IllegalArgumentException("Versions list cannot be null");
        }
        if (versions.isEmpty()) {
            LOGGER.warning("The list of versions is empty. Returning an empty map.");
            return versionMap;
        }

        for (Version version : versions) {
            if (version != null && !(version.getName() == null || version.getName().trim().isEmpty())) {
                versionMap.put(version.getName(), version);
            }
        }

        if (versionMap.isEmpty()) {
            LOGGER.warning("No valid versions found in the list. Returning an empty map.");
        }

        return versionMap;
    }
}
