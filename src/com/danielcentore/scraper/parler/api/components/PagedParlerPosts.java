package com.danielcentore.scraper.parler.api.components;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("pendingFollowers")
    String pendingFollowers;

    // We get this for search results but not from users
    @JsonProperty("totalPosts")
    String totalPosts;

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

    public String getPendingFollowers() {
        return pendingFollowers;
    }

    public void setPendingFollowers(String pendingFollowers) {
        this.pendingFollowers = pendingFollowers;
    }

    public String getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(String totalPosts) {
        this.totalPosts = totalPosts;
    }
}
