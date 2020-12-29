package com.danielcentore.scraper.parler.api.components;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.danielcentore.scraper.parler.PUtils;
import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.ParlerTime;
import com.danielcentore.scraper.parler.db.ListToJsonConverter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Parler User.
 * 
 * Can be converted from Parler API JSON and to/from the database.
 * 
 * Many fields are not always set.
 *
 * @author Daniel Centore
 */
@Entity
@Table(name = "users")
public class ParlerUser extends ParlerResponse {

    // == Parler Raw Fields == //
    String parlerId;

    @JsonProperty("accountColor")
    String accountColor;

    @JsonProperty("bio")
    String bio;

    @JsonProperty("blocked")
    Boolean blocked;

    @JsonProperty("coverPhoto")
    String coverPhoto;

    @JsonProperty("followed")
    Boolean followed;

    @JsonProperty("human")
    Boolean human;

    @JsonProperty("integration")
    Boolean integration;

    @JsonProperty("joined")
    String joined;

    @JsonProperty("name")
    String name;

    @JsonProperty("muted")
    Boolean muted;

    @JsonProperty("pendingFollow")
    Boolean pendingFollow;

    @JsonProperty("private")
    Boolean privateAccount;

    @JsonProperty("profilePhoto")
    String profilePhoto;

    @JsonProperty("rss")
    Boolean rss;

    @JsonProperty("username")
    String username;

    @JsonProperty("verified")
    Boolean verified;

    @JsonProperty("verifiedComments")
    Boolean verifiedComments;

    @JsonProperty("badges")
    List<Long> badges;

    @JsonProperty("interactions")
    Integer interactions;

    @JsonProperty("state")
    Integer state;

    @JsonProperty("banned")
    Boolean banned;

    @JsonProperty("isFollowingYou")
    Boolean isFollowingYou;

    // == Weird Parler Numbers (e.g. 2.3m) == //

    @JsonProperty("score")
    Long score;

    @JsonProperty("comments")
    Long comments;

    @JsonProperty("followers")
    Long followers;

    @JsonProperty("following")
    Long following;

    @JsonProperty("likes")
    Long likes;

    @JsonProperty("posts")
    Long posts;

    @JsonProperty("media")
    Long media;

    // == Scraper Fields ==
    boolean fullyScanned;

    public ParlerUser(@JsonProperty("id") String idA,
            @JsonProperty("_id") String idB,
            @JsonProperty("score") String score,
            @JsonProperty("comments") String comments,
            @JsonProperty("followers") String followers,
            @JsonProperty("following") String following,
            @JsonProperty("likes") String likes,
            @JsonProperty("posts") String posts,
            @JsonProperty("media") String media) {
        this.parlerId = (idA != null && !idA.isEmpty()) ? idA : idB;
        this.score = PUtils.deparlify(score);
        this.comments = PUtils.deparlify(comments);
        this.followers = PUtils.deparlify(followers);
        this.following = PUtils.deparlify(following);
        this.likes = PUtils.deparlify(likes);
        this.posts = PUtils.deparlify(posts);
        this.media = PUtils.deparlify(media);
        this.fullyScanned = false;
    }

    public ParlerUser() {
        // Needed for Hibernate to load
    }

    public ParlerUser setFullyScanned() {
        this.fullyScanned = true;
        return this;
    }

    @Transient
    public String getUrlEncodedParlerId() {
        return ParlerClient.urlencode(this.parlerId);
    }

    @Transient
    public String getUrlEncodedUsername() {
        return ParlerClient.urlencode(this.username);
    }

    @Id
    @Column(name = "id")
    public String getParlerId() {
        return parlerId;
    }

    public void setParlerId(String parlerId) {
        this.parlerId = parlerId;
    }

    @Column(name = "account_color")
    public String getAccountColor() {
        return accountColor;
    }

    public void setAccountColor(String accountColor) {
        this.accountColor = accountColor;
    }

    @Column(name = "bio")
    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    @Column(name = "blocked")
    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    @Column(name = "cover_photo")
    public String getCoverPhoto() {
        return coverPhoto;
    }

    public void setCoverPhoto(String coverPhoto) {
        this.coverPhoto = coverPhoto;
    }

    @Column(name = "followed")
    public Boolean getFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }

    @Column(name = "human")
    public Boolean getHuman() {
        return human;
    }

    public void setHuman(Boolean human) {
        this.human = human;
    }

    @Column(name = "integration")
    public Boolean getIntegration() {
        return integration;
    }

    public void setIntegration(Boolean integration) {
        this.integration = integration;
    }

    @Column(name = "joined")
    public String getJoined() {
        return joined;
    }

    public void setJoined(String joined) {
        this.joined = joined;
    }
    
    @Transient
    public ParlerTime getJoinerParlerTime() {
        return getJoined() == null ? null : ParlerTime.fromCompressedParlerTimestamp(getJoined());
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "muted")
    public Boolean getMuted() {
        return muted;
    }

    public void setMuted(Boolean muted) {
        this.muted = muted;
    }

    @Column(name = "pending_follow")
    public Boolean getPendingFollow() {
        return pendingFollow;
    }

    public void setPendingFollow(Boolean pendingFollow) {
        this.pendingFollow = pendingFollow;
    }

    @Column(name = "private_account")
    public Boolean getPrivateAccount() {
        return privateAccount;
    }

    public void setPrivateAccount(Boolean privateAccount) {
        this.privateAccount = privateAccount;
    }

    @Column(name = "profile_photo")
    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    @Column(name = "rss")
    public Boolean getRss() {
        return rss;
    }

    public void setRss(Boolean rss) {
        this.rss = rss;
    }

    @Column(name = "username", unique = true)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column(name = "verified")
    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    @Column(name = "verified_comments")
    public Boolean getVerifiedComments() {
        return verifiedComments;
    }

    public void setVerifiedComments(Boolean verifiedComments) {
        this.verifiedComments = verifiedComments;
    }

    @Column(name = "badges")
    @Convert(converter = ListToJsonConverter.class)
    public List<Long> getBadges() {
        return badges;
    }

    public void setBadges(List<Long> badges) {
        this.badges = badges;
    }

    @Column(name = "interactions")
    public Integer getInteractions() {
        return interactions;
    }

    public void setInteractions(Integer interactions) {
        this.interactions = interactions;
    }

    @Column(name = "state")
    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    @Column(name = "banned")
    public Boolean getBanned() {
        return banned;
    }

    public void setBanned(Boolean banned) {
        this.banned = banned;
    }

    @Column(name = "is_following_you")
    public Boolean getIsFollowingYou() {
        return isFollowingYou;
    }

    public void setIsFollowingYou(Boolean isFollowingYou) {
        this.isFollowingYou = isFollowingYou;
    }

    @Column(name = "score")
    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    @Column(name = "comments")
    public Long getComments() {
        return comments;
    }

    public void setComments(Long comments) {
        this.comments = comments;
    }

    @Column(name = "followers")
    public Long getFollowers() {
        return followers;
    }

    public void setFollowers(Long followers) {
        this.followers = followers;
    }

    @Column(name = "following")
    public Long getFollowing() {
        return following;
    }

    public void setFollowing(Long following) {
        this.following = following;
    }

    @Column(name = "likes")
    public Long getLikes() {
        return likes;
    }

    public void setLikes(Long likes) {
        this.likes = likes;
    }

    @Column(name = "posts")
    public Long getPosts() {
        return posts;
    }

    public void setPosts(Long posts) {
        this.posts = posts;
    }

    @Column(name = "media")
    public Long getMedia() {
        return media;
    }

    public void setMedia(Long media) {
        this.media = media;
    }

    @Column(name = "fully_scanned")
    public boolean isFullyScanned() {
        return fullyScanned;
    }

    public void setFullyScanned(boolean fullyScanned) {
        this.fullyScanned = fullyScanned;
    }

    @Override
    public String toString() {
        return "ParlerUser [parlerId=" + parlerId + ", name=" + name + ", username=" + username + ", score=" + score
                + ", fullyScanned=" + fullyScanned + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parlerId == null) ? 0 : parlerId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ParlerUser other = (ParlerUser) obj;
        if (parlerId == null) {
            if (other.parlerId != null)
                return false;
        } else if (!parlerId.equals(other.parlerId))
            return false;
        return true;
    }

}
