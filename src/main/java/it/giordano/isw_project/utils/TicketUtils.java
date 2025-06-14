package it.giordano.isw_project.utils;

import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;
import it.giordano.isw_project.services.JiraScraperService;
import jakarta.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;

public final class TicketUtils {

    private TicketUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void setFixedVersionFromFixedVersions(@Nullable Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null");
        }

        // Set most recent fixed version
        if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()) {
            ticket.setFixedVersion(null);
        } else {
            ticket.setFixedVersion(VersionUtils.findNewestVersionFromVersions(ticket.getFixedVersions()));
        }
    }

    /**
     * Sets derived version information for a ticket.
     *
     * <p>Calculates and sets the injected version and opening version for the ticket
     * based on the affected versions and creation date.</p>
     *
     * @param ticket the ticket to update
     * @param versionMap the map of all available versions
     * @throws IllegalArgumentException if ticket or versionMap is null
     */
    public static void setDerivedTicketVersionsFromItsVersions(@Nullable Ticket ticket, @Nullable Map<String, Version> versionMap) {
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }

        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
        }

        setTicketInjectedVersionFromAffectedVersions(ticket);
        setTicketOpeningVersionFromItsProjectVersions(ticket, versionMap);
    }

    /**
     * Sets the opening version for a ticket.
     *
     * <p>The opening version is the most recent version that was released
     * before or on the ticket creation date. This represents the version
     * that was current when the ticket was opened.</p>
     *
     * @param ticket the ticket to update
     * @param versionMap the map of all available versions
     * @throws IllegalArgumentException if ticket or versionMap is null
     */
    private static void setTicketOpeningVersionFromItsProjectVersions(@Nullable Ticket ticket, @Nullable Map<String, Version> versionMap) {
        if (ticket == null || versionMap == null) {
            throw new IllegalArgumentException("Ticket and version map cannot be null");
        }
        if (versionMap.isEmpty()) {
            throw new IllegalArgumentException("Version map cannot be empty");
        }

        Date createdDate = ticket.getCreatedDate();
        if (createdDate == null) {
            return;
        }

        Version latestVersion = null;

        for (Version version : versionMap.values()) {
            if (VersionUtils.isValidVersionForOpening(version, createdDate) &&
                    VersionUtils.isMoreRecentThanCurrent(version, latestVersion)) {
                latestVersion = version;
            }
        }

        ticket.setOpeningVersion(latestVersion);
    }

    /**
     * Sets the injected version for a ticket.
     *
     * <p>The injected version is determined as the oldest affected version
     * that has a release date. This represents the version where the bug
     * was likely introduced.</p>
     *
     * @param ticket the ticket to update
     * @throws IllegalArgumentException if ticket is null
     */
    private static void setTicketInjectedVersionFromAffectedVersions(@Nullable Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null");
        }

        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions == null || affectedVersions.isEmpty()) {
            return;
        }

        Version oldestVersion = VersionUtils.findOldestVersionFromVersions(affectedVersions);
        if (oldestVersion == null) {
            ticket.setInjectedVersion(null);
            return;
        }
        ticket.setInjectedVersion(oldestVersion);
    }
}
