package it.giordano.isw_project.services;

import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;
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
import java.net.URLEncoder;
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
    @Nonnull private static final Logger LOGGER = Objects.requireNonNull(Logger.getLogger(JiraScraperService.class.getName()));



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


    // VERSIONS METHODS --> Main methods, all non-reusable outside this class

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
    public static List<Version> getProjectVersions(@Nullable String projectKey) throws IOException {
        if ((projectKey == null || projectKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        String url = buildVersionsUrl(projectKey);
        if (url.trim().isEmpty()) {
            throw new IllegalArgumentException("Constructed URL cannot be null or empty");
        }

        String jsonResponse = executeGetRequest(url);
        if ((jsonResponse == null || jsonResponse.trim().isEmpty())) {
            throw new IOException("jsonResponse is null or empty for " + projectKey);
        }

        return parseVersionsFromJsonResponse(jsonResponse, projectKey);
    }

    // VERSIONS METHODS --> Helper methods, all non-reusable outside this class

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
                                                               @Nullable String projectKey) {
        List<Version> versions = new ArrayList<>();

        if ((jsonResponse == null || jsonResponse.trim().isEmpty())) {
            throw new IllegalArgumentException("JSON response cannot be null or empty");
        }
        if ((projectKey == null || projectKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        JSONArray versionArray = new JSONArray(jsonResponse);
        if (versionArray.isEmpty()) {
            LOGGER.log(Level.INFO, "No versions found for project {0}\n", projectKey);
            return versions;
        }

        for (int i = 0; i < versionArray.length(); i++) {
            JSONObject versionJson = versionArray.optJSONObject(i);
            if (versionJson != null && !versionJson.isEmpty()) {
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
    private static Version parseVersionFromJson(@Nullable JSONObject versionJson) {
        if (versionJson == null) {
            throw new IllegalArgumentException("Version JSON object cannot be null");
        }
        if (versionJson.isEmpty()) {
            LOGGER.warning("Empty version JSON object encountered\n");
            return new Version(); // Return an empty version if JSON is empty
        }

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
    private static void setVersionReleaseDate(@Nullable Version version, @Nullable JSONObject versionJson) {
        if (version == null) {
            throw new IllegalArgumentException("Version object cannot be null");
        }
        if (versionJson == null) {
            throw new IllegalArgumentException("Version JSON object cannot be null");
        }
        if (versionJson.isEmpty()) {
            LOGGER.warning("Empty version JSON object encountered\n");
            version.setReleaseDate(null); // Set release date to null if JSON is empty
            return;
        }

        String dateStr = versionJson.optString(FIELD_RELEASE_DATE, null);
        if ((dateStr == null || dateStr.trim().isEmpty())) {
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
    private static String buildVersionsUrl(@Nullable String projectKey) {
        if ((projectKey == null || projectKey.trim().isEmpty())) {
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
    private static void logVersionsRetrievalResult(@Nullable List<Version> versions, @Nullable String projectKey) {
        if ((projectKey == null || projectKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        if (versions == null) {
            throw new IllegalArgumentException("Versions list cannot be null");
        }
        else {
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
    public static String executeGetRequest(@Nullable String url) throws IOException {
        if ((url == null || url.trim().isEmpty())) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            assert httpClient != null;
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                assert response != null;
                int statusCode = Objects.requireNonNull(response.getStatusLine()).getStatusCode();

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

    //reusable also in other classes
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
    public static Date parseDate(@Nullable String dateString, @Nullable SimpleDateFormat dateFormat)
            throws ParseException {
        if (dateFormat == null) {
            throw new IllegalArgumentException("Date format cannot be null");
        }
        if ((dateString == null || dateString.trim().isEmpty())) {
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
    public static List<Ticket> getProjectTickets(@Nullable String projectKey, @Nullable List<Version> versions) throws IOException {
        if ((projectKey == null || projectKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("Versions list cannot be null or empty");
        }

        Map<String, Version> versionMap = createVersionMap(versions);
        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
        }

        String jql = buildJqlQuery(projectKey);
        if (jql.trim().isEmpty()) {
            throw new IllegalArgumentException("JQL query cannot be null or empty");
        }

        String encodedJql = encodeJql(jql);
        if (encodedJql.trim().isEmpty()) {
            throw new IllegalArgumentException("Encoded JQL query cannot be null or empty");
        }

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
    public static Map<String, Version> createVersionMap(@Nullable List<Version> versions) {
        Map<String, Version> versionMap = new HashMap<>();
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("Versions list cannot be null");
        }

        for (Version version : versions) {
            if (version != null && !(version.getName() == null || version.getName().trim().isEmpty())) {
                versionMap.put(version.getName(), version);
            }
        }

        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty - no valid versions found");
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
    public static Version findOldestVersionWithReleaseDate(@Nullable List<Version> versions) {
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("Versions list cannot be null or empty");
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
    private static boolean isValidVersionForOpening(@Nullable Version version, @Nullable Date createdDate) {
        if (createdDate == null) {
            throw new IllegalArgumentException("Created date cannot be null");
        }
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
    private static boolean isMoreRecentThanCurrent(@Nullable Version version, @Nullable Version currentLatest) {
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
    public static String buildJqlQuery(@Nullable String projectKey) {
        if ((projectKey == null || projectKey.trim().isEmpty())) {
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
    private static String encodeJql(@Nullable String jql) {
        if ((jql == null || jql.trim().isEmpty())) {
            throw new IllegalArgumentException("JQL query cannot be null or empty");
        }
        return Objects.requireNonNull(URLEncoder.encode(jql, StandardCharsets.UTF_8));
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
    public static String buildTicketsUrl(@Nullable String encodedJql, int startAt) {
        if ((encodedJql == null || encodedJql.trim().isEmpty())) {
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
    private static List<Ticket> fetchAllTickets(@Nullable String encodedJql,
                                                @Nullable Map<String, Version> versionMap) throws IOException {

        if ((encodedJql == null || encodedJql.trim().isEmpty())) {
            throw new IllegalArgumentException("Encoded JQL query cannot be null or empty");
        }
        if (versionMap == null || versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be null or empty");
        }

        List<Ticket> tickets = new ArrayList<>();
        int startAt = 0;
        int total = 0;
        boolean firstPage = true;

        do {
            String url = buildTicketsUrl(encodedJql, startAt);
            String jsonResponse = executeGetRequest(url);

            if ((jsonResponse == null || jsonResponse.trim().isEmpty())) {
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
    private static void processIssuesArray(@Nullable JSONArray issuesArray,
                                           @Nullable List<Ticket> tickets,
                                           @Nullable Map<String, Version> versionMap) {

        if (issuesArray == null || tickets == null || versionMap == null) {
            throw new IllegalArgumentException("Issues array, tickets list, and version map cannot be null");
        }
        if (issuesArray.isEmpty()) {
            LOGGER.warning("Issues array is empty\n");
            return;
        }
        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
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
    private static Ticket parseTicket(@Nullable JSONObject issueJson,
                                      @Nullable Map<String, Version> versionMap) {
        if (issueJson == null || versionMap == null) {
            throw new IllegalArgumentException("Issue JSON and version map cannot be null");
        }

        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
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
    private static void setTicketDates(@Nullable Ticket ticket, @Nullable JSONObject fields) {

        if (ticket == null || fields == null) {
            throw new IllegalArgumentException("Ticket and fields cannot be null");
        }

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
    private static void setTicketCreatedDate(@Nullable Ticket ticket, @Nullable JSONObject fields,
                                             @Nullable SimpleDateFormat dateFormat) {
        if (ticket == null || fields == null || dateFormat == null) {
            throw new IllegalArgumentException("Ticket, fields, and date format cannot be null");
        }

        String createdDateStr = fields.optString(FIELD_CREATED_DATE, null);
        if (!(createdDateStr == null || createdDateStr.trim().isEmpty())) {
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
    private static void setTicketResolutionDate(@Nullable Ticket ticket, @Nullable JSONObject fields,
                                                @Nullable SimpleDateFormat dateFormat) {
        if (ticket == null || fields == null || dateFormat == null) {
            throw new IllegalArgumentException("Ticket, fields, and date format cannot be null");
        }

        String resolutionDateStr = fields.optString(FIELD_RESOLUTION_DATE, null);
        if (!(resolutionDateStr == null || resolutionDateStr.trim().isEmpty())) {
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
    private static void setTicketVersions(@Nullable Ticket ticket, @Nullable JSONObject fields,
                                          @Nullable Map<String, Version> versionMap) {
        if (ticket == null || fields == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket, fields, and version map cannot be null");
        }

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
    private static Version mostRecentVersionFromList(@Nullable List<Version> versions) {
        if (versions == null){
            return null; // Return null if the list is null
        }

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
    private static void addVersionsFromJsonArray(@Nullable Ticket ticket, @Nullable JSONArray versionsArray,
                                                 @Nullable Map<String, Version> versionMap, boolean isFixVersion) {
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }

        if (versionsArray == null || versionsArray.isEmpty()) {
            return;
        }

        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionJson = versionsArray.optJSONObject(i);
            if (versionJson != null) {
                processVersionJson(ticket, versionJson, versionMap, isFixVersion);
            }
        }
    }

    private static void processVersionJson(@Nullable Ticket ticket, @Nullable JSONObject versionJson,
                                           @Nullable Map<String, Version> versionMap, boolean isFixVersion) {
        if (ticket == null || versionJson == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket, version JSON, and version map cannot be null");
        }

        String versionName = versionJson.optString(FIELD_NAME, null);

        if (!(versionName != null && !versionName.trim().isEmpty())) {
            return;
        }

        Version version = versionMap.get(versionName);
        if (version != null) {
            addVersionToTicket(ticket, version, isFixVersion);
        }
    }

    //possible reuse
    private static void addVersionToTicket(@Nullable Ticket ticket, @Nullable Version version, boolean isFixVersion) {
        if (ticket == null || version == null) {
            throw new IllegalArgumentException("Ticket and version cannot be null");
        }

        if (isFixVersion) {
            ticket.addFixedVersion(version);
        } else {
            ticket.addAffectedVersion(version);
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
    private static void setDerivedVersions(@Nullable Ticket ticket, @Nullable Map<String, Version> versionMap) {
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }

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
    private static void setInjectedVersionFromAffectedVersions(@Nullable Ticket ticket) {
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
    private static void setOpeningVersion(@Nullable Ticket ticket, @Nullable Map<String, Version> versionMap) {
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