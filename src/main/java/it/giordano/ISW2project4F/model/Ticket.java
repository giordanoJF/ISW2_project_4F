package it.giordano.ISW2project4F.model;

import java.util.List;
import java.util.Date;

public class Ticket {
    private String key;
    private String summary;
    private String description;
    private Date createdDate;
    private Date resolutionDate;
    private String status;
    private String resolution;
    private String openingVersion; // OV
    private List<String> fixedVersions;   // FV (changed from String to List<String>)
    private List<String> affectedVersions; // AV
    private String injectedVersion; // IV (oldest AV)

//if missing AV (Affected Versions): Empty array (`[]`), never null
//if missing FV (Fixed Versions): Empty array (`[]`), never null
//if missing IV (Injected Version): `null` if missing, converted to empty string when exported to CSV
//if missing OV (Opening Version): `null` if missing, converted to empty string when exported to CSV


    // Constructor
    public Ticket() {
    }

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(Date resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getOpeningVersion() {
        return openingVersion;
    }

    public void setOpeningVersion(String openingVersion) {
        this.openingVersion = openingVersion;
    }

    public List<String> getFixedVersions() {
        return fixedVersions;
    }

    public void setFixedVersions(List<String> fixedVersions) {
        this.fixedVersions = fixedVersions;
    }

    public List<String> getAffectedVersions() {
        return affectedVersions;
    }

    public void setAffectedVersions(List<String> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    public String getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(String injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                ", status='" + status + '\'' +
                ", resolution='" + resolution + '\'' +
                ", openingVersion='" + openingVersion + '\'' +
                ", fixedVersions=" + fixedVersions +
                ", affectedVersions=" + affectedVersions +
                ", injectedVersion='" + injectedVersion + '\'' +
                '}';
    }
}