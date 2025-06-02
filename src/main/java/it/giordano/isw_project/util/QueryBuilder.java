package it.giordano.isw_project.util;

import static it.giordano.isw_project.service.JiraService.JIRA_BASE_URL;
import static it.giordano.isw_project.service.JiraService.MAX_RESULTS_PER_PAGE;

public class QueryBuilder {

    private QueryBuilder() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds the JQL query for retrieving tickets.
     *
     * @param projectKey The project key
     * @return JQL query string
     */
    public static String buildJqlQuery(String projectKey) {
        return "project=" + projectKey +
                " AND issuetype=Bug" +
                " AND resolution=Fixed" +
                " AND status in (Closed, Resolved)";
    }

    /**
     * Builds the URL for the tickets search API.
     *
     * @param encodedJql The encoded JQL query
     * @param startAt The pagination start index
     * @return The URL for the tickets search API
     */
    public static String buildTicketsUrl(String encodedJql, int startAt) {
        return JIRA_BASE_URL + "/search?jql=" + encodedJql +
                "&startAt=" + startAt +
                "&maxResults=" + MAX_RESULTS_PER_PAGE +
                "&fields=key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";
    }

}
