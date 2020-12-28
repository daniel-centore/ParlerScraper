package com.danielcentore.scraper.parler.db;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerLink;
import com.danielcentore.scraper.parler.api.components.ParlerPost;
import com.danielcentore.scraper.parler.api.components.ParlerUser;

public class ScraperDb {

    private Session session;

    public ScraperDb() {
        SessionFactory sessionFactory = new Configuration()
                .configure(new File("./hibernate.cfg.xml"))
                .buildSessionFactory();

        session = sessionFactory.openSession();

        // Switch to Write-Ahead Logging mode (allows concurrent db access?)
        //	session.beginTransaction();
        //	session.createSQLQuery("PRAGMA journal_mode=WAL;");
        //	session.getTransaction().commit();
    }

    public void storePagedPosts(PagedParlerPosts pagedPosts) {
        storePosts(pagedPosts.getPosts());
        storePosts(pagedPosts.getPostReferences());

        storeLinks(pagedPosts.getUrls());

        storeUsers(pagedPosts.getUsers());
    }

    private void storeLinks(List<ParlerLink> links) {
        if (links == null) {
            return;
        }

        List<String> parlerIds = links.stream().map(post -> post.getLinkId()).collect(Collectors.toList());

        MultiIdentifierLoadAccess<ParlerLink> multiLoadAccess = session.byMultipleIds(ParlerLink.class);
        List<ParlerLink> existingLinksList = multiLoadAccess.multiLoad(parlerIds);
        HashMap<String, ParlerLink> existingLinks = new HashMap<>();
        for (ParlerLink link : existingLinksList) {
            if (link != null) {
                existingLinks.put(link.getLinkId(), link);
            }
        }

        session.beginTransaction();
        for (ParlerLink link : links) {
            if (existingLinks.containsKey(link.getLinkId())) {
                session.detach(existingLinks.get(link.getLinkId()));
            }
            session.saveOrUpdate(link);
        }
        session.getTransaction().commit();
    }

    public void storePagedUsers(PagedParlerUsers pagedUsers) {
        storeUsers(pagedUsers.getUsers());
    }

    public void storePosts(List<ParlerPost> posts) {
        if (posts == null) {
            return;
        }

        storeHashtags(posts);

        List<String> parlerIds = posts.stream().map(post -> post.getParlerId()).collect(Collectors.toList());

        MultiIdentifierLoadAccess<ParlerPost> multiLoadAccess = session.byMultipleIds(ParlerPost.class);
        List<ParlerPost> existingPostsList = multiLoadAccess.multiLoad(parlerIds);
        HashMap<String, ParlerPost> existingPosts = new HashMap<>();
        for (ParlerPost post : existingPostsList) {
            if (post != null) {
                existingPosts.put(post.getParlerId(), post);
            }
        }

        session.beginTransaction();
        for (ParlerPost post : posts) {
            if (existingPosts.containsKey(post.getParlerId())) {
                session.detach(existingPosts.get(post.getParlerId()));
            }
            session.saveOrUpdate(post);
        }
        session.getTransaction().commit();
    }

    public void storeHashtags(List<ParlerPost> posts) {
        if (posts == null) {
            return;
        }

        // TODO
    }

    public void storeUser(ParlerUser user) {
        List<ParlerUser> input = new ArrayList<ParlerUser>();
        input.add(user);
        storeUsers(input);
    }

    public void storeUsers(List<ParlerUser> users) {
        List<String> parlerIds = users.stream().map(user -> user.getParlerId()).collect(Collectors.toList());

        MultiIdentifierLoadAccess<ParlerUser> multiLoadAccess = session.byMultipleIds(ParlerUser.class);
        List<ParlerUser> existingUsersList = multiLoadAccess.multiLoad(parlerIds);
        HashMap<String, ParlerUser> existingUsers = new HashMap<>();
        for (ParlerUser user : existingUsersList) {
            if (user != null) {
                existingUsers.put(user.getParlerId(), user);
            }
        }

        session.beginTransaction();
        for (ParlerUser user : users) {
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
        session.getTransaction().commit();
    }

    @SuppressWarnings("unchecked")
    public List<ParlerUser> getAllNotWorthlessUsers() {
        return session.createQuery("FROM ParlerUser U WHERE U.score > 0").getResultList();
    }

}
