
package it.giordano.ISW2project4F;

import it.giordano.ISW2project4F.controller.JiraController;
import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;

import java.util.List;

public class Main {
    private static final String PROJECT_KEY = "BOOKKEEPER";

    public static void main(String[] args) {
        JiraController controller = new JiraController();

        // Retrieve data
        List<Version> versions = controller.getProjectVersions(PROJECT_KEY);
        List<Ticket> tickets = controller.getProjectTickets(PROJECT_KEY);

        // Export versions to CSV
        controller.exportVersionsToCsv(versions, PROJECT_KEY);

        // Export tickets to CSV
        controller.exportTicketsToCsv(tickets, PROJECT_KEY);
    }
}