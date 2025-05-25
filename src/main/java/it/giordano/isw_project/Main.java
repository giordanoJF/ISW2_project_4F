
package it.giordano.isw_project;

import it.giordano.isw_project.controller.CsvController;
import it.giordano.isw_project.controller.JiraController;
import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.List;

public class Main {
    private static final String PROJECT_KEY = "BOOKKEEPER";

    public static void main(String[] args) {
        JiraController JiraController = new JiraController();
        CsvController CsvController = new CsvController();

        // Retrieve data
        List<Version> versions = JiraController.getProjectVersions(PROJECT_KEY);
        List<Ticket> tickets = JiraController.getProjectTickets(PROJECT_KEY);

        // Export versions to CSV
        CsvController.exportVersionsToCsv(versions, PROJECT_KEY);

        // Export tickets to CSV
        CsvController.exportTicketsToCsv(tickets, PROJECT_KEY);
    }
}