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
            if (TicketCleaner.hasRequiredVersions(ticket) && !hasZeroDenominator(ticket) && TicketValidator.hasConsistentVersions(ticket)) {
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
            if (TicketCleaner.hasRequiredVersions(ticket) && !hasZeroDenominator(ticket) && TicketValidator.hasConsistentVersions(ticket)) {
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
        Version fv = VersionUtils.getLatestVersion(ticket.getFixedVersions());
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

    /**
     * Predicts the injected version of a ticket based on a proportion value.
     *
     * @param ticket         The ticket for which to predict the injected version
     * @param p              The proportion value to use for prediction
     * @param projectVersions The list of all project versions
     * @return The predicted injected version, or null if a prediction cannot be made
     */
    public static Version predictIV(Ticket ticket, double p, List<Version> projectVersions) {
        // Validate inputs
        if (!isValidInput(ticket, projectVersions)) {
            return null;
        }

        // Get the latest fixed version and opening version
        Version fv = VersionUtils.getLatestVersion(ticket.getFixedVersions());
        Version ov = ticket.getOpeningVersion();

        // Ensure versions have release dates
        if (!hasValidReleaseDates(fv, ov)) {
            return null;
        }

        // Calculate predicted date
        Date predictedDate = calculatePredictedDate(fv, ov, p);

        // Find oldest version and check suitability
        Version oldestVersion = findOldestVersion(projectVersions);
        checkPredictionSuitability(ticket, predictedDate, oldestVersion);

        // Find the best matching version for the predicted date
        return findBestMatchingVersion(projectVersions, predictedDate);
    }

    /**
     * Validates the input parameters for the predictIV method.
     */
    private static boolean isValidInput(Ticket ticket, List<Version> projectVersions) {
        return ticket != null
                && ticket.getFixedVersions() != null
                && !ticket.getFixedVersions().isEmpty()
                && ticket.getOpeningVersion() != null
                && projectVersions != null
                && !projectVersions.isEmpty();
    }

    /**
     * Checks if the given versions have valid release dates.
     */
    private static boolean hasValidReleaseDates(Version fv, Version ov) {
        return fv != null
                && fv.getReleaseDate() != null
                && ov.getReleaseDate() != null;
    }

    /**
     * Calculates the predicted date based on the proportion formula.
     */
    private static Date calculatePredictedDate(Version fv, Version ov, double p) {
        double fvTime = fv.getReleaseDate().getTime();
        double ovTime = ov.getReleaseDate().getTime();
        double predictedTime = fvTime - (fvTime - ovTime) * p;
        return new java.util.Date((long) predictedTime);
    }

    /**
     * Finds the oldest version in the project versions list.
     */
    private static Version findOldestVersion(List<Version> projectVersions) {
        Version oldestVersion = null;
        for (Version version : projectVersions) {
            if (version.getReleaseDate() != null &&
                    (oldestVersion == null || version.getReleaseDate().before(oldestVersion.getReleaseDate()))) {
                oldestVersion = version;
            }
        }
        return oldestVersion;
    }

    /**
     * Checks if the prediction is suitable and sets the flag accordingly.
     */
    private static void checkPredictionSuitability(Ticket ticket, Date predictedDate, Version oldestVersion) {
        if (oldestVersion != null && predictedDate.before(oldestVersion.getReleaseDate())) {
            ticket.setUnsuitablePredictedIV(true);
        }
    }

    /**
     * Finds the best matching version for the predicted date.
     */
    private static Version findBestMatchingVersion(List<Version> projectVersions, Date predictedDate) {
        Version latestVersionBeforePrediction = null;

        for (Version version : projectVersions) {
            if (version.getReleaseDate() == null) {
                continue;
            }

            // If this version was released before or exactly at the predicted time
            if (!version.getReleaseDate().after(predictedDate) &&
                    (latestVersionBeforePrediction == null ||
                            version.getReleaseDate().after(latestVersionBeforePrediction.getReleaseDate()))) {
                latestVersionBeforePrediction = version;
            }
        }

        //        // Log a warning if no suitable version was found
//        if (latestVersionBeforePrediction == null) {
//            LOGGER.warning("No suitable version found for ticket " + ticket.getKey() +
//                    " - Predicted date: " + predictedDate +
//                    " - Oldest version date: " +
//                    (oldestVersion != null ? oldestVersion.getReleaseDate() : "No versions with date"));
//        }

        return latestVersionBeforePrediction;
    }

    public static boolean hasZeroDenominator(Ticket ticket) {
        Version lv = VersionUtils.getLatestVersion(ticket.getFixedVersions());
        if (lv != null && lv.getReleaseDate() != null && ticket.getOpeningVersion().getReleaseDate() != null) {
            return lv.getReleaseDate().equals(ticket.getOpeningVersion().getReleaseDate());
        } else {
            return false;
        }
    }
}