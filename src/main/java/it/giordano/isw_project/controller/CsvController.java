package it.giordano.isw_project.controller;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import it.giordano.isw_project.util.CsvExporter;

import java.io.IOException;
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
        catch (IOException e) {
            // I/O errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, "{0}: I/O Error", errorMessage);
            System.exit(1);
        }
        catch (Exception e) {
            // Per altre eccezioni non previste
            LOGGER.log(Level.SEVERE, "{0}: Unexpected error - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        }

    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier {
        void get() throws IOException;
    }
}