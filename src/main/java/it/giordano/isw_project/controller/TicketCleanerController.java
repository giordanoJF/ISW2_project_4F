package it.giordano.isw_project.controller;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import it.giordano.isw_project.util.TicketCleaner;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
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
     */
    public void cleanTickets(List<Ticket> tickets) {
        executeWithErrorHandling(() -> TicketCleaner.cleanTargetTickets(tickets),
                "Error cleaning tickets");
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
        } catch (NoSuchElementException e) {
            LOGGER.log(Level.SEVERE, "{0}: Element not found - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "{0}: Invalid argument - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        } catch (ConcurrentModificationException e) {
            LOGGER.log(Level.SEVERE, "{0}: Concurrent modification error - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        } catch (Exception e) {
            // Catch-all for other unexpected exceptions
            LOGGER.log(Level.SEVERE, "{0}: Unexpected error - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
        }
    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier {
        void get() throws NullPointerException, NoSuchElementException, IllegalArgumentException, ConcurrentModificationException;
    }
}