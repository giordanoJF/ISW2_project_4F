package it.giordano.isw_project.controller;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.util.TicketsStats;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller responsible for generating and logging statistical information about tickets.
 * Provides functionality to analyze ticket data and produce summary statistics.
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
     * @throws RuntimeException If an error occurs during execution, program will terminate
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
            // For other unexpected exceptions
            LOGGER.log(Level.SEVERE, "{0}: Unexpected error - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        }
    }

    /**
     * Functional interface for operations that may throw exceptions related to statistics calculations.
     * Used for handling various calculation and validation exceptions.
     */
    @FunctionalInterface
    private interface ExceptionHandlingSupplier {
        /**
         * Executes the operation.
         *
         * @throws NullPointerException if a null value is encountered during processing
         * @throws ArithmeticException if a calculation error occurs (division by zero, etc.)
         * @throws IllegalArgumentException if an invalid argument is provided
         */
        void get() throws NullPointerException, ArithmeticException, IllegalArgumentException;
    }
}