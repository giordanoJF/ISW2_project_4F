package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

public class TicketValidator {

    private TicketValidator(){
        throw  new IllegalStateException("Utility class");
    }

    public static boolean hasConsistentVersions(Ticket ticket) {
        Version lv = VersionUtils.getLatestVersion(ticket.getFixedVersions());
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
            if (!VersionUtils.isVersionNewer(fixedVersion, affectedVersion)) {
                return true;
            }
        }

        return false;
    }
}
