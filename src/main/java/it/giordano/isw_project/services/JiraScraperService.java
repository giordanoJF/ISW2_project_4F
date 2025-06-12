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

/**
 * Service class for scraping data from Apache Jira REST API.
 *
 * <p>This utility class provides methods to retrieve project versions and tickets
 * from Apache Jira instances. It handles pagination, date parsing, and version
 * mapping for bug tracking analysis.</p>
 *
 * <p>The service is designed to work with Apache Jira's REST API v2 and focuses
 * on extracting bug-related tickets with their associated version information.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Retrieval of project versions with release dates</li>
 *   <li>Fetching of resolved/closed bug tickets</li>
 *   <li>Automatic pagination handling for large datasets</li>
 *   <li>Version mapping and derivation for tickets</li>
 *   <li>Robust error handling and logging</li>
 * </ul>
 *
 */
public final class JiraScraperService {

    // API Configuration
    /** Base URL for Apache Jira REST API v2 */
    public static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";

    /** Maximum number of results to fetch per API request */
    public static final int MAX_RESULTS_PER_PAGE = 100;



    // Logger
    /** Logger instance for this class */
    private static final Logger LOGGER = Logger.getLogger(JiraScraperService.class.getName());



    // JSON field names for versions
    /** JSON field name for version ID */
    private static final String FIELD_ID = "id";

    /** JSON field name for version name */
    private static final String FIELD_NAME = "name";

    /** JSON field name for version release date */
    private static final String FIELD_RELEASE_DATE = "releaseDate";



    // Date format patterns
    /** Date format pattern for ticket timestamps */
    private static final String TICKET_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /** Date format pattern for version release dates */
    private static final String VERSION_DATE_FORMAT_PATTERN = "yyyy-MM-dd";



    // JSON structure constants
    /** JSON field name for total count */
    private static final String TOTAL = "total";

    /** JSON field name for issues array */
    private static final String ISSUES = "issues";

    /** JSON field name for fields object */
    private static final String FIELDS = "fields";



    // JSON field names for tickets
    /** JSON field name for ticket key */
    private static final String FIELD_KEY = "key";

    /** JSON field name for ticket creation date */
    private static final String FIELD_CREATED_DATE = "created";

    /** JSON field name for ticket resolution date */
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";

    /** JSON field name for fix versions */
    private static final String FIELD_FIX_VERSIONS = "fixVersions";

    /** JSON field name for affected versions */
    private static final String FIELD_VERSIONS = "versions"; // Affected Versions



    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws IllegalStateException always, as this is a utility class
     */
    private JiraScraperService() {
        throw new IllegalStateException("Utility class");
    }



    /**
     * Retrieves all versions for a given Jira project.
     *
     * <p>This method fetches all versions associated with a project from the Jira API,
     * including version names, IDs, and release dates where available.</p>
     *
     * @param projectKey the Jira project key (e.g., "HADOOP", "SPARK")
     * @return a list of Version objects containing version information
     * @throws IOException if there's an error communicating with the Jira API
     * @throws IllegalArgumentException if projectKey is null or empty
     */
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



    /**
     * Parses version information from a JSON response.
     *
     * <p>Converts the raw JSON response from the Jira API into a list of Version objects,
     * handling any parsing errors gracefully.</p>
     *
     * @param jsonResponse the JSON response string from the Jira API
     * @param projectKey the project key for logging purposes
     * @return a list of parsed Version objects
     * @throws IllegalArgumentException if projectKey is null or empty
     */
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

        logVersionsRetrievalResult(versions, projectKey);
        return versions;
    }

    /**
     * Parses a single version from a JSON object.
     *
     * <p>Extracts version ID, name, and release date from the JSON representation
     * of a version returned by the Jira API.</p>
     *
     * @param versionJson the JSON object representing a single version
     * @return a Version object with populated fields
     * @throws IllegalArgumentException if versionJson is null
     */
    @Nonnull
    private static Version parseVersionFromJson(@Nonnull JSONObject versionJson) {

        Version version = new Version();
        version.setId(versionJson.optString(FIELD_ID, null));
        version.setName(versionJson.optString(FIELD_NAME, null));
        setVersionReleaseDate(version, versionJson);
        return version;
    }

    /**
     * Sets the release date for a version from JSON data.
     *
     * <p>Attempts to parse the release date string from the JSON object and
     * sets it on the version. If parsing fails, the release date is set to null
     * and a warning is logged.</p>
     *
     * @param version the Version object to update
     * @param versionJson the JSON object containing version data
     * @throws IllegalArgumentException if either parameter is null
     */
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
            LOGGER.log(Level.WARNING, "Failed to parse release date for version {0}: {1}\n",
                    new Object[]{version.getName(), dateStr});
            version.setReleaseDate(null);
        }
    }

    /**
     * Builds the URL for retrieving project versions.
     *
     * @param projectKey the Jira project key
     * @return the complete URL for the versions API endpoint
     * @throws IllegalArgumentException if projectKey is null or empty
     */
    @Nonnull
    private static String buildVersionsUrl(@Nonnull String projectKey) {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }
        return String.format("%s/project/%s/versions", JIRA_BASE_URL, projectKey);
    }

    /**
     * Logs the result of version retrieval.
     *
     * @param versions the list of retrieved versions
     * @param projectKey the project key for logging context
     * @throws IllegalArgumentException if versions is null or projectKey is null/empty
     */
    private static void logVersionsRetrievalResult(@Nonnull List<Version> versions, @Nonnull String projectKey) {
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

    //reusable only in this class
    /**
     * Executes an HTTP GET request to the specified URL.
     *
     * <p>This method handles the HTTP communication with the Jira API,
     * including proper resource management and error handling.</p>
     *
     * @param url the URL to send the GET request to
     * @return the response body as a string, or null if the request failed
     * @throws IOException if there's an error during the HTTP request
     * @throws IllegalArgumentException if url is null or empty
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
                    LOGGER.log(Level.WARNING, "HTTP request failed with status: {0} for URL: {1}\n",
                            new Object[]{statusCode, url});
                    return null;
                }

                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }

    //reusable only in this class
    /**
     * Parses a date string using the specified format.
     *
     * <p>Utility method for parsing date strings with proper null handling.</p>
     *
     * @param dateString the date string to parse
     * @param dateFormat the SimpleDateFormat to use for parsing
     * @return the parsed Date object, or null if dateString is null/empty
     * @throws ParseException if the date string cannot be parsed
     * @throws IllegalArgumentException if dateFormat is null
     */
    @Nullable
    public static Date parseDate(@Nullable String dateString, @Nonnull SimpleDateFormat dateFormat)
            throws ParseException {
        if (Consistency.isStrNullOrEmpty(dateString)) {
            return null;
        }
        return dateFormat.parse(dateString);
    }



    // TICKETS METHODS --> Main methods

    /**
     * Retrieves all tickets for a given project that match the bug criteria.
     *
     * <p>This method fetches all resolved/closed bug tickets from the specified project,
     * handling pagination automatically. It also associates tickets with their versions
     * and derives additional version information.</p>
     *
     * <p>The method retrieves tickets that match the following criteria:</p>
     * <ul>
     *   <li>Issue type: Bug</li>
     *   <li>Resolution: Fixed</li>
     *   <li>Status: Closed or Resolved</li>
     * </ul>
     *
     * @param projectKey the Jira project key
     * @param versions the list of project versions for mapping
     * @return a list of Ticket objects with populated version information
     * @throws IOException if there's an error communicating with the Jira API
     * @throws IllegalArgumentException if projectKey is null/empty or versions is null
     */
    @Nonnull
    public static List<Ticket> getProjectTickets(@Nonnull String projectKey, @Nonnull List<Version> versions) throws IOException {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
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



    // TICKETS METHODS --> Helper methods

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
    public static Map<String, Version> createVersionMap(@Nonnull List<Version> versions) {
        Map<String, Version> versionMap = new HashMap<>();

        if (versions.isEmpty()) {
            return versionMap;
        }

        for (Version version : versions) {
            if (version != null && !Consistency.isStrNullOrEmpty(version.getName())) {
                versionMap.put(version.getName(), version);
            }
        }

        return versionMap;
    }

    /**
     * Returns the list of required fields for ticket queries.
     *
     * @return a comma-separated string of field names required for ticket analysis
     */
    @Nonnull
    private static String getRequiredFields() {
        return "key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
    }

    //possible reuse
    /**
     * Finds the oldest version with a release date from a list of versions.
     *
     * <p>This method is used to determine the injected version for tickets,
     * which is typically the oldest affected version that has a release date.</p>
     *
     * @param versions the list of versions to search
     * @return the oldest version with a release date, or null if none found
     * @throws IllegalArgumentException if versions is null
     */
    @Nullable
    public static Version findOldestVersionWithReleaseDate(@Nonnull List<Version> versions) {

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

    //possible reuse
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
    private static boolean isValidVersionForOpening(@Nullable Version version, @Nonnull Date createdDate) {
        return version != null &&
                version.getReleaseDate() != null &&
                !version.getReleaseDate().after(createdDate);
    }

    //possible reuse
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
    private static boolean isMoreRecentThanCurrent(@Nonnull Version version, @Nullable Version currentLatest) {
        if (version.getReleaseDate() == null) {
            return false; // If the version has no release date, it cannot be more recent
        }
        return currentLatest == null ||
                (currentLatest.getReleaseDate() != null &&
                        version.getReleaseDate().after(currentLatest.getReleaseDate()));
    }

    /**
     * Builds the JQL (Jira Query Language) query for retrieving bug tickets.
     *
     * <p>Constructs a query that filters for resolved/closed bug tickets
     * in the specified project.</p>
     *
     * @param projectKey the Jira project key
     * @return the JQL query string
     * @throws IllegalArgumentException if projectKey is null or empty
     */
    @Nonnull
    public static String buildJqlQuery(@Nonnull String projectKey) {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        return String.format("project=%s AND issuetype=Bug AND resolution=Fixed AND status in (Closed, Resolved)",
                projectKey);
    }

    /**
     * URL-encodes a JQL query string.
     *
     * @param jql the JQL query to encode
     * @return the URL-encoded JQL query
     * @throws IllegalArgumentException if jql is null or empty
     */
    @Nonnull
    private static String encodeJql(@Nonnull String jql) {
        if (Consistency.isStrNullOrEmpty(jql)) {
            throw new IllegalArgumentException("JQL query cannot be null or empty");
        }
        return java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
    }

    /**
     * Builds the URL for ticket search API requests.
     *
     * <p>Constructs the complete URL for searching tickets with pagination support.</p>
     *
     * @param encodedJql the URL-encoded JQL query
     * @param startAt the starting index for pagination
     * @return the complete URL for the ticket search API
     * @throws IllegalArgumentException if encodedJql is null/empty or startAt is negative
     */
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

    /**
     * Fetches all tickets matching the query with automatic pagination.
     *
     * <p>This method handles the pagination logic to retrieve all tickets
     * that match the specified JQL query, processing them in batches.</p>
     *
     * @param encodedJql the URL-encoded JQL query
     * @param versionMap the map of versions for ticket processing
     * @return a list of all fetched and processed tickets
     * @throws IOException if there's an error during API communication
     * @throws IllegalArgumentException if encodedJql is null/empty or versionMap is null
     */
    @Nonnull
    private static List<Ticket> fetchAllTickets(@Nonnull String encodedJql,
                                                @Nonnull Map<String, Version> versionMap) throws IOException {

        if (Consistency.isStrNullOrEmpty(encodedJql)) {
            throw new IllegalArgumentException("Encoded JQL query cannot be null or empty");
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

    /**
     * Processes an array of issues from the JSON response.
     *
     * <p>Converts each issue in the JSON array to a Ticket object and
     * adds it to the tickets list.</p>
     *
     * @param issuesArray the JSON array containing issue objects
     * @param tickets the list to add processed tickets to
     * @param versionMap the map of versions for ticket processing
     * @throws IllegalArgumentException if any parameter is null
     */
    private static void processIssuesArray(@Nonnull JSONArray issuesArray,
                                           @Nonnull List<Ticket> tickets,
                                           @Nonnull Map<String, Version> versionMap) {

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

    /**
     * Parses a single ticket from its JSON representation.
     *
     * <p>Converts a JSON object representing a Jira issue into a Ticket object,
     * populating all relevant fields including dates, versions, and derived information.</p>
     *
     * @param issueJson the JSON object representing the issue
     * @param versionMap the map of versions for ticket processing
     * @return a fully populated Ticket object
     * @throws IllegalArgumentException if issueJson or versionMap is null
     */
    @Nonnull
    private static Ticket parseTicket(@Nonnull JSONObject issueJson,
                                      @Nonnull Map<String, Version> versionMap) {
        if (versionMap.isEmpty()) {
            LOGGER.info("Version map is empty - creating ticket with basic info only\n");
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

    /**
     * Sets the date fields for a ticket from JSON data.
     *
     * <p>Parses and sets both creation and resolution dates for the ticket.</p>
     *
     * @param ticket the ticket to update
     * @param fields the JSON fields object containing date information
     * @throws IllegalArgumentException if ticket or fields is null
     */
    private static void setTicketDates(@Nonnull Ticket ticket, @Nonnull JSONObject fields) {

        SimpleDateFormat dateFormat = new SimpleDateFormat(TICKET_DATE_FORMAT_PATTERN);

        setTicketCreatedDate(ticket, fields, dateFormat);
        setTicketResolutionDate(ticket, fields, dateFormat);
    }

    /**
     * Sets the creation date for a ticket from JSON data.
     *
     * @param ticket the ticket to update
     * @param fields the JSON fields object
     * @param dateFormat the date format to use for parsing
     * @throws IllegalArgumentException if any parameter is null
     */
    private static void setTicketCreatedDate(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                             @Nonnull SimpleDateFormat dateFormat) {

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

    /**
     * Sets the resolution date for a ticket from JSON data.
     *
     * @param ticket the ticket to update
     * @param fields the JSON fields object
     * @param dateFormat the date format to use for parsing
     * @throws IllegalArgumentException if any parameter is null
     */
    private static void setTicketResolutionDate(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                                @Nonnull SimpleDateFormat dateFormat) {

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

    /**
     * Sets the version information for a ticket from JSON data.
     *
     * <p>Processes both fix versions and affected versions from the JSON fields.</p>
     *
     * @param ticket the ticket to update
     * @param fields the JSON fields object
     * @param versionMap the map of versions for lookup
     * @throws IllegalArgumentException if ticket, fields, or versionMap is null
     */
    private static void setTicketVersions(@Nonnull Ticket ticket, @Nonnull JSONObject fields,
                                          @Nonnull Map<String, Version> versionMap) {
        if (versionMap.isEmpty()) {
            return;
        }

        // Set fixed versions
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_FIX_VERSIONS), versionMap, true);

        // Set affected versions
        addVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_VERSIONS), versionMap, false);

        // Set most recent fixed version
        if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()) {
            ticket.setFixedVersion(null);
        } else {
            ticket.setFixedVersion(mostRecentVersionFromList(ticket.getFixedVersions()));
        }
    }

    //possible reuse
    /**
     * Finds the most recent version from a list of versions.
     *
     * <p>Used to determine the most recent fixed version for a ticket.</p>
     *
     * @param versions the list of versions to search
     * @return the most recent Version object, or null if the list is empty
     */
    @Nullable
    private static Version mostRecentVersionFromList(@Nonnull List<Version> versions) {
        if (versions.isEmpty()) {
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
     * Adds versions to a ticket from a JSON array.
     *
     * <p>Processes an array of version objects from JSON and adds them to the ticket
     * as either fix versions or affected versions based on the isFixVersion flag.</p>
     *
     * @param ticket the ticket to update
     * @param versionsArray the JSON array containing version objects
     * @param versionMap the map of versions for lookup
     * @param isFixVersion true if these are fix versions, false for affected versions
     * @throws IllegalArgumentException if ticket or versionMap is null
     */
    private static void addVersionsFromJsonArray(@Nonnull Ticket ticket, @Nullable JSONArray versionsArray,
                                                 @Nonnull Map<String, Version> versionMap, boolean isFixVersion) {

        if (versionsArray == null || versionsArray.isEmpty()) {
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

    /**
     * Sets derived version information for a ticket.
     *
     * <p>Calculates and sets the injected version and opening version for the ticket
     * based on the affected versions and creation date.</p>
     *
     * @param ticket the ticket to update
     * @param versionMap the map of all available versions
     * @throws IllegalArgumentException if ticket or versionMap is null
     */
    private static void setDerivedVersions(@Nonnull Ticket ticket, @Nonnull Map<String, Version> versionMap) {
        if (versionMap.isEmpty()) {
            LOGGER.log(Level.WARNING, "Version map is empty - skipping derived versions for ticket  {0}\n", ticket.getKey());
            return;
        }

        setInjectedVersionFromAffectedVersions(ticket);
        setOpeningVersion(ticket, versionMap);
    }

    //possible reuse
    /**
     * Sets the injected version for a ticket.
     *
     * <p>The injected version is determined as the oldest affected version
     * that has a release date. This represents the version where the bug
     * was likely introduced.</p>
     *
     * @param ticket the ticket to update
     * @throws IllegalArgumentException if ticket is null
     */
    private static void setInjectedVersionFromAffectedVersions(@Nonnull Ticket ticket) {

        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions == null || affectedVersions.isEmpty()) {
            return;
        }

        Version oldestVersion = findOldestVersionWithReleaseDate(affectedVersions);
        ticket.setInjectedVersion(oldestVersion);
    }

    /**
     * Sets the opening version for a ticket.
     *
     * <p>The opening version is the most recent version that was released
     * before or on the ticket creation date. This represents the version
     * that was current when the ticket was opened.</p>
     *
     * @param ticket the ticket to update
     * @param versionMap the map of all available versions
     * @throws IllegalArgumentException if ticket or versionMap is null
     */
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
}