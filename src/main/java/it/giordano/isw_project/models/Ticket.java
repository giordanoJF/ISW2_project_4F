package it.giordano.isw_project.models;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Nullable
public class Ticket {
    @Nullable private String key;
    @Nullable private Date createdDate;
    @Nullable private Date resolutionDate;
    @Nullable private Version openingVersion;
    @Nullable private Version fixedVersion;
    @Nullable private List<Version> fixedVersions;
    @Nullable private List<Version> affectedVersions;
    @Nullable private Version injectedVersion;
    @Nullable private Boolean unsuitablePredictedIV;


    // Getters and Setters
    @Nullable
    public String getKey() {
        return key;
    }

    public void setKey(@Nullable String key) {
        this.key = key;
    }

    @Nullable
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(@Nullable Date createdDate) {
        this.createdDate = createdDate;
    }

    @Nullable
    public Date getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(@Nullable Date resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    @Nullable
    public Version getOpeningVersion() {
        return openingVersion;
    }

    public void setOpeningVersion(@Nullable Version openingVersion) {
        this.openingVersion = openingVersion;
    }

    @Nullable
    public Version getFixedVersion() {
        return fixedVersion;
    }

    public void setFixedVersion(@Nullable Version fixedVersion) {
        this.fixedVersion = fixedVersion;
    }

    @Nullable
    public List<Version> getAffectedVersions() {
        return affectedVersions;
    }

    public void setAffectedVersions(@Nullable List<Version> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    public void addAffectedVersion(@Nullable Version version) {
        if (this.affectedVersions == null) {
            this.affectedVersions = new ArrayList<>();
        }
        this.affectedVersions.add(version);
    }

    @Nullable
    public Version getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(@Nullable Version injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    @Nullable
    public Boolean getUnsuitablePredictedIV() {
        return unsuitablePredictedIV;
    }

    public void setUnsuitablePredictedIV(@Nullable Boolean unsuitablePredictedIV) {
        this.unsuitablePredictedIV = unsuitablePredictedIV;
    }

    @Nullable
    public List<Version> getFixedVersions() {
        return fixedVersions;
    }
    public void setFixedVersions(@Nullable List<Version> fixedVersions) {
        this.fixedVersions = fixedVersions;
    }
    public void addFixedVersion(@Nullable Version version) {
        if (this.fixedVersions == null) {
            this.fixedVersions = new ArrayList<>();
        }
        this.fixedVersions.add(version);
    }
}
