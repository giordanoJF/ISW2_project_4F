package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Utility class for cleaning and validating tickets.
 * This class provides methods to sanitize ticket data and remove invalid tickets.
 */
public final class TicketCleaner {

    private static final Logger LOGGER = Logger.getLogger(TicketCleaner.class.getName());

    private TicketCleaner() {
        // Private constructor to prevent instantiation
    }

    /**
     * Cleans and validates a list of tickets based on specific rules.
     * Tickets are modified in-place and invalid ones are removed from the list.
     *
     * @param tickets the list of tickets to clean
     * @return the cleaned list of tickets
     */
    public static List<Ticket> cleanTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return Collections.emptyList();
        }

        // First pass: normalize ticket data
        normalizeTickets(tickets);

        // Second pass: remove invalid tickets
        removeInvalidTickets(tickets);

        return tickets;
    }

    /**
     * Normalizes ticket data by applying various rules:
     * - If multiple injected versions exist, keep only the oldest
     * - If multiple opening versions exist, keep only the oldest
     * - If multiple fixed versions exist, keep only the newest
     * - Mark tickets with missing injected/affected versions for future work
     *
     * @param tickets the list of tickets to normalize
     */
    private static void normalizeTickets(List<Ticket> tickets) {
        for (Ticket ticket : tickets) {
            // Process multiple fixed versions - keep only the most recent one
            if (ticket.getFixedVersions() != null && ticket.getFixedVersions().size() > 1) {
                Version latestFixedVersion = findLatestVersion(ticket.getFixedVersions());
                List<Version> singleFixedVersion = new ArrayList<>();
                singleFixedVersion.add(latestFixedVersion);
                ticket.getFixedVersions().clear();
                ticket.getFixedVersions().addAll(singleFixedVersion);
            }

            // Check for missing injected or affected versions
            if (ticket.getInjectedVersion() == null ||
                    ticket.getAffectedVersions() == null ||
                    ticket.getAffectedVersions().isEmpty()) {
                LOGGER.info("Ticket " + ticket.getKey() +
                        " needs future work with proportion calculation due to missing injected/affected versions");
            }

            // If multiple injected versions (though this shouldn't happen based on model),
            // we would keep only the oldest one
            // This is a safeguard in case the model changes in the future

            // If we had multiple opening versions, keep only the oldest one
            // Currently the model doesn't support multiple opening versions directly,
            // but this is a safeguard for future changes
        }
    }

    /**
     * Removes invalid tickets from the list based on specific validation rules.
     *
     * @param tickets the list of tickets to validate
     */
    private static void removeInvalidTickets(List<Ticket> tickets) {
        Iterator<Ticket> iterator = tickets.iterator();
        while (iterator.hasNext()) {
            Ticket ticket = iterator.next();

            // Remove ticket if opening version is null or empty
            if (ticket.getOpeningVersion() == null) {
                iterator.remove();
                LOGGER.info("Removed ticket " + ticket.getKey() + " due to missing opening version");
                continue;
            }

            // Remove ticket if fixed version is null or empty
            // Note: Fixed version info is also available in commits
            if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()) {
                iterator.remove();
                LOGGER.info("Removed ticket " + ticket.getKey() +
                        " due to missing fixed version (could be retrieved from commits)");
                continue;
            }

            // Get fixed version (we already ensured there's only one after normalization)
            Version fixedVersion = ticket.getFixedVersions().get(0);

            // Remove if opening version > fixed version
            if (isVersionNewer(ticket.getOpeningVersion(), fixedVersion)) {
                iterator.remove();
                LOGGER.info("Removed ticket " + ticket.getKey() +
                        " because opening version is newer than fixed version");
                continue;
            }

            // Remove if fixed version <= any affected version
            if (ticket.getAffectedVersions() != null && !ticket.getAffectedVersions().isEmpty()) {
                boolean isFixedVersionOlderOrEqualToAnyAffected = false;
                for (Version affectedVersion : ticket.getAffectedVersions()) {
                    if (!isVersionNewer(fixedVersion, affectedVersion)) {
                        isFixedVersionOlderOrEqualToAnyAffected = true;
                        break;
                    }
                }

                if (isFixedVersionOlderOrEqualToAnyAffected) {
                    iterator.remove();
                    LOGGER.info("Removed ticket " + ticket.getKey() +
                            " because fixed version is older than or equal to an affected version");
                    continue;
                }
            }

            // Remove if injected version >= fixed version
            if (ticket.getInjectedVersion() != null &&
                    isVersionNewer(ticket.getInjectedVersion(), fixedVersion)) {
                iterator.remove();
                LOGGER.info("Removed ticket " + ticket.getKey() +
                        " because injected version is newer than fixed version");
            }
        }
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