package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TicketsStats {
    private static final Logger LOGGER = Logger.getLogger(TicketsStats.class.getName());

    private TicketsStats() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Registra le statistiche sui ticket.
     *
     * @param tickets La lista di ticket da analizzare
     */
    public static void logTicketStatistics(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            LOGGER.info("No tickets to analyze for statistics");
            return;
        }

        int totalTickets = tickets.size();
        TicketStatistics stats = collectTicketStatistics(tickets, totalTickets);

        LOGGER.log(Level.INFO,
                """
                        Ticket statistics for {0} tickets:
                        - Missing Injected Version: {1} ({2,number,#.##}%)
                        - Missing Opening Version: {3} ({4,number,#.##}%)
                        - Missing Affected Versions: {5} ({6,number,#.##}%)
                        - Missing Fixed Versions: {7} ({8,number,#.##}%)""",
                new Object[]{
                        totalTickets,
                        stats.ticketsWithoutIV, stats.percentIV,
                        stats.ticketsWithoutOV, stats.percentOV,
                        stats.ticketsWithoutAV, stats.percentAV,
                        stats.ticketsWithoutFV, stats.percentFV
                });
    }

    /**
     * Collects statistics about tickets.
     *
     * @param tickets The list of tickets to analyze
     * @param totalTickets The total number of tickets
     * @return A TicketStatistics object with the collected statistics
     */
    private static TicketStatistics collectTicketStatistics(List<Ticket> tickets, int totalTickets) {
        TicketStatistics stats = new TicketStatistics();

        for (Ticket ticket : tickets) {
            if (ticket == null) {
                continue;
            }

            if (isInjectedVersionMissing(ticket)) {
                stats.ticketsWithoutIV++;
            }

            if (isOpeningVersionMissing(ticket)) {
                stats.ticketsWithoutOV++;
            }

            if (isAffectedVersionsMissing(ticket)) {
                stats.ticketsWithoutAV++;
            }

            if (isFixedVersionsMissing(ticket)) {
                stats.ticketsWithoutFV++;
            }
        }

        // Calculate percentages
        stats.percentIV = calculatePercentage(stats.ticketsWithoutIV, totalTickets);
        stats.percentOV = calculatePercentage(stats.ticketsWithoutOV, totalTickets);
        stats.percentAV = calculatePercentage(stats.ticketsWithoutAV, totalTickets);
        stats.percentFV = calculatePercentage(stats.ticketsWithoutFV, totalTickets);

        return stats;
    }

    /**
     * Checks if the injected version is missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if the injected version is missing, false otherwise
     */
    private static boolean isInjectedVersionMissing(Ticket ticket) {
        return ticket.getInjectedVersion() == null ||
                (ticket.getInjectedVersion().getName() == null ||
                        ticket.getInjectedVersion().getName().isEmpty());
    }

    /**
     * Checks if the opening version is missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if the opening version is missing, false otherwise
     */
    private static boolean isOpeningVersionMissing(Ticket ticket) {
        return ticket.getOpeningVersion() == null ||
                (ticket.getOpeningVersion().getName() == null ||
                        ticket.getOpeningVersion().getName().isEmpty());
    }

    /**
     * Checks if affected versions are missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if affected versions are missing, false otherwise
     */
    private static boolean isAffectedVersionsMissing(Ticket ticket) {
        return ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty();
    }

    /**
     * Checks if fixed versions are missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if fixed versions are missing, false otherwise
     */
    private static boolean isFixedVersionsMissing(Ticket ticket) {
        return ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty();
    }

    /**
     * Calculates a percentage.
     *
     * @param part The part
     * @param total The total
     * @return The percentage
     */
    private static double calculatePercentage(int part, int total) {
        return part * 100.0 / total;
    }

    /**
     * Helper class for storing ticket statistics.
     */
    private static class TicketStatistics {
        int ticketsWithoutIV = 0;
        int ticketsWithoutOV = 0;
        int ticketsWithoutAV = 0;
        int ticketsWithoutFV = 0;
        double percentIV = 0.0;
        double percentOV = 0.0;
        double percentAV = 0.0;
        double percentFV = 0.0;
    }

}
