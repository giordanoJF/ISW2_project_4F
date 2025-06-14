package it.giordano.isw_project.services;

import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;
import it.giordano.isw_project.utils.JsonParser;
import it.giordano.isw_project.utils.VersionUtils;
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
 * <p>This class works only with non-empty list of project versions.</p>
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

    // JSON structure constants
    /** JSON field name for total count */
    private static final String TOTAL = "total";

    /** JSON field name for issues array */
    private static final String ISSUES = "issues";



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
    public static List<Version> getProjectVersionsFromJiraApi(@Nullable String projectKey) throws IOException {
        if ((projectKey == null || projectKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        String url = buildVersionsUrl(projectKey);
        if (url.trim().isEmpty()) {
            throw new IllegalArgumentException("Constructed URL cannot be null or empty for project " + projectKey);
        }

        String jsonResponse = executeGetRequest(url);
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new IOException("jsonResponse is null or empty for " + projectKey);
        }

        List<Version> versions = JsonParser.buildVersionsFromJsonResponse(jsonResponse, projectKey);

        logVersionsRetrievalResult(versions, projectKey);

        return versions;
    }

    // VERSIONS METHODS --> Helper methods, all non-reusable outside this class

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
    public static List<Ticket> getProjectTicketsFromJiraApi(@Nullable String projectKey, @Nullable List<Version> versions) throws IOException {
        if ((projectKey == null || projectKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("Versions list cannot be null or empty"); // this method accepts only non-empty/non-null list of versions
        }

        Map<String, Version> versionMap = VersionUtils.createVersionMap(versions);
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
     * Returns the list of required fields for ticket queries.
     *
     * @return a comma-separated string of field names required for ticket analysis
     */
    @Nonnull
    private static String getRequiredFields() {
        return "key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
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
            // TODO: implement a retry mechanism for failed requests
            String url = buildTicketsUrl(encodedJql, startAt);
            if (url.trim().isEmpty()) {
                throw new IllegalArgumentException("Constructed URL cannot be null or empty");
            }

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
                JsonParser.processJsonIssuesArray(issuesArray, tickets, versionMap);
            } else {
                LOGGER.warning("No issues array found in response\n");
            }

            startAt += MAX_RESULTS_PER_PAGE;
        } while (startAt < total);

        LOGGER.log(Level.INFO, "Successfully processed {0} tickets\n", tickets.size());
        return tickets;
    }

    /**
     * Adds a version to a ticket as either a fix version or an affected version.
     *
     * <p>Utility method to encapsulate the logic of adding versions to tickets.</p>
     * <p>Adds also versions with no info inside.</p>
     *
     * @param ticket the ticket to update
     * @param version the version to add
     * @param isFixVersion true if this is a fix version, false for affected version
     * @throws IllegalArgumentException if ticket or version is null
     *
     * TODO: This method could be improved. It seems too specific.
     */
    public static void addFixedOrAffectedToTicket(@Nullable Ticket ticket, @Nullable Version version, boolean isFixVersion) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null");
        }
        if (version == null) {
            LOGGER.warning("Version is null, skipping addition to ticket: " + ticket.getKey());
            return;
        }

        if (isFixVersion) {
            ticket.addFixedVersion(version);
        } else {
            ticket.addAffectedVersion(version);
        }
    }
}