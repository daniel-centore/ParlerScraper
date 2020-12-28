package com.danielcentore.scraper.parler.api.components;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PagedParlerUsers extends PagedParlerResponse {

    List<ParlerUser> users;

    public PagedParlerUsers(
            @JsonProperty("followers") List<ParlerUser> followers,
            @JsonProperty("followees") List<ParlerUser> followees) {
        if (followers != null) {
            users = followers;
        } else {
            users = followees;
        }
    }

    @Override
    public String toString() {
        return "PagedParlerUsers [users=" + users + ", last=" + last + ", prev=" + prev + ", next=" + next + "]";
    }

    public List<ParlerUser> getUsers() {
        return users;
    }

    public void setUsers(List<ParlerUser> users) {
        this.users = users;
    }

}
