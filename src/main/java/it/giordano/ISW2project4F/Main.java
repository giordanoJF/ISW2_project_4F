
package it.giordano.ISW2project4F;

import it.giordano.ISW2project4F.controller.CsvController;
import it.giordano.ISW2project4F.controller.JiraController;
import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;

import java.util.List;

public class Main {
    private static final String PROJECT_KEY = "OPENJPA";

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