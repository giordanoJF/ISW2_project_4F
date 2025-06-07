package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for cleaning and validating tickets.
 * This class provides methods to sanitize ticket data and remove invalid tickets.
 */
public class TicketCleaner {

    private static final Logger LOGGER = Logger.getLogger(TicketCleaner.class.getName());
    private static final double COLD_PROPORTION = 0.2;
    private static final double INCREMENTAL_PROPORTION = 0.8;

    private TicketCleaner() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Cleans and validates a list of tickets based on specific rules.
     * Tickets are modified in-place and invalid ones are removed from the list.
     *
     * @param tickets the list of tickets to clean
     */
    public static void cleanTargetTickets(List<Ticket> tickets, List<Ticket> coldStartTickets, List<Version> targetProjectVersions) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }

        for (Ticket ticket : tickets) {

            multipleToSingleFixedVersions(ticket);

            // If multiple injected versions (though this shouldn't happen based on model),
            // we would keep only the oldest one
            // This is a safeguard in case the model changes in the future

            // If we had multiple opening versions, keep only the oldest one
            // Currently the model doesn't support multiple opening versions directly,
            // but this is a safeguard for future changes
        }

        //need this because some tickets have no opening version
        removeInvalidTickets(tickets);

        // Sort tickets by opening version date
        tickets.sort(Comparator.comparing(t -> t.getOpeningVersion().getReleaseDate()));

        // Split point calculation
        int splitIndex = (int) (tickets.size() * COLD_PROPORTION);

//        // Split into cold and incremental sets
//        List<Ticket> coldSet = new ArrayList<>(tickets.subList(0, splitIndex));
//        List<Ticket> incrementalSet = new ArrayList<>(tickets.subList(splitIndex, tickets.size()));

        
        
//      DO PROPORTION HERE
        proportionWithColdStart(tickets, splitIndex, coldStartTickets, targetProjectVersions); //20%
        
        proportionWithIncremental(tickets, splitIndex, targetProjectVersions); //80%
        setAffectedVersionsAfterProportion(tickets, targetProjectVersions);

        //need this because now we have predicted IV and AV, but they could be inconsistent
        removeInvalidTickets(tickets);

        //check if unsuitablePredictedIV are the only null values
        Misc.logUnsuitableTicketsConsistency(tickets);

    }

    private static void setAffectedVersionsAfterProportion(List<Ticket> tickets, List<Version> targetProjectVersions) {
        for (Ticket ticket : tickets) {
            if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
                if (ticket.getInjectedVersion() == null) {
                    // Skip tickets without injected version
                    continue;
                }

                List<Version> affectedVersions = new ArrayList<>();
                Version injectedVersion = ticket.getInjectedVersion();
                Version fixedVersion = ticket.getFixedVersions().getFirst();

                for (Version version : targetProjectVersions) {
                    if (!Misc.isVersionNewer(injectedVersion, version) && Misc.isVersionNewer(fixedVersion, version)) {
                        affectedVersions.add(version);
                    }
                }
                ticket.setAffectedVersions(affectedVersions);
            }
        }
    }


    private static void proportionWithIncremental(List<Ticket> tickets, int splitIndex, List<Version> targetProjectVersions) {
        for (Ticket ticket : tickets.subList(splitIndex, tickets.size()) ) {
            if (ticket.getInjectedVersion() == null) {
                double p = Proportion.evaluatePIncremental(tickets, tickets.indexOf(ticket));
                Version predictedIV = Proportion.predictIV(ticket, p, targetProjectVersions);
                ticket.setInjectedVersion(predictedIV);
            }

        }
    }

    private static void proportionWithColdStart(List<Ticket> tickets, int splitIndex, List<Ticket> coldStartTickets, List<Version> targetProjectVersions) {
        double p = Proportion.evaluatePColdStart(coldStartTickets);
        for (Ticket ticket : tickets.subList(0, splitIndex)) {
            if (ticket.getInjectedVersion() == null) {
                Version predictedIV = Proportion.predictIV(ticket, p, targetProjectVersions);
                ticket.setInjectedVersion(predictedIV);
            }
            
        }
    }

    private static void multipleToSingleFixedVersions(Ticket ticket) {
        // Process multiple fixed versions - keep only the most recent one
        if (ticket.getFixedVersions() != null && ticket.getFixedVersions().size() > 1) {
            Version latestFixedVersion = Misc.findLatestVersion(ticket.getFixedVersions());
            List<Version> singleFixedVersion = new ArrayList<>();
            singleFixedVersion.add(latestFixedVersion);
            ticket.getFixedVersions().clear();
            ticket.getFixedVersions().addAll(singleFixedVersion);
        }
    }

    /**
     * Removes invalid tickets from the list based on specific validation rules.
     *
     * @param tickets the list of tickets to validate
     */
    private static void removeInvalidTickets(List<Ticket> tickets) {
        tickets.removeIf(Misc::shouldRemoveTicket);
    }

    

    

    


}