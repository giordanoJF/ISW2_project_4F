package it.giordano.ISW2project4F.util;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;
import it.giordano.ISW2project4F.service.TicketCleaningService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CsvExporter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Exports versions to a CSV file in the target folder.
     *
     * @param versions List of versions to export
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     * @throws IOException If an error occurs during file creation
     */
    public static String exportVersionsAsCsv(List<Version> versions, String projectKey) throws IOException {
        String fileName = "target/" + projectKey + "_versions.csv";
        File directory = new File("target");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            // Write header
            writer.append("ID,Name,Released,Archived,ReleaseDate\n");

            // Write data
            for (Version version : versions) {
                writer.append(version.getId())
                        .append(",")
                        .append(version.getName())
                        .append(",")
                        .append(String.valueOf(version.isReleased()))
                        .append(",")
                        .append(String.valueOf(version.isArchived()))
                        .append(",");

                if (version.getReleaseDate() != null) {
                    writer.append(DATE_FORMAT.format(version.getReleaseDate()));
                }

                writer.append("\n");
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
        String fileName = "target/" + projectKey + "_tickets.csv";
        File directory = new File("target");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            // Write header
            writer.append("Key,Summary,Status,Resolution,CreatedDate,ResolutionDate,OpeningVersion,FixedVersions,InjectedVersion,AffectedVersions\n");

            // Write data
            for (Ticket ticket : tickets) {
                writer.append(escapeCsvField(ticket.getKey()))
                        .append(",")
                        .append(escapeCsvField(ticket.getSummary()))
                        .append(",")
                        .append(escapeCsvField(ticket.getStatus()))
                        .append(",")
                        .append(escapeCsvField(ticket.getResolution()))
                        .append(",");

                // CreatedDate
                if (ticket.getCreatedDate() != null) {
                    writer.append(DATE_FORMAT.format(ticket.getCreatedDate()));
                }
                writer.append(",");

                // ResolutionDate
                if (ticket.getResolutionDate() != null) {
                    writer.append(DATE_FORMAT.format(ticket.getResolutionDate()));
                }
                writer.append(",");

                // OpeningVersion
                if (ticket.getOpeningVersion() != null) {
                    writer.append(escapeCsvField(ticket.getOpeningVersion().getName()));
                }
                writer.append(",");

                // FixedVersions - Updated to handle multiple versions
                if (ticket.getFixedVersions() != null && !ticket.getFixedVersions().isEmpty()) {
                    List<String> versionNames = new ArrayList<>();
                    for (Version version : ticket.getFixedVersions()) {
                        versionNames.add(version.getName());
                    }
                    writer.append(escapeCsvField(String.join(";", versionNames)));
                }
                writer.append(",");

                // InjectedVersion
                if (ticket.getInjectedVersion() != null) {
                    writer.append(escapeCsvField(ticket.getInjectedVersion().getName()));
                }
                writer.append(",");

                // AffectedVersions
                if (ticket.getAffectedVersions() != null && !ticket.getAffectedVersions().isEmpty()) {
                    List<String> versionNames = new ArrayList<>();
                    for (Version version : ticket.getAffectedVersions()) {
                        versionNames.add(version.getName());
                    }
                    writer.append(escapeCsvField(String.join(";", versionNames)));
                }

                writer.append("\n");
            }
        }

        return fileName;
    }

    /**
     * Escape special characters in CSV fields.
     */
    private static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // If the field contains comma, newline, or double quote, escape it
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    /**
     * Exports removed tickets to a CSV file in the target folder.
     *
     * @param removedTickets List of removed tickets with reasons
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     * @throws IOException If an error occurs during file creation
     */
    public static String exportRemovedTicketsAsCsv(List<TicketCleaningService.RemovedTicket> removedTickets, String projectKey) throws IOException {
        String fileName = "target/" + projectKey + "_removed_tickets.csv";
        File directory = new File("target");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            // Write header
            writer.append("Key,Summary,RemovalReason\n");

            // Write data
            for (TicketCleaningService.RemovedTicket removedTicket : removedTickets) {
                Ticket ticket = removedTicket.getTicket();
                writer.append(escapeCsvField(ticket.getKey()))
                        .append(",")
                        .append(escapeCsvField(ticket.getSummary()))
                        .append(",")
                        .append(escapeCsvField(removedTicket.getReason()))
                        .append("\n");
            }
        }

        return fileName;
    }

}