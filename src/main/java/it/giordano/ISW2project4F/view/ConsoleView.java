package it.giordano.ISW2project4F.view;

import it.giordano.ISW2project4F.model.Ticket;
import it.giordano.ISW2project4F.model.Version;

import java.util.List;

public class ConsoleView {
    public void displayVersions(List<Version> versions) {
        System.out.println("Project Versions:");
        for (Version version : versions) {
            System.out.println(version);
        }
        System.out.println("Total versions: " + versions.size());
    }

    public void displayTickets(List<Ticket> tickets) {
        System.out.println("Bug Tickets (Fixed & Closed/Resolved):");
        for (Ticket ticket : tickets) {
            System.out.println(ticket);
        }
        System.out.println("Total tickets: " + tickets.size());
    }
}