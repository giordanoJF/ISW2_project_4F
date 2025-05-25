package it.giordano.ISW2project4F.controller;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;
import it.giordano.ISW2project4F.service.JiraService;
import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JiraController {
    private static final Logger LOGGER = Logger.getLogger(JiraController.class.getName());

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
        } catch (IOException e) {
            // I/O errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, errorMessage + ": I/O Error", e);
            System.err.println("Critical error: " + errorMessage + ": " + e.getMessage());
            System.exit(1);
            return null;
        }
        catch (ParseException e) {
            // Date parsing errors
            LOGGER.log(Level.SEVERE, errorMessage + ": Date format error", e);
            System.err.println("Error parsing date: " + errorMessage + ": " + e.getMessage());
            System.exit(1);
            return null;
        }
        catch (JSONException e) {
            // Data format errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, errorMessage + ": Data format error", e);
            System.err.println("Critical error: " + errorMessage + ": " + e.getMessage());
            System.exit(1);
            return null;
        }
        catch (Exception e) {
            // Unexpected errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, errorMessage + ": Unexpected error", e);
            System.err.println("Critical error: " + errorMessage + ": " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier<T> {
        T get() throws IOException, ParseException, JSONException, Exception;
    }
}