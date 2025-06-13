package it.giordano.isw_project.utils;

import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;
import jakarta.annotation.Nullable;

public final class TicketUtils {

    private TicketUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Adds a version to a ticket as either a fix version or an affected version.
     *
     * <p>Utility method to encapsulate the logic of adding versions to tickets.</p>
     *
     * @param ticket the ticket to update
     * @param version the version to add
     * @param isFixVersion true if this is a fix version, false for affected version
     * @throws IllegalArgumentException if ticket or version is null
     *
     * TODO: This method could be improved. It seems too specific.
     */
    public static void addFixedOrAffectedToTicket(@Nullable Ticket ticket, @Nullable Version version, boolean isFixVersion) {
        if (ticket == null || version == null) {
            throw new IllegalArgumentException("Ticket and version cannot be null");
        }

        if (isFixVersion) {
            ticket.addFixedVersion(version);
        } else {
            ticket.addAffectedVersion(version);
        }
    }
}
