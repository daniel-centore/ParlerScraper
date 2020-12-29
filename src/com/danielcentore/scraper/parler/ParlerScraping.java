package com.danielcentore.scraper.parler;

import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;

import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.ParlerTime;
import com.danielcentore.scraper.parler.api.ScrapeType;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerHashtag;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.db.ScraperDb;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;

/**
 * Handles the primary logic behind scraping
 *
 * @author Daniel Centore
 */
public class ParlerScraping {

    private static final String TAB = Main.TAB;

    private ScraperDb db;
    private ParlerClient client;
    private ParlerScraperGui gui;

    private volatile boolean stopRequested = false;

    public ParlerScraping(ScraperDb db, ParlerClient client, ParlerScraperGui gui) {
        this.db = db;
        this.client = client;
        this.gui = gui;
    }

    public void scrape(ParlerTime startTime, ParlerTime endTime, List<String> seeds) {
        stopRequested = false;

        gui.println("######################");
        gui.println("### Scraping Seeds ###");
        gui.println("######################");
        for (String seed : seeds) {
            if (stopRequested) {
                return;
            }

            seed = seed.trim();
            if (seed.isEmpty()) {
                continue;
            } else if (seed.startsWith("#")) {
                scrapeHashtag(seed.substring(1), true);
            } else {
                scrapeUsername(seed, true);
            }
        }

        gui.println("#########################");
        gui.println("### Scraping Randomly ###");
        gui.println("#########################");
        while (!stopRequested) {
            ParlerUser user = getWeightedRandomUser();
            scrapeUser(user, false);
        }
    }

    private void scrapeHashtag(String hashtag, boolean skipIfExists) {
        hashtag = hashtag.toLowerCase();

        gui.println("Scraping #" + hashtag);

        if (skipIfExists) {
            ParlerHashtag parlerHashtag = db.getParlerHashtag(hashtag);
            if (parlerHashtag != null && parlerHashtag.getTotalPosts() != null) {
                gui.println(TAB + "Already exists in local DB; skipping");
                return;
            }
        }

        gui.println(TAB + "Fetching from API...");
        PagedParlerPosts hashtagPosts = client.fetchPagedHashtag(hashtag);

        gui.println(TAB + "Storing in local DB...");
        db.storePagedPosts(hashtagPosts);
        db.storeHashtagTotalPostCount(hashtag, hashtagPosts.getTotalPosts());
        db.storeScrapedRange(ScrapeType.HASHTAG_POSTS, hashtag, hashtagPosts);

        gui.println(TAB + "Done.");
    }

    private void scrapeUser(ParlerUser user, boolean skipIfExists) {
        scrapeUsername(user.getUsername(), skipIfExists);
    }

    private void scrapeUsername(String username, boolean skipIfExists) {
        gui.println("Scraping @" + username);

        ParlerUser profile = db.getParlerUserByUsername(username);
        if (skipIfExists) {
            if (profile != null && profile.isFullyScanned()) {
                gui.println(TAB + "Already exists in local DB; skipping");
                return;
            }
        }

        if (stopRequested) {
            return;
        }

        if (profile == null || !profile.isFullyScanned()) {
            gui.println(TAB + "Fetching profile from API...");
            profile = client.fetchProfile(username);

            gui.println(TAB + "Storing profile in local DB...");
            db.storeUser(profile);
        } else {
            gui.println(TAB + "Using fully scanned profile from local DB");
        }
        String userId = profile.getParlerId();

        if (stopRequested) {
            return;
        }

        {
            gui.println(TAB + "Fetching posts from API...");
            PagedParlerPosts pagedPosts = client.fetchPagedPosts(profile, getRandomUserTime(profile));
            int postCount = pagedPosts.getPostCount();
            gui.println(TAB + "Storing " + postCount + " posts in local DB...");
            db.storePagedPosts(pagedPosts);
            db.storeScrapedRange(ScrapeType.USER_POSTS, userId, pagedPosts);
        }

        if (stopRequested) {
            return;
        }

        {
            gui.println(TAB + "Fetching followees from API...");
            PagedParlerUsers pagedFollowing = client.fetchFollowers(profile, getRandomUserTime(profile));
            int followingCount = pagedFollowing.getUsers().size();
            gui.println(TAB + "Storing " + followingCount + " followees in local DB...");
            db.storePagedUsers(pagedFollowing);
            db.storeScrapedRange(ScrapeType.USER_FOLLOWEES, userId, pagedFollowing);
        }

        if (stopRequested) {
            return;
        }

        {
            gui.println(TAB + "Fetching followers from API...");
            PagedParlerUsers pagedFollowers = client.fetchFollowers(profile, getRandomUserTime(profile));
            int followersCount = pagedFollowers.getUsers().size();
            gui.println(TAB + "Storing " + followersCount + " followers in local DB...");
            db.storePagedUsers(pagedFollowers);
            db.storeScrapedRange(ScrapeType.USER_FOLLOWERS, userId, pagedFollowers);
        }

        gui.println(TAB + "Done.");
    }

    private ParlerUser getWeightedRandomUser() {
        // TODO: Make sure the creation time is before or during the time window!
        throw new NotYetImplementedException();
    }

    private ParlerTime getRandomUserTime(ParlerUser user) {
        return null; // TODO!!!!!!!!!!!
    }

    private String getWeightedRandomHashtag() {
        throw new NotYetImplementedException();
    }

    private ParlerTime getRandomHashtagTime(String hashtag) {
        throw new NotYetImplementedException();
    }

    public void stop() {
        this.stopRequested = true;
    }
}
