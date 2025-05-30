package it.giordano.isw_project.controller;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.util.TicketsStats;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for ticket statistics operations.
 */
public class TicketStatsController {
    private static final Logger LOGGER = Logger.getLogger(TicketStatsController.class.getName());

    /**
     * Generates and logs statistics for a list of tickets.
     *
     * @param tickets the list of tickets to analyze
     */
    public void generateTicketStatistics(List<Ticket> tickets) {
        executeWithErrorHandling(() -> TicketsStats.logTicketStatistics(tickets),
                "Error generating ticket statistics");
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
        } catch (Exception e) {
            // General errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, "{0}: {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        }
    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier {
        void get() throws Exception;
    }
}