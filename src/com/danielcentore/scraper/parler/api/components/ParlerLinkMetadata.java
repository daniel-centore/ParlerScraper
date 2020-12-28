package com.danielcentore.scraper.parler.api.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents Parler Link Metadata
 * 
 * This is used just for JSON extraction. These properties are ultimately encoded into the {@link ParlerLink}.
 * 
 * The metadata often contains lots of weird properties, we just ignore them.
 *
 * @author Daniel Centore
 */
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
