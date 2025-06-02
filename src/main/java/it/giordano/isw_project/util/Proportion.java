package it.giordano.isw_project.util;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for calculating proportions related to ticket versions.
 */
public class Proportion {

    private static final Logger LOGGER = Logger.getLogger(Proportion.class.getName());

    private Proportion() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Calculates the cold start proportion value based on the formula P = (FV - IV) / (FV - OV)
     * P is evaluated ONLY using other projects, maybe we can use also some P of valid tickets.
     * 
     * where:
     * - FV is the Fixed Version
     * - IV is the Injected Version
     * - OV is the Opening Version
     *
     * @param tickets List of tickets to analyze
     * @return The average proportion value across all valid tickets
     */
    public static double evaluatePColdStart(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            LOGGER.warning("No tickets provided for coldStart calculation");
            return 0.0;
        }

        double totalProportion = 0.0;
        int validTickets = 0;

        for (Ticket ticket : tickets) {
            // Check if ticket has all required versions
            if (Consistency.hasRequiredVersions(ticket) && !Misc.hasZeroDenominator(ticket) && Consistency.hasConsistentVersions(ticket)) {
                double p = calculateProportion(ticket);
                if (!Double.isNaN(p) && !Double.isInfinite(p)) {
                    totalProportion += p;
                    validTickets++;
                }
            }
        }

        if (validTickets == 0) {
            LOGGER.warning("No valid tickets found for coldStart calculation");
            return 0.0;
        }

        return totalProportion / validTickets;
    }

    // could be improved in performance using old p instead of recalculating it
    public static double evaluatePIncremental(List<Ticket> tickets, int index) {
        if (tickets == null || tickets.isEmpty()) {
            LOGGER.warning("No tickets provided for incremental calculation");
            return 0.0;
        }

        double totalProportion = 0.0;
        int validTickets = 0;

        for (Ticket ticket : tickets.subList(0, index)) {
            if (Consistency.hasRequiredVersions(ticket) && !Misc.hasZeroDenominator(ticket) && Consistency.hasConsistentVersions(ticket)) {
                double p = calculateProportion(ticket);
                if (!Double.isNaN(p) && !Double.isInfinite(p)) {
                    totalProportion += p;
                    validTickets++;
                }
            }
        }

        if (validTickets == 0) {
            LOGGER.warning("No valid tickets found for incremental calculation");
            return 0.0;
        }

        return totalProportion / validTickets;

    }

    private static double calculateProportion(Ticket ticket) {
        // Get the latest fixed version (FV)
        Version fv = Misc.getLatestVersion(ticket.getFixedVersions());
        if (fv == null) {
            LOGGER.warning("Could not determine latest fixed version for ticket: " + ticket.getKey());
            return 0.0;
        }

        Version iv = ticket.getInjectedVersion();
        Version ov = ticket.getOpeningVersion();

        // Get the indices of these versions to calculate their relative positions
        double fvIndex = fv.getReleaseDate() != null && fv.getReleaseDate().getTime() > 0 ?
                (double) fv.getReleaseDate().getTime() : 0.0;
        double ivIndex = iv.getReleaseDate() != null && iv.getReleaseDate().getTime() > 0 ?
                (double) iv.getReleaseDate().getTime() : 0.0;
        double ovIndex = ov.getReleaseDate() != null && ov.getReleaseDate().getTime() > 0 ?
                (double) ov.getReleaseDate().getTime() : 0.0;

        // Calculate P = (FV - IV) / (FV - OV) with explicit double casting
        double numerator = fvIndex - ivIndex;
        double denominator = fvIndex - ovIndex;

        // Avoid division by zero
        if (denominator == 0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    public static Version predictIV(Ticket ticket, double p, List<Version> projectVersions) {
        if (ticket == null || ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()
                || ticket.getOpeningVersion() == null || projectVersions == null || projectVersions.isEmpty()) {
            return null;
        }

        Version fv = Misc.getLatestVersion(ticket.getFixedVersions());
        Version ov = ticket.getOpeningVersion();

        if (fv == null || fv.getReleaseDate() == null || ov.getReleaseDate() == null) {
            return null;
        }

        double fvTime = fv.getReleaseDate().getTime();
        double ovTime = ov.getReleaseDate().getTime();

        // Calculate predicted IV time using the proportion formula
        double predictedTime = fvTime - (fvTime - ovTime) * p;
        Date predictedDate = new java.util.Date((long) predictedTime);

        // Find the latest version released on or before the predicted date
        Version latestVersionBeforePrediction = null;

        for (Version version : projectVersions) {
            if (version.getReleaseDate() == null) {
                continue;
            }

            // If this version was released before or exactly at the predicted time
            if (!version.getReleaseDate().after(predictedDate)) {
                // If we haven't found a version yet OR this version is newer than our current best match
                if (latestVersionBeforePrediction == null ||
                        version.getReleaseDate().after(latestVersionBeforePrediction.getReleaseDate())) {
                    latestVersionBeforePrediction = version;
                }
            }
        }

        return latestVersionBeforePrediction;
    }


}