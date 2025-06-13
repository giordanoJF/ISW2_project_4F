package it.giordano.isw_project;

import it.giordano.isw_project.controllers.JiraScraperController;
import it.giordano.isw_project.models.Ticket;
import it.giordano.isw_project.models.Version;

import java.util.List;

public class Main {
    private static final String PROJECT_KEY = "OPENJPA";

    public static void main(String[] args) {
        // Controllers initialization
        JiraScraperController jiraScraperController = new JiraScraperController();

        // Controllers logic
        List<Version> versions = jiraScraperController.getProjectVersions(PROJECT_KEY);
        List<Ticket> tickets = jiraScraperController.getProjectTickets(PROJECT_KEY, versions);

    }
}
