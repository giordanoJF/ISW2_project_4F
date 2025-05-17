package it.giordano.ISW2project4F.controller;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;
import it.giordano.ISW2project4F.service.JiraService;
import it.giordano.ISW2project4F.util.CsvExporter;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JiraController {
    private static final Logger LOGGER = Logger.getLogger(JiraController.class.getName());

    public JiraController() {
    }

    /**
     * Retrieves all versions of a project.
     *
     * @param projectKey The key of the project
     * @return List of versions
     */
    public List<Version> getProjectVersions(String projectKey) {
        return executeWithErrorHandling(() -> JiraService.getProjectVersions(projectKey),
                "Error retrieving project versions for " + projectKey);
    }

    /**
     * Retrieves tickets from a project.
     *
     * @param projectKey The key of the project
     * @return List of tickets
     */
    public List<Ticket> getProjectTickets(String projectKey) {
        return executeWithErrorHandling(() -> JiraService.getProjectTickets(projectKey),
                "Error retrieving tickets for " + projectKey);
    }

    /**
     * Exports versions to a CSV file.
     *
     * @param versions   List of versions to export
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     */
    public String exportVersionsToCsv(List<Version> versions, String projectKey) {
        return executeWithErrorHandling(() -> CsvExporter.exportVersionsAsCsv(versions, projectKey),
                "Error exporting versions to CSV for " + projectKey);
    }

    /**
     * Exports tickets to a CSV file.
     *
     * @param tickets    List of tickets to export
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     */
    public String exportTicketsToCsv(List<Ticket> tickets, String projectKey) {
        return executeWithErrorHandling(() -> CsvExporter.exportTicketsAsCsv(tickets, projectKey),
                "Error exporting tickets to CSV for " + projectKey);
    }

    /**
     * Executes a function with standardized error handling.
     * In case of error, logs the exception and terminates the program.
     *
     * @param supplier     The function to execute
     * @param errorMessage The error message to log if an exception occurs
     * @param <T>          The return type of the function
     * @return The result of the function
     */
    private <T> T executeWithErrorHandling(IOSupplier<T> supplier, String errorMessage) {
        try {
            return supplier.get();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, errorMessage, e);
            System.err.println("Critical error: " + errorMessage);
            System.exit(1);
            return null;
        }
    }

    // Functional interface for IO operations
    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws IOException;
    }
}

//    /**
//     * Cleans the list of tickets based on specified validation rules.
//     *
//     * @param tickets List of tickets to clean
//     * @param projectKey The project key for exporting removed tickets
//     * @return List of valid tickets
//     */
//    public List<Ticket> cleanTickets(List<Ticket> tickets, String projectKey) {
//        TicketCleaningService cleaningService = new TicketCleaningService();
//
//        // Clean tickets
//        List<Ticket> cleanedTickets = cleaningService.cleanTickets(tickets);
//
//        // Export removed tickets
//        List<TicketCleaningService.RemovedTicket> removedTickets = cleaningService.getRemovedTickets();
//        if (!removedTickets.isEmpty()) {
//            try {
//                String exportPath = CsvExporter.exportRemovedTicketsAsCsv(removedTickets, projectKey);
//                System.out.println("Removed tickets exported to: " + exportPath);
//            } catch (IOException e) {
//                System.err.println("Error exporting removed tickets to CSV: " + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//
//        return cleanedTickets;
//    }