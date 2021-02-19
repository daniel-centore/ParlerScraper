package com.danielcentore.scraper.parler;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
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

    // The earliest possible time (since Parler was restarted)
    //    private static final ParlerTime EARLIEST_TIME = ParlerTime.fromYyyyMmDd("2021-02-14");

    // Many users have a "I just joined Parler! Looking forward to meeting everyone here." post, let's get real posts
    public static final int MINIMUM_POSTS = 2;

    private ScraperDb db;
    private ParlerClient client;
    private ParlerScraperGui gui;

    private ParlerTime startTime;
    private ParlerTime endTime;
    private boolean now;

    private volatile boolean stopRequested = false;

    public ParlerScraping(ScraperDb db, ParlerClient client, ParlerScraperGui gui) {
        this.db = db;
        this.client = client;
        this.gui = gui;
    }

    public void scrape(ParlerTime startTime, ParlerTime endTime, List<String> seeds, int userRatio, int hashtagRatio,
            boolean now)
            throws InterruptedIOException {
        stopRequested = false;

        this.startTime = startTime;
        this.endTime = endTime;
        this.now = now;

        gui.println("Scraping from " + this.startTime.toSimpleDateTimeMsFormat() + " thru "
                + (now ? "the current time" : this.endTime.toSimpleDateTimeMsFormat() + " UTC"));

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
            for (int i = 0; i < userRatio && !stopRequested; ++i) {
                maybeUpdateEndtime();

                UserResult user = getWeightedRandomUser();
                String debug = String.format("days=%.1f", user.score);
                if (user.user.getPosts() != null) {
                    debug += String.format("; posts=%,d", user.user.getPosts());
                }
                scrapeUser(user.user, false, debug);
            }

            if (stopRequested) {
                return;
            }

            for (int i = 0; i < hashtagRatio && !stopRequested; ++i) {
                maybeUpdateEndtime();

                ParlerHashtag hashtag = getWeightedRandomHashtag();
                String htDebug = String.format("encounters=%,d", hashtag.getEncounters());
                if (hashtag.getTotalPosts() != null) {
                    htDebug += String.format("; posts=%,d", hashtag.getTotalPosts());
                }
                scrapeHashtag(hashtag.getHashtag(), false, htDebug);
            }
        }
    }

    private void maybeUpdateEndtime() {
        if (now) {
            this.endTime = ParlerTime.now();
            gui.println("Updated endtime to " + this.endTime.toSimpleDateTimeMsFormat());
        }
    }

    private void scrapeHashtag(String hashtag, boolean skipIfExists, String debug) throws InterruptedIOException {
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

    /**
     * NOTE: If multiple users exist with the same username, this might update a different user with the same username
     * instead
     * 
     * @param user
     * @param skipIfExists
     * @param debug
     * @throws InterruptedIOException
     */
    private void scrapeUser(ParlerUser user, boolean skipIfExists, String debug) throws InterruptedIOException {
        scrapeUsername(user.getUsername(), skipIfExists, debug);
    }

    private void scrapeUsername(String username, boolean skipIfExists, String debugInfo) throws InterruptedIOException {
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

        if (profile == null) {
            gui.println(TAB + "Failed to fetch @" + username + "; skipping...");
            return;
        }

        gui.println(TAB + "Storing profile in local DB...");
        db.storeUser(profile);

        String userId = profile.getParlerId();

        if (stopRequested) {
            return;
        }

        ParlerTime start = this.startTime;
        ParlerTime end = this.endTime;

        // Move start to the user's joined time if they joined after the start
        ParlerTime joinedTime = profile.getJoinedParlerTime();
        if (joinedTime != null && start.compareTo(joinedTime) < 0) {
            start = joinedTime;
        }

        //        // No reason to scan before the Parler relaunch
        //        if (start.compareTo(EARLIEST_TIME) < 0) {
        //            start = EARLIEST_TIME;
        //        }

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

        // Likes seem to be not working in the Feb 2021 relaunch  
        //        if (profile.getLikes() != null && profile.getLikes() == 0) {
        //            gui.println(TAB + "User has liked 0 posts; skipping post likes API call");
        //        } else {
        //            ParlerTime randomTime = getRandomTime(ScrapeType.USER_LIKES, userId, start, end);
        //            gui.println(TAB + "Fetching liked posts from API [" + randomTime.toSimpleDateTimeFormat() + "]...");
        //            PagedParlerPosts pagedPosts = client.fetchPagedLikes(profile, randomTime);
        //            if (pagedPosts != null) {
        //                int postCount = pagedPosts.getPostCount();
        //                gui.println(TAB + "Storing " + postCount + " posts in local DB...");
        //                db.storePagedPosts(pagedPosts);
        //            } else {
        //                gui.println(TAB + "Failure - Blacklisting this query and earlier");
        //            }
        //            db.storeScrapedRange(ScrapeType.USER_LIKES, userId, randomTime, pagedPosts);
        //        }
        //        
        //        if (stopRequested) {
        //            return;
        //        }

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

        // Followers viewing seems to have been more or less disabled in the Feb 2021 relaunch  
        //        if (stopRequested) {
        //            return;
        //        }
        //
        //        if (profile.getFollowers() != null && profile.getFollowers() == 0) {
        //            gui.println(TAB + "User has 0 followers; skipping followers API call");
        //        } else {
        //            ParlerTime randomTime = getRandomTime(ScrapeType.USER_FOLLOWERS, userId, earliest, end);
        //            gui.println(TAB + "Fetching followers from API [" + randomTime.toSimpleDateTimeFormat() + "]...");
        //            PagedParlerUsers pagedFollowers = client.fetchFollowers(profile, randomTime);
        //            if (pagedFollowers != null) {
        //                int followersCount = pagedFollowers.getUsers().size();
        //                gui.println(TAB + "Storing " + followersCount + " followers in local DB...");
        //                db.storePagedUsers(pagedFollowers);
        //            } else {
        //                gui.println(TAB + "Failure - Blacklisting this query and earlier");
        //            }
        //            db.storeScrapedRange(ScrapeType.USER_FOLLOWERS, userId, randomTime, pagedFollowers);
        //        }

        gui.println(TAB + "Done.");
    }

    private UserResult getWeightedRandomUser() {
        List<ScrapedRange> allRanges = db.getAllRanges(ScrapeType.USER_POSTS);
        HashMap<String, List<ScrapedRange>> allRangesMap = new HashMap<>();
        for (ScrapedRange sr : allRanges) {
            if (!allRangesMap.containsKey(sr.getScrapedId())) {
                allRangesMap.put(sr.getScrapedId(), new ArrayList<ScrapedRange>());
            }
            allRangesMap.get(sr.getScrapedId()).add(sr);
        }

        List<ParlerUser> allNotWorthlessUsers = db.getAllPublicNotWorthlessUsers(MINIMUM_POSTS)
                .stream()
                // Only include users who existed before the end date
                .filter(user -> user.getJoinedParlerTime().compareTo(this.endTime) < 0)
                .collect(Collectors.toList());

        List<Pair<UserResult, Double>> userWeights = allNotWorthlessUsers.stream()
                .map(i -> new UserResult(i, weighUser(i, allRangesMap)))
                .map(i -> new Pair<UserResult, Double>(i, i.score))
                .collect(Collectors.toList());

        return new EnumeratedDistribution<>(userWeights).sample();
    }

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
        List<Pair<ParlerHashtag, Double>> hashtagWeights = allHashtags.parallelStream()
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

    /**
     * This currently weighs the user by computing the number of unscraped days they have existed within the start-end
     * bounds.
     * 
     * @param user
     * @param allUserRanges
     * @return
     */
    private double weighUser(ParlerUser user, HashMap<String, List<ScrapedRange>> allUserRanges) {
        ParlerTime start = this.startTime;
        ParlerTime end = this.endTime;

        // Move start to the user's joined time if they joined after the start
        ParlerTime joinedTime = user.getJoinedParlerTime();
        if (joinedTime != null && start.compareTo(joinedTime) < 0) {
            start = joinedTime;
        }

        String id = user.getParlerId();
        List<ScrapedRange> allRanges = allUserRanges.get(id);
        if (allRanges == null) {
            allRanges = new ArrayList<>();
        }
        // Include range of everything up until the earliest time
        allRanges.add(new ScrapedRange(ScrapeType.USER_POSTS, id, ParlerTime.fromUnixTimestampMs(0L),
                ParlerTime.fromUnixTimestampMs(0L), null, null));
        List<TimeInterval> ranges = PUtils.mergeScrapedRanges(allRanges);

        long min = start.toUnixTimeMs();
        long max = end.toUnixTimeMs();

        long weight = max - min;

        for (TimeInterval ti : ranges) {
            if (ti.max < min || ti.min > max) {
                continue;
            } else if (ti.min < min && ti.max > max) {
                weight = 0;
                break;
            } else if (ti.min < min) {
                weight -= (ti.max - min);
            } else if (ti.max > max) {
                weight -= (max - ti.min);
            } else {
                weight -= (ti.max - ti.min);
            }
        }

        return weight / (1000.0 * 60 * 60 * 24);
    }

    public void stop() {
        this.stopRequested = true;
    }
}

class UserResult {
    ParlerUser user;
    double score;

    UserResult(ParlerUser user, double score) {
        this.user = user;
        this.score = score;
    }
}
