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
        } catch (NullPointerException e) {
            LOGGER.log(Level.SEVERE, "{0}: Null value encountered - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        } catch (ArithmeticException e) {
            LOGGER.log(Level.SEVERE, "{0}: Calculation error - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "{0}: Invalid argument - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        } catch (Exception e) {
            // Per altre eccezioni non previste
            LOGGER.log(Level.SEVERE, "{0}: Unexpected error - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        }

    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier {
        void get() throws NullPointerException, ArithmeticException, IllegalArgumentException;
    }
}