package it.giordano.isw_project.services;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JiraScraperService {

    // API Configuration
    public static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";

    // Logger
    private static final Logger LOGGER = Logger.getLogger(JiraScraperService.class.getName());

    // JSON field names for versions
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_RELEASE_DATE = "releaseDate";

    // Date format patterns
    private static final String VERSION_DATE_FORMAT_PATTERN = "yyyy-MM-dd";


    private JiraScraperService() {
        throw new IllegalStateException("Utility class\n");
    }







    //main methods
    @Nonnull
    public static List<Version> getProjectVersions(@Nonnull String projectKey) throws IOException {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty\n");
        }

        String url = buildVersionsUrl(projectKey);
        if (Consistency.isStrNullOrEmpty(url)) {
            LOGGER.warning("buildVersionsUrl returned null or empty\n");
            return new ArrayList<>();
        }

        String jsonResponse = executeGetRequest(url);
        if (Consistency.isStrNullOrEmpty(jsonResponse)) {
            LOGGER.warning("Empty response from Jira API for project versions\n");
            return new ArrayList<>();
        }

        return parseVersionsFromJsonResponse(jsonResponse, projectKey);
    }












    //parse from JSON methods
    @Nonnull
    private static List<Version> parseVersionsFromJsonResponse(@Nonnull String jsonResponse,@Nonnull String projectKey) {
        List<Version> versions = new ArrayList<>();

        if (Consistency.isStrNullOrEmpty(jsonResponse)) {
            LOGGER.warning("Empty response from Jira API for project versions\n");
            return versions;
        }
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            LOGGER.warning("Project key cannot be null or empty\n");
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

        logVersionsResult(versions, projectKey);
        return versions;
    }

    @Nonnull
    private static Version parseVersionFromJson(@Nonnull JSONObject versionJson) {

        Version version = new Version();
        version.setId(versionJson.optString(FIELD_ID, null));
        version.setName(versionJson.optString(FIELD_NAME, null));
        setVersionReleaseDate(version, versionJson);
        return version;
    }










    //set version fields methods
    private static void setVersionReleaseDate(@Nonnull Version version,@Nonnull JSONObject versionJson) {
        String dateStr = versionJson.optString(FIELD_RELEASE_DATE, null);

        if (Consistency.isStrNullOrEmpty(dateStr)) {
            version.setReleaseDate(null);
            return;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(VERSION_DATE_FORMAT_PATTERN);
            version.setReleaseDate(parseDate(dateStr, dateFormat));
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "Failed to parse release date for version {0}: {1}\n", new Object[]{version.getName(), dateStr});
            version.setReleaseDate(null);
        }
    }








    //url methods
    @Nonnull
    private static String buildVersionsUrl(@Nonnull String projectKey) {
        if (Consistency.isStrNullOrEmpty(projectKey)) {
            throw new IllegalArgumentException("Project key cannot be null or empty\n");
        }
        return String.format("%s/project/%s/versions", JIRA_BASE_URL, projectKey);
    }

    @Nullable
    public static String executeGetRequest(@Nonnull String url) throws IOException {
        if (Consistency.isStrNullOrEmpty(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty\n");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    LOGGER.log(Level.WARNING, "HTTP request failed with status: {0} for URL: {1}\n", new Object[]{statusCode, url});
                    return null;
                }

                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }










    //helper methods
    @Nullable
    public static Date parseDate(@Nullable String dateString, @Nullable SimpleDateFormat dateFormat) throws ParseException {
        if (dateFormat == null) {
            throw new IllegalArgumentException("Date format cannot be null\n");
        }
        if (Consistency.isStrNullOrEmpty(dateString)) {
            return null;
        }
        return dateFormat.parse(dateString);
    }










    // Logging methods
    private static void logVersionsResult(@Nonnull List<Version> versions, @Nonnull String projectKey) {
        if (versions.isEmpty()) {
            LOGGER.log(Level.INFO, "No versions found for project {0}\n", projectKey);
        } else {
            LOGGER.log(Level.INFO, "Retrieved {0} versions for project {1}\n", new Object[]{versions.size(), projectKey});
        }
    }

}
