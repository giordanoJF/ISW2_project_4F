package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility class for exporting data to CSV files.
 */
public class CsvExporter {
    private static final Logger LOGGER = Logger.getLogger(CsvExporter.class.getName());
    private static final String OUTPUT_DIR = "target";
    private static final String CSV_EXTENSION = ".csv";
    private static final String CSV_SEPARATOR = ",";

    // Private constructor to prevent instantiation
    private CsvExporter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Gets a version name safely.
     */
    static String getVersionName(Version version) {
        return version != null ? version.getName() : "";
    }

    /**
     * Configuration class for ticket export columns.
     */
    public static class TicketExportConfig {
        private boolean includeKey = true;
        private boolean includeSummary = false;
        private boolean includeStatus = false;
        private boolean includeResolution = false;
        private boolean includeCreatedDate = true;
        private boolean includeResolutionDate = false;
        private boolean includeOpeningVersion = true;
        private boolean includeFixedVersions = true;
        private boolean includeInjectedVersion = true;
        private boolean includeAffectedVersions = true;

        public TicketExportConfig() {
            // Default configuration
        }

        // Fluent setters
        public TicketExportConfig setIncludeKey(boolean includeKey) {
            this.includeKey = includeKey;
            return this;
        }

        public TicketExportConfig setIncludeSummary(boolean includeSummary) {
            this.includeSummary = includeSummary;
            return this;
        }

        public TicketExportConfig setIncludeStatus(boolean includeStatus) {
            this.includeStatus = includeStatus;
            return this;
        }

        public TicketExportConfig setIncludeResolution(boolean includeResolution) {
            this.includeResolution = includeResolution;
            return this;
        }

        public TicketExportConfig setIncludeCreatedDate(boolean includeCreatedDate) {
            this.includeCreatedDate = includeCreatedDate;
            return this;
        }

        public TicketExportConfig setIncludeResolutionDate(boolean includeResolutionDate) {
            this.includeResolutionDate = includeResolutionDate;
            return this;
        }

        public TicketExportConfig setIncludeOpeningVersion(boolean includeOpeningVersion) {
            this.includeOpeningVersion = includeOpeningVersion;
            return this;
        }

        public TicketExportConfig setIncludeFixedVersions(boolean includeFixedVersions) {
            this.includeFixedVersions = includeFixedVersions;
            return this;
        }

        public TicketExportConfig setIncludeInjectedVersion(boolean includeInjectedVersion) {
            this.includeInjectedVersion = includeInjectedVersion;
            return this;
        }

        public TicketExportConfig setIncludeAffectedVersions(boolean includeAffectedVersions) {
            this.includeAffectedVersions = includeAffectedVersions;
            return this;
        }
    }

    /**
     * Configuration class for version export columns.
     */
    public static class VersionExportConfig {
        private boolean includeId = true;
        private boolean includeName = true;
        private boolean includeReleased = false;
        private boolean includeArchived = false;
        private boolean includeReleaseDate = true;

        public VersionExportConfig() {
            // Default configuration
        }

        // Fluent setters
        public VersionExportConfig setIncludeId(boolean includeId) {
            this.includeId = includeId;
            return this;
        }

        public VersionExportConfig setIncludeName(boolean includeName) {
            this.includeName = includeName;
            return this;
        }

        public VersionExportConfig setIncludeReleased(boolean includeReleased) {
            this.includeReleased = includeReleased;
            return this;
        }

        public VersionExportConfig setIncludeArchived(boolean includeArchived) {
            this.includeArchived = includeArchived;
            return this;
        }

        public VersionExportConfig setIncludeReleaseDate(boolean includeReleaseDate) {
            this.includeReleaseDate = includeReleaseDate;
            return this;
        }
    }

    /**
     * Exports versions to a CSV file in the target folder with default configuration.
     *
     * @param versions   List of versions to export
     * @param projectKey The project key for naming the file
     * @throws IOException If an error occurs during file creation
     */
    public static void exportVersionsAsCsv(List<Version> versions, String projectKey) throws IOException {
        exportVersionsAsCsv(versions, projectKey, new VersionExportConfig());
    }

    /**
     * Exports versions to a CSV file in the target folder with custom configuration.
     *
     * @param versions   List of versions to export
     * @param projectKey The project key for naming the file
     * @param config     Configuration for export columns
     * @throws IOException If an error occurs during file creation
     */
    public static void exportVersionsAsCsv(List<Version> versions, String projectKey, VersionExportConfig config) throws IOException {
        String fileName = createFileName(projectKey, "versions");
        ensureDirectoryExists();
        
        // Create headers
        List<String> headers = createVersionHeaders(config);
        
        try (CsvWriter writer = new CsvWriter(fileName)) {
            // Write headers
            writer.writeLine(headers.toArray(new String[0]));
            
            // Write each version data
            for (Version version : versions) {
                List<String> values = createVersionValues(version, config);
                writer.writeLine(values.toArray(new String[0]));
            }
        }

        LOGGER.log(Level.INFO, "Exported versions CSV file: {0}", fileName);
    }
    
    private static List<String> createVersionHeaders(VersionExportConfig config) {
        List<String> headers = new ArrayList<>();
        if (config.includeId) headers.add("ID");
        if (config.includeName) headers.add("Name");
        if (config.includeReleased) headers.add("Released");
        if (config.includeArchived) headers.add("Archived");
        if (config.includeReleaseDate) headers.add("ReleaseDate");
        return headers;
    }
    
    private static List<String> createVersionValues(Version version, VersionExportConfig config) {
        List<String> values = new ArrayList<>();
        if (config.includeId) values.add(version.getId());
        if (config.includeName) values.add(version.getName());
        if (config.includeReleased) values.add(String.valueOf(version.isReleased()));
        if (config.includeArchived) values.add(String.valueOf(version.isArchived()));
        if (config.includeReleaseDate) values.add(DateUtils.formatDate(version.getReleaseDate()));
        return values;
    }

    /**
     * Exports tickets to a CSV file in the target folder with default configuration.
     *
     * @param tickets    List of tickets to export
     * @param projectKey The project key for naming the file
     * @throws IOException If an error occurs during file creation
     */
    public static void exportTicketsAsCsv(List<Ticket> tickets, String projectKey) throws IOException {
        exportTicketsAsCsv(tickets, projectKey, new TicketExportConfig());
    }

    /**
     * Exports tickets to a CSV file in the target folder with custom configuration.
     *
     * @param tickets    List of tickets to export
     * @param projectKey The project key for naming the file
     * @param config     Configuration for export columns
     * @throws IOException If an error occurs during file creation
     */
    public static void exportTicketsAsCsv(List<Ticket> tickets, String projectKey, TicketExportConfig config) throws IOException {
        String fileName = createFileName(projectKey, "tickets");
        ensureDirectoryExists();
        
        // Create headers
        List<String> headers = createTicketHeaders(config);
        
        try (CsvWriter writer = new CsvWriter(fileName)) {
            // Write headers
            writer.writeLine(headers.toArray(new String[0]));
            
            // Write each ticket data
            for (Ticket ticket : tickets) {
                List<String> values = createTicketValues(ticket, config);
                writer.writeLine(values.toArray(new String[0]));
            }
        }

        LOGGER.log(Level.INFO, "Exported tickets CSV file: {0}", fileName);
    }
    
    private static List<String> createTicketHeaders(TicketExportConfig config) {
        List<String> headers = new ArrayList<>();
        if (config.includeKey) headers.add("Key");
        if (config.includeSummary) headers.add("Summary");
        if (config.includeStatus) headers.add("Status");
        if (config.includeResolution) headers.add("Resolution");
        if (config.includeCreatedDate) headers.add("CreatedDate");
        if (config.includeResolutionDate) headers.add("ResolutionDate");
        if (config.includeOpeningVersion) headers.add("OpeningVersion");
        if (config.includeFixedVersions) headers.add("FixedVersions");
        if (config.includeInjectedVersion) headers.add("InjectedVersion");
        if (config.includeAffectedVersions) headers.add("AffectedVersions");
        return headers;
    }
    
    private static List<String> createTicketValues(Ticket ticket, TicketExportConfig config) {
        List<String> values = new ArrayList<>();
        if (config.includeKey) values.add(ticket.getKey());
        if (config.includeSummary) values.add(ticket.getSummary());
        if (config.includeStatus) values.add(ticket.getStatus());
        if (config.includeResolution) values.add(ticket.getResolution());
        if (config.includeCreatedDate) values.add(DateUtils.formatDate(ticket.getCreatedDate()));
        if (config.includeResolutionDate) values.add(DateUtils.formatDate(ticket.getResolutionDate()));
        if (config.includeOpeningVersion) values.add(getVersionName(ticket.getOpeningVersion()));
        if (config.includeFixedVersions) values.add(formatVersionsList(ticket.getFixedVersions()));
        if (config.includeInjectedVersion) values.add(getVersionName(ticket.getInjectedVersion()));
        if (config.includeAffectedVersions) values.add(formatVersionsList(ticket.getAffectedVersions()));
        return values;
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
            boolean created = directory.mkdirs();
            if (!created) {
                LOGGER.warning("Failed to create output directory: " + OUTPUT_DIR);
            }
        }
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