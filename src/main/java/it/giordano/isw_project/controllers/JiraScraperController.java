package it.giordano.isw_project.controllers;

import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;
import it.giordano.isw_project.services.JiraScraperService;
import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JiraScraperController {
    private static final Logger LOGGER = Logger.getLogger(JiraScraperController.class.getName());

    public List<Version> getProjectVersions(String projectKey) {
        return executeWithErrorHandling(() -> JiraScraperService.getProjectVersions(projectKey),
                "Error retrieving project versions for " + projectKey);
    }

    public List<Ticket> getProjectTickets(String projectKey, List<Version> versions) {
        return executeWithErrorHandling(() -> JiraScraperService.getProjectTickets(projectKey, versions),
                "Error retrieving tickets for " + projectKey);
    }

    @FunctionalInterface
    private interface ExceptionHandlingSupplier<T> {
        T get() throws IOException, IllegalArgumentException, IllegalStateException, ParseException, JSONException, NullPointerException;
    }

    //all exceptions not handled by the service is handled here
    private <T> T executeWithErrorHandling(ExceptionHandlingSupplier<T> supplier, String errorMessage) {
        try {
            return supplier.get();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "{0}: I/O Error\n", errorMessage);
            System.exit(1);
            return null;
        }
        catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "{0}: Invalid argument - {1}\n", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
        catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "{0}: Illegal state - {1}\n", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
        catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "{0}: Parsing error - {1}\n", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
        catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "{0}: JSON parsing error - {1}\n", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
        catch (NullPointerException e) {
            LOGGER.log(Level.SEVERE, "{0}: Null pointer encountered - {1}\n", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "{0}: Unexpected error - {1}\n", new Object[]{errorMessage, e.getMessage()});
            System.exit(1);
            return null;
        }
    }

}
