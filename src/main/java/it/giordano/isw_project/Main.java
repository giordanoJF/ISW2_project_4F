
package it.giordano.isw_project;

import it.giordano.isw_project.controller.CsvController;
import it.giordano.isw_project.controller.JiraController;
import it.giordano.isw_project.controller.TicketCleanerController;
import it.giordano.isw_project.controller.TicketStatsController;
import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import it.giordano.isw_project.util.TicketCleaner;
import it.giordano.isw_project.util.TicketsStats;

import java.util.List;

public class Main {
    private static final String PROJECT_KEY = "BOOKKEEPER";
    private static final String COLD_START_PROJECT_KEY = "OPENJPA";

    public static void main(String[] args) {
        JiraController jiraController = new JiraController();
        TicketStatsController ticketStatsController = new TicketStatsController();
        TicketCleanerController ticketCleanerController = new TicketCleanerController();
        CsvController csvController = new CsvController();

        // Retrieve data
        List<Version> versions = jiraController.getProjectVersions(PROJECT_KEY);
        List<Ticket> tickets = jiraController.getProjectTickets(PROJECT_KEY, versions);
        List<Version> coldStartVersions = jiraController.getProjectVersions(COLD_START_PROJECT_KEY);
        List<Ticket> coldStartTickets = jiraController.getProjectTickets(COLD_START_PROJECT_KEY, coldStartVersions);
        ticketStatsController.generateTicketStatistics(tickets);

        //clean data
        ticketCleanerController.cleanTickets(tickets, coldStartTickets, versions);
        ticketStatsController.generateTicketStatistics(tickets);

        // Export versions to CSV
        csvController.exportVersionsToCsv(versions, PROJECT_KEY);
//
        // Export tickets to CSV
        csvController.exportTicketsToCsv(tickets, PROJECT_KEY);
    }
}