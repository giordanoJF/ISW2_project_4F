package it.giordano.isw_project.services;

import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;
import it.giordano.isw_project.utils.Consistency;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

public final class JiraScraperService {

    // API Configuration
    public static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";
    public static final int MAX_RESULTS_PER_PAGE = 100;

    // Logger
    private static final Logger LOGGER = Logger.getLogger(JiraScraperService.class.getName());

    // JSON field names for versions
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_RELEASE_DATE = "releaseDate";

    // Date format patterns
    private static final String TICKET_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String VERSION_DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    // JSON structure constants
    private static final String TOTAL = "total";
    private static final String ISSUES = "issues";
    private static final String FIELDS = "fields";

    // JSON field names for tickets
    private static final String FIELD_KEY = "key";
    private static final String FIELD_CREATED_DATE = "created";
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";
    private static final String FIELD_FIX_VERSIONS = "fixVersions";
    private static final String FIELD_VERSIONS = "versions"; // Affected Versions





    private JiraScraperService() {
        throw new IllegalStateException("Utility class");
    }






    // VERSIONS METHODS
    @Nonnull
    public static List<Version> getProjectVersions(@Nonnull String projectKey) throws IOException {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        String url = buildVersionsUrl(projectKey);
        if (Consistency.isStrNullOrEmpty(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String jsonResponse = executeGetRequest(url);
        if (Consistency.isStrNullOrEmpty(jsonResponse)) {
            LOGGER.warning("Empty response from Jira API for project versions\n");
            return new ArrayList<>();
        }

        return parseVersionsFromJsonResponse(jsonResponse, projectKey);
    }



    //parse from JSON methods for versions
    @Nonnull
    private static List<Version> parseVersionsFromJsonResponse(@Nullable String jsonResponse,
                                                               @Nonnull String projectKey) {
        List<Version> versions = new ArrayList<>();

        if (Consistency.isStrNullOrEmpty(jsonResponse)) {
            LOGGER.warning("Empty response from Jira API for project versions\n");
            return versions;
        }
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

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

    @Nonnull
    private static Version parseVersionFromJson(@Nonnull JSONObject versionJson) {
        if (versionJson == null) {
            throw new IllegalArgumentException("Version JSON object cannot be null");
        }

        Version version = new Version();
        version.setId(versionJson.optString(FIELD_ID, null));
        version.setName(versionJson.optString(FIELD_NAME, null));
        setVersionReleaseDate(version, versionJson);
        return version;
    }



    //set version fields methods
    private static void setVersionReleaseDate(@Nonnull Version version,@Nonnull JSONObject versionJson) {
        if (version == null || versionJson == null) {
            throw new IllegalArgumentException("Version and version JSON object cannot be null");
        }

        String dateStr = versionJson.optString(FIELD_RELEASE_DATE, null);

        if (Consistency.isStrNullOrEmpty(dateStr)) {
            version.setReleaseDate(null);
            return;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(VERSION_DATE_FORMAT_PATTERN);
            version.setReleaseDate(parseDate(dateStr, dateFormat));
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "Failed to parse release date for version {0}: {1}\n",
                    new Object[]{version.getName(), dateStr});
            version.setReleaseDate(null);
        }
    }



    //url methods for versions
    @Nonnull
    private static String buildVersionsUrl(@Nonnull String projectKey) {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }
        return String.format("%s/project/%s/versions", JIRA_BASE_URL, projectKey);
    }



    // Logging methods for versions
    private static void logVersionsResult(@Nonnull List<Version> versions, @Nonnull String projectKey) {
        if (versions == null) {
            throw new IllegalArgumentException("Versions list cannot be null");
        }
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        if (versions.isEmpty()) {
            LOGGER.log(Level.INFO, "No versions found for project {0}\n", projectKey);
        } else {
            LOGGER.log(Level.INFO, "Retrieved {0} versions for project {1}\n", new Object[]{versions.size(), projectKey});
        }
    }









    // COMMON METHODS FOR TICKETS AND VERSIONS


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
                    LOGGER.log(Level.WARNING, "HTTP request failed with status: {0} for URL: {1}\n",
                            new Object[]{statusCode, url});
                    return null;
                }

                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }

    @Nullable
    public static Date parseDate(@Nullable String dateString, @Nonnull SimpleDateFormat dateFormat)
            throws ParseException {
        if (dateFormat == null) {
            throw new IllegalArgumentException("Date format cannot be null");
        }
        if (Consistency.isStrNullOrEmpty(dateString)) {
            return null;
        }
        return dateFormat.parse(dateString);
    }









    // TICKETS METHODS

    @Nonnull
    public static List<Ticket> getProjectTickets(@Nonnull String projectKey, @Nonnull List<Version> versions) throws IOException {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        if (versions == null) {
            throw new IllegalArgumentException("Versions list cannot be null");
        }

        if (versions.isEmpty()) {
            LOGGER.warning("Versions list is empty - no tickets will be processed\n");
            return new ArrayList<>();
        }

        Map<String, Version> versionMap = createVersionMap(versions);
        String jql = buildJqlQuery(projectKey);
        String encodedJql = encodeJql(jql);

        return fetchAllTickets(encodedJql, versionMap);
    }



    //helper methods for tickets
    @Nonnull
    public static Map<String, Version> createVersionMap(@Nonnull List<Version> versions) { //we are creating a map of versions with their names as keys
        Map<String, Version> versionMap = new HashMap<>();

        if (versions == null) {
            throw new IllegalArgumentException("Versions list cannot be null");
        }

        if (versions.isEmpty()) {
            return versionMap;
        }

        for (Version version : versions) {
            if (version != null && !Consistency.isStrNullOrEmpty(version.getName())) { //we take only versions with a name
                versionMap.put(version.getName(), version);
            }
        }

        return versionMap;
    }

    @Nonnull
    private static String getRequiredFields() {
        return "key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
    }

    @Nullable
    public static Version findOldestVersionWithReleaseDate(@Nonnull List<Version> versions) {
        if (versions == null) {
            throw new IllegalArgumentException("Versions list cannot be null");
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

    private static boolean isValidVersionForOpening(@Nullable Version version, @Nonnull Date createdDate) {
        if (createdDate == null) {
            throw new IllegalArgumentException("Created date cannot be null");
        }
        return version != null &&
                version.getReleaseDate() != null &&
                !version.getReleaseDate().after(createdDate);
    }

    private static boolean isMoreRecentThanCurrent(@Nonnull Version version, @Nullable Version currentLatest) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        if (version.getReleaseDate() == null) {
            return false; // If the version has no release date, it cannot be more recent
        }
        return currentLatest == null ||
                (currentLatest.getReleaseDate() != null &&
                        version.getReleaseDate().after(currentLatest.getReleaseDate()));
    }



    //url methods for tickets
    @Nonnull
    public static String buildJqlQuery(@Nonnull String projectKey) {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        return String.format("project=%s AND issuetype=Bug AND resolution=Fixed AND status in (Closed, Resolved)",
                projectKey);
    }

    @Nonnull
    private static String encodeJql(@Nonnull String jql) {
        if (Consistency.isStrNullOrEmpty(jql)) {
            throw new IllegalArgumentException("JQL query cannot be null or empty");
        }
        return java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
    }

    @Nonnull
    public static String buildTicketsUrl(@Nonnull String encodedJql, int startAt) {
        if (Consistency.isStrNullOrEmpty(encodedJql)) {
            throw new IllegalArgumentException("Encoded JQL query cannot be null or empty");
        }

        if (startAt < 0) {
            throw new IllegalArgumentException("Start index cannot be negative");
        }

        return String.format("%s/search?jql=%s&startAt=%d&maxResults=%d&fields=%s",
                JIRA_BASE_URL, encodedJql, startAt, MAX_RESULTS_PER_PAGE, getRequiredFields());
    }



    // Fetch & parsing methods for tickets
    @Nonnull
    private static List<Ticket> fetchAllTickets(@Nonnull String encodedJql,
                                                @Nonnull Map<String, Version> versionMap) throws IOException {

        if (Consistency.isStrNullOrEmpty(encodedJql)) {
            throw new IllegalArgumentException("Encoded JQL query cannot be null or empty");
        }
        if (versionMap == null) {
            throw new IllegalArgumentException("Version map cannot be null");
        }

        List<Ticket> tickets = new ArrayList<>();
        int startAt = 0;
        int total = 0;
        boolean firstPage = true;

        do {
            String url = buildTicketsUrl(encodedJql, startAt);
            String jsonResponse = executeGetRequest(url);

            if (Consistency.isStrNullOrEmpty(jsonResponse)) {
                LOGGER.warning("Empty response from Jira API for tickets search\n");
                break;
            }

            JSONObject responseObj = new JSONObject(jsonResponse);

            if (firstPage) {
                total = responseObj.optInt(TOTAL, 0);
                firstPage = false;
                LOGGER.log(Level.INFO, "Found {0} tickets to process\n", total);
            }

            JSONArray issuesArray = responseObj.optJSONArray(ISSUES);
            if (issuesArray != null) {
                processIssuesArray(issuesArray, tickets, versionMap);
            } else {
                LOGGER.warning("No issues array found in response\n");
            }

            startAt += MAX_RESULTS_PER_PAGE;
        } while (startAt < total);

        LOGGER.log(Level.INFO, "Successfully processed {0} tickets\n", tickets.size());
        return tickets;
    }

    private static void processIssuesArray(@Nonnull JSONArray issuesArray,
                                           @Nonnull List<Ticket> tickets,
                                           @Nonnull Map<String, Version> versionMap) {

        if (issuesArray == null) {
            throw new IllegalArgumentException("Issues array cannot be null");
        }
        if (tickets == null) {
            throw new IllegalArgumentException("Tickets list cannot be null");
        }
        if (versionMap == null) {
            throw new IllegalArgumentException("Version map cannot be null");
        }
        if (issuesArray.isEmpty()) {
            LOGGER.warning("Issues array is empty\n");
            return;
        }

        for (int i = 0; i < issuesArray.length(); i++) {
            JSONObject issueJson = issuesArray.optJSONObject(i);
            if (issueJson != null) {
                Ticket ticket = parseTicket(issueJson, versionMap);
                tickets.add(ticket);
            }
        }
    }

    @Nonnull
    private static Ticket parseTicket(@Nonnull JSONObject issueJson,
                                      @Nonnull Map<String, Version> versionMap) {
        if (versionMap.isEmpty()) {
            LOGGER.info("Version map is empty - creating ticket with basic info only\n");
        }

        if (issueJson == null) {
            throw new IllegalArgumentException("Issue JSON object cannot be null");
        }
        if (versionMap == null) {
            throw new IllegalArgumentException("Version map cannot be null");
        }

        Ticket ticket = new Ticket();
        ticket.setKey(issueJson.optString(FIELD_KEY, null));

        JSONObject fields = issueJson.optJSONObject(FIELDS);
        if (fields == null) {
            LOGGER.log(Level.WARNING, "No fields found for ticket {0}\n", ticket.getKey());
            return ticket; // Return ticket with just the key
        }

        setTicketDates(ticket, fields);
        setTicketVersions(ticket, fields, versionMap);
        setDerivedVersions(ticket, versionMap);

        return ticket;
    }



    // Set fields methods for tickets
    private static void setTicketDates(@Nonnull Ticket ticket, @Nonnull JSONObject fields) {
        if (ticket == null || fields == null) {
            throw new IllegalArgumentException("Ticket and fields cannot be null");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(TICKET_DATE_FORMAT_PATTERN);

        setTicketCreatedDate(ticket, fields, dateFormat);
        setTicketResolutionDate(ticket, fields, dateFormat);
    }

    private static void setTicketCreatedDate(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                             @Nonnull SimpleDateFormat dateFormat) {
        if (ticket == null || fields == null || dateFormat == null) {
            throw new IllegalArgumentException("Ticket, fields, and date format cannot be null");
        }

        String createdDateStr = fields.optString(FIELD_CREATED_DATE, null);
        if (!Consistency.isStrNullOrEmpty(createdDateStr)) {
            try {
                ticket.setCreatedDate(parseDate(createdDateStr, dateFormat));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Failed to parse created date for ticket {0}: {1}\n",
                        new Object[]{ticket.getKey(), createdDateStr});
                ticket.setCreatedDate(null);
            }
        }
        else {
            ticket.setCreatedDate(null);
        }
    }

    private static void setTicketResolutionDate(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                                @Nonnull SimpleDateFormat dateFormat) {
        if (ticket == null || fields == null || dateFormat == null) {
            throw new IllegalArgumentException("Ticket, fields, and date format cannot be null");
        }

        String resolutionDateStr = fields.optString(FIELD_RESOLUTION_DATE, null);
        if (!Consistency.isStrNullOrEmpty(resolutionDateStr)) {
            try {
                ticket.setResolutionDate(parseDate(resolutionDateStr, dateFormat));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Failed to parse resolution date for ticket {0}: {1}\n",
                        new Object[]{ticket.getKey(), resolutionDateStr});
                ticket.setResolutionDate(null);
            }
        }
        else {
            ticket.setResolutionDate(null);
        }
    }

    private static void setTicketVersions(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                          @Nonnull Map<String, Version> versionMap) {
        if (versionMap.isEmpty()) {
            return;
        }

        if (ticket == null || fields == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket, fields, and version map cannot be null");
        }

        // Set fixed versions
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_FIX_VERSIONS), versionMap, true);

        // Set affected versions
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_VERSIONS), versionMap, false);
    }

    private static void addVersionsFromJsonArray(@Nonnull Ticket ticket, @Nullable JSONArray versionsArray,
                                                 @Nonnull Map<String, Version> versionMap, boolean isFixVersion) {
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }

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
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }
        if (versionMap.isEmpty()) {
            LOGGER.log(Level.WARNING, "Version map is empty - skipping derived versions for ticket  {0}\n", ticket.getKey());
            return;
        }

        setInjectedVersion(ticket);
        setOpeningVersion(ticket, versionMap);
    }

    private static void setInjectedVersion(@Nonnull Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null");
        }

        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions == null || affectedVersions.isEmpty()) {
            return;
        }

        Version oldestVersion = findOldestVersionWithReleaseDate(affectedVersions);
        ticket.setInjectedVersion(oldestVersion);
    }

    private static void setOpeningVersion(@Nonnull Ticket ticket, @Nonnull Map<String, Version> versionMap) {
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }

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





}
