package it.giordano.ISW2project4F.util;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CsvExporter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String OUTPUT_DIR = "target";
    private static final String CSV_EXTENSION = ".csv";
    private static final String CSV_SEPARATOR = ",";

    /**
     * Exports versions to a CSV file in the target folder.
     *
     * @param versions List of versions to export
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     * @throws IOException If an error occurs during file creation
     */
    public static String exportVersionsAsCsv(List<Version> versions, String projectKey) throws IOException {
        String fileName = createFileName(projectKey, "versions");
        ensureDirectoryExists();

        try (CsvWriter writer = new CsvWriter(fileName)) {
            // Write header
            writer.writeLine("ID", "Name", "Released", "Archived", "ReleaseDate");

            // Write data
            for (Version version : versions) {
                writer.writeLine(
                        version.getId(),
                        version.getName(),
                        String.valueOf(version.isReleased()),
                        String.valueOf(version.isArchived()),
                        formatDate(version.getReleaseDate())
                );
            }
        }

        return fileName;
    }

    /**
     * Exports tickets to a CSV file in the target folder.
     *
     * @param tickets List of tickets to export
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     * @throws IOException If an error occurs during file creation
     */
    public static String exportTicketsAsCsv(List<Ticket> tickets, String projectKey) throws IOException {
        String fileName = createFileName(projectKey, "tickets");
        ensureDirectoryExists();

        try (CsvWriter writer = new CsvWriter(fileName)) {
            // Write header
            writer.writeLine(
                    "Key", "Summary", "Status", "Resolution", "CreatedDate",
                    "ResolutionDate", "OpeningVersion", "FixedVersions",
                    "InjectedVersion", "AffectedVersions"
            );

            // Write data
            for (Ticket ticket : tickets) {
                writer.writeLine(
                        ticket.getKey(),
                        ticket.getSummary(),
                        ticket.getStatus(),
                        ticket.getResolution(),
                        formatDate(ticket.getCreatedDate()),
                        formatDate(ticket.getResolutionDate()),
                        getVersionName(ticket.getOpeningVersion()),
                        formatVersionsList(ticket.getFixedVersions()),
                        getVersionName(ticket.getInjectedVersion()),
                        formatVersionsList(ticket.getAffectedVersions())
                );
            }
        }

        return fileName;
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
