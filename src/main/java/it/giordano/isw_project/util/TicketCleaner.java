package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
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
        predictIvWithProportionColdStart(tickets, splitIndex, coldStartTickets, targetProjectVersions); //20%
        
        predictIvWithProportionIncremental(tickets, splitIndex, targetProjectVersions); //80%
        setAffectedVersionsAfterProportion(tickets, targetProjectVersions);

        //need this because now we have predicted IV and AV, but they could be inconsistent
        removeInvalidTickets(tickets);

        //check if unsuitablePredictedIV are the only null values
//        logUnsuitableTicketsConsistency(tickets);

        //here we need to remove tickets with unsuitable predicted IV
        tickets.removeIf(ticket -> ticket.getUnsuitablePredictedIV() != null && ticket.getUnsuitablePredictedIV());

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
                    if (!VersionUtils.isVersionNewer(injectedVersion, version) && VersionUtils.isVersionNewer(fixedVersion, version)) {
                        affectedVersions.add(version);
                    }
                }
                ticket.setAffectedVersions(affectedVersions);
            }
        }
    }

    private static void predictIvWithProportionIncremental(List<Ticket> tickets, int splitIndex, List<Version> targetProjectVersions) {
        for (Ticket ticket : tickets.subList(splitIndex, tickets.size()) ) {
            if (ticket.getInjectedVersion() == null) {
                double p = Proportion.evaluatePIncremental(tickets, tickets.indexOf(ticket));
                Version predictedIV = Proportion.predictIV(ticket, p, targetProjectVersions);
                ticket.setInjectedVersion(predictedIV);
            }

        }
    }

    private static void predictIvWithProportionColdStart(List<Ticket> tickets, int splitIndex, List<Ticket> coldStartTickets, List<Version> targetProjectVersions) {
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
            Version latestFixedVersion = VersionUtils.findLatestVersion(ticket.getFixedVersions());
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
        tickets.removeIf(TicketCleaner::shouldRemoveTicket);
    }

    /**
     * Checks if the injected version is missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if the injected version is missing, false otherwise
     */
    public static boolean isInjectedVersionMissing(Ticket ticket) {
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
    public static boolean isOpeningVersionMissing(Ticket ticket) {
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
    public static boolean isAffectedVersionsMissing(Ticket ticket) {
        return ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty();
    }

    /**
     * Checks if fixed versions are missing from a ticket.
     *
     * @param ticket The ticket to check
     * @return true if fixed versions are missing, false otherwise
     */
    public static boolean isFixedVersionsMissing(Ticket ticket) {
        return ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty();
    }

    public static boolean hasRequiredVersions(Ticket ticket) {
        return ticket.getFixedVersions() != null && !ticket.getFixedVersions().isEmpty() &&
                ticket.getInjectedVersion() != null &&
                ticket.getOpeningVersion() != null;
    }

//    /**
//     * Analyzes tickets to check if those with null IV/AV exactly match those with unsuitablePredictedIV set to true.
//     *
//     * @param tickets List of tickets to analyze
//     * @return true if the set of tickets with null IV/AV exactly matches the set of tickets with unsuitablePredictedIV=true
//     */
//    public static boolean logUnsuitableTicketsConsistency(List<Ticket> tickets) {
//        if (tickets == null || tickets.isEmpty()) {
//            TicketCleaner.LOGGER.warning("No tickets provided for analysis");
//            return false;
//        }
//
//        List<Ticket> ticketsWithNullVersions = new ArrayList<>();
//        List<Ticket> ticketsWithUnsuitablePredictedIV = new ArrayList<>();
//
//        // Analyze tickets and populate lists
//        for (Ticket ticket : tickets) {
//            boolean hasNullVersions = ticket.getInjectedVersion() == null ||
//                    (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty());
//
//            if (hasNullVersions) {
//                ticketsWithNullVersions.add(ticket);
//            }
//
//            if (Boolean.TRUE.equals(ticket.getUnsuitablePredictedIV())) {
//                ticketsWithUnsuitablePredictedIV.add(ticket);
//            }
//        }
//
//        // Log results
////        LOGGER.info("Total tickets analyzed: " + tickets.size());
//        TicketCleaner.LOGGER.log(Level.INFO, "Tickets with null IV or AV: {0}", ticketsWithNullVersions.size());
//        TicketCleaner.LOGGER.log(Level.INFO, "Tickets with unsuitablePredictedIV=true: {0}", ticketsWithUnsuitablePredictedIV.size());
//
//        // Check if the two sets have the same size
//        if (ticketsWithNullVersions.size() != ticketsWithUnsuitablePredictedIV.size()) {
//            TicketCleaner.LOGGER.warning("Sets have different sizes. Cannot be exactly the same.");
//            return false;
//        }
//
//        // Check if all tickets with null versions are in the unsuitable set
//        for (Ticket nullVersionTicket : ticketsWithNullVersions) {
//            boolean found = false;
//            for (Ticket unsuitableTicket : ticketsWithUnsuitablePredictedIV) {
//                if (nullVersionTicket.getKey().equals(unsuitableTicket.getKey())) {
//                    found = true;
//                    break;
//                }
//            }
//
//            if (!found) {
//                TicketCleaner.LOGGER.warning("Ticket " + nullVersionTicket.getKey() +
//                        " has null IV/AV but is not marked as unsuitable");
//                return false;
//            }
//        }
//
//        // Check if all unsuitable tickets have null versions
//        for (Ticket unsuitableTicket : ticketsWithUnsuitablePredictedIV) {
//            boolean found = false;
//            for (Ticket nullVersionTicket : ticketsWithNullVersions) {
//                if (unsuitableTicket.getKey().equals(nullVersionTicket.getKey())) {
//                    found = true;
//                    break;
//                }
//            }
//
//            if (!found) {
//                TicketCleaner.LOGGER.warning("Ticket " + unsuitableTicket.getKey() +
//                        " is marked as unsuitable but has non-null IV/AV");
//                return false;
//            }
//        }
//
//        TicketCleaner.LOGGER.info("The sets of tickets with null IV/AV and unsuitable predicted IV are exactly the same.");
//        return true;
//    }

    /**
     * Determines if a ticket should be removed based on validation rules.
     *
     * @param ticket the ticket to validate
     * @return true if the ticket should be removed, false otherwise
     */
    public static boolean shouldRemoveTicket(Ticket ticket) {
        // Early validation checks
        if (hasBasicValidationIssues(ticket)) {
            return true;
        }

        Version fixedVersion = ticket.getFixedVersions().getFirst();

        // Version relationship validations
        if (hasVersionRelationshipIssues(ticket, fixedVersion)) {
            return true;
        }

        // Affected versions validation
        if (hasAffectedVersionIssues(ticket, fixedVersion)) {
            return true;
        }

        // Final check: injected version equals fixed version
        return hasInjectedEqualsFixed(ticket, fixedVersion);
    }

    /**
     * Checks for basic validation issues (null checks and empty collections).
     */
    private static boolean hasBasicValidationIssues(Ticket ticket) {
        if (ticket.getOpeningVersion() == null) {
            return true;
        }

        return ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty();
    }

    /**
     * Checks for version relationship issues between opening, injected, and fixed versions.
     */
    private static boolean hasVersionRelationshipIssues(Ticket ticket, Version fixedVersion) {
        // Check if opening version > fixed version
        if (VersionUtils.isVersionNewer(ticket.getOpeningVersion(), fixedVersion)) {
            return true;
        }

        // Check if fixed version <= any affected version
        if (TicketValidator.isFixedVersionInvalidWithAffected(ticket, fixedVersion)) {
            return true;
        }

        // Check if injected version >= fixed version
        if (isInjectedVersionNewerThanFixed(ticket, fixedVersion)) {
            return true;
        }

        // Check if opening version < injected version
        return VersionUtils.isVersionNewer(ticket.getInjectedVersion(), ticket.getOpeningVersion());
    }

    /**
     * Checks if injected version is newer than fixed version.
     */
    private static boolean isInjectedVersionNewerThanFixed(Ticket ticket, Version fixedVersion) {
        return ticket.getInjectedVersion() != null &&
                VersionUtils.isVersionNewer(ticket.getInjectedVersion(), fixedVersion);
    }

    /**
     * Validates affected versions against injected and fixed versions.
     */
    private static boolean hasAffectedVersionIssues(Ticket ticket, Version fixedVersion) {
        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
            return false;
        }

        for (Version av : ticket.getAffectedVersions()) {
            if (isAffectedVersionInvalid(av, fixedVersion, ticket.getInjectedVersion())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an affected version violates the date constraints.
     */
    private static boolean isAffectedVersionInvalid(Version affectedVersion, Version fixedVersion, Version injectedVersion) {
        if (affectedVersion.getReleaseDate() == null ||
                fixedVersion.getReleaseDate() == null ||
                injectedVersion == null) {
            return false;
        }

        Date avDate = affectedVersion.getReleaseDate();
        Date fixedDate = fixedVersion.getReleaseDate();
        Date injectedDate = injectedVersion.getReleaseDate();

        return avDate.after(fixedDate) || avDate.before(injectedDate);
    }

    /**
     * Checks if injected version equals fixed version (should be removed).
     */
    private static boolean hasInjectedEqualsFixed(Ticket ticket, Version fixedVersion) {
        if (ticket.getInjectedVersion() == null || ticket.getFixedVersions() == null) {
            return false;
        }

        return ticket.getInjectedVersion().getReleaseDate().equals(fixedVersion.getReleaseDate());
    }

}