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

    private static final String FIELD_KEY = "key";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CREATED_DATE = "created";
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RESOLUTION = "resolution";
    private static final String FIELD_FIX_VERSIONS = "fixVersions";
    private static final String FIELD_VERSIONS = "versions"; //AV

    private static final int MAX_RESULTS_PER_PAGE = 100;
    private static final String TOTAL = "total";
    private static final String ISSUES = "issues";
    private static final String FIELDS = "fields";

    /**
     * Retrieves all versions of a project from Jira.
     *
     * @param projectKey The key of the project
     * @return List of versions for the project
     * @throws IOException If an error occurs during the HTTP request
     */
    public static List<Version> getProjectVersions(String projectKey) throws IOException, ParseException {
        List<Version> versions = new ArrayList<>();
        String url = JIRA_BASE_URL + "/project/" + projectKey + "/versions";
        String jsonResponse = executeGetRequest(url);

        JSONArray versionArray = new JSONArray(jsonResponse);
        for (int i = 0; i < versionArray.length(); i++) {
            JSONObject versionJson = versionArray.optJSONObject(i);
            if (versionJson != null) {
                versions.add(parseVersionFromJson(versionJson));
            }
        }

        return versions;
    }

    /**
     * Parses a JSON object into a Version entity.
     */
    private static Version parseVersionFromJson(JSONObject versionJson) throws ParseException {
        Version version = new Version();

        version.setId(versionJson.optString(FIELD_ID));
        version.setName(versionJson.optString(FIELD_NAME));
        version.setReleased(versionJson.optBoolean(FIELD_RELEASED));
        version.setArchived(versionJson.optBoolean(FIELD_ARCHIVED));

        String dateStr = versionJson.optString(FIELD_RELEASE_DATE);
        if (dateStr != null && !dateStr.isEmpty()) {
            version.setReleaseDate(parseDate(dateStr, VERSION_DATE_FORMAT));
        } else {
            version.setReleaseDate(null);
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
    public static List<Ticket> getProjectTickets(String projectKey) throws IOException, ParseException {
        List<Ticket> tickets = new ArrayList<>();
        List<Version> versions = getProjectVersions(projectKey);
        Map<String, Version> versionMap = createVersionMap(versions);

        String jql = "project=" + projectKey +
                " AND issuetype=Bug" +
                " AND resolution=Fixed" +
                " AND status in (Closed, Resolved)";
        String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);

        int startAt = 0;
        int total;

        do {
            String url = JIRA_BASE_URL + "/search?jql=" + encodedJql +
                "&startAt=" + startAt +
                "&maxResults=" + MAX_RESULTS_PER_PAGE +
                "&fields=key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
            
            String jsonResponse = executeGetRequest(url);
            JSONObject responseObj = new JSONObject(jsonResponse);
            total = responseObj.getInt(TOTAL);
            JSONArray issuesArray = responseObj.getJSONArray(ISSUES);

            if (issuesArray != null) {
                for (int i = 0; i < issuesArray.length(); i++) {
                    JSONObject issueJson = issuesArray.optJSONObject(i);
                    if (issueJson != null) {
                        tickets.add(parseTicket(issueJson, versionMap));
                    }
                }
            }

            startAt += MAX_RESULTS_PER_PAGE;
        } while (startAt < total);

        logTicketStatistics(tickets);

        return tickets;
    }

    /**
     * Creates a map of version names to Version objects.
     */
    private static Map<String, Version> createVersionMap(List<Version> versions) {
        Map<String, Version> versionMap = new HashMap<>();
        for (Version version : versions) {
            versionMap.put(version.getName(), version);
        }
        return versionMap;
    }

    /**
     * Parses a JSON issue into a Ticket object.
     */
    private static Ticket parseTicket(JSONObject issueJson, Map<String, Version> versionMap) throws ParseException {
        Ticket ticket = new Ticket();
        ticket.setKey(issueJson.optString(FIELD_KEY));
        JSONObject fields = issueJson.optJSONObject(FIELDS);
        
        if (fields == null) {
            return ticket;
        }

        // Set basic fields
        ticket.setSummary(fields.optString(FIELD_SUMMARY));
        ticket.setDescription(fields.optString(FIELD_DESCRIPTION));

        // Set dates
        String createdDateStr = fields.optString(FIELD_CREATED_DATE);
        if (createdDateStr != null && !createdDateStr.isEmpty()) {
            ticket.setCreatedDate(parseDate(createdDateStr, JIRA_DATE_FORMAT));
        }
        
        String resolutionDateStr = fields.optString(FIELD_RESOLUTION_DATE);
        if (resolutionDateStr != null && !resolutionDateStr.isEmpty()) {
            ticket.setResolutionDate(parseDate(resolutionDateStr, JIRA_DATE_FORMAT));
        }

        // Set status and resolution
        JSONObject statusObj = fields.optJSONObject(FIELD_STATUS);
        if (statusObj != null) {
            ticket.setStatus(statusObj.optString(FIELD_NAME));
        }
        
        JSONObject resolutionObj = fields.optJSONObject(FIELD_RESOLUTION);
        if (resolutionObj != null) {
            ticket.setResolution(resolutionObj.optString(FIELD_NAME));
        }

        // Set versions
        setTicketVersions(ticket, fields, versionMap);
        
        // Set derived versions
        setDerivedVersions(ticket, versionMap);

        return ticket;
    }

    /**
     * Sets the fixed and affected versions of a ticket.
     */
    private static void setTicketVersions(Ticket ticket, JSONObject fields, Map<String, Version> versionMap) {
        // Add fixed versions
        JSONArray fixVersions = fields.optJSONArray(FIELD_FIX_VERSIONS);
        if (fixVersions != null) {
            for (int i = 0; i < fixVersions.length(); i++) {
                JSONObject versionJson = fixVersions.optJSONObject(i);
                if (versionJson != null) {
                    String versionName = versionJson.optString(FIELD_NAME);
                    if (!versionName.isEmpty() && versionMap.containsKey(versionName)) {
                        ticket.addFixedVersion(versionMap.get(versionName));
                    }
                }
            }
        }

        // Add affected versions
        JSONArray versionsArray = fields.optJSONArray(FIELD_VERSIONS);
        if (versionsArray != null) {
            for (int i = 0; i < versionsArray.length(); i++) {
                JSONObject versionJson = versionsArray.optJSONObject(i);
                if (versionJson != null) {
                    String versionName = versionJson.optString(FIELD_NAME);
                    if (!versionName.isEmpty() && versionMap.containsKey(versionName)) {
                        ticket.addAffectedVersion(versionMap.get(versionName));
                    }
                }
            }
        }
    }

    /**
     * Sets the derived versions (injected version and opening version) of a ticket.
     */
    private static void setDerivedVersions(Ticket ticket, Map<String, Version> versionMap) {
        // Set injected version (oldest affected version)
        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions != null && !affectedVersions.isEmpty()) {
            Version oldestVersion = affectedVersions.getFirst();
            for (Version version : affectedVersions) {
                if (version.getReleaseDate() != null &&
                        (oldestVersion.getReleaseDate() == null ||
                                version.getReleaseDate().before(oldestVersion.getReleaseDate()))) {
                    oldestVersion = version;
                }
            }
            ticket.setInjectedVersion(oldestVersion);
        }

        // Set opening version (latest version released before ticket creation)
        if (ticket.getCreatedDate() != null) {
            Version latestVersion = null;
            for (Version version : versionMap.values()) {
                if (version.getReleaseDate() != null && !version.getReleaseDate().after(ticket.getCreatedDate())) {
                    if (latestVersion == null || version.getReleaseDate().after(latestVersion.getReleaseDate())) {
                        latestVersion = version;
                    }
                }
            }
            ticket.setOpeningVersion(latestVersion);
        }
    }

    /**
     * Parses a date string using the provided date format.
     * @throws ParseException if the date string cannot be parsed
     */
    private static Date parseDate(String dateString, SimpleDateFormat dateFormat) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return dateFormat.parse(dateString);
    }


    /**
     * Executes a GET request to the specified URL.
     */
    private static String executeGetRequest(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            }
        }
    }

    private static void logTicketStatistics(List<Ticket> tickets) {
        int totalTickets = tickets.size();
        int ticketsWithoutIV = 0;
        int ticketsWithoutOV = 0;
        int ticketsWithoutAV = 0;
        int ticketsWithoutFV = 0;

        for (Ticket ticket : tickets) {
            if (ticket.getInjectedVersion() == null || ticket.getInjectedVersion().getName().isEmpty())
                ticketsWithoutIV++;
            if (ticket.getOpeningVersion() == null || ticket.getOpeningVersion().getName().isEmpty())
                ticketsWithoutOV++;
            if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty())
                ticketsWithoutAV++;
            if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty())
                ticketsWithoutFV++;
        }

        LOGGER.log(Level.INFO, """
                        Ticket statistics for {0} tickets:
                        - Missing Injected Version: {1} ({2}%)
                        - Missing Opening Version: {3} ({4}%)
                        - Missing Affected Versions: {5} ({6}%)
                        - Missing Fixed Versions: {7} ({8}%)""",
                new Object[]{
                        totalTickets,
                        ticketsWithoutIV, ticketsWithoutIV * 100.0 / totalTickets,
                        ticketsWithoutOV, ticketsWithoutOV * 100.0 / totalTickets,
                        ticketsWithoutAV, ticketsWithoutAV * 100.0 / totalTickets,
                        ticketsWithoutFV, ticketsWithoutFV * 100.0 / totalTickets
                });
    }



}