package it.giordano.isw_project.service;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for interacting with Jira API.
 * Handles retrieving project versions and tickets.
 */
public class JiraService {
    private static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";
    private static final Logger LOGGER = Logger.getLogger(JiraService.class.getName());

    // Campi JSON per le versioni
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_RELEASED = "released";
    private static final String FIELD_ARCHIVED = "archived";
    private static final String FIELD_RELEASE_DATE = "releaseDate";

    // Campi JSON per i ticket
    private static final String FIELD_KEY = "key";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CREATED_DATE = "created";
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RESOLUTION = "resolution";
    private static final String FIELD_FIX_VERSIONS = "fixVersions";
    private static final String FIELD_VERSIONS = "versions"; // AV = Affected Versions

    // Costanti per la paginazione e struttura JSON
    private static final int MAX_RESULTS_PER_PAGE = 100;
    private static final String TOTAL = "total";
    private static final String ISSUES = "issues";
    private static final String FIELDS = "fields";

    // Formati delle date
    private static final String TICKET_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String VERSION_DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    // Private constructor to prevent instantiation
    private JiraService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Recupera tutte le versioni di un progetto da Jira.
     *
     * @param projectKey Il codice del progetto
     * @return Lista delle versioni del progetto. Può essere vuota se non ci sono versioni.
     * @throws IOException Se si verifica un errore durante la richiesta HTTP
     */
    public static List<Version> getProjectVersions(String projectKey) throws IOException {
        if (projectKey == null || projectKey.isEmpty()) {
            LOGGER.warning("Project key is null or empty");
            return new ArrayList<>();
        }

        List<Version> versions = new ArrayList<>();
        String url = JIRA_BASE_URL + "/project/" + projectKey + "/versions";
        String jsonResponse = executeGetRequest(url);

        if (jsonResponse == null || jsonResponse.isEmpty()) {
            LOGGER.warning("Empty response from Jira API for project versions");
            return versions;
        }

        JSONArray versionArray = new JSONArray(jsonResponse);
        for (int i = 0; i < versionArray.length(); i++) {
            JSONObject versionJson = versionArray.optJSONObject(i);
            if (versionJson != null) {
                Version version = parseVersionFromJson(versionJson);
                versions.add(version);
            }
        }

        if (versions.isEmpty()) {
            LOGGER.log(Level.INFO, "No versions found for project {0}", projectKey);
        } else {
            LOGGER.log(Level.INFO, "Retrieved {0} versions for project {1}", new Object[]{versions.size(), projectKey});
        }

        return versions;
    }

    /**
     * Converte un oggetto JSON in un oggetto Version.
     *
     * @param versionJson L'oggetto JSON contenente le informazioni sulla versione
     * @return Oggetto Version popolato con dati dal JSON. Non restituisce mai null.
     */
    private static Version parseVersionFromJson(JSONObject versionJson) {
        if (versionJson == null) {
            return new Version();
        }

        Version version = new Version();
        version.setId(versionJson.optString(FIELD_ID, ""));
        version.setName(versionJson.optString(FIELD_NAME, ""));
        version.setReleased(versionJson.optBoolean(FIELD_RELEASED, false));
        version.setArchived(versionJson.optBoolean(FIELD_ARCHIVED, false));

        String dateStr = versionJson.optString(FIELD_RELEASE_DATE, "");
        if (!dateStr.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(VERSION_DATE_FORMAT_PATTERN);
                version.setReleaseDate(parseDate(dateStr, dateFormat));
            } catch (ParseException e) {
                LOGGER.warning("Failed to parse release date for version " + version.getName() + ": " + dateStr);
                version.setReleaseDate(null);
            }
        } else {
            version.setReleaseDate(null);
        }

        return version;
    }

    /**
     * Recupera i ticket di bug da Jira che sono stati risolti e chiusi/risolti.
     *
     * @param projectKey Il codice del progetto
     * @return Lista di ticket che soddisfano i criteri
     * @throws IOException Se si verifica un errore durante la richiesta HTTP
     */
    public static List<Ticket> getProjectTickets(String projectKey) throws IOException {
        if (projectKey == null || projectKey.isEmpty()) {
            LOGGER.warning("Project key is null or empty");
            return new ArrayList<>();
        }

        List<Ticket> tickets = new ArrayList<>();
        List<Version> versions = getProjectVersions(projectKey);
        Map<String, Version> versionMap = createVersionMap(versions);

        if (versions.isEmpty()) {
            LOGGER.log(Level.WARNING, "No versions found for project {0}. Cannot retrieve tickets.", projectKey);
            return tickets;
        }

        String jql = buildJqlQuery(projectKey);
        String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
        tickets = fetchAllTickets(encodedJql, versionMap);

        logTicketStatistics(tickets);
        return tickets;
    }

    /**
     * Builds the JQL query for retrieving tickets.
     *
     * @param projectKey The project key
     * @return JQL query string
     */
    private static String buildJqlQuery(String projectKey) {
        return "project=" + projectKey +
                " AND issuetype=Bug" +
                " AND resolution=Fixed" +
                " AND status in (Closed, Resolved)";
    }

    /**
     * Fetches all tickets from Jira API with pagination.
     *
     * @param encodedJql The encoded JQL query
     * @param versionMap Map of version names to Version objects
     * @return List of tickets
     * @throws IOException If an HTTP error occurs
     */
    private static List<Ticket> fetchAllTickets(String encodedJql, Map<String, Version> versionMap) throws IOException {
        List<Ticket> tickets = new ArrayList<>();
        int startAt = 0;
        int total = 0;
        boolean firstPage = true;

        do {
            String url = buildTicketsUrl(encodedJql, startAt);
            String jsonResponse = executeGetRequest(url);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                LOGGER.warning("Empty response from Jira API for tickets search");
                break;
            }

            JSONObject responseObj = new JSONObject(jsonResponse);

            if (firstPage) {
                total = responseObj.optInt(TOTAL, 0);
                firstPage = false;
                LOGGER.log(Level.INFO, "Found {0} tickets", total);
            }

            JSONArray issuesArray = responseObj.optJSONArray(ISSUES);

            if (issuesArray != null) {
                processIssuesArray(issuesArray, tickets, versionMap);
            } else {
                LOGGER.warning("No issues array found in response");
            }

            startAt += MAX_RESULTS_PER_PAGE;
        } while (startAt < total);

        return tickets;
    }

    /**
     * Builds the URL for the tickets search API.
     *
     * @param encodedJql The encoded JQL query
     * @param startAt The pagination start index
     * @return The URL for the tickets search API
     */
    private static String buildTicketsUrl(String encodedJql, int startAt) {
        return JIRA_BASE_URL + "/search?jql=" + encodedJql +
                "&startAt=" + startAt +
                "&maxResults=" + MAX_RESULTS_PER_PAGE +
                "&fields=key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
    }

    /**
     * Processes the issues array and adds tickets to the list.
     *
     * @param issuesArray The JSON array of issues
     * @param tickets The list to add tickets to
     * @param versionMap Map of version names to Version objects
     */
    private static void processIssuesArray(JSONArray issuesArray, List<Ticket> tickets, Map<String, Version> versionMap) {
        for (int i = 0; i < issuesArray.length(); i++) {
            JSONObject issueJson = issuesArray.optJSONObject(i);
            if (issueJson != null) {
                Ticket ticket = parseTicket(issueJson, versionMap);
                tickets.add(ticket);
            }
        }
    }

    /**
     * Crea una mappa di nomi di versione su oggetti Version.
     *
     * @param versions Lista di oggetti Version da mappare
     * @return Mappa con nomi di versione come chiavi e oggetti Version come valori
     */
    private static Map<String, Version> createVersionMap(List<Version> versions) {
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
     * Analizza un problema JSON in un oggetto Ticket.
     *
     * @param issueJson L'oggetto JSON contenente i dati del ticket
     * @param versionMap Mappa di nomi di versione a oggetti Version
     * @return Un oggetto Ticket popolato con i dati del JSON
     */
    private static Ticket parseTicket(JSONObject issueJson, Map<String, Version> versionMap) {
        if (issueJson == null) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.setKey(issueJson.optString(FIELD_KEY, ""));
        
        JSONObject fields = issueJson.optJSONObject(FIELDS);
        if (fields == null) {
            LOGGER.warning("No fields found for ticket " + ticket.getKey());
            return ticket;
        }

        setBasicTicketFields(ticket, fields);
        setTicketDates(ticket, fields);
        setTicketStatusFields(ticket, fields);
        setTicketVersions(ticket, fields, versionMap);
        setDerivedVersions(ticket, versionMap);

        return ticket;
    }

    /**
     * Sets the basic fields of a ticket.
     *
     * @param ticket The ticket to update
     * @param fields The JSON object with ticket fields
     */
    private static void setBasicTicketFields(Ticket ticket, JSONObject fields) {
        ticket.setSummary(fields.optString(FIELD_SUMMARY, ""));
        ticket.setDescription(fields.optString(FIELD_DESCRIPTION, ""));
    }

    /**
     * Sets the date fields of a ticket.
     *
     * @param ticket The ticket to update
     * @param fields The JSON object with ticket fields
     */
    private static void setTicketDates(Ticket ticket, JSONObject fields) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(TICKET_DATE_FORMAT_PATTERN);
        
        String createdDateStr = fields.optString(FIELD_CREATED_DATE, "");
        if (!createdDateStr.isEmpty()) {
            try {
                ticket.setCreatedDate(parseDate(createdDateStr, dateFormat));
            } catch (ParseException e) {
                LOGGER.warning("Failed to parse created date for ticket " + ticket.getKey() + ": " + createdDateStr);
            }
        }

        String resolutionDateStr = fields.optString(FIELD_RESOLUTION_DATE, "");
        if (!resolutionDateStr.isEmpty()) {
            try {
                ticket.setResolutionDate(parseDate(resolutionDateStr, dateFormat));
            } catch (ParseException e) {
                LOGGER.warning("Failed to parse resolution date for ticket " + ticket.getKey() + ": " + resolutionDateStr);
            }
        }
    }

    /**
     * Sets the status fields of a ticket.
     *
     * @param ticket The ticket to update
     * @param fields The JSON object with ticket fields
     */
    private static void setTicketStatusFields(Ticket ticket, JSONObject fields) {
        JSONObject statusObj = fields.optJSONObject(FIELD_STATUS);
        if (statusObj != null) {
            ticket.setStatus(statusObj.optString(FIELD_NAME, ""));
        }

        JSONObject resolutionObj = fields.optJSONObject(FIELD_RESOLUTION);
        if (resolutionObj != null) {
            ticket.setResolution(resolutionObj.optString(FIELD_NAME, ""));
        }
    }

    /**
     * Imposta le versioni fisse e interessate di un ticket.
     *
     * @param ticket Il ticket da aggiornare
     * @param fields L'oggetto JSON con i campi del ticket
     * @param versionMap Mappa di nomi di versione a oggetti Version
     */
    private static void setTicketVersions(Ticket ticket, JSONObject fields, Map<String, Version> versionMap) {
        if (ticket == null || fields == null || versionMap == null || versionMap.isEmpty()) {
            return;
        }

        // Aggiungi versioni fisse
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_FIX_VERSIONS), versionMap, true);

        // Aggiungi versioni interessate
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_VERSIONS), versionMap, false);
    }

    /**
     * Adds versions from a JSON array to a ticket.
     *
     * @param ticket The ticket to update
     * @param versionsArray The JSON array of versions
     * @param versionMap Map of version names to Version objects
     * @param isFixVersion Whether the versions are fixed versions or affected versions
     */
    private static void addVersionsFromJsonArray(Ticket ticket, JSONArray versionsArray, 
                                                Map<String, Version> versionMap, boolean isFixVersion) {
        if (versionsArray == null) {
            return;
        }
        
        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionJson = versionsArray.optJSONObject(i);
            if (versionJson != null) {
                String versionName = versionJson.optString(FIELD_NAME, "");
                if (!versionName.isEmpty() && versionMap.containsKey(versionName)) {
                    if (isFixVersion) {
                        ticket.addFixedVersion(versionMap.get(versionName));
                    } else {
                        ticket.addAffectedVersion(versionMap.get(versionName));
                    }
                }
            }
        }
    }

    /**
     * Imposta le versioni derivate (versione iniettata e versione di apertura) di un ticket.
     *
     * @param ticket Il ticket da aggiornare
     * @param versionMap Mappa di nomi di versione a oggetti Version
     */
    private static void setDerivedVersions(Ticket ticket, Map<String, Version> versionMap) {
        if (ticket == null || versionMap == null || versionMap.isEmpty()) {
            return;
        }

        setInjectedVersion(ticket);
        setOpeningVersion(ticket, versionMap);
    }

    /**
     * Sets the injected version of a ticket.
     *
     * @param ticket The ticket to update
     */
    private static void setInjectedVersion(Ticket ticket) {
        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions == null || affectedVersions.isEmpty()) {
            return;
        }

        Version oldestVersion = findOldestVersionWithReleaseDate(affectedVersions);
        if (oldestVersion != null) {
            ticket.setInjectedVersion(oldestVersion);
        }
    }

    /**
     * Finds the oldest version with a release date.
     *
     * @param versions The list of versions to search
     * @return The oldest version with a release date, or null if none found
     */
    private static Version findOldestVersionWithReleaseDate(List<Version> versions) {
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
     * Sets the opening version of a ticket.
     *
     * @param ticket The ticket to update
     * @param versionMap Map of version names to Version objects
     */
    private static void setOpeningVersion(Ticket ticket, Map<String, Version> versionMap) {
        if (ticket.getCreatedDate() == null) {
            return;
        }

        Version latestVersion = null;

        for (Version version : versionMap.values()) {
            if (version != null &&
                    version.getReleaseDate() != null &&
                    !version.getReleaseDate().after(ticket.getCreatedDate()) &&
                    (latestVersion == null || version.getReleaseDate().after(latestVersion.getReleaseDate()))) {

                latestVersion = version;
            }
        }


        ticket.setOpeningVersion(latestVersion);
    }

    /**
     * Analizza una stringa di data utilizzando il formato di data fornito.
     *
     * @param dateString La stringa di data da analizzare
     * @param dateFormat Il formato di data da utilizzare
     * @return Oggetto Date analizzato dalla stringa di data Jira, o null se la stringa di data è null o vuota
     * @throws ParseException Se la stringa di data non può essere analizzata
     */
    private static Date parseDate(String dateString, SimpleDateFormat dateFormat) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return dateFormat.parse(dateString);
    }

    /**
     * Esegue una richiesta GET all'URL specificato.
     *
     * @param url L'URL a cui inviare la richiesta GET
     * @return La risposta come stringa
     * @throws IOException Se si verifica un errore durante la richiesta HTTP
     */
    private static String executeGetRequest(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    LOGGER.log(Level.WARNING, "HTTP request failed with status: {0}", statusCode);
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

    /**
     * Registra le statistiche sui ticket.
     *
     * @param tickets La lista di ticket da analizzare
     */
    private static void logTicketStatistics(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            LOGGER.info("No tickets to analyze for statistics");
            return;
        }
        
        int totalTickets = tickets.size();
        TicketStatistics stats = collectTicketStatistics(tickets, totalTickets);

        LOGGER.log(Level.INFO,
                """
                        Ticket statistics for {0} tickets:
                        - Missing Injected Version: {1} ({2,number,#.##}%)
                        - Missing Opening Version: {3} ({4,number,#.##}%)
                        - Missing Affected Versions: {5} ({6,number,#.##}%)
                        - Missing Fixed Versions: {7} ({8,number,#.##}%)""",
            new Object[]{
                totalTickets,
                stats.ticketsWithoutIV, stats.percentIV,
                stats.ticketsWithoutOV, stats.percentOV,
                stats.ticketsWithoutAV, stats.percentAV,
                stats.ticketsWithoutFV, stats.percentFV
            });
    }

    /**
     * Collects statistics about tickets.
     *
     * @param tickets The list of tickets to analyze
     * @param totalTickets The total number of tickets
     * @return A TicketStatistics object with the collected statistics
     */
    private static TicketStatistics collectTicketStatistics(List<Ticket> tickets, int totalTickets) {
        TicketStatistics stats = new TicketStatistics();
        
        for (Ticket ticket : tickets) {
            if (ticket == null) {
                continue;
            }
            
            if (isInjectedVersionMissing(ticket)) {
                stats.ticketsWithoutIV++;
            }
            
            if (isOpeningVersionMissing(ticket)) {
                stats.ticketsWithoutOV++;
            }
            
            if (isAffectedVersionsMissing(ticket)) {
                stats.ticketsWithoutAV++;
            }
            
            if (isFixedVersionsMissing(ticket)) {
                stats.ticketsWithoutFV++;
            }
        }
        
        // Calculate percentages
        stats.percentIV = calculatePercentage(stats.ticketsWithoutIV, totalTickets);
        stats.percentOV = calculatePercentage(stats.ticketsWithoutOV, totalTickets);
        stats.percentAV = calculatePercentage(stats.ticketsWithoutAV, totalTickets);
        stats.percentFV = calculatePercentage(stats.ticketsWithoutFV, totalTickets);
        
        return stats;
    }

    /**
     * Checks if the injected version is missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if the injected version is missing, false otherwise
     */
    private static boolean isInjectedVersionMissing(Ticket ticket) {
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
    private static boolean isOpeningVersionMissing(Ticket ticket) {
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
    private static boolean isAffectedVersionsMissing(Ticket ticket) {
        return ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty();
    }

    /**
     * Checks if fixed versions are missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if fixed versions are missing, false otherwise
     */
    private static boolean isFixedVersionsMissing(Ticket ticket) {
        return ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty();
    }

    /**
     * Calculates a percentage.
     *
     * @param part The part
     * @param total The total
     * @return The percentage
     */
    private static double calculatePercentage(int part, int total) {
        return part * 100.0 / total;
    }

    /**
     * Helper class for storing ticket statistics.
     */
    private static class TicketStatistics {
        int ticketsWithoutIV = 0;
        int ticketsWithoutOV = 0;
        int ticketsWithoutAV = 0;
        int ticketsWithoutFV = 0;
        double percentIV = 0.0;
        double percentOV = 0.0;
        double percentAV = 0.0;
        double percentFV = 0.0;
    }
}