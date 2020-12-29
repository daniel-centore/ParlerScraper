package com.danielcentore.scraper.parler;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.util.Pair;

import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.ParlerTime;
import com.danielcentore.scraper.parler.api.ScrapeType;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerHashtag;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.api.components.ScrapedRange;
import com.danielcentore.scraper.parler.db.ScraperDb;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;

/**
 * Handles the primary logic behind scraping
 *
 * @author Daniel Centore
 */
public class ParlerScraping {

    private static final String TAB = Main.TAB;

    private static final RandomDataGenerator random = new RandomDataGenerator();

    private static final int UNENCOUNTERED_BIAS = 2;
    private static final int USERS_PER_HASHTAG = 5;

    private ScraperDb db;
    private ParlerClient client;
    private ParlerScraperGui gui;

    private ParlerTime startTime;
    private ParlerTime endTime;

    private volatile boolean stopRequested = false;

    public ParlerScraping(ScraperDb db, ParlerClient client, ParlerScraperGui gui) {
        this.db = db;
        this.client = client;
        this.gui = gui;
    }

    public void scrape(ParlerTime startTime, ParlerTime endTime, List<String> seeds) {
        stopRequested = false;

        this.startTime = startTime;
        this.endTime = endTime;

        gui.println("Scraping from " + startTime.toSimpleDateTimeMsFormat() + " thru "
                + endTime.toSimpleDateTimeMsFormat());

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
                scrapeHashtag(seed.substring(1), true, "seed");
            } else {
                scrapeUsername(seed, true, "seed");
            }
        }

        gui.println("#########################");
        gui.println("### Scraping Randomly ###");
        gui.println("#########################");
        while (!stopRequested) {
            for (int i = 0; i < USERS_PER_HASHTAG; ++i) {
                ParlerUser user = getWeightedRandomUser();
                scrapeUser(user, false);
            }
            
            ParlerHashtag hashtag = getWeightedRandomHashtag();
            String htDebug = String.format("encounters=%,d", hashtag.getEncounters());
            if (hashtag.getTotalPosts() != null) {
                htDebug += String.format("; posts=%d", hashtag.getTotalPosts());
            }
            scrapeHashtag(hashtag.getHashtag(), false, htDebug);
        }

        //        while (!stopRequested) {
        //            //            System.out.println(getRandomTime(ScrapeType.HASHTAG_POSTS, "parler").toSimpleFormat());
        //            scrapeHashtag("parler", false, "");
        //            //
        //            //            scrapeUsername("SeanHannity", false, "");
        //            PUtils.sleep(5000);
        //        }
    }

    private void scrapeHashtag(String hashtag, boolean skipIfExists, String debug) {
        hashtag = hashtag.toLowerCase();

        gui.println("Scraping #" + hashtag + " (" + debug + ")");

        if (skipIfExists) {
            ParlerHashtag parlerHashtag = db.getParlerHashtag(hashtag);
            if (parlerHashtag != null && parlerHashtag.getTotalPosts() != null) {
                gui.println(TAB + "Already exists in local DB; skipping");
                return;
            }
        }

        ParlerTime randomTime = getRandomTime(ScrapeType.HASHTAG_POSTS, hashtag, this.startTime, this.endTime);
        gui.println(TAB + "Fetching posts from API [" + randomTime.toSimpleDateTimeFormat() + "]...");
        PagedParlerPosts hashtagPosts = client.fetchPagedHashtag(hashtag, randomTime);

        if (hashtagPosts != null) {
            int postCount = hashtagPosts.getPostCount();
            gui.println(TAB + "Storing " + postCount + " posts in local DB...");
            db.storePagedPosts(hashtagPosts);
            db.storeHashtagTotalPostCount(hashtag, hashtagPosts.getTotalPosts());
        } else {
            gui.println(TAB + "Failure - Blacklisting this query and earlier");
        }
        db.storeScrapedRange(ScrapeType.HASHTAG_POSTS, hashtag, randomTime, hashtagPosts);

        gui.println(TAB + "Done.");
    }

    private void scrapeUser(ParlerUser user, boolean skipIfExists) {
        scrapeUsername(user.getUsername(), skipIfExists, String.format("score=%,d", user.getScore()));
    }

    private void scrapeUsername(String username, boolean skipIfExists, String debugInfo) {
        gui.println("Scraping @" + username + " (" + debugInfo + ")");

        if (skipIfExists) {
            ParlerUser profile = db.getParlerUserByUsername(username);
            if (profile != null && profile.isFullyScanned()) {
                gui.println(TAB + "Already exists in local DB; skipping");
                return;
            }
        }

        if (stopRequested) {
            return;
        }

        gui.println(TAB + "Fetching profile from API...");
        ParlerUser profile = client.fetchProfile(username);

        gui.println(TAB + "Storing profile in local DB...");
        db.storeUser(profile);

        String userId = profile.getParlerId();

        if (stopRequested) {
            return;
        }

        ParlerTime start = this.startTime;
        ParlerTime end = this.endTime;

        // Move start to the user's joined time if they joined after the start
        ParlerTime joinedTime = profile.getJoinerParlerTime();
        if (joinedTime != null && start.compareTo(joinedTime) < 0) {
            start = joinedTime;
        }
        
        // Use the user's joined time for followers and following
        ParlerTime earliest = joinedTime != null ? joinedTime : startTime;

        if (profile.getPosts() != null && profile.getPosts() == 0) {
            gui.println(TAB + "User has 0 posts; skipping post API call");
        } else {
            ParlerTime randomTime = getRandomTime(ScrapeType.USER_POSTS, userId, start, end);
            gui.println(TAB + "Fetching posts from API [" + randomTime.toSimpleDateTimeFormat() + "]...");
            PagedParlerPosts pagedPosts = client.fetchPagedPosts(profile, randomTime);
            if (pagedPosts != null) {
                int postCount = pagedPosts.getPostCount();
                gui.println(TAB + "Storing " + postCount + " posts in local DB...");
                db.storePagedPosts(pagedPosts);
            } else {
                gui.println(TAB + "Failure - Blacklisting this query and earlier");
            }
            db.storeScrapedRange(ScrapeType.USER_POSTS, userId, randomTime, pagedPosts);
        }

        if (stopRequested) {
            return;
        }

        if (profile.getFollowing() != null && profile.getFollowing() == 0) {
            gui.println(TAB + "User is following 0 people; skipping followee API call");
        } else {
            ParlerTime randomTime = getRandomTime(ScrapeType.USER_FOLLOWEES, userId, earliest, end);
            gui.println(TAB + "Fetching followees from API [" + randomTime.toSimpleDateTimeFormat() + "]...");
            PagedParlerUsers pagedFollowing = client.fetchFollowers(profile, randomTime);
            if (pagedFollowing != null) {
                int followingCount = pagedFollowing.getUsers().size();
                gui.println(TAB + "Storing " + followingCount + " followees in local DB...");
                db.storePagedUsers(pagedFollowing);
            } else {
                gui.println(TAB + "Failure - Blacklisting this query and earlier");
            }
            db.storeScrapedRange(ScrapeType.USER_FOLLOWEES, userId, randomTime, pagedFollowing);
        }

        if (stopRequested) {
            return;
        }

        if (profile.getFollowers() != null && profile.getFollowers() == 0) {
            gui.println(TAB + "User has 0 followers; skipping followers API call");
        } else {
            ParlerTime randomTime = getRandomTime(ScrapeType.USER_FOLLOWERS, userId, earliest, end);
            gui.println(TAB + "Fetching followers from API [" + randomTime.toSimpleDateTimeFormat() + "]...");
            PagedParlerUsers pagedFollowers = client.fetchFollowers(profile, randomTime);
            if (pagedFollowers != null) {
                int followersCount = pagedFollowers.getUsers().size();
                gui.println(TAB + "Storing " + followersCount + " followers in local DB...");
                db.storePagedUsers(pagedFollowers);
            } else {
                gui.println(TAB + "Failure - Blacklisting this query and earlier");
            }
            db.storeScrapedRange(ScrapeType.USER_FOLLOWERS, userId, randomTime, pagedFollowers);
        }

        gui.println(TAB + "Done.");
    }

    private ParlerUser getWeightedRandomUser() {
        List<ParlerUser> allNotWorthlessUsers = db.getAllPublicNotWorthlessUsers();
        List<Pair<ParlerUser, Double>> userWeights = allNotWorthlessUsers.stream()
                .map(i -> new Pair<ParlerUser, Double>(i, weighUser(i)))
                .collect(Collectors.toList());

        return new EnumeratedDistribution<>(userWeights).sample();
    }

    private double weighUser(ParlerUser i) {
        return Math.log(i.getScore());
    }

    // TODO: Random user selection should only select users who existed before the endDate

    /**
     * Gets a random time for this particular query which is not within a range which has already been scraped (or
     * attempted to be scraped and failed)
     * 
     * @param scrapeType
     * @param id
     * @return
     */
    private ParlerTime getRandomTime(ScrapeType scrapeType, String id, ParlerTime start, ParlerTime end) {
        List<ScrapedRange> allRanges = db.getAllRanges(scrapeType, id);
        // Include range of everything up until the earliest time
        allRanges.add(new ScrapedRange(scrapeType, id, ParlerTime.fromUnixTimestampMs(0L), start, null, null));
        List<TimeInterval> ranges = PUtils.mergeScrapedRanges(allRanges);

        long maxMs = end.toUnixTimeMs();

        long maxRandom = maxMs;
        for (TimeInterval ti : ranges) {
            if (ti.min > maxMs) {
                break;
            }

            maxRandom -= (Math.min(ti.max, maxMs) - ti.min);
        }

        long value = random.nextLong(0, maxRandom);

        for (TimeInterval ti : ranges) {
            if (value > ti.min) {
                value += (ti.max - ti.min);
            }
        }

        return ParlerTime.fromUnixTimestampMs(value);
    }

    private ParlerHashtag getWeightedRandomHashtag() {
        List<ParlerHashtag> allHashtags = db.getAllHashtags();

        SimpleRegression encountersToPosts = new SimpleRegression();
        int nonNull = 0;
        for (ParlerHashtag ht : allHashtags) {
            if (ht.getTotalPosts() != null) {
                nonNull++;
                encountersToPosts.addData(ht.getEncounters(), ht.getTotalPosts());
            }
        }

        final int nonNullF = nonNull;
        List<Pair<ParlerHashtag, Double>> hashtagWeights = allHashtags.stream()
                .map(ht -> new Pair<ParlerHashtag, Double>(ht, weighHashtag(ht, encountersToPosts, nonNullF)))
                .filter(htp -> htp.getSecond() > Double.NEGATIVE_INFINITY)
                .collect(Collectors.toList());
        return new EnumeratedDistribution<>(hashtagWeights).sample();
    }

    private double weighHashtag(ParlerHashtag hashtag, SimpleRegression encountersToPosts, int nonNull) {
        Long totalPosts = hashtag.getTotalPosts();
        if (totalPosts == null) {
            long encounters = hashtag.getEncounters();
            if (nonNull > 1) {
                totalPosts = (long) encountersToPosts.predict(encounters) * UNENCOUNTERED_BIAS;
            } else {
                totalPosts = encounters * UNENCOUNTERED_BIAS;
            }
        }
        return totalPosts <= 0 ? Double.NEGATIVE_INFINITY : Math.log(totalPosts);
    }

    public void stop() {
        this.stopRequested = true;
    }
}
