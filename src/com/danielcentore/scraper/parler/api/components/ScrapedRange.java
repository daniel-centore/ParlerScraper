package com.danielcentore.scraper.parler.api.components;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.danielcentore.scraper.parler.api.ParlerTime;
import com.danielcentore.scraper.parler.api.ScrapeType;

@Entity
@Table(name = "scraped_ranges")
public class ScrapedRange {

    int id;
    ScrapeType scrapedType;
    String scrapedId;
    String scrapeStart;
    String scrapeEnd;

    public ScrapedRange(ScrapeType scrapedType, String scrapedId, ParlerTime scrapeStart, ParlerTime scrapeEnd) {
        this.scrapedType = scrapedType;
        this.scrapedId = scrapedId;
        this.scrapeStart = scrapeStart == null ? null : scrapeStart.toParlerTimestamp();
        this.scrapeEnd = scrapeEnd == null ? null : scrapeEnd.toParlerTimestamp();
    }

    public ScrapedRange() {
        // Needed for Hibernate
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "scraped_type")
    public ScrapeType getScrapedType() {
        return scrapedType;
    }

    public void setScrapedType(ScrapeType scrapedType) {
        this.scrapedType = scrapedType;
    }

    @Column(name = "scraped_id")
    public String getScrapedId() {
        return scrapedId;
    }

    public void setScrapedId(String scrapedId) {
        this.scrapedId = scrapedId;
    }

    @Column(name = "scrape_start")
    public String getScrapeStart() {
        return scrapeStart;
    }

    public void setScrapeStart(String scrapeStart) {
        this.scrapeStart = scrapeStart;
    }

    @Column(name = "scrape_end")
    public String getScrapeEnd() {
        return scrapeEnd;
    }

    public void setScrapeEnd(String scrapeEnd) {
        this.scrapeEnd = scrapeEnd;
    }

}