package it.giordano.isw_project.controller;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import it.giordano.isw_project.service.JiraService;
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
    public List<Ticket> getProjectTickets(String projectKey, List<Version> versions) {
        return executeWithErrorHandling(() -> JiraService.getProjectTickets(projectKey, versions),
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
            LOGGER.log(Level.SEVERE, "{0}: I/O Error", errorMessage);
            System.exit(1);
            return null;
        }
        catch (ParseException e) {
            // Date parsing errors
            LOGGER.log(Level.SEVERE, "{0}: Date format error", errorMessage);
            System.exit(1);
            return null;
        }
        catch (JSONException e) {
            // JSON format errors are critical - log and terminate
            LOGGER.log(Level.SEVERE, "{0}: JSON format error", errorMessage);
            System.exit(1);
            return null;
        }
        catch (Exception e) {
            // Per altre eccezioni non previste
            LOGGER.log(Level.SEVERE, "{0}: Unexpected error - {1}", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier<T> {
        T get() throws IOException, ParseException, JSONException;
    }
}