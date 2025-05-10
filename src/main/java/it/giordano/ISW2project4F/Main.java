
package it.giordano.ISW2project4F;

import it.giordano.ISW2project4F.controller.JiraController;
import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;
import it.giordano.ISW2project4F.view.ConsoleView;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        JiraController controller = new JiraController();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the target project key (e.g., BOOKKEEPER, HADOOP, etc.):");
        String projectKey = scanner.nextLine();

        // Retrieve versions
        List<Version> versions = controller.getProjectVersions(projectKey);

        // Retrieve tickets
        List<Ticket> tickets = controller.retrieveTickets(projectKey);

        // Export data to CSV files
        String versionsFile = controller.exportVersionsAsCsv(versions, projectKey);
        if (versionsFile != null) {
            System.out.println("Versions exported to: " + versionsFile);
        }

        String ticketsFile = controller.exportTicketsAsCsv(tickets, projectKey);
        if (ticketsFile != null) {
            System.out.println("Tickets exported to: " + ticketsFile);
        }

        scanner.close();
    }
}