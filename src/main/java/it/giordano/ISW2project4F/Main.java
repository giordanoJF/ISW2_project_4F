
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
        String versionsFile = controller.exportVersionsToCsv(versions, PROJECT_KEY);
        printExportResult("Versions", versionsFile);

        // Export tickets to CSV
        String ticketsFile = controller.exportTicketsToCsv(tickets, PROJECT_KEY);
        printExportResult("Tickets", ticketsFile);
    }



    /**
     * Prints the result of an export operation.
     *
     * @param dataType The type of data exported
     * @param filePath The path to the exported file, or null if export failed
     */
    private static void printExportResult(String dataType, String filePath) {
        if (filePath != null) {
            System.out.println(dataType + " exported to: " + filePath);
        } else {
            System.err.println("Failed to export " + dataType);
        }
    }
}