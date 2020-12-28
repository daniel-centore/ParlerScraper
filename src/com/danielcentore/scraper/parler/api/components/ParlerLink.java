package com.danielcentore.scraper.parler.api.components;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "links")
public class ParlerLink {

    // e.g. "xXtqI"
    String linkId;

    // e.g. "20201224130358"
    @JsonProperty("createdAt")
    String createdAt;

    // e.g. "thejeffreylord.com"
    @JsonProperty("domain")
    String domain;

    // e.g.
    // "https://thejeffreylord.com/bailout-money-going-to-performing-arts-museums-and-gender-programs-in-pakistan-before-american-people/"
    @JsonProperty("long")
    String longUrl;

    // e.g.
    // "thejeffreylord.com/bailout-money-going-to-performing-arts-museums-and-gender-programs-in-pakistan-before-american-people/"
    @JsonProperty("modified")
    String modifiedUrl;

    // e.g. "https://api.parler.com/l/xXtqI"
    @JsonProperty("short")
    String shortUrl;

    // e.g. "VALID"
    @JsonProperty("state")
    String state;

    // == From Metadata == //
    String metadataUrl;
    String metadataType;
    String metadataImage;
    String metadataTitle;
    String metadataSite;
    Long metadataLength;
    String metadataLocale;
    String metadataMimeType;
    String metadataSiteName;
    String metadataDescription;
    String metadataVideo;

    public ParlerLink(@JsonProperty("id") String idA,
            @JsonProperty("_id") String idB,
            @JsonProperty("metadata") ParlerLinkMetadata metadata) {
        this.linkId = (idA != null && !idA.isEmpty()) ? idA : idB;

        this.metadataUrl = metadata.url;
        this.metadataType = metadata.type;
        this.metadataImage = metadata.image;
        this.metadataTitle = metadata.title;
        this.metadataSite = metadata.site;
        this.metadataLength = metadata.length;
        this.metadataLocale = metadata.locale;
        this.metadataMimeType = metadata.mimeType;
        this.metadataSiteName = metadata.siteName;
        this.metadataDescription = metadata.description;
        this.metadataVideo = metadata.video;
    }

    public ParlerLink() {
        // Needed for Hibernate to load
    }

    @Id
    @Column(name = "link_id")
    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    @Column(name = "created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Column(name = "domain")
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Column(name = "long_url")
    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    @Column(name = "modified_url")
    public String getModifiedUrl() {
        return modifiedUrl;
    }

    public void setModifiedUrl(String modifiedUrl) {
        this.modifiedUrl = modifiedUrl;
    }

    @Column(name = "short_url")
    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    @Column(name = "state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Column(name = "metadata_url")
    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    @Column(name = "metadata_type")
    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = metadataType;
    }

    @Column(name = "metadata_image")
    public String getMetadataImage() {
        return metadataImage;
    }

    public void setMetadataImage(String metadataImage) {
        this.metadataImage = metadataImage;
    }

    @Column(name = "metadata_title")
    public String getMetadataTitle() {
        return metadataTitle;
    }

    public void setMetadataTitle(String metadataTitle) {
        this.metadataTitle = metadataTitle;
    }

    @Column(name = "metadata_site")
    public String getMetadataSite() {
        return metadataSite;
    }

    public void setMetadataSite(String metadataSite) {
        this.metadataSite = metadataSite;
    }

    @Column(name = "metadata_length")
    public Long getMetadataLength() {
        return metadataLength;
    }

    public void setMetadataLength(Long metadataLength) {
        this.metadataLength = metadataLength;
    }

    @Column(name = "metadata_locale")
    public String getMetadataLocale() {
        return metadataLocale;
    }

    public void setMetadataLocale(String metadataLocale) {
        this.metadataLocale = metadataLocale;
    }

    @Column(name = "metadata_mime_type")
    public String getMetadataMimeType() {
        return metadataMimeType;
    }

    public void setMetadataMimeType(String metadataMimeType) {
        this.metadataMimeType = metadataMimeType;
    }

    @Column(name = "metadata_site_name")
    public String getMetadataSiteName() {
        return metadataSiteName;
    }

    public void setMetadataSiteName(String metadataSiteName) {
        this.metadataSiteName = metadataSiteName;
    }

    @Column(name = "metadata_description")
    public String getMetadataDescription() {
        return metadataDescription;
    }

    public void setMetadataDescription(String metadataDescription) {
        this.metadataDescription = metadataDescription;
    }

    @Column(name = "metadata_video")
    public String getMetadataVideo() {
        return metadataVideo;
    }

    public void setMetadataVideo(String metadataVideo) {
        this.metadataVideo = metadataVideo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((linkId == null) ? 0 : linkId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ParlerLink other = (ParlerLink) obj;
        if (linkId == null) {
            if (other.linkId != null)
                return false;
        } else if (!linkId.equals(other.linkId))
            return false;
        return true;
    }

}
