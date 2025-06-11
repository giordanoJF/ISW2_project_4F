package it.giordano.isw_project.models;

import jakarta.annotation.Nullable;

import java.util.Date;

@Nullable
public class Version {
    @Nullable private String id;
    @Nullable private String name;
    @Nullable private Date releaseDate;


    //getters and setters
    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public Date getReleaseDate() {
        return releaseDate;
    }
    public void setReleaseDate(@Nullable Date releaseDate) {
        this.releaseDate = releaseDate;
    }
}
