package com.danielcentore.scraper.parler.api.components;

import java.util.List;

import com.danielcentore.scraper.parler.PUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.okhttp.internal.Util;


/**
 * Used for deserializing a paged response containing posts. This can come from, e.g., a hashtag search or a profile
 * query.
 *
 * @author Daniel Centore
 */
public class PagedParlerPosts extends PagedParlerResponse {

    @JsonProperty("badge")
    Integer badge;

    @JsonProperty("badgeString")
    String badgeString;

    @JsonProperty("posts")
    List<ParlerPost> posts;

    // These are posts referred to from the actual posts
    @JsonProperty("postRefs")
    List<ParlerPost> postReferences;

    @JsonProperty("urls")
    List<ParlerLink> urls;

    @JsonProperty("users")
    List<ParlerUser> users;

    // == Weird Parler Numbers (e.g. "2.3m") == //

    Long pendingFollowers;

    // We get this for search results but not from users
    Long totalPosts;

    public PagedParlerPosts(@JsonProperty("pendingFollowers") String pendingFollowers,
            @JsonProperty("totalPosts") String totalPosts) {
        this.pendingFollowers = PUtils.deparlify(pendingFollowers);
        this.totalPosts = PUtils.deparlify(totalPosts);
    }

    public Integer getBadge() {
        return badge;
    }

    public void setBadge(Integer badge) {
        this.badge = badge;
    }

    public String getBadgeString() {
        return badgeString;
    }

    public void setBadgeString(String badgeString) {
        this.badgeString = badgeString;
    }

    public List<ParlerPost> getPosts() {
        return posts;
    }

    public void setPosts(List<ParlerPost> posts) {
        this.posts = posts;
    }

    public List<ParlerPost> getPostReferences() {
        return postReferences;
    }

    public void setPostReferences(List<ParlerPost> postReferences) {
        this.postReferences = postReferences;
    }

    public List<ParlerLink> getUrls() {
        return urls;
    }

    public void setUrls(List<ParlerLink> urls) {
        this.urls = urls;
    }

    public List<ParlerUser> getUsers() {
        return users;
    }

    public void setUsers(List<ParlerUser> users) {
        this.users = users;
    }

    public Long getPendingFollowers() {
        return pendingFollowers;
    }

    public void setPendingFollowers(Long pendingFollowers) {
        this.pendingFollowers = pendingFollowers;
    }

    public Long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Long totalPosts) {
        this.totalPosts = totalPosts;
    }
}
