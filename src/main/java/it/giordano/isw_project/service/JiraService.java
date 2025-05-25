package it.giordano.isw_project.service;

import it.giordano.isw_project.model.Ticket;
import it.giordano.isw_project.model.Version;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JiraService {
    private static final String JIRA_BASE_URL = "https://issues.apache.org/jira/rest/api/2";
    private static final SimpleDateFormat TICKET_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final SimpleDateFormat VERSION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger LOGGER = Logger.getLogger(JiraService.class.getName());

    // Campi JSON per le versioni
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_RELEASED = "released";
    private static final String FIELD_ARCHIVED = "archived";
    private static final String FIELD_RELEASE_DATE = "releaseDate";

    // Campi JSON per i ticket
    private static final String FIELD_KEY = "key";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CREATED_DATE = "created";
    private static final String FIELD_RESOLUTION_DATE = "resolutiondate";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RESOLUTION = "resolution";
    private static final String FIELD_FIX_VERSIONS = "fixVersions";
    private static final String FIELD_VERSIONS = "versions"; // AV = Affected Versions

    // Costanti per la paginazione e struttura JSON
    private static final int MAX_RESULTS_PER_PAGE = 100;
    private static final String TOTAL = "total";
    private static final String ISSUES = "issues";
    private static final String FIELDS = "fields";

    /**
     * Recupera tutte le versioni di un progetto da Jira.
     *
     * @param projectKey Il codice del progetto
     * @return Lista delle versioni del progetto. Può essere vuota se non ci sono versioni.
     * @throws IOException Se si verifica un errore durante la richiesta HTTP
     */
    public static List<Version> getProjectVersions(String projectKey) throws IOException {
        if (projectKey == null || projectKey.isEmpty()) {
            LOGGER.warning("Project key is null or empty");
            return new ArrayList<>();
        }

        List<Version> versions = new ArrayList<>();
        String url = JIRA_BASE_URL + "/project/" + projectKey + "/versions";
        String jsonResponse = executeGetRequest(url);

        if (jsonResponse == null || jsonResponse.isEmpty()) {
            LOGGER.warning("Empty response from Jira API for project versions");
            return versions;
        }

        JSONArray versionArray = new JSONArray(jsonResponse);
        for (int i = 0; i < versionArray.length(); i++) {
            JSONObject versionJson = versionArray.optJSONObject(i);
            if (versionJson != null) {
                Version version = parseVersionFromJson(versionJson);
                versions.add(version);
            }
        }

        if (versions.isEmpty()) {
            LOGGER.info("No versions found for project " + projectKey);
        } else {
            LOGGER.info("Retrieved " + versions.size() + " versions for project " + projectKey);
        }

        return versions;
    }

    /**
     * Converte un oggetto JSON in un oggetto Version.
     *
     * @param versionJson L'oggetto JSON contenente le informazioni sulla versione
     * @return Oggetto Version popolato con dati dal JSON. Non restituisce mai null.
     */
    private static Version parseVersionFromJson(JSONObject versionJson) {
        if (versionJson == null) {
            return new Version();
        }

        Version version = new Version();
        version.setId(versionJson.optString(FIELD_ID, ""));
        version.setName(versionJson.optString(FIELD_NAME, ""));
        version.setReleased(versionJson.optBoolean(FIELD_RELEASED, false));
        version.setArchived(versionJson.optBoolean(FIELD_ARCHIVED, false));

        String dateStr = versionJson.optString(FIELD_RELEASE_DATE, "");
        if (!dateStr.isEmpty()) {
            try {
                version.setReleaseDate(parseDate(dateStr, VERSION_DATE_FORMAT));
            } catch (ParseException e) {
                LOGGER.warning("Failed to parse release date for version " + version.getName() + ": " + dateStr);
                version.setReleaseDate(null);
            }
        } else {
            version.setReleaseDate(null);
        }

        return version;
    }

    /**
     * Recupera i ticket di bug da Jira che sono stati risolti e chiusi/risolti.
     *
     * @param projectKey Il codice del progetto
     * @return Lista di ticket che soddisfano i criteri
     * @throws IOException Se si verifica un errore durante la richiesta HTTP
     */
    public static List<Ticket> getProjectTickets(String projectKey) throws IOException {
        if (projectKey == null || projectKey.isEmpty()) {
            LOGGER.warning("Project key is null or empty");
            return new ArrayList<>();
        }

        List<Ticket> tickets = new ArrayList<>();
        List<Version> versions = getProjectVersions(projectKey);
        Map<String, Version> versionMap = createVersionMap(versions);

        if (versions.isEmpty()) {
            LOGGER.warning("No versions found for project " + projectKey + ". Cannot retrieve tickets.");
            return tickets;
        }

        String jql = "project=" + projectKey +
                " AND issuetype=Bug" +
                " AND resolution=Fixed" +
                " AND status in (Closed, Resolved)";
        String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);

        int startAt = 0;
        int total = 0;
        boolean firstPage = true;

        do {
            String url = JIRA_BASE_URL + "/search?jql=" + encodedJql +
                    "&startAt=" + startAt +
                    "&maxResults=" + MAX_RESULTS_PER_PAGE +
                    "&fields=key,summary,description,created,resolutiondate,status,resolution,versions,fixVersions";

            String jsonResponse = executeGetRequest(url);
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                LOGGER.warning("Empty response from Jira API for tickets search");
                break;
            }

            JSONObject responseObj = new JSONObject(jsonResponse);
            
            if (firstPage) {
                total = responseObj.optInt(TOTAL, 0);
                firstPage = false;
                LOGGER.info("Found " + total + " tickets for project " + projectKey);
            }

            JSONArray issuesArray = responseObj.optJSONArray(ISSUES);
            
            if (issuesArray == null) {
                LOGGER.warning("No issues array found in response");
                break;
            }

            for (int i = 0; i < issuesArray.length(); i++) {
                JSONObject issueJson = issuesArray.optJSONObject(i);
                if (issueJson != null) {
                    Ticket ticket = parseTicket(issueJson, versionMap);
                    if (ticket != null) {
                        tickets.add(ticket);
                    }
                }
            }

            startAt += MAX_RESULTS_PER_PAGE;
        } while (startAt < total);

        logTicketStatistics(tickets);

        return tickets;
    }

    /**
     * Crea una mappa di nomi di versione su oggetti Version.
     *
     * @param versions Lista di oggetti Version da mappare
     * @return Mappa con nomi di versione come chiavi e oggetti Version come valori
     */
    private static Map<String, Version> createVersionMap(List<Version> versions) {
        Map<String, Version> versionMap = new HashMap<>();
        
        if (versions == null || versions.isEmpty()) {
            return versionMap;
        }
        
        for (Version version : versions) {
            if (version != null && version.getName() != null && !version.getName().isEmpty()) {
                versionMap.put(version.getName(), version);
            }
        }
        
        return versionMap;
    }

    /**
     * Analizza un problema JSON in un oggetto Ticket.
     *
     * @param issueJson L'oggetto JSON contenente i dati del ticket
     * @param versionMap Mappa di nomi di versione a oggetti Version
     * @return Un oggetto Ticket popolato con i dati del JSON
     */
    private static Ticket parseTicket(JSONObject issueJson, Map<String, Version> versionMap) {
        if (issueJson == null) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.setKey(issueJson.optString(FIELD_KEY, ""));
        
        JSONObject fields = issueJson.optJSONObject(FIELDS);
        if (fields == null) {
            LOGGER.warning("No fields found for ticket " + ticket.getKey());
            return ticket;
        }

        // Imposta campi di base
        ticket.setSummary(fields.optString(FIELD_SUMMARY, ""));
        ticket.setDescription(fields.optString(FIELD_DESCRIPTION, ""));

        // Imposta date
        String createdDateStr = fields.optString(FIELD_CREATED_DATE, "");
        if (!createdDateStr.isEmpty()) {
            try {
                ticket.setCreatedDate(parseDate(createdDateStr, TICKET_DATE_FORMAT));
            } catch (ParseException e) {
                LOGGER.warning("Failed to parse created date for ticket " + ticket.getKey() + ": " + createdDateStr);
            }
        }

        String resolutionDateStr = fields.optString(FIELD_RESOLUTION_DATE, "");
        if (!resolutionDateStr.isEmpty()) {
            try {
                ticket.setResolutionDate(parseDate(resolutionDateStr, TICKET_DATE_FORMAT));
            } catch (ParseException e) {
                LOGGER.warning("Failed to parse resolution date for ticket " + ticket.getKey() + ": " + resolutionDateStr);
            }
        }

        // Imposta stato e risoluzione
        JSONObject statusObj = fields.optJSONObject(FIELD_STATUS);
        if (statusObj != null) {
            ticket.setStatus(statusObj.optString(FIELD_NAME, ""));
        }

        JSONObject resolutionObj = fields.optJSONObject(FIELD_RESOLUTION);
        if (resolutionObj != null) {
            ticket.setResolution(resolutionObj.optString(FIELD_NAME, ""));
        }

        // Imposta versioni
        setTicketVersions(ticket, fields, versionMap);

        // Imposta versioni derivate
        setDerivedVersions(ticket, versionMap);

        return ticket;
    }

    /**
     * Imposta le versioni fisse e interessate di un ticket.
     *
     * @param ticket Il ticket da aggiornare
     * @param fields L'oggetto JSON con i campi del ticket
     * @param versionMap Mappa di nomi di versione a oggetti Version
     */
    private static void setTicketVersions(Ticket ticket, JSONObject fields, Map<String, Version> versionMap) {
        if (ticket == null || fields == null || versionMap == null || versionMap.isEmpty()) {
            return;
        }

        // Aggiungi versioni fisse
        JSONArray fixVersions = fields.optJSONArray(FIELD_FIX_VERSIONS);
        if (fixVersions != null) {
            for (int i = 0; i < fixVersions.length(); i++) {
                JSONObject versionJson = fixVersions.optJSONObject(i);
                if (versionJson != null) {
                    String versionName = versionJson.optString(FIELD_NAME, "");
                    if (!versionName.isEmpty() && versionMap.containsKey(versionName)) {
                        ticket.addFixedVersion(versionMap.get(versionName));
                    }
                }
            }
        }

        // Aggiungi versioni interessate
        JSONArray versionsArray = fields.optJSONArray(FIELD_VERSIONS);
        if (versionsArray != null) {
            for (int i = 0; i < versionsArray.length(); i++) {
                JSONObject versionJson = versionsArray.optJSONObject(i);
                if (versionJson != null) {
                    String versionName = versionJson.optString(FIELD_NAME, "");
                    if (!versionName.isEmpty() && versionMap.containsKey(versionName)) {
                        ticket.addAffectedVersion(versionMap.get(versionName));
                    }
                }
            }
        }
    }

    /**
     * Imposta le versioni derivate (versione iniettata e versione di apertura) di un ticket.
     *
     * @param ticket Il ticket da aggiornare
     * @param versionMap Mappa di nomi di versione a oggetti Version
     */
    private static void setDerivedVersions(Ticket ticket, Map<String, Version> versionMap) {
        if (ticket == null || versionMap == null || versionMap.isEmpty()) {
            return;
        }

        // Imposta la versione iniettata (la versione interessata più vecchia)
        List<Version> affectedVersions = ticket.getAffectedVersions();
        if (affectedVersions != null && !affectedVersions.isEmpty()) {
            Version oldestVersion = null;
            
            // Inizializza con la prima versione che ha una data di rilascio
            for (Version version : affectedVersions) {
                if (version != null && version.getReleaseDate() != null) {
                    oldestVersion = version;
                    break;
                }
            }
            
            // Trova la versione più vecchia tra quelle con data di rilascio
            if (oldestVersion != null) {
                for (Version version : affectedVersions) {
                    if (version != null && version.getReleaseDate() != null && 
                        version.getReleaseDate().before(oldestVersion.getReleaseDate())) {
                        oldestVersion = version;
                    }
                }
                ticket.setInjectedVersion(oldestVersion);
            }
//            else if (!affectedVersions.isEmpty()) {
//                // Se nessuna ha una data di rilascio, prendi la prima
//                ticket.setInjectedVersion(affectedVersions.get(0));
//            }
        }

        // Imposta la versione di apertura (l'ultima versione rilasciata prima della creazione del ticket)
        if (ticket.getCreatedDate() != null) {
            Version latestVersion = null;
            
            for (Version version : versionMap.values()) {
                if (version != null && version.getReleaseDate() != null && 
                    !version.getReleaseDate().after(ticket.getCreatedDate())) {
                    if (latestVersion == null || 
                        version.getReleaseDate().after(latestVersion.getReleaseDate())) {
                        latestVersion = version;
                    }
                }
            }
            
            ticket.setOpeningVersion(latestVersion);
        }
    }

    /**
     * Analizza una stringa di data utilizzando il formato di data fornito.
     *
     * @param dateString La stringa di data da analizzare
     * @param dateFormat Il formato di data da utilizzare
     * @return Oggetto Date analizzato dalla stringa di data Jira, o null se la stringa di data è null o vuota
     * @throws ParseException Se la stringa di data non può essere analizzata
     */
    private static Date parseDate(String dateString, SimpleDateFormat dateFormat) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return dateFormat.parse(dateString);
    }

    /**
     * Esegue una richiesta GET all'URL specificato.
     *
     * @param url L'URL a cui inviare la richiesta GET
     * @return La risposta come stringa
     * @throws IOException Se si verifica un errore durante la richiesta HTTP
     */
    private static String executeGetRequest(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    LOGGER.warning("HTTP request failed with status: " + response.getStatusLine().getStatusCode());
                    return null;
                }
                
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return null;
                }
                
                return EntityUtils.toString(entity);
            }
        }
    }

    /**
     * Registra le statistiche sui ticket.
     *
     * @param tickets La lista di ticket da analizzare
     */
    private static void logTicketStatistics(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            LOGGER.info("No tickets to analyze for statistics");
            return;
        }
        
        int totalTickets = tickets.size();
        int ticketsWithoutIV = 0;
        int ticketsWithoutOV = 0;
        int ticketsWithoutAV = 0;
        int ticketsWithoutFV = 0;

        for (Ticket ticket : tickets) {
            if (ticket == null) continue;
            
            if (ticket.getInjectedVersion() == null || 
                (ticket.getInjectedVersion().getName() == null || 
                 ticket.getInjectedVersion().getName().isEmpty())) {
                ticketsWithoutIV++;
            }
            
            if (ticket.getOpeningVersion() == null || 
                (ticket.getOpeningVersion().getName() == null || 
                 ticket.getOpeningVersion().getName().isEmpty())) {
                ticketsWithoutOV++;
            }
            
            if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isEmpty()) {
                ticketsWithoutAV++;
            }
            
            if (ticket.getFixedVersions() == null || ticket.getFixedVersions().isEmpty()) {
                ticketsWithoutFV++;
            }
        }

        double percentIV = ticketsWithoutIV * 100.0 / totalTickets;
        double percentOV = ticketsWithoutOV * 100.0 / totalTickets;
        double percentAV = ticketsWithoutAV * 100.0 / totalTickets;
        double percentFV = ticketsWithoutFV * 100.0 / totalTickets;

        LOGGER.log(Level.INFO,
                """
                        Ticket statistics for {0} tickets:
                        - Missing Injected Version: {1} ({2,number,#.##}%)
                        - Missing Opening Version: {3} ({4,number,#.##}%)
                        - Missing Affected Versions: {5} ({6,number,#.##}%)
                        - Missing Fixed Versions: {7} ({8,number,#.##}%)""",
            new Object[]{
                totalTickets,
                ticketsWithoutIV, percentIV,
                ticketsWithoutOV, percentOV,
                ticketsWithoutAV, percentAV,
                ticketsWithoutFV, percentFV
            });
    }
}