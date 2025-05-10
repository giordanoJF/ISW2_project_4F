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

public class JiraService {
    private static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

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
            Version version = new Version();
            version.setId(versionJson.getString("id"));
            version.setName(versionJson.getString("name"));
            version.setReleased(versionJson.getBoolean("released"));
            version.setArchived(versionJson.optBoolean("archived", false));

            if (versionJson.has("releaseDate")) {
                try {
                    Date releaseDate = new SimpleDateFormat("yyyy-MM-dd").parse(versionJson.getString("releaseDate"));
                    version.setReleaseDate(releaseDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            versions.add(version);
        }

        return versions;
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
        Map<String, Version> versionMap = new HashMap<>();

        for (Version version : versions) {
            versionMap.put(version.getName(), version);
        }

        // JQL query to get fixed and closed/resolved bug issues
        String jql = "project=" + projectKey +
                " AND issuetype=Bug" +
                " AND resolution=Fixed" +
                " AND status in (Closed, Resolved)";

        String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
        int startAt = 0;
        int maxResults = 100;
        int total;

        do {
            String url = JIRA_BASE_URL + "/search?jql=" + encodedJql +
                    "&startAt=" + startAt +
                    "&maxResults=" + maxResults +
                    "&fields=key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";

            String jsonResponse = executeGetRequest(url);
            JSONObject responseObj = new JSONObject(jsonResponse);

            total = responseObj.getInt("total");
            JSONArray issuesArray = responseObj.getJSONArray("issues");

            for (int i = 0; i < issuesArray.length(); i++) {
                JSONObject issueJson = issuesArray.getJSONObject(i);
                Ticket ticket = parseTicket(issueJson, versionMap);
                tickets.add(ticket);
            }

            startAt += maxResults;
        } while (startAt < total);

        return tickets;
    }

    /**
     * Parses a JSON issue into a Ticket object.
     */
    private Ticket parseTicket(JSONObject issueJson, Map<String, Version> versionMap) {
        Ticket ticket = new Ticket();
        ticket.setKey(issueJson.getString("key"));

        JSONObject fields = issueJson.getJSONObject("fields");

        // Set basic fields
        ticket.setSummary(fields.optString("summary", null));
        ticket.setDescription(fields.optString("description", null));

        // Set created date
        try {
            if (fields.has("created")) {
                ticket.setCreatedDate(DATE_FORMAT.parse(fields.getString("created"))); //different dateformat from the version, because is possible to have more tickets in the same day
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Set resolution date
        try {
            if (fields.has("resolutiondate") && !fields.isNull("resolutiondate")) {
                ticket.setResolutionDate(DATE_FORMAT.parse(fields.getString("resolutiondate")));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Set status
        if (fields.has("status") && !fields.isNull("status")) {
            ticket.setStatus(fields.getJSONObject("status").getString("name"));
        }

        // Set resolution
        if (fields.has("resolution") && !fields.isNull("resolution")) {
            ticket.setResolution(fields.getJSONObject("resolution").getString("name"));
        }

        // Set fixed versions (FV)
        if (fields.has("fixVersions")) {
            JSONArray fixVersionsArray = fields.getJSONArray("fixVersions");
            for (int i = 0; i < fixVersionsArray.length(); i++) {
                JSONObject versionJson = fixVersionsArray.getJSONObject(i);
                String versionName = versionJson.getString("name");
                if (versionMap.containsKey(versionName)) {
                    ticket.addFixedVersion(versionMap.get(versionName));
                }
            }
        }

        // Set affected versions (AV)
        if (fields.has("versions")) {
            JSONArray versionsArray = fields.getJSONArray("versions");
            for (int i = 0; i < versionsArray.length(); i++) {
                JSONObject versionJson = versionsArray.getJSONObject(i);
                String versionName = versionJson.getString("name");
                if (versionMap.containsKey(versionName)) {
                    ticket.addAffectedVersion(versionMap.get(versionName));
                }
            }
        }

        // Set injected version (IV) as the oldest AV
        if (ticket.getAffectedVersions() != null && !ticket.getAffectedVersions().isEmpty()) {
            Version oldestVersion = ticket.getAffectedVersions().getFirst();

            for (Version version : ticket.getAffectedVersions()) {
                if (version.getReleaseDate() != null &&
                        (oldestVersion.getReleaseDate() == null ||
                                version.getReleaseDate().before(oldestVersion.getReleaseDate()))) {
                    oldestVersion = version;
                }
            }

            ticket.setInjectedVersion(oldestVersion);
        }

        // Set opening version (OV)
        // We need to find the version active at the time the ticket was created
        if (ticket.getCreatedDate() != null) {
            Version latestBeforeCreation = getLatestBeforeCreation(versionMap, ticket);

            if (latestBeforeCreation != null) {
                ticket.setOpeningVersion(latestBeforeCreation);
            }
        }

        return ticket;
    }

    private static Version getLatestBeforeCreation(Map<String, Version> versionMap, Ticket ticket) {
        Version latestBeforeCreation = null;

        for (Version version : versionMap.values()) {
            if (version.getReleaseDate() != null &&
                    !version.getReleaseDate().after(ticket.getCreatedDate())) {
                // Find the most recent version that was released before the ticket was created
                if (latestBeforeCreation == null ||
                        version.getReleaseDate().after(latestBeforeCreation.getReleaseDate())) {
                    latestBeforeCreation = version;
                }
            }
        }
        return latestBeforeCreation;
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