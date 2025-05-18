package it.giordano.ISW2project4F.controller;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;
import it.giordano.ISW2project4F.util.CsvExporter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvController {
    private static final Logger LOGGER = Logger.getLogger(CsvController.class.getName());

    /**
     * Exports versions to a CSV file.
     *
     * @param versions   List of versions to export
     * @param projectKey The project key for naming the file
     */
    public void exportVersionsToCsv(List<Version> versions, String projectKey) {
        executeWithErrorHandling(() -> CsvExporter.exportVersionsAsCsv(versions, projectKey),
                "Error exporting versions to CSV for " + projectKey);
    }

    /**
     * Exports tickets to a CSV file.
     *
     * @param tickets    List of tickets to export
     * @param projectKey The project key for naming the file
     */
    public void exportTicketsToCsv(List<Ticket> tickets, String projectKey) {
        executeWithErrorHandling(() -> CsvExporter.exportTicketsAsCsv(tickets, projectKey),
                "Error exporting tickets to CSV for " + projectKey);
    }

    /**
     * Executes a function with standardized error handling.
     * In case of error, logs the exception and terminates the program.
     *
     * @param supplier     The function to execute
     * @param errorMessage The error message to log if an exception occurs
     */
    private void executeWithErrorHandling(ExceptionHandlingSupplier supplier, String errorMessage) {
        try {
            supplier.get();
        }
        catch (Exception e) {
            // Unexpected errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, errorMessage + ": Unexpected error", e);
            System.err.println("Critical error: " + errorMessage + ": " + e.getMessage());
            System.exit(1);
        }
    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier {
        void get() throws Exception;
    }
}