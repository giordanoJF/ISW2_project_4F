package it.giordano.isw_project.service;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import it.giordano.isw_project.util.Consistency;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for interacting with Jira API.
 * Provides methods to retrieve project versions and tickets from Jira.
 * Handles HTTP requests, JSON parsing, and data mapping to domain objects.
 * Includes pagination support for large result sets.
 */
public final class JiraService {

    // API Configuration
    public static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";
    public static final int MAX_RESULTS_PER_PAGE = 100;

    private static final Logger LOGGER = Logger.getLogger(JiraService.class.getName());

    // JSON field names for versions
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_RELEASED = "released";
    private static final String FIELD_ARCHIVED = "archived";
    private static final String FIELD_RELEASE_DATE = "releaseDate";

    // JSON field names for tickets
    private static final String FIELD_KEY = "key";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CREATED_DATE = "created";
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RESOLUTION = "resolution";
    private static final String FIELD_FIX_VERSIONS = "fixVersions";
    private static final String FIELD_VERSIONS = "versions"; // Affected Versions

    // JSON structure constants
    private static final String TOTAL = "total";
    private static final String ISSUES = "issues";
    private static final String FIELDS = "fields";

    // Date format patterns
    private static final String TICKET_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String VERSION_DATE_FORMAT_PATTERN = "yyyy-MM-dd";






    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private JiraService() {
        throw new IllegalStateException("Utility class");
    }







    /**
     * Retrieves all versions of a project from Jira.
     *
     * @param projectKey The project identifier code, must not be null or empty
     * @return List of versions for the project. Never null, but may be empty if no versions exist
     * @throws IllegalArgumentException if projectKey is null or empty
     * @throws IOException if an HTTP error occurs during the request
     */
    @Nonnull
    public static List<Version> getProjectVersions(@Nonnull String projectKey) throws IOException {
        validateProjectKey(projectKey);

        String url = buildVersionsUrl(projectKey);
        String jsonResponse = executeGetRequest(url);

        if (Consistency.isStrNullOrEmpty(jsonResponse)) {
            LOGGER.warning("Empty response from Jira API for project versions");
            return new ArrayList<>();
        }

        return parseVersionsFromJsonResponse(jsonResponse, projectKey);
    }

    /**
     * Retrieves bug tickets from Jira that have been resolved and closed.
     *
     * @param projectKey The project identifier code, must not be null or empty
     * @param versions List of project versions to associate with tickets, must not be null
     * @return List of tickets that meet the criteria. Never null, but may be empty
     * @throws IllegalArgumentException if projectKey is null/empty or versions is null
     * @throws IOException if an HTTP error occurs during the request
     */
    @Nonnull
    public static List<Ticket> getProjectTickets(@Nonnull String projectKey, @Nonnull List<Version> versions)
            throws IOException {
        validateProjectKey(projectKey);
        Objects.requireNonNull(versions, "Versions list cannot be null");

        if (versions.isEmpty()) {
            LOGGER.warning("Versions list is empty - no tickets will be processed");
            return new ArrayList<>();
        }

        Map<String, Version> versionMap = createVersionMap(versions);
        String jql = buildJqlQuery(projectKey);
        String encodedJql = encodeJql(jql);

        return fetchAllTickets(encodedJql, versionMap);
    }









    /**
     * Executes a GET request to the specified URL.
     *
     * @param url The URL to send the GET request to, must not be null or empty
     * @return The response as a string, or null if the request failed
     * @throws IllegalArgumentException if url is null or empty
     * @throws IOException if an HTTP error occurs during the request
     */
    @Nullable
    public static String executeGetRequest(@Nonnull String url) throws IOException {
        if (Consistency.isStrNullOrEmpty(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    LOGGER.log(Level.WARNING, "HTTP request failed with status: {0} for URL: {1}",
                            new Object[]{statusCode, url});
                    return null;
                }

                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }

    /**
     * Builds the JQL query for retrieving bug tickets.
     *
     * @param projectKey The project key, must not be null or empty
     * @return JQL query string
     */
    @Nonnull
    public static String buildJqlQuery(@Nonnull String projectKey) {
        validateProjectKey(projectKey);

        return String.format("project=%s AND issuetype=Bug AND resolution=Fixed AND status in (Closed, Resolved)",
                projectKey);
    }

    /**
     * Builds the URL for the tickets search API.
     *
     * @param encodedJql The encoded JQL query, must not be null
     * @param startAt The pagination start index, must be non-negative
     * @return The URL for the tickets search API
     */
    @Nonnull
    public static String buildTicketsUrl(@Nonnull String encodedJql, int startAt) {
        Objects.requireNonNull(encodedJql, "Encoded JQL cannot be null");

        if (startAt < 0) {
            throw new IllegalArgumentException("Start index cannot be negative");
        }

        return String.format("%s/search?jql=%s&startAt=%d&maxResults=%d&fields=%s",
                JIRA_BASE_URL, encodedJql, startAt, MAX_RESULTS_PER_PAGE, getRequiredFields());
    }

    @Nonnull
    private static String buildVersionsUrl(@Nonnull String projectKey) {
        return String.format("%s/project/%s/versions", JIRA_BASE_URL, projectKey);
    }

    @Nonnull
    private static String encodeJql(@Nonnull String jql) {
        return java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
    }













    @Nonnull
    private static List<Ticket> fetchAllTickets(@Nonnull String encodedJql,
                                                @Nonnull Map<String, Version> versionMap) throws IOException {
        List<Ticket> tickets = new ArrayList<>();
        int startAt = 0;
        int total = 0;
        boolean firstPage = true;

        do {
            String url = buildTicketsUrl(encodedJql, startAt);
            String jsonResponse = executeGetRequest(url);

            if (Consistency.isStrNullOrEmpty(jsonResponse)) {
                LOGGER.warning("Empty response from Jira API for tickets search");
                break;
            }

            JSONObject responseObj = new JSONObject(jsonResponse);

            if (firstPage) {
                total = responseObj.optInt(TOTAL, 0);
                firstPage = false;
                LOGGER.log(Level.INFO, "Found {0} tickets to process", total);
            }

            JSONArray issuesArray = responseObj.optJSONArray(ISSUES);
            if (issuesArray != null) {
                processIssuesArray(issuesArray, tickets, versionMap);
            } else {
                LOGGER.warning("No issues array found in response");
            }

            startAt += MAX_RESULTS_PER_PAGE;
        } while (startAt < total);

        LOGGER.log(Level.INFO, "Successfully processed {0} tickets", tickets.size());
        return tickets;
    }

    private static void processIssuesArray(@Nonnull JSONArray issuesArray,
                                           @Nonnull List<Ticket> tickets,
                                           @Nonnull Map<String, Version> versionMap) {
        for (int i = 0; i < issuesArray.length(); i++) {
            JSONObject issueJson = issuesArray.optJSONObject(i);
            if (issueJson != null) {
                Ticket ticket = parseTicket(issueJson, versionMap);
                tickets.add(ticket);
            }
        }
    }

    @Nonnull
    private static String getRequiredFields() {
        return "key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
    }

    @Nonnull
    private static List<Version> parseVersionsFromJsonResponse(@Nonnull String jsonResponse,
                                                               @Nonnull String projectKey) {
        List<Version> versions = new ArrayList<>();
        JSONArray versionArray = new JSONArray(jsonResponse);

        for (int i = 0; i < versionArray.length(); i++) {
            JSONObject versionJson = versionArray.optJSONObject(i);
            if (versionJson != null) {
                Version version = parseVersionFromJson(versionJson);
                versions.add(version);
            }
        }

        logVersionsResult(versions, projectKey);
        return versions;
    }

    @Nullable
    private static Version parseVersionFromJson(@Nullable JSONObject versionJson) {
        if (versionJson == null) {
            return null;
        }

        Version version = new Version();
        version.setId(versionJson.optString(FIELD_ID, null));
        version.setName(versionJson.optString(FIELD_NAME, null));
        version.setReleased(versionJson.optBoolean(FIELD_RELEASED, false));
        version.setArchived(versionJson.optBoolean(FIELD_ARCHIVED, false));

        setVersionReleaseDate(version, versionJson);
        return version;
    }

    private static void setVersionReleaseDate(@Nonnull Version version, @Nonnull JSONObject versionJson) {
        String dateStr = versionJson.optString(FIELD_RELEASE_DATE, null);

        if (Consistency.isStrNullOrEmpty(dateStr)) {
            version.setReleaseDate(null);
            return;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(VERSION_DATE_FORMAT_PATTERN);
            version.setReleaseDate(parseDate(dateStr, dateFormat));
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "Failed to parse release date for version {0}: {1}",
                    new Object[]{version.getName(), dateStr});
            version.setReleaseDate(null);
        }
    }

    @Nullable
    private static Ticket parseTicket(@Nullable JSONObject issueJson,
                                      @Nonnull Map<String, Version> versionMap) {
        if (issueJson == null) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.setKey(issueJson.optString(FIELD_KEY, null));

        JSONObject fields = issueJson.optJSONObject(FIELDS);
        if (fields == null) {
            LOGGER.warning("No fields found for ticket " + ticket.getKey());
            return ticket; // Return ticket with just the key
        }

        setBasicTicketFields(ticket, fields);
        setTicketDates(ticket, fields);
        setTicketStatusFields(ticket, fields);
        setTicketVersions(ticket, fields, versionMap);
        setDerivedVersions(ticket, versionMap);

        return ticket;
    }

    private static void setBasicTicketFields(@Nonnull Ticket ticket, @Nonnull JSONObject fields) {
        ticket.setSummary(fields.optString(FIELD_SUMMARY, null));
        ticket.setDescription(fields.optString(FIELD_DESCRIPTION, null));
    }

    private static void setTicketDates(@Nonnull Ticket ticket, @Nonnull JSONObject fields) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(TICKET_DATE_FORMAT_PATTERN);

        setTicketCreatedDate(ticket, fields, dateFormat);
        setTicketResolutionDate(ticket, fields, dateFormat);
    }

    private static void setTicketCreatedDate(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                             @Nonnull SimpleDateFormat dateFormat) {
        String createdDateStr = fields.optString(FIELD_CREATED_DATE, null);
        if (!Consistency.isStrNullOrEmpty(createdDateStr)) {
            try {
                ticket.setCreatedDate(parseDate(createdDateStr, dateFormat));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Failed to parse created date for ticket {0}: {1}",
                        new Object[]{ticket.getKey(), createdDateStr});
                ticket.setCreatedDate(null);
            }
        }
    }

    private static void setTicketResolutionDate(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                                @Nonnull SimpleDateFormat dateFormat) {
        String resolutionDateStr = fields.optString(FIELD_RESOLUTION_DATE, null);
        if (!Consistency.isStrNullOrEmpty(resolutionDateStr)) {
            try {
                ticket.setResolutionDate(parseDate(resolutionDateStr, dateFormat));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Failed to parse resolution date for ticket {0}: {1}",
                        new Object[]{ticket.getKey(), resolutionDateStr});
                ticket.setResolutionDate(null);
            }
        }
    }

    private static void setTicketStatusFields(@Nonnull Ticket ticket, @Nonnull JSONObject fields) {
        JSONObject statusObj = fields.optJSONObject(FIELD_STATUS);
        if (statusObj != null) {
            ticket.setStatus(statusObj.optString(FIELD_NAME, null));
        }

        JSONObject resolutionObj = fields.optJSONObject(FIELD_RESOLUTION);
        if (resolutionObj != null) {
            ticket.setResolution(resolutionObj.optString(FIELD_NAME, null));
        }
    }

    private static void setTicketVersions(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                          @Nonnull Map<String, Version> versionMap) {
        if (versionMap.isEmpty()) {
            return;
        }

        // Set fixed versions
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_FIX_VERSIONS), versionMap, true);

        // Set affected versions
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_VERSIONS), versionMap, false);
    }

    private static void addVersionsFromJsonArray(@Nonnull Ticket ticket, @Nullable JSONArray versionsArray,
                                                 @Nonnull Map<String, Version> versionMap, boolean isFixVersion) {
        if (versionsArray == null) {
            return;
        }

        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionJson = versionsArray.optJSONObject(i);
            if (versionJson != null) {
                String versionName = versionJson.optString(FIELD_NAME, null);
                Version version = versionMap.get(versionName);

                if (!Consistency.isStrNullOrEmpty(versionName) && version != null) {
                    if (isFixVersion) {
                        ticket.addFixedVersion(version);
                    } else {
                        ticket.addAffectedVersion(version);
                    }
                }
            }
        }
    }

    private static void setDerivedVersions(@Nonnull Ticket ticket, @Nonnull Map<String, Version> versionMap) {
        if (versionMap.isEmpty()) {
            return;
        }

        setInjectedVersion(ticket);
        setOpeningVersion(ticket, versionMap);
    }

    private static void setInjectedVersion(@Nonnull Ticket ticket) {
        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions == null || affectedVersions.isEmpty()) {
            return;
        }

        Version oldestVersion = findOldestVersionWithReleaseDate(affectedVersions);
        ticket.setInjectedVersion(oldestVersion);
    }

    private static void setOpeningVersion(@Nonnull Ticket ticket, @Nonnull Map<String, Version> versionMap) {
        Date createdDate = ticket.getCreatedDate();
        if (createdDate == null) {
            return;
        }

        Version latestVersion = null;

        for (Version version : versionMap.values()) {
            if (isValidVersionForOpening(version, createdDate) &&
                    isMoreRecentThanCurrent(version, latestVersion)) {
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
    public static Date parseDate(String dateString, SimpleDateFormat dateFormat) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return dateFormat.parse(dateString);
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

    private static void logVersionsResult(@Nonnull List<Version> versions, @Nonnull String projectKey) {
        if (versions.isEmpty()) {
            LOGGER.log(Level.INFO, "No versions found for project {0}", projectKey);
        } else {
            LOGGER.log(Level.INFO, "Retrieved {0} versions for project {1}",
                    new Object[]{versions.size(), projectKey});
        }
    }

    private static void validateProjectKey(@Nullable String projectKey) {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }
    }

    private static boolean isValidVersionForOpening(@Nullable Version version, @Nonnull Date createdDate) {
        return version != null &&
                version.getReleaseDate() != null &&
                !version.getReleaseDate().after(createdDate);
    }

    private static boolean isMoreRecentThanCurrent(@Nonnull Version version, @Nullable Version currentLatest) {
        return currentLatest == null ||
                (currentLatest.getReleaseDate() != null &&
                        version.getReleaseDate().after(currentLatest.getReleaseDate()));
    }

}
