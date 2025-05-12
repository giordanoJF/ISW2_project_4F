
package it.giordano.ISW2project4F.service;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;
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

public class JiraService {
    private static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";
    private static final SimpleDateFormat JIRA_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final SimpleDateFormat VERSION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger LOGGER = Logger.getLogger(JiraService.class.getName());

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_RELEASED = "released";
    private static final String FIELD_ARCHIVED = "archived";
    private static final String FIELD_RELEASE_DATE = "releaseDate";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RESOLUTION = "resolution";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_FIX_VERSIONS = "fixVersions";
    private static final String FIELD_VERSIONS = "versions";

    private static final int MAX_RESULTS_PER_PAGE = 100;

    /**
     * Retrieves all versions of a project from Jira.
     *
     * @param projectKey The key of the project
     * @return List of versions for the project
     * @throws IOException If an error occurs during the HTTP request
     */
    public List<Version> getProjectVersions(String projectKey) throws IOException {
        List<Version> versions = new ArrayList<>();
        String url = JIRA_BASE_URL + "/project/" + projectKey + "/versions";
        String jsonResponse = executeGetRequest(url);

        JSONArray versionArray = new JSONArray(jsonResponse);
        for (int i = 0; i < versionArray.length(); i++) {
            JSONObject versionJson = versionArray.getJSONObject(i);
            versions.add(parseVersionFromJson(versionJson));
        }

        return versions;
    }

    /**
     * Parses a JSON object into a Version entity.
     */
    private Version parseVersionFromJson(JSONObject versionJson) {
        Version version = new Version();
        version.setId(versionJson.getString(FIELD_ID));
        version.setName(versionJson.getString(FIELD_NAME));
        version.setReleased(versionJson.getBoolean(FIELD_RELEASED));
        version.setArchived(versionJson.optBoolean(FIELD_ARCHIVED, false));

        if (versionJson.has(FIELD_RELEASE_DATE)) {
            version.setReleaseDate(parseDate(versionJson.getString(FIELD_RELEASE_DATE), VERSION_DATE_FORMAT));
        }

        return version;
    }

    /**
     * Retrieves bug tickets from Jira that are fixed and closed/resolved.
     *
     * @param projectKey The key of the project
     * @return List of tickets meeting the criteria
     * @throws IOException If an error occurs during the HTTP request
     */
    public List<Ticket> retrieveTickets(String projectKey) throws IOException {
        List<Ticket> tickets = new ArrayList<>();
        List<Version> versions = getProjectVersions(projectKey);
        Map<String, Version> versionMap = createVersionMap(versions);

        String jql = buildJqlQuery(projectKey);
        String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);

        int startAt = 0;
        int total;

        do {
            String url = buildSearchUrl(encodedJql, startAt, MAX_RESULTS_PER_PAGE);
            String jsonResponse = executeGetRequest(url);

            JSONObject responseObj = new JSONObject(jsonResponse);
            total = responseObj.getInt("total");
            JSONArray issuesArray = responseObj.getJSONArray("issues");

            for (int i = 0; i < issuesArray.length(); i++) {
                JSONObject issueJson = issuesArray.getJSONObject(i);
                Ticket ticket = parseTicket(issueJson, versionMap);
                tickets.add(ticket);
            }

            startAt += MAX_RESULTS_PER_PAGE;
        } while (startAt < total);

        return tickets;
    }

    /**
     * Creates a map of version names to Version objects.
     */
    private Map<String, Version> createVersionMap(List<Version> versions) {
        Map<String, Version> versionMap = new HashMap<>();
        for (Version version : versions) {
            versionMap.put(version.getName(), version);
        }
        return versionMap;
    }

    /**
     * Builds the JQL query for retrieving bug tickets.
     */
    private String buildJqlQuery(String projectKey) {
        return "project=" + projectKey +
                " AND issuetype=Bug" +
                " AND resolution=Fixed" +
                " AND status in (Closed, Resolved)";
    }

    /**
     * Builds the search URL with pagination parameters.
     */
    private String buildSearchUrl(String encodedJql, int startAt, int maxResults) {
        return JIRA_BASE_URL + "/search?jql=" + encodedJql +
                "&startAt=" + startAt +
                "&maxResults=" + maxResults +
                "&fields=key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions"; //maybe use constants?
    }

    /**
     * Parses a JSON issue into a Ticket object.
     */
    private Ticket parseTicket(JSONObject issueJson, Map<String, Version> versionMap) {
        Ticket ticket = new Ticket();
        ticket.setKey(issueJson.getString("key"));
        JSONObject fields = issueJson.getJSONObject("fields");

        setBasicTicketFields(ticket, fields);
        setTicketDates(ticket, fields);
        setTicketStatusAndResolution(ticket, fields);
        setTicketVersions(ticket, fields, versionMap);
        setDerivedVersions(ticket, versionMap);

        return ticket;
    }

    /**
     * Sets the basic fields of a ticket.
     */
    private void setBasicTicketFields(Ticket ticket, JSONObject fields) {
        ticket.setSummary(fields.optString(FIELD_SUMMARY, null));
        ticket.setDescription(fields.optString(FIELD_DESCRIPTION, null));
    }

    /**
     * Sets the dates of a ticket.
     */
    private void setTicketDates(Ticket ticket, JSONObject fields) {
        if (fields.has(FIELD_CREATED)) {
            ticket.setCreatedDate(parseDate(fields.getString(FIELD_CREATED), JIRA_DATE_FORMAT));
        }

        if (fields.has(FIELD_RESOLUTION_DATE) && !fields.isNull(FIELD_RESOLUTION_DATE)) {
            ticket.setResolutionDate(parseDate(fields.getString(FIELD_RESOLUTION_DATE), JIRA_DATE_FORMAT));
        }
    }

    /**
     * Sets the status and resolution of a ticket.
     */
    private void setTicketStatusAndResolution(Ticket ticket, JSONObject fields) {
        if (fields.has(FIELD_STATUS) && !fields.isNull(FIELD_STATUS)) {
            ticket.setStatus(fields.getJSONObject(FIELD_STATUS).getString(FIELD_NAME));
        }

        if (fields.has(FIELD_RESOLUTION) && !fields.isNull(FIELD_RESOLUTION)) {
            ticket.setResolution(fields.getJSONObject(FIELD_RESOLUTION).getString(FIELD_NAME));
        }
    }

    /**
     * Sets the fixed and affected versions of a ticket.
     */
    private void setTicketVersions(Ticket ticket, JSONObject fields, Map<String, Version> versionMap) {
        if (fields.has(FIELD_FIX_VERSIONS)) {
            addVersionsToTicket(ticket, fields.getJSONArray(FIELD_FIX_VERSIONS), versionMap, ticket::addFixedVersion);
        }

        if (fields.has(FIELD_VERSIONS)) {
            addVersionsToTicket(ticket, fields.getJSONArray(FIELD_VERSIONS), versionMap, ticket::addAffectedVersion);
        }
    }

    /**
     * Functional interface for adding versions to a ticket.
     */
    @FunctionalInterface
    private interface VersionAdder {
        void add(Version version);
    }

    /**
     * Adds versions from a JSON array to a ticket using the provided adder function.
     */
    private void addVersionsToTicket(Ticket ticket, JSONArray versionsArray, Map<String, Version> versionMap, VersionAdder adder) {
        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionJson = versionsArray.getJSONObject(i);
            String versionName = versionJson.getString(FIELD_NAME);
            if (versionMap.containsKey(versionName)) {
                adder.add(versionMap.get(versionName));
            }
        }
    }

    /**
     * Sets the derived versions (injected version and opening version) of a ticket.
     */
    private void setDerivedVersions(Ticket ticket, Map<String, Version> versionMap) {
        setInjectedVersion(ticket);
        setOpeningVersion(ticket, versionMap);
    }

    /**
     * Sets the injected version of a ticket as the oldest affected version.
     */
    private void setInjectedVersion(Ticket ticket) {
        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions != null && !affectedVersions.isEmpty()) {
            Version oldestVersion = findOldestVersion(affectedVersions);
            ticket.setInjectedVersion(oldestVersion);
        }
    }

    /**
     * Finds the oldest version from a list of versions.
     */
    private Version findOldestVersion(List<Version> versions) {
        Version oldestVersion = versions.getFirst();
        for (Version version : versions) {
            if (version.getReleaseDate() != null &&
                    (oldestVersion.getReleaseDate() == null ||
                            version.getReleaseDate().before(oldestVersion.getReleaseDate()))) {
                oldestVersion = version;
            }
        }
        return oldestVersion;
    }

    /**
     * Sets the opening version of a ticket.
     */
    private void setOpeningVersion(Ticket ticket, Map<String, Version> versionMap) {
        if (ticket.getCreatedDate() != null) {
            Version latestBeforeCreation = findLatestVersionBeforeDate(versionMap.values(), ticket.getCreatedDate());
            ticket.setOpeningVersion(latestBeforeCreation);
        }
    }

    /**
     * Finds the latest version released before a given date.
     */
    private Version findLatestVersionBeforeDate(Collection<Version> versions, Date date) {
        Version latestVersion = null;
        for (Version version : versions) {
            if (version.getReleaseDate() != null && !version.getReleaseDate().after(date)) {
                if (latestVersion == null || version.getReleaseDate().after(latestVersion.getReleaseDate())) {
                    latestVersion = version;
                }
            }
        }
        return latestVersion;
    }

    /**
     * Parses a date string using the provided date format.
     */
    private Date parseDate(String dateString, SimpleDateFormat dateFormat) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "Failed to parse date: " + dateString, e);
            return null;
        }
    }

    /**
     * Executes a GET request to the specified URL.
     */
    private String executeGetRequest(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            }
        }
    }
}