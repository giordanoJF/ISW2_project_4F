
package it.giordano.ISW2project4F;

import it.giordano.ISW2project4F.controller.JiraController;
import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;

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
        List<Ticket> tickets = controller.getProjectTickets(projectKey);

//        // Clean tickets
//        List<Ticket> cleanedTickets = controller.cleanTickets(tickets, projectKey);

        // Export versions to CSV files
        String versionsFile = controller.exportVersionsToCsv(versions, projectKey);
        if (versionsFile != null) {
            System.out.println("Versions exported to: " + versionsFile);
        }

        // Export tickets to CSV files
        String ticketsFile = controller.exportTicketsToCsv(tickets, projectKey);
        if (ticketsFile != null) {
            System.out.println("Tickets exported to: " + ticketsFile);
        }

        scanner.close();
    }
}