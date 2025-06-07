package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

import static it.giordano.isw_project.service.JiraService.JIRA_BASE_URL;
import static it.giordano.isw_project.service.JiraService.MAX_RESULTS_PER_PAGE;

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

    /**
     * Checks if the fixed version is invalid when compared with affected versions.
     *
     * @param ticket the ticket to check
     * @param fixedVersion the fixed version to compare
     * @return true if the ticket should be removed, false otherwise
     */
    public static boolean isFixedVersionInvalidWithAffected(Ticket ticket, Version fixedVersion) {
        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
            return false;
        }

        for (Version affectedVersion : ticket.getAffectedVersions()) {
            if (!isVersionNewer(fixedVersion, affectedVersion)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if a ticket should be removed based on validation rules.
     *
     * @param ticket the ticket to validate
     * @return true if the ticket should be removed, false otherwise
     */
    public static boolean shouldRemoveTicket(Ticket ticket) {
        // Check for missing opening version
        if (ticket.getOpeningVersion() == null) {
            return true;
        }

        // Check for missing fixed versions
        if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()) {
            return true;
        }

        // Get fixed version (we already ensured there's only one after normalization)
        Version fixedVersion = ticket.getFixedVersions().getFirst();

        // Check if opening version > fixed version
        if (isVersionNewer(ticket.getOpeningVersion(), fixedVersion)) {
            return true;
        }

//        // Check for missing injected version AFTER proportion calculation
//        if (ticket.getInjectedVersion() == null) {
//            return true;
//        }
//
//        // Check for missing affected versions AFTER proportion calculation
//        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
//            return true;
//        }

        // Check if fixed version <= any affected version
        if (isFixedVersionInvalidWithAffected(ticket, fixedVersion)) {
            return true;
        }

        // Check if injected version >= fixed version
        if (ticket.getInjectedVersion() != null &&
                isVersionNewer(ticket.getInjectedVersion(), fixedVersion)) {
            return true;
        }

        // Check if opening version < injected version
        if ( isVersionNewer(ticket.getInjectedVersion(), ticket.getOpeningVersion()) ) {
            return true;
        }

        // check if every av is >= IV and < FV
        if (ticket.getAffectedVersions() != null && !ticket.getAffectedVersions().isEmpty()) {
            for (Version av : ticket.getAffectedVersions()) {
                if (av.getReleaseDate() != null && fixedVersion.getReleaseDate() != null) {
                    if (av.getReleaseDate().after(fixedVersion.getReleaseDate()) || av.getReleaseDate().before(ticket.getInjectedVersion().getReleaseDate())) {
                        return true;
                    }
                }
            }
        }

        // Check if injected = fixed, we skip it because the bug is never produced
        if (ticket.getInjectedVersion() != null && ticket.getFixedVersions() != null) {
            if (ticket.getInjectedVersion().getReleaseDate().equals(fixedVersion.getReleaseDate())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the injected version is missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if the injected version is missing, false otherwise
     */
    public static boolean isInjectedVersionMissing(Ticket ticket) {
        return ticket.getInjectedVersion() == null ||
                (ticket.getInjectedVersion().getName() == null ||
                        ticket.getInjectedVersion().getName().isEmpty());
    }

    /**
     * Checks if the opening version is missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if the opening version is missing, false otherwise
     */
    public static boolean isOpeningVersionMissing(Ticket ticket) {
        return ticket.getOpeningVersion() == null ||
                (ticket.getOpeningVersion().getName() == null ||
                        ticket.getOpeningVersion().getName().isEmpty());
    }

    /**
     * Checks if affected versions are missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if affected versions are missing, false otherwise
     */
    public static boolean isAffectedVersionsMissing(Ticket ticket) {
        return ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty();
    }

    /**
     * Checks if fixed versions are missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if fixed versions are missing, false otherwise
     */
    public static boolean isFixedVersionsMissing(Ticket ticket) {
        return ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty();
    }

    public static boolean hasConsistentVersions(Ticket ticket) {
        Version lv = getLatestVersion(ticket.getFixedVersions());
        if (lv != null && lv.getReleaseDate() != null &&
                ticket.getOpeningVersion().getReleaseDate() != null &&
                ticket.getInjectedVersion().getReleaseDate() != null) {

            return lv.getReleaseDate().after(ticket.getOpeningVersion().getReleaseDate())
                    && lv.getReleaseDate().after(ticket.getInjectedVersion().getReleaseDate())
                    && (ticket.getInjectedVersion().getReleaseDate().before(ticket.getOpeningVersion().getReleaseDate())
                    || ticket.getInjectedVersion().getReleaseDate().equals(ticket.getOpeningVersion().getReleaseDate()));
        }
        else {
            return false;
        }
    }

    public static boolean hasRequiredVersions(Ticket ticket) {
        return ticket.getFixedVersions() != null && !ticket.getFixedVersions().isEmpty() &&
                ticket.getInjectedVersion() != null &&
                ticket.getOpeningVersion() != null;
    }

    /**
     * Analyzes tickets to check if those with null IV/AV exactly match those with unsuitablePredictedIV set to true.
     *
     * @param tickets List of tickets to analyze
     * @return true if the set of tickets with null IV/AV exactly matches the set of tickets with unsuitablePredictedIV=true
     */
    public static boolean logUnsuitableTicketsConsistency(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            Consistency.LOGGER.warning("No tickets provided for analysis");
            return false;
        }

        List<Ticket> ticketsWithNullVersions = new ArrayList<>();
        List<Ticket> ticketsWithUnsuitablePredictedIV = new ArrayList<>();

        // Analyze tickets and populate lists
        for (Ticket ticket : tickets) {
            boolean hasNullVersions = ticket.getInjectedVersion() == null ||
                    (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty());

            if (hasNullVersions) {
                ticketsWithNullVersions.add(ticket);
            }

            if (Boolean.TRUE.equals(ticket.getUnsuitablePredictedIV())) {
                ticketsWithUnsuitablePredictedIV.add(ticket);
            }
        }

        // Log results
//        LOGGER.info("Total tickets analyzed: " + tickets.size());
        Consistency.LOGGER.info("Tickets with null IV or AV: " + ticketsWithNullVersions.size());
        Consistency.LOGGER.info("Tickets with unsuitablePredictedIV=true: " + ticketsWithUnsuitablePredictedIV.size());

        // Check if the two sets have the same size
        if (ticketsWithNullVersions.size() != ticketsWithUnsuitablePredictedIV.size()) {
            Consistency.LOGGER.warning("Sets have different sizes. Cannot be exactly the same.");
            return false;
        }

        // Check if all tickets with null versions are in the unsuitable set
        for (Ticket nullVersionTicket : ticketsWithNullVersions) {
            boolean found = false;
            for (Ticket unsuitableTicket : ticketsWithUnsuitablePredictedIV) {
                if (nullVersionTicket.getKey().equals(unsuitableTicket.getKey())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Consistency.LOGGER.warning("Ticket " + nullVersionTicket.getKey() +
                        " has null IV/AV but is not marked as unsuitable");
                return false;
            }
        }

        // Check if all unsuitable tickets have null versions
        for (Ticket unsuitableTicket : ticketsWithUnsuitablePredictedIV) {
            boolean found = false;
            for (Ticket nullVersionTicket : ticketsWithNullVersions) {
                if (unsuitableTicket.getKey().equals(nullVersionTicket.getKey())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Consistency.LOGGER.warning("Ticket " + unsuitableTicket.getKey() +
                        " is marked as unsuitable but has non-null IV/AV");
                return false;
            }
        }

        Consistency.LOGGER.info("The sets of tickets with null IV/AV and unsuitable predicted IV are exactly the same.");
        return true;
    }

    /**
     * Builds the JQL query for retrieving tickets.
     *
     * @param projectKey The project key
     * @return JQL query string
     */
    public static String buildJqlQuery(String projectKey) {
        return "project=" + projectKey +
                " AND issuetype=Bug" +
                " AND resolution=Fixed" +
                " AND status in (Closed, Resolved)";
    }

    /**
     * Builds the URL for the tickets search API.
     *
     * @param encodedJql The encoded JQL query
     * @param startAt The pagination start index
     * @return The URL for the tickets search API
     */
    public static String buildTicketsUrl(String encodedJql, int startAt) {
        return JIRA_BASE_URL + "/search?jql=" + encodedJql +
                "&startAt=" + startAt +
                "&maxResults=" + MAX_RESULTS_PER_PAGE +
                "&fields=key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
    }

    /**
     * Esegue una richiesta GET all'URL specificato.
     *
     * @param url L'URL a cui inviare la richiesta GET
     * @return La risposta come stringa
     * @throws IOException Se si verifica un errore durante la richiesta HTTP
     */
    public static String executeGetRequest(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    UrlRequests.LOGGER.log(Level.WARNING, "HTTP request failed with status: {0}", statusCode);
                    return null;
                }

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return null;
                }

                return EntityUtils.toString(entity);
            }
        }
    }
}
