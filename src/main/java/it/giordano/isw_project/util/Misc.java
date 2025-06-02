package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Misc {

    private Misc(){
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

    /**
     * Calculates a percentage.
     *
     * @param part The part
     * @param total The total
     * @return The percentage
     */
    public static double calculatePercentage(int part, int total) {
        return part * 100.0 / total;
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

    public static boolean hasZeroDenominator(Ticket ticket) {
        Version lv = Misc.getLatestVersion(ticket.getFixedVersions());
        if (lv != null && lv.getReleaseDate() != null && ticket.getOpeningVersion().getReleaseDate() != null) {
            return lv.getReleaseDate().equals(ticket.getOpeningVersion().getReleaseDate());
        }
        else {
            return false;
        }
    }

    /**
     * Analizza una stringa di data utilizzando il formato di data fornito.
     *
     * @param dateString La stringa di data da analizzare
     * @param dateFormat Il formato di data da utilizzare
     * @return Oggetto Date analizzato dalla stringa di data Jira, o null se la stringa di data è null o vuota
     * @throws ParseException Se la stringa di data non può essere analizzata
     */
    public static Date parseDate(String dateString, SimpleDateFormat dateFormat) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return dateFormat.parse(dateString);
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
