package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Consistency {

    private static final Logger LOGGER = Logger.getLogger(Consistency.class.getName());


    private Consistency() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Checks if the fixed version is invalid when compared with affected versions.
     *
     * @param ticket the ticket to check
     * @param fixedVersion the fixed version to compare
     * @return true if the ticket should be removed, false otherwise
     */
    public static boolean isFixedVersionInvalidWithAffected(Ticket ticket, Version fixedVersion) {
        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
            return false;
        }

        for (Version affectedVersion : ticket.getAffectedVersions()) {
            if (!Misc.isVersionNewer(fixedVersion, affectedVersion)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if a ticket should be removed based on validation rules.
     *
     * @param ticket the ticket to validate
     * @return true if the ticket should be removed, false otherwise
     */
    public static boolean shouldRemoveTicket(Ticket ticket) {
        // Check for missing opening version
        if (ticket.getOpeningVersion() == null) {
            return true;
        }

        // Check for missing fixed versions
        if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()) {
            return true;
        }

        // Get fixed version (we already ensured there's only one after normalization)
        Version fixedVersion = ticket.getFixedVersions().getFirst();

        // Check if opening version > fixed version
        if (Misc.isVersionNewer(ticket.getOpeningVersion(), fixedVersion)) {
            return true;
        }

//        // Check for missing injected version AFTER proportion calculation
//        if (ticket.getInjectedVersion() == null) {
//            return true;
//        }
//
//        // Check for missing affected versions AFTER proportion calculation
//        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
//            return true;
//        }

        // Check if fixed version <= any affected version
        if (Consistency.isFixedVersionInvalidWithAffected(ticket, fixedVersion)) {
            return true;
        }

        // Check if injected version >= fixed version
        if (ticket.getInjectedVersion() != null &&
                Misc.isVersionNewer(ticket.getInjectedVersion(), fixedVersion)) {
            return true;
        }

        // Check if opening version < injected version
        if ( Misc.isVersionNewer(ticket.getInjectedVersion(), ticket.getOpeningVersion()) ) {
            return true;
        }

        // check if every av is >= IV and < FV
        if (ticket.getAffectedVersions() != null && !ticket.getAffectedVersions().isEmpty()) {
            for (Version av : ticket.getAffectedVersions()) {
                if (av.getReleaseDate() != null && fixedVersion.getReleaseDate() != null) {
                    if (av.getReleaseDate().after(fixedVersion.getReleaseDate()) || av.getReleaseDate().before(ticket.getInjectedVersion().getReleaseDate())) {
                        return true;
                    }
                }
            }
        }

        // Check if injected = fixed, we skip it because the bug is never produced
        if (ticket.getInjectedVersion() != null && ticket.getFixedVersions() != null) {
            if (ticket.getInjectedVersion().getReleaseDate().equals(fixedVersion.getReleaseDate())) {
                return true;
            }
        }

        return false;
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

    public static boolean hasConsistentVersions(Ticket ticket) {
        Version lv = Misc.getLatestVersion(ticket.getFixedVersions());
        if (lv != null && lv.getReleaseDate() != null &&
                ticket.getOpeningVersion().getReleaseDate() != null &&
                ticket.getInjectedVersion().getReleaseDate() != null) {

            return lv.getReleaseDate().after(ticket.getOpeningVersion().getReleaseDate())
                    && lv.getReleaseDate().after(ticket.getInjectedVersion().getReleaseDate())
                    && (ticket.getInjectedVersion().getReleaseDate().before(ticket.getOpeningVersion().getReleaseDate())
                    || ticket.getInjectedVersion().getReleaseDate().equals(ticket.getOpeningVersion().getReleaseDate()));
        }
        else {
            return false;
        }
    }

    public static boolean hasRequiredVersions(Ticket ticket) {
        return ticket.getFixedVersions() != null && !ticket.getFixedVersions().isEmpty() &&
                ticket.getInjectedVersion() != null &&
                ticket.getOpeningVersion() != null;
    }

    /**
     * Analyzes tickets to check if those with null IV/AV exactly match those with unsuitablePredictedIV set to true.
     *
     * @param tickets List of tickets to analyze
     * @return true if the set of tickets with null IV/AV exactly matches the set of tickets with unsuitablePredictedIV=true
     */
    public static boolean logUnsuitableTicketsConsistency(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            LOGGER.warning("No tickets provided for analysis");
            return false;
        }

        List<Ticket> ticketsWithNullVersions = new ArrayList<>();
        List<Ticket> ticketsWithUnsuitablePredictedIV = new ArrayList<>();

        // Analyze tickets and populate lists
        for (Ticket ticket : tickets) {
            boolean hasNullVersions = ticket.getInjectedVersion() == null ||
                    (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty());

            if (hasNullVersions) {
                ticketsWithNullVersions.add(ticket);
            }

            if (Boolean.TRUE.equals(ticket.getUnsuitablePredictedIV())) {
                ticketsWithUnsuitablePredictedIV.add(ticket);
            }
        }

        // Log results
//        LOGGER.info("Total tickets analyzed: " + tickets.size());
        LOGGER.info("Tickets with null IV or AV: " + ticketsWithNullVersions.size());
        LOGGER.info("Tickets with unsuitablePredictedIV=true: " + ticketsWithUnsuitablePredictedIV.size());

        // Check if the two sets have the same size
        if (ticketsWithNullVersions.size() != ticketsWithUnsuitablePredictedIV.size()) {
            LOGGER.warning("Sets have different sizes. Cannot be exactly the same.");
            return false;
        }

        // Check if all tickets with null versions are in the unsuitable set
        for (Ticket nullVersionTicket : ticketsWithNullVersions) {
            boolean found = false;
            for (Ticket unsuitableTicket : ticketsWithUnsuitablePredictedIV) {
                if (nullVersionTicket.getKey().equals(unsuitableTicket.getKey())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                LOGGER.warning("Ticket " + nullVersionTicket.getKey() +
                        " has null IV/AV but is not marked as unsuitable");
                return false;
            }
        }

        // Check if all unsuitable tickets have null versions
        for (Ticket unsuitableTicket : ticketsWithUnsuitablePredictedIV) {
            boolean found = false;
            for (Ticket nullVersionTicket : ticketsWithNullVersions) {
                if (unsuitableTicket.getKey().equals(nullVersionTicket.getKey())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                LOGGER.warning("Ticket " + unsuitableTicket.getKey() +
                        " is marked as unsuitable but has non-null IV/AV");
                return false;
            }
        }

        LOGGER.info("The sets of tickets with null IV/AV and unsuitable predicted IV are exactly the same.");
        return true;
    }


}
