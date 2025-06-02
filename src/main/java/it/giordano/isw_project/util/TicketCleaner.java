package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Utility class for cleaning and validating tickets.
 * This class provides methods to sanitize ticket data and remove invalid tickets.
 */
public class TicketCleaner {

    private static final Logger LOGGER = Logger.getLogger(TicketCleaner.class.getName());

    private TicketCleaner() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Cleans and validates a list of tickets based on specific rules.
     * Tickets are modified in-place and invalid ones are removed from the list.
     *
     * @param tickets the list of tickets to clean
     */
    public static void cleanTargetTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }
        
        for (Ticket ticket : tickets) {
            multipleToSingleFixedVersions(ticket);
        }

        //do proportion here

        removeInvalidTickets(tickets);

            // If multiple injected versions (though this shouldn't happen based on model),
            // we would keep only the oldest one
            // This is a safeguard in case the model changes in the future

            // If we had multiple opening versions, keep only the oldest one
            // Currently the model doesn't support multiple opening versions directly,
            // but this is a safeguard for future changes

//        coldStart(needProportion, coldStartTickets); //20%
//        incremental(needProportion); //80%
//        setAffectedVersions(needProportion, projectVersions);
//        tickets.addAll(needProportion);
        

    }

    private static void multipleToSingleFixedVersions(Ticket ticket) {
        // Process multiple fixed versions - keep only the most recent one
        if (ticket.getFixedVersions() != null && ticket.getFixedVersions().size() > 1) {
            Version latestFixedVersion = findLatestVersion(ticket.getFixedVersions());
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
     * Determines if a ticket should be removed based on validation rules.
     *
     * @param ticket the ticket to validate
     * @return true if the ticket should be removed, false otherwise
     */
    private static boolean shouldRemoveTicket(Ticket ticket) {
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
        if (isVersionNewer(ticket.getOpeningVersion(), fixedVersion)) {
            return true;
        }

        // Check for missing injected version AFTER proportion calculation
        if (ticket.getInjectedVersion() == null) {
            return true;
        }
        
        // Check for missing affected versions AFTER proportion calculation
        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
            return true;
        }
        
        // Check if fixed version <= any affected version
        if (isFixedVersionInvalidWithAffected(ticket, fixedVersion)) {
            return true;
        }

        // Check if injected version >= fixed version
        if (ticket.getInjectedVersion() != null &&
                isVersionNewer(ticket.getInjectedVersion(), fixedVersion)) {
            return true;
        }
        
        // Check if opening version < injected version
        if ( isVersionNewer(ticket.getInjectedVersion(), ticket.getOpeningVersion()) ) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the fixed version is invalid when compared with affected versions.
     *
     * @param ticket the ticket to check
     * @param fixedVersion the fixed version to compare
     * @return true if the ticket should be removed, false otherwise
     */
    private static boolean isFixedVersionInvalidWithAffected(Ticket ticket, Version fixedVersion) {
        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
            return false;
        }

        for (Version affectedVersion : ticket.getAffectedVersions()) {
            if (!isVersionNewer(fixedVersion, affectedVersion)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the latest (newest) version from a list of versions.
     *
     * @param versions the list of versions to search
     * @return the latest version
     */
    private static Version findLatestVersion(List<Version> versions) {
        return versions.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Version::getReleaseDate))
                .orElse(null);
    }

    /**
     * Determines if the first version is newer than the second version.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return true if version1 is newer than version2, false otherwise
     */
    private static boolean isVersionNewer(Version version1, Version version2) {
        if (version1 == null || version2 == null) {
            return false;
        }

        // Compare by release date
        return version1.getReleaseDate().after(version2.getReleaseDate());
    }
}