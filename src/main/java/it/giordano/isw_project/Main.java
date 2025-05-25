
package it.giordano.isw_project;

import it.giordano.isw_project.controller.CsvController;
import it.giordano.isw_project.controller.JiraController;
import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.List;

public class Main {
    private static final String PROJECT_KEY = "BOOKKEEPER";

    public static void main(String[] args) {
        JiraController jiraController = new JiraController();
        CsvController csvController = new CsvController();

        // Retrieve data
        List<Version> versions = jiraController.getProjectVersions(PROJECT_KEY);
        List<Ticket> tickets = jiraController.getProjectTickets(PROJECT_KEY);

        // Export versions to CSV
        csvController.exportVersionsToCsv(versions, PROJECT_KEY);

        // Export tickets to CSV
        csvController.exportTicketsToCsv(tickets, PROJECT_KEY);
    }
}