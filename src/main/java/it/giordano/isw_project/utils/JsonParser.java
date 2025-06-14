package it.giordano.isw_project.utils;

import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;
import it.giordano.isw_project.services.JiraScraperService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JsonParser {

    @Nonnull private static final Logger LOGGER = Objects.requireNonNull(Logger.getLogger(JsonParser.class.getName()));

    /** JSON field name for version ID */
    private static final String FIELD_ID = "id";

    /** JSON field name for version name */
    private static final String FIELD_NAME = "name";

    /** JSON field name for version release date */
    private static final String FIELD_RELEASE_DATE = "releaseDate";

    /** JSON field name for ticket key */
    private static final String FIELD_KEY = "key";

    /** JSON field name for fields object */
    private static final String FIELDS = "fields";

    /** JSON field name for ticket creation date */
    private static final String FIELD_CREATED_DATE = "created";

    /** JSON field name for ticket resolution date */
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";

    /** JSON field name for fix versions */
    private static final String FIELD_FIX_VERSIONS = "fixVersions";

    /** JSON field name for affected versions */
    private static final String FIELD_VERSIONS = "versions"; // Affected Versions

    /** Date format pattern for version release dates */
    private static final String VERSION_DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    /** Date format pattern for ticket timestamps */
    private static final String TICKET_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /** Log message when no fields are found for a ticket */
    private static final String NO_FIELDS_FOUND = "No fields found for ticket {0}\n";


    private JsonParser() {
        throw new IllegalStateException("Utility class");
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
    public static List<Version> buildVersionsFromJsonResponse(@Nullable String jsonResponse,
                                                              @Nullable String projectKey) {
        if ((jsonResponse == null || jsonResponse.trim().isEmpty())) {
            throw new IllegalArgumentException("JSON str response cannot be null or empty");
        }
        if ((projectKey == null || projectKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }

        List<Version> versions = new ArrayList<>();

        JSONArray versionArray = new JSONArray(jsonResponse);
        if (versionArray.isEmpty()) {
            LOGGER.log(Level.INFO, "No versions found for project {0}\n", projectKey);
            return versions;
        }

        for (int i = 0; i < versionArray.length(); i++) {
            JSONObject versionJson = versionArray.optJSONObject(i);
            if (versionJson != null && !versionJson.isEmpty()) { // probably we are not adding empty versions
                Version version = buildVersionFromJson(versionJson);
                versions.add(version);
            }
        }
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
    private static Version buildVersionFromJson(@Nullable JSONObject versionJson) {
        if (versionJson == null) {
            throw new IllegalArgumentException("Version JSON object cannot be null");
        }
        if (versionJson.isEmpty()) {
            LOGGER.warning("Empty version JSON object encountered\n");
            return new Version(); // Return an empty version if JSON is empty. never reached from the caller.
        }

        Version version = new Version();
        version.setId(versionJson.optString(FIELD_ID, null));
        version.setName(versionJson.optString(FIELD_NAME, null));
        setVersionReleaseDateFromJson(version, versionJson);
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
    private static void setVersionReleaseDateFromJson(@Nullable Version version, @Nullable JSONObject versionJson) {
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
            version.setReleaseDate(DateUtils.strToDate(dateStr, dateFormat));
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "Failed to parse release date for version {0}: {1}\n",
                    new Object[]{version.getName(), dateStr});
            version.setReleaseDate(null);
        }
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
    public static void processJsonIssuesArray(@Nullable JSONArray issuesArray,
                                              @Nullable List<Ticket> tickets,
                                              @Nullable Map<String, Version> versionMap) {

        if (issuesArray == null || tickets == null || versionMap == null) {
            throw new IllegalArgumentException("Issues array, tickets list, and version map cannot be null");
        }
        if (issuesArray.isEmpty()) {
            LOGGER.warning("Issues array is empty\n");
            return; // So the caller returns an empty list of tickets
        }
        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
        }

        for (int i = 0; i < issuesArray.length(); i++) {
            JSONObject issueJson = issuesArray.optJSONObject(i);
            if (issueJson != null) {
                Ticket ticket = buildTicketFromJson(issueJson, versionMap);
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
    private static Ticket buildTicketFromJson(@Nullable JSONObject issueJson,
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
        if (fields == null || fields.isEmpty()) {
            LOGGER.log(Level.WARNING, NO_FIELDS_FOUND, ticket.getKey());
            return ticket; // Return ticket with just the key
        }

        setAllTicketDatesFromJson(ticket, fields);
        setNonDerivedTicketVersionsFromJson(ticket, fields, versionMap);
        TicketUtils.setFixedVersionFromFixedVersions(ticket);
        TicketUtils.setDerivedTicketVersionsFromItsVersions(ticket, versionMap);

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
    private static void setAllTicketDatesFromJson(@Nullable Ticket ticket, @Nullable JSONObject fields) {

        if (ticket == null || fields == null) {
            throw new IllegalArgumentException("Ticket and fields cannot be null");
        }
        if (fields.isEmpty()) {
            LOGGER.log(Level.WARNING, NO_FIELDS_FOUND, ticket.getKey());
            return; // Return without setting dates if fields is empty
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(TICKET_DATE_FORMAT_PATTERN);

        setTicketCreatedDateFromJson(ticket, fields, dateFormat);
        setTicketResolutionDateFromJson(ticket, fields, dateFormat);
    }

    /**
     * Sets the creation date for a ticket from JSON data.
     *
     * @param ticket the ticket to update
     * @param fields the JSON fields object
     * @param dateFormat the date format to use for parsing
     * @throws IllegalArgumentException if any parameter is null
     */
    private static void setTicketCreatedDateFromJson(@Nullable Ticket ticket, @Nullable JSONObject fields,
                                                     @Nullable SimpleDateFormat dateFormat) {
        if (ticket == null || fields == null || dateFormat == null) {
            throw new IllegalArgumentException("Ticket, fields, and date format cannot be null");
        }
        if (fields.isEmpty()) {
            LOGGER.log(Level.WARNING, NO_FIELDS_FOUND, ticket.getKey());
            ticket.setCreatedDate(null); // Set created date to null if fields is empty
            return;
        }

        String createdDateStr = fields.optString(FIELD_CREATED_DATE, null);
        if (!(createdDateStr == null || createdDateStr.trim().isEmpty())) {
            try {
                ticket.setCreatedDate(DateUtils.strToDate(createdDateStr, dateFormat));
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
    private static void setTicketResolutionDateFromJson(@Nullable Ticket ticket, @Nullable JSONObject fields,
                                                        @Nullable SimpleDateFormat dateFormat) {
        if (ticket == null || fields == null || dateFormat == null) {
            throw new IllegalArgumentException("Ticket, fields, and date format cannot be null");
        }
        if (fields.isEmpty()) {
            LOGGER.log(Level.WARNING, NO_FIELDS_FOUND, ticket.getKey());
            ticket.setResolutionDate(null); // Set resolution date to null if fields is empty
            return;
        }

        String resolutionDateStr = fields.optString(FIELD_RESOLUTION_DATE, null);
        if (!(resolutionDateStr == null || resolutionDateStr.trim().isEmpty())) {
            try {
                ticket.setResolutionDate(DateUtils.strToDate(resolutionDateStr, dateFormat));
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
    private static void setNonDerivedTicketVersionsFromJson(@Nullable Ticket ticket, @Nullable JSONObject fields,
                                                            @Nullable Map<String, Version> versionMap) {
        if (ticket == null || fields == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket, fields, and version map cannot be null");
        }

        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
        }

        // TODO: check if correct
        if (fields.isEmpty()) {
            LOGGER.log(Level.WARNING, NO_FIELDS_FOUND, ticket.getKey());
            return; // Return without setting versions if fields is empty.
        }

        // Set fixed versions
        addFixOrAffectedVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_FIX_VERSIONS), versionMap, true);

        // Set affected versions
        addFixOrAffectedVersionsFromJsonArray(ticket, fields.optJSONArray(FIELD_VERSIONS), versionMap, false);
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
    private static void addFixOrAffectedVersionsFromJsonArray(@Nullable Ticket ticket, @Nullable JSONArray versionsArray,
                                                              @Nullable Map<String, Version> versionMap, boolean isFixVersion) {
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }
        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
        }

        if (versionsArray == null || versionsArray.isEmpty()) {
            return;
        }

        // TODO: check if logic is correct
        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionJson = versionsArray.optJSONObject(i);
            if (versionJson != null && !versionJson.isEmpty()) { // Probably we are not adding empty versions
                addFixOrAffectedVersionsFromJson(ticket, versionJson, versionMap, isFixVersion);
            }
        }
    }

    /**
     * Processes a single version JSON object and adds it to the ticket.
     *
     * <p>Extracts the version name from the JSON and looks it up in the version map.
     * If found, adds it to the ticket as either a fix version or an affected version.</p>
     *
     * @param ticket the ticket to update
     * @param versionJson the JSON object representing a version
     * @param versionMap the map of versions for lookup
     * @param isFixVersion true if this is a fix version, false for affected version
     * @throws IllegalArgumentException if ticket, versionJson, or versionMap is null
     */
    private static void addFixOrAffectedVersionsFromJson(@Nullable Ticket ticket, @Nullable JSONObject versionJson,
                                                         @Nullable Map<String, Version> versionMap, boolean isFixVersion) {
        if (ticket == null || versionJson == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket, version JSON, and version map cannot be null");
        }
        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
        }
        if (versionJson.isEmpty()) {
            LOGGER.log(Level.WARNING, "Empty version JSON object encountered for ticket {0}\n", ticket.getKey());
            return; // Skip empty version JSON objects
        }

        String versionName = versionJson.optString(FIELD_NAME, null);

        if (!(versionName != null && !versionName.trim().isEmpty())) {
            return;
        }

        Version version = versionMap.get(versionName);
        if (version != null) {
            JiraScraperService.addFixedOrAffectedToTicket(ticket, version, isFixVersion);
        }
    }
}
