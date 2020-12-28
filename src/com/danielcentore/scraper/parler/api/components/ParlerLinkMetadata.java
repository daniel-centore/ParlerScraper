package com.danielcentore.scraper.parler.api.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// The metadata often contains lots of weird properties, just ignore them
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParlerLinkMetadata {

    @JsonProperty("url")
    String url;

    @JsonProperty("type")
    String type;

    @JsonProperty("image")
    String image;

    @JsonProperty("title")
    String title;

    @JsonProperty("site")
    String site;

    @JsonProperty("length")
    Long length;

    @JsonProperty("locale")
    String locale;

    @JsonProperty("mimeType")
    String mimeType;

    @JsonProperty("site_name")
    String siteName;

    @JsonProperty("description")
    String description;

    @JsonProperty("video")
    String video;
}
