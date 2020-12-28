package com.danielcentore.scraper.parler.api.components;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "hashtags")
public class ParlerHashtag {

    private String hashtag;

    private Long totalPosts;

    // The number of times we've seen this hashtag while scraping
    private long encounters;

    public ParlerHashtag(String hashtag) {
        this(hashtag, null);
    }

    public ParlerHashtag(String hashtag, Long totalPosts) {
        this.hashtag = hashtag.toLowerCase();
        this.totalPosts = totalPosts;
    }

    public ParlerHashtag() {
        // Needed for Hibernate to load
    }

    @Id
    @Column(name = "hashtag_lowercase")
    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String hashtag) {
        this.hashtag = hashtag.toLowerCase();
    }

    @Column(name = "total_posts")
    public Long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Long totalPosts) {
        this.totalPosts = totalPosts;
    }

    @Column(name = "encounters")
    public Long getEncounters() {
        return encounters;
    }

    public void setEncounters(Long hashtagEncounters) {
        this.encounters = hashtagEncounters;
    }

}
