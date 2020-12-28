package com.danielcentore.scraper.parler;

import java.io.FileNotFoundException;

import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.db.ScraperDb;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        ScraperDb scraperDb = new ScraperDb();

        ParlerClient client = new ParlerClient();

        //	ParlerUser profile = client.fetchProfile("TuckerCarlson");
        //	scraperDb.storeUser(profile);

        //	System.out.println(profile);
        //	PagedParlerUsers following = client.fetchFollowing(profile);
        //	System.out.println(following);
        //	PagedParlerUsers followers = client.fetchFollowers(profile);
        //	System.out.println(followers);
        //	
        //	scraperDb.storePagedUsers(following);
        //	scraperDb.storePagedUsers(followers);
        //	
        //	System.out.println(scraperDb.getAllNotWorthlessUsers());

        //	PagedParlerPosts posts = client.fetchPagedPosts(profile);
        //	scraperDb.storePagedPosts(posts);
        //	System.out.println(posts);
        PagedParlerPosts hastagPosts = client.fetchPagedHashtag("maga");
        scraperDb.storePagedPosts(hastagPosts);
        //	System.out.println(hastagPosts);
    }

    public static void resumeUsernameScrapeBtn(String jst, String mst) {

    }

    public static void stopUsernameScrapeBtn() {

    }

    public static void resumePostScrapeBtn(String jst, String mst, String startDate, String endDate,
            int maxPostsPerUser) {
    }

    public static void stopPostScrapeBtn() {

    }

}
