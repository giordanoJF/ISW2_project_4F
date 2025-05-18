package it.giordano.ISW2project4F.util;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CsvExporter {
    private static final Logger LOGGER = Logger.getLogger(CsvExporter.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String OUTPUT_DIR = "target";
    private static final String CSV_EXTENSION = ".csv";
    private static final String CSV_SEPARATOR = ",";

    // Column selection flags for ticket export
    public static boolean INCLUDE_KEY = true;
    public static boolean INCLUDE_SUMMARY = false;
    public static boolean INCLUDE_STATUS = false;
    public static boolean INCLUDE_RESOLUTION = false;
    public static boolean INCLUDE_CREATED_DATE = true;
    public static boolean INCLUDE_RESOLUTION_DATE = true;
    public static boolean INCLUDE_OPENING_VERSION = true;
    public static boolean INCLUDE_FIXED_VERSIONS = true;
    public static boolean INCLUDE_INJECTED_VERSION = true;
    public static boolean INCLUDE_AFFECTED_VERSIONS = true;

    // Column selection flags for version export
    public static boolean INCLUDE_VERSION_ID = true;
    public static boolean INCLUDE_VERSION_NAME = true;
    public static boolean INCLUDE_VERSION_RELEASED = true;
    public static boolean INCLUDE_VERSION_ARCHIVED = true;
    public static boolean INCLUDE_VERSION_RELEASE_DATE = true;


    /**
     * Exports versions to a CSV file in the target folder.
     *
     * @param versions   List of versions to export
     * @param projectKey The project key for naming the file
     * @throws IOException If an error occurs during file creation
     */
    public static void exportVersionsAsCsv(List<Version> versions, String projectKey) throws IOException {
        String fileName = createFileName(projectKey, "versions");
        ensureDirectoryExists();

        try (CsvWriter writer = new CsvWriter(fileName)) {
            // Build header based on include flags
            List<String> headers = new ArrayList<>();
            if (INCLUDE_VERSION_ID) headers.add("ID");
            if (INCLUDE_VERSION_NAME) headers.add("Name");
            if (INCLUDE_VERSION_RELEASED) headers.add("Released");
            if (INCLUDE_VERSION_ARCHIVED) headers.add("Archived");
            if (INCLUDE_VERSION_RELEASE_DATE) headers.add("ReleaseDate");

            writer.writeLine(headers.toArray(new String[0]));

            // Write data
            for (Version version : versions) {
                List<String> values = new ArrayList<>();

                if (INCLUDE_VERSION_ID) values.add(version.getId());
                if (INCLUDE_VERSION_NAME) values.add(version.getName());
                if (INCLUDE_VERSION_RELEASED) values.add(String.valueOf(version.isReleased()));
                if (INCLUDE_VERSION_ARCHIVED) values.add(String.valueOf(version.isArchived()));
                if (INCLUDE_VERSION_RELEASE_DATE) values.add(formatDate(version.getReleaseDate()));

                writer.writeLine(values.toArray(new String[0]));
            }
        }

        LOGGER.info("Exported versions CSV file: " + fileName);
    }


    /**
     * Exports tickets to a CSV file in the target folder.
     *
     * @param tickets    List of tickets to export
     * @param projectKey The project key for naming the file
     * @throws IOException If an error occurs during file creation
     */
    public static void exportTicketsAsCsv(List<Ticket> tickets, String projectKey) throws IOException {
        String fileName = createFileName(projectKey, "tickets");
        ensureDirectoryExists();

        try (CsvWriter writer = new CsvWriter(fileName)) {
            List<String> headers = getHeaders();

            writer.writeLine(headers.toArray(new String[0]));

            // Write data
            for (Ticket ticket : tickets) {
                List<String> values = new ArrayList<>();

                if (INCLUDE_KEY) values.add(ticket.getKey());
                if (INCLUDE_SUMMARY) values.add(ticket.getSummary());
                if (INCLUDE_STATUS) values.add(ticket.getStatus());
                if (INCLUDE_RESOLUTION) values.add(ticket.getResolution());
                if (INCLUDE_CREATED_DATE) values.add(formatDate(ticket.getCreatedDate()));
                if (INCLUDE_RESOLUTION_DATE) values.add(formatDate(ticket.getResolutionDate()));
                if (INCLUDE_OPENING_VERSION) values.add(getVersionName(ticket.getOpeningVersion()));
                if (INCLUDE_FIXED_VERSIONS) values.add(formatVersionsList(ticket.getFixedVersions()));
                if (INCLUDE_INJECTED_VERSION) values.add(getVersionName(ticket.getInjectedVersion()));
                if (INCLUDE_AFFECTED_VERSIONS) values.add(formatVersionsList(ticket.getAffectedVersions()));

                writer.writeLine(values.toArray(new String[0]));
            }
        }

        LOGGER.info("Exported tickets CSV file: " + fileName);
    }

    private static List<String> getHeaders() {
        List<String> headers = new ArrayList<>();
        if (INCLUDE_KEY) headers.add("Key");
        if (INCLUDE_SUMMARY) headers.add("Summary");
        if (INCLUDE_STATUS) headers.add("Status");
        if (INCLUDE_RESOLUTION) headers.add("Resolution");
        if (INCLUDE_CREATED_DATE) headers.add("CreatedDate");
        if (INCLUDE_RESOLUTION_DATE) headers.add("ResolutionDate");
        if (INCLUDE_OPENING_VERSION) headers.add("OpeningVersion");
        if (INCLUDE_FIXED_VERSIONS) headers.add("FixedVersions");
        if (INCLUDE_INJECTED_VERSION) headers.add("InjectedVersion");
        if (INCLUDE_AFFECTED_VERSIONS) headers.add("AffectedVersions");
        return headers;
    }


    /**
     * Creates a filename for the CSV export.
     */
    private static String createFileName(String projectKey, String suffix) {
        return OUTPUT_DIR + File.separator + projectKey + "_" + suffix + CSV_EXTENSION;
    }

    /**
     * Ensures the target directory exists.
     */
    private static void ensureDirectoryExists() {
        File directory = new File(OUTPUT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Formats a date according to the standard format or returns empty string.
     */
    private static String formatDate(Date date) {
        return date != null ? DATE_FORMAT.format(date) : "";
    }

    /**
     * Gets a version name safely.
     */
    private static String getVersionName(Version version) {
        return version != null ? version.getName() : "";
    }

    /**
     * Formats a list of versions as a semicolon-separated string.
     */
    private static String formatVersionsList(List<Version> versions) {
        if (versions == null || versions.isEmpty()) {
            return "";
        }

        return versions.stream()
                .map(Version::getName)
                .collect(Collectors.joining(";"));
    }

    /**
     * Helper class to handle CSV writing operations.
     */
    private static class CsvWriter implements AutoCloseable {
        private final FileWriter fileWriter;

        public CsvWriter(String fileName) throws IOException {
            this.fileWriter = new FileWriter(fileName);
        }

        public void writeLine(String... fields) throws IOException {
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    fileWriter.append(CSV_SEPARATOR);
                }
                fileWriter.append(escapeCsvField(fields[i]));
            }
            fileWriter.append("\n");
        }

        @Override
        public void close() throws IOException {
            fileWriter.close();
        }

        /**
         * Escape special characters in CSV fields.
         */
        private String escapeCsvField(String field) {
            if (field == null) {
                return "";
            }

            // If the field contains comma, newline, or double quote, escape it
            if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                return "\"" + field.replace("\"", "\"\"") + "\"";
            }

            return field;
        }
    }
}
