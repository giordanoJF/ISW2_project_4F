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

            if (TicketCleaner.isInjectedVersionMissing(ticket)) {
                stats.ticketsWithoutIV++;
            }

            if (TicketCleaner.isOpeningVersionMissing(ticket)) {
                stats.ticketsWithoutOV++;
            }

            if (TicketCleaner.isAffectedVersionsMissing(ticket)) {
                stats.ticketsWithoutAV++;
            }

            if (TicketCleaner.isFixedVersionsMissing(ticket)) {
                stats.ticketsWithoutFV++;
            }
        }

        // Calculate percentages
        stats.percentIV = MathUtils.calculatePercentage(stats.ticketsWithoutIV, totalTickets);
        stats.percentOV = MathUtils.calculatePercentage(stats.ticketsWithoutOV, totalTickets);
        stats.percentAV = MathUtils.calculatePercentage(stats.ticketsWithoutAV, totalTickets);
        stats.percentFV = MathUtils.calculatePercentage(stats.ticketsWithoutFV, totalTickets);

        return stats;
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
