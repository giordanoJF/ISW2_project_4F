package it.giordano.isw_project.controller;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.util.TicketCleaner;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for ticket cleaning operations.
 */
public class TicketCleanerController {
    private static final Logger LOGGER = Logger.getLogger(TicketCleanerController.class.getName());

    /**
     * Cleans a list of tickets by applying validation rules and removing invalid entries.
     *
     * @param tickets the list of tickets to clean
     * @return the cleaned list of tickets
     */
    public List<Ticket> cleanTickets(List<Ticket> tickets) {
        return executeWithErrorHandling(() -> TicketCleaner.cleanTickets(tickets),
                "Error cleaning tickets");
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
    private <T> T executeWithErrorHandling(ExceptionHandlingSupplier<T> supplier, String errorMessage) {
        try {
            return supplier.get();
        } catch (Exception e) {
            // General errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, "{0}: {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier<T> {
        T get() throws Exception;
    }
}