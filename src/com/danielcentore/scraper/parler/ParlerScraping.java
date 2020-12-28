package com.danielcentore.scraper.parler;

import org.hibernate.cfg.NotYetImplementedException;

import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.ParlerTime;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.db.ScraperDb;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;

/**
 * Handles the primary logic behind scraping
 *
 * @author Daniel Centore
 */
public class ParlerScraping {

    private ScraperDb db;
    private ParlerClient client;
    ParlerScraperGui gui;
    
    private volatile boolean stopRequested = false;

    public ParlerScraping(ScraperDb db, ParlerClient client, ParlerScraperGui gui) {
        this.db = db;
        this.client = client;
        this.gui = gui;
    }

    public void scrape(ParlerTime startTime, ParlerTime endTime) {
        stopRequested = false;

//        fetchSeedProfiles();

        while (!stopRequested) {
            //            // Fetch a random user's followers, following, and posts
            //            ParlerUser randomUser = getWeightedRandomUser();
            //            PagedParlerPosts userPosts = client.fetchPagedPosts(randomUser, getRandomUserTime(randomUser));
            //            db.storePagedPosts(userPosts);
            //            PagedParlerUsers following = client.fetchFollowing(randomUser, getRandomUserTime(randomUser));
            //            db.storePagedUsers(following);
            //            PagedParlerUsers followers = client.fetchFollowers(randomUser, getRandomUserTime(randomUser));
            //            db.storePagedUsers(followers);
            //
            //            // Fetch posts from a random hashtag
            //            String randomHashtag = getWeightedRandomHashtag();
            //            PagedParlerPosts hashtagPosts = client.fetchPagedHashtag(randomHashtag,
            //                    getRandomHashtagTime(randomHashtag));
            //            db.storePagedPosts(hashtagPosts);
            
            gui.println("Task 1...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            
            if (stopRequested) {
                return;
            }
            
            gui.println("Task 2....");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }
    
    public void stop() {
        this.stopRequested = true;
    }

    private void fetchSeedProfiles() {
        throw new NotYetImplementedException();
    }

    private ParlerUser getWeightedRandomUser() {
        // TODO: Make sure the creation time is before or during the time window!
        throw new NotYetImplementedException();
    }

    private ParlerTime getRandomUserTime(ParlerUser user) {
        throw new NotYetImplementedException();
    }

    private String getWeightedRandomHashtag() {
        throw new NotYetImplementedException();
    }

    private ParlerTime getRandomHashtagTime(String hashtag) {
        throw new NotYetImplementedException();
    }

}
