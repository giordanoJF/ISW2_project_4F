package it.giordano.isw_project.model;

import java.util.ArrayList;
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
    private Version openingVersion; // OV
    private List<Version> fixedVersions; // FV
    private List<Version> affectedVersions; // AV
    private Version injectedVersion; // IV (oldest AV)

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

    public Version getOpeningVersion() {
        return openingVersion;
    }

    public void setOpeningVersion(Version openingVersion) {
        this.openingVersion = openingVersion;
    }

    public List<Version> getFixedVersions() {
        return fixedVersions;
    }

    public void addFixedVersion(Version version) {
        if (this.fixedVersions == null) {
            this.fixedVersions = new ArrayList<>();
        }
        this.fixedVersions.add(version);
    }

    public List<Version> getAffectedVersions() {
        return affectedVersions;
    }

    public void addAffectedVersion(Version version) {
        if (this.affectedVersions == null) {
            this.affectedVersions = new ArrayList<>();
        }
        this.affectedVersions.add(version);
    }

    public Version getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(Version injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                ", status='" + status + '\'' +
                ", resolution='" + resolution + '\'' +
                ", openingVersion=" + (openingVersion != null ? openingVersion.getName() : "null") +
                ", fixedVersions=" + getVersionNames(fixedVersions) +
                ", affectedVersions=" + getVersionNames(affectedVersions) +
                ", injectedVersion=" + (injectedVersion != null ? injectedVersion.getName() : "null") +
                '}';
    }

    private List<String> getVersionNames(List<Version> versions) {
        List<String> names = new ArrayList<>();
        if (versions != null) {
            for (Version version : versions) {
                names.add(version.getName());
            }
        }
        return names;
    }
}