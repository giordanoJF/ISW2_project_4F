package it.giordano.ISW2project4F.controller;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;
import it.giordano.ISW2project4F.service.JiraService;
import it.giordano.ISW2project4F.service.TicketCleaningService;
import it.giordano.ISW2project4F.util.CsvExporter;

import java.io.IOException;
import java.util.List;

public class JiraController {
    private final JiraService jiraService;
    
    public JiraController() {
        this.jiraService = new JiraService();
    }
    
    /**
     * Retrieves all versions of a project.
     * 
     * @param projectKey The key of the project
     * @return List of versions
     */
    public List<Version> getProjectVersions(String projectKey) {
        try {
            return jiraService.getProjectVersions(projectKey);
        } catch (IOException e) {
            System.err.println("Error retrieving project versions: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    /**
     * Retrieves tickets from a project.
     * 
     * @param projectKey The key of the project
     * @return List of tickets
     */
    public List<Ticket> retrieveTickets(String projectKey) {
        try {
            return jiraService.retrieveTickets(projectKey);
        } catch (IOException e) {
            System.err.println("Error retrieving tickets: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    /**
     * Exports versions to a CSV file.
     * 
     * @param versions List of versions to export
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     */
    public String exportVersionsAsCsv(List<Version> versions, String projectKey) {
        try {
            return CsvExporter.exportVersionsAsCsv(versions, projectKey);
        } catch (IOException e) {
            System.err.println("Error exporting versions to CSV: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Exports tickets to a CSV file.
     * 
     * @param tickets List of tickets to export
     * @param projectKey The project key for naming the file
     * @return The path to the created file
     */
    public String exportTicketsAsCsv(List<Ticket> tickets, String projectKey) {
        try {
            return CsvExporter.exportTicketsAsCsv(tickets, projectKey);
        } catch (IOException e) {
            System.err.println("Error exporting tickets to CSV: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Cleans the list of tickets based on specified validation rules.
     *
     * @param tickets List of tickets to clean
     * @param projectKey The project key for exporting removed tickets
     * @return List of valid tickets
     */
    public List<Ticket> cleanTickets(List<Ticket> tickets, String projectKey) {
        TicketCleaningService cleaningService = new TicketCleaningService();

        // Clean tickets
        List<Ticket> cleanedTickets = cleaningService.cleanTickets(tickets);

        // Export removed tickets
        List<TicketCleaningService.RemovedTicket> removedTickets = cleaningService.getRemovedTickets();
        if (!removedTickets.isEmpty()) {
            try {
                String exportPath = CsvExporter.exportRemovedTicketsAsCsv(removedTickets, projectKey);
                System.out.println("Removed tickets exported to: " + exportPath);
            } catch (IOException e) {
                System.err.println("Error exporting removed tickets to CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return cleanedTickets;
    }

}