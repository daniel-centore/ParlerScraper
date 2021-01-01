package com.danielcentore.scraper.parler.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.danielcentore.scraper.parler.Main;
import com.danielcentore.scraper.parler.PUtils;
import com.danielcentore.scraper.parler.api.ParlerTime;
import com.danielcentore.scraper.parler.api.ScrapeType;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerResponse;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerHashtag;
import com.danielcentore.scraper.parler.api.components.ParlerLink;
import com.danielcentore.scraper.parler.api.components.ParlerPost;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.api.components.ScrapedRange;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;

/**
 * Stores and retrieves data from the local sqlite database
 *
 * @author Daniel Centore
 */
public class ScraperDb {

    public static final String TAB = Main.TAB;

    public static final int DB_FAIL_TIME_MS = 1000;

    private Session session;
    private ParlerScraperGui gui;

    private ParlerTime startTime;
    private ParlerTime endTime;

    public ScraperDb(ParlerScraperGui gui) {
        this.gui = gui;

        // Adjusting logging level
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);
        System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
        System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "WARNING");

        SessionFactory sessionFactory = new Configuration()
                .configure(new File("./hibernate.cfg.xml"))
                .buildSessionFactory();

        session = sessionFactory.openSession();
    }

    public void updateStartEndTime(ParlerTime startTime, ParlerTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        updateStatusArea();
    }

    public void storePagedPosts(PagedParlerPosts pagedPosts) {
        storePosts(pagedPosts.getPosts());
        storePosts(pagedPosts.getPostReferences());

        storeLinks(pagedPosts.getUrls());

        storeUsers(pagedPosts.getUsers());
    }

    private void storeLinks(Collection<ParlerLink> links) {
        if (links == null) {
            return;
        }

        // Make sure there's only one of each
        links = new HashSet<ParlerLink>(links);

        List<String> parlerIds = links.stream().map(post -> post.getLinkId()).collect(Collectors.toList());

        MultiIdentifierLoadAccess<ParlerLink> multiLoadAccess = session.byMultipleIds(ParlerLink.class);
        List<ParlerLink> existingLinksList = multiLoadAccess.multiLoad(parlerIds);
        HashMap<String, ParlerLink> existingLinks = new HashMap<>();
        for (ParlerLink link : existingLinksList) {
            if (link != null) {
                existingLinks.put(link.getLinkId(), link);
            }
        }

        while (true) {
            try {
                beginTransaction();
                for (ParlerLink link : links) {
                    if (existingLinks.containsKey(link.getLinkId())) {
                        session.detach(existingLinks.get(link.getLinkId()));
                    }
                    session.saveOrUpdate(link);
                }
                endTransaction();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                gui.println("> Local DB transaction failed, retrying...");
                PUtils.sleep(DB_FAIL_TIME_MS);
            }
        }
    }

    public void storePagedUsers(PagedParlerUsers pagedUsers) {
        storeUsers(pagedUsers.getUsers());
    }

    public void storePosts(Collection<ParlerPost> posts) {
        if (posts == null) {
            return;
        }

        storeHashtags(posts);

        // Make sure there's only one of each
        posts = new HashSet<>(posts);

        List<String> parlerIds = posts.stream().map(post -> post.getParlerId()).collect(Collectors.toList());

        MultiIdentifierLoadAccess<ParlerPost> multiLoadAccess = session.byMultipleIds(ParlerPost.class);
        List<ParlerPost> existingPostsList = multiLoadAccess.multiLoad(parlerIds);
        HashMap<String, ParlerPost> existingPosts = new HashMap<>();
        for (ParlerPost post : existingPostsList) {
            if (post != null) {
                existingPosts.put(post.getParlerId(), post);
            }
        }

        while (true) {
            try {
                beginTransaction();
                for (ParlerPost post : posts) {
                    if (existingPosts.containsKey(post.getParlerId())) {
                        session.detach(existingPosts.get(post.getParlerId()));
                    }
                    session.saveOrUpdate(post);
                }
                endTransaction();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                gui.println("> Local DB transaction failed, retrying...");
                PUtils.sleep(DB_FAIL_TIME_MS);
            }
        }
    }

    public void storeHashtags(Collection<ParlerPost> posts) {
        if (posts == null) {
            return;
        }
        // Make sure there's only one of each
        posts = new HashSet<ParlerPost>(posts);

        HashMap<String, Integer> hashTagsAdded = new HashMap<>();

        for (ParlerPost post : posts) {
            if (post == null) {
                continue;
            }
            for (String ht : post.getHashtags()) {
                ht = ht.toLowerCase();
                if (!hashTagsAdded.containsKey(ht)) {
                    hashTagsAdded.put(ht, 0);
                }
                hashTagsAdded.put(ht, hashTagsAdded.get(ht) + 1);
            }
        }

        MultiIdentifierLoadAccess<ParlerHashtag> multiLoadAccess = session.byMultipleIds(ParlerHashtag.class);
        List<ParlerHashtag> existingHashtagsList = multiLoadAccess.multiLoad(new ArrayList<>(hashTagsAdded.keySet()));
        HashMap<String, ParlerHashtag> existingHashtags = new HashMap<>();
        for (ParlerHashtag pht : existingHashtagsList) {
            if (pht == null) {
                continue;
            }
            existingHashtags.put(pht.getHashtag(), pht);
        }

        while (true) {
            try {
                beginTransaction();
                for (String hashtag : hashTagsAdded.keySet()) {
                    ParlerHashtag parlerHashtag = existingHashtags.containsKey(hashtag)
                            ? existingHashtags.get(hashtag)
                            : new ParlerHashtag(hashtag);
                    parlerHashtag.setEncounters(parlerHashtag.getEncounters() + hashTagsAdded.get(hashtag));
                    session.saveOrUpdate(parlerHashtag);
                }
                endTransaction();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                gui.println("> Local DB transaction failed, retrying...");
                PUtils.sleep(DB_FAIL_TIME_MS);
            }
        }
    }

    public void storeHashtagTotalPostCount(String hashtag, Long totalPosts) {
        ParlerHashtag parlerHashtag = getParlerHashtag(hashtag);
        if (parlerHashtag == null) {
            parlerHashtag = new ParlerHashtag(hashtag);
        }
        parlerHashtag.setTotalPosts(totalPosts);

        while (true) {
            try {
                beginTransaction();
                session.saveOrUpdate(parlerHashtag);
                endTransaction();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                gui.println("> Local DB transaction failed, retrying...");
                PUtils.sleep(DB_FAIL_TIME_MS);
            }
        }
    }

    public ParlerHashtag getParlerHashtag(String hashtag) {
        return session.byId(ParlerHashtag.class).load(hashtag.toLowerCase());
    }

    public ParlerUser getParlerUserByUsername(String username) {
        @SuppressWarnings("unchecked")
        List<ParlerUser> result = session.createQuery("FROM ParlerUser U WHERE U.username = :username")
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    public void storeUser(ParlerUser user) {
        if (user == null) {
            return;
        }

        List<ParlerUser> input = new ArrayList<ParlerUser>();
        input.add(user);
        storeUsers(input);
    }

    public void storeUsers(Collection<ParlerUser> users) {
        if (users == null) {
            return;
        }

        // Make sure there's only one of each
        users = new HashSet<ParlerUser>(users);

        List<String> parlerIds = users.stream().map(user -> user.getParlerId()).collect(Collectors.toList());

        MultiIdentifierLoadAccess<ParlerUser> multiLoadAccess = session.byMultipleIds(ParlerUser.class);
        List<ParlerUser> existingUsersList = multiLoadAccess.multiLoad(parlerIds);
        HashMap<String, ParlerUser> existingUsers = new HashMap<>();
        for (ParlerUser user : existingUsersList) {
            if (user != null) {
                existingUsers.put(user.getParlerId(), user);
            }
        }

        ParlerUser currentUser = null;
        while (true) {
            try {
                beginTransaction();
                for (ParlerUser user : users) {
                    currentUser = user;
                    ParlerUser existingUser = existingUsers.get(user.getParlerId());

                    if (existingUser == null) {
                        // User not yet in db
                        session.save(user);
                    } else {
                        session.detach(existingUser);
                        if (!existingUser.isFullyScanned()) {
                            // User in db but is not fully scanned, so update it
                            session.update(user);
                        } else if (existingUser.isFullyScanned() && user.isFullyScanned()) {
                            // User in db and fully scanned, but so is the new one, so update it
                            session.update(user);
                        }
                    }
                }
                currentUser = null;
                endTransaction();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                gui.println("> Local DB transaction failed, retrying...");
                if (currentUser != null) {
                    gui.println("> Failed user was: " + currentUser.getUsername() + " " + currentUser.getParlerId());
                }
                PUtils.sleep(DB_FAIL_TIME_MS);
            }
        }
    }

    public void storeScrapedRange(ScrapeType scrapedType,
            String scrapedId,
            ParlerTime startTime,
            PagedParlerResponse response) {
        if (response != null && response.getLast()) {
            gui.println("> Last pagination - marking as complete thru 1970");
        }
        while (true) {
            try {
                beginTransaction();
                session.save(new ScrapedRange(
                        scrapedType,
                        scrapedId,
                        startTime,
                        // If the scrape failed or is the last, mark everything as occupied from start of time to the request
                        (response == null || response.getLast())
                                ? ParlerTime.fromUnixTimestampMs(0L)
                                : response.getNextKey(),
                        response != null,
                        ParlerTime.now().toParlerTimestamp()));
                endTransaction();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                gui.println("> Local DB transaction failed, retrying...");
                PUtils.sleep(DB_FAIL_TIME_MS);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<ParlerUser> getAllPublicNotWorthlessUsers(long minPosts) {
        return session
                .createQuery(
                        "FROM ParlerUser U WHERE U.privateAccount = 0 AND (U.posts is null OR U.posts > :minPosts)")
                .setParameter("minPosts", minPosts)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<ParlerHashtag> getAllHashtags() {
        return session.createQuery("FROM ParlerHashtag").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<ScrapedRange> getAllRanges(ScrapeType scrapeType, String id) {
        return session.createQuery("FROM ScrapedRange R WHERE R.scrapedId = :id AND R.scrapedType = :scrapedType")
                .setParameter("id", id)
                .setParameter("scrapedType", scrapeType)
                .getResultList();
    }

    public void beginTransaction() {
        try {
            session.beginTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            session.getTransaction().rollback();
        }
    }

    public void endTransaction() {
        session.getTransaction().commit();
        updateStatusArea();
    }

    public void updateStatusArea() {
        long totalUsers = (long) session.createQuery("SELECT count(*) FROM ParlerUser")
                .getSingleResult();
        long totalPosts = (long) session.createQuery("SELECT count(*) FROM ParlerPost")
                .getSingleResult();
        long totalHashtags = (long) session.createQuery("SELECT count(*) FROM ParlerHashtag")
                .getSingleResult();
        long uniquePostCreators = (long) session.createQuery("SELECT count(DISTINCT creator_id) FROM ParlerPost")
                .getSingleResult();

        String inDateRange = "[Press Start to Update]";
        if (this.startTime != null && this.endTime != null) {
            long totalPostsInDateRange = (long) session
                    .createQuery("SELECT count(*) FROM ParlerPost P WHERE P.createdAt > :min AND P.createdAt < :max")
                    .setParameter("min", this.startTime.toParlerCompressedTimestamp())
                    .setParameter("max", this.endTime.toParlerCompressedTimestamp())
                    .getSingleResult();
            inDateRange = String.format("%,d", totalPostsInDateRange);
        }

        String text = String.format(
                "Total Posts: %,d\n"
                        + TAB + "Distinct post creators: %,d\n"
                        + "Total Users: %,d\n"
                        + "Total Hashtags: %,d\n"
                        + "\n"
                        + "Posts (%s thru %s): %s\n",
                totalPosts,
                uniquePostCreators,
                totalUsers,
                totalHashtags,
                startTime == null ? "n/a" : startTime.toSimpleDateFormat(),
                endTime == null ? "n/a" : endTime.toSimpleDateFormat(),
                inDateRange);

        gui.setStatusArea(text);
    }

}
