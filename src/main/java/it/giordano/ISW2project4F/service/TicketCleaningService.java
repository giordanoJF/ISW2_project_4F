//package it.giordano.ISW2project4F.service;
//
//import it.giordano.ISW2project4F.model.Ticket;
//import it.giordano.ISW2project4F.model.Version;
//
//import java.util.*;
//
//public class TicketCleaningService {
//
//    /**
//     * Object to store a removed ticket and the reason for removal
//     */
//    public static class RemovedTicket {
//        private final Ticket ticket;
//        private final String reason;
//
//        public RemovedTicket(Ticket ticket, String reason) {
//            this.ticket = ticket;
//            this.reason = reason;
//        }
//
//        public Ticket getTicket() {
//            return ticket;
//        }
//
//        public String getReason() {
//            return reason;
//        }
//    }
//
//    /**
//     * Cleans the list of tickets based on specified validation rules.
//     *
//     * @param tickets List of tickets to clean
//     * @return List of valid tickets
//     */
//    public List<Ticket> cleanTickets(List<Ticket> tickets) {
//        List<Ticket> validTickets = new ArrayList<>();
//        List<RemovedTicket> removedTickets = new ArrayList<>();
//
//        for (Ticket ticket : tickets) {
//            // Make a copy of the ticket to work with
//            Ticket processedTicket = processTicket(ticket);
//
//            // Check if the ticket is valid
//            String invalidReason = validateTicket(processedTicket);
//
//            if (invalidReason == null) {
//                // Ticket is valid, add to valid list
//                validTickets.add(processedTicket);
//            } else {
//                // Ticket is invalid, add to removed list with reason
//                removedTickets.add(new RemovedTicket(ticket, invalidReason));
//            }
//        }
//
//        // Store removed tickets for later CSV export
//        this.removedTickets = removedTickets;
//
//        return validTickets;
//    }
//
//    private List<RemovedTicket> removedTickets = new ArrayList<>();
//
//    /**
//     * Gets the list of removed tickets and their removal reasons.
//     *
//     * @return List of removed tickets with reasons
//     */
//    public List<RemovedTicket> getRemovedTickets() {
//        return removedTickets;
//    }
//
//    /**
//     * Processes a ticket to ensure it follows the defined rules:
//     * - Keep only the latest Fixed Version if multiple are present
//     *
//     * @param ticket The ticket to process
//     * @return A new processed ticket
//     */
//    private Ticket processTicket(Ticket ticket) {
//        // Create a new ticket to avoid modifying the original
//        Ticket processedTicket = new Ticket();
//        processedTicket.setKey(ticket.getKey());
//        processedTicket.setSummary(ticket.getSummary());
//        processedTicket.setDescription(ticket.getDescription());
//        processedTicket.setCreatedDate(ticket.getCreatedDate());
//        processedTicket.setResolutionDate(ticket.getResolutionDate());
//        processedTicket.setStatus(ticket.getStatus());
//        processedTicket.setResolution(ticket.getResolution());
//        processedTicket.setOpeningVersion(ticket.getOpeningVersion());
//        processedTicket.setAffectedVersions(new ArrayList<>(ticket.getAffectedVersions()));
//        processedTicket.setInjectedVersion(ticket.getInjectedVersion());
//
//        // Process Fixed Versions - keep only the latest one
//        if (ticket.getFixedVersions() != null && !ticket.getFixedVersions().isEmpty()) {
//            // Find the latest (newest) fixed version
//            Version latestFV = getLatestVersion(ticket.getFixedVersions());
//
//            if (latestFV != null) {
//                List<Version> fixedVersions = new ArrayList<>();
//                fixedVersions.add(latestFV);
//                processedTicket.setFixedVersions(fixedVersions);
//            } else {
//                processedTicket.setFixedVersions(new ArrayList<>());
//            }
//        } else {
//            processedTicket.setFixedVersions(new ArrayList<>());
//        }
//
//        return processedTicket;
//    }
//
//    /**
//     * Validates a ticket based on specified rules.
//     * Returns null if the ticket is valid, or a reason string if invalid.
//     *
//     * @param ticket The ticket to validate
//     * @return Reason for removal or null if valid
//     */
//    private String validateTicket(Ticket ticket) {
//        // Check if there's no Opening Version (OV)
//        if (ticket.getOpeningVersion() == null) {
//            return "No Opening Version (OV)";
//        }
//
//        // Check if there's no Fixed Version (FV)
//        if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()) {
//            return "No Fixed Version (FV)";
//        }
//
////        // Check if there's no Affected Version (AV)
////        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
////            return "No Affected Version (AV)";
////        }
//
//        // Get the oldest Affected Version (AV)
//        Version oldestAV = getOldestVersion(ticket.getAffectedVersions());
//
//        // Check if oldest AV is after OV
//        if (oldestAV != null && ticket.getOpeningVersion() != null &&
//                isVersionAfter(oldestAV, ticket.getOpeningVersion())) {
//            return "Oldest Affected Version (AV) is after Opening Version (OV)";
//        }
//
//        // Get the latest Fixed Version (FV)
//        Version latestFV = null;
//        if (!ticket.getFixedVersions().isEmpty()) {
//            latestFV = ticket.getFixedVersions().get(0); // As we already kept only the latest one in processTicket
//        }
//
//        // Check if any AV is >= latest FV
//        for (Version av : ticket.getAffectedVersions()) {
//            if (latestFV != null && (isVersionAfter(av, latestFV) || versionsEqual(av, latestFV))) {
//                return "One or more Affected Versions (AV) are >= the latest Fixed Version (FV)";
//            }
//        }
//
//        // Check if OV > FV
//        if (ticket.getOpeningVersion() != null && latestFV != null &&
//                isVersionAfter(ticket.getOpeningVersion(), latestFV)) {
//            return "Opening Version (OV) > Fixed Version (FV)";
//        }
//
//        // All validations passed
//        return null;
//    }
//
//    /**
//     * Gets the latest (newest) version from a list of versions.
//     *
//     * @param versions List of versions
//     * @return The latest version or null if the list is empty
//     */
//    private Version getLatestVersion(List<Version> versions) {
//        if (versions == null || versions.isEmpty()) {
//            return null;
//        }
//
//        return versions.stream()
//                .filter(v -> v.getReleaseDate() != null)
//                .max(Comparator.comparing(Version::getReleaseDate))
//                .orElse(versions.get(0)); // If no release dates, return the first one
//    }
//
//    /**
//     * Gets the oldest version from a list of versions.
//     *
//     * @param versions List of versions
//     * @return The oldest version or null if the list is empty
//     */
//    private Version getOldestVersion(List<Version> versions) {
//        if (versions == null || versions.isEmpty()) {
//            return null;
//        }
//
//        return versions.stream()
//                .filter(v -> v.getReleaseDate() != null)
//                .min(Comparator.comparing(Version::getReleaseDate))
//                .orElse(versions.get(0)); // If no release dates, return the first one
//    }
//
//    /**
//     * Checks if version1 is after version2 based on release date.
//     *
//     * @param version1 First version
//     * @param version2 Second version
//     * @return True if version1 is after version2
//     */
//    private boolean isVersionAfter(Version version1, Version version2) {
//        if (version1 == null || version2 == null) {
//            return false;
//        }
//
//        Date date1 = version1.getReleaseDate();
//        Date date2 = version2.getReleaseDate();
//
//        if (date1 == null || date2 == null) {
//            return false;
//        }
//
//        return date1.after(date2);
//    }
//
//    /**
//     * Checks if two versions are equal based on release date.
//     *
//     * @param version1 First version
//     * @param version2 Second version
//     * @return True if versions have the same release date
//     */
//    private boolean versionsEqual(Version version1, Version version2) {
//        if (version1 == null || version2 == null) {
//            return false;
//        }
//
//        Date date1 = version1.getReleaseDate();
//        Date date2 = version2.getReleaseDate();
//
//        if (date1 == null || date2 == null) {
//            return false;
//        }
//
//        return date1.equals(date2);
//    }
//}