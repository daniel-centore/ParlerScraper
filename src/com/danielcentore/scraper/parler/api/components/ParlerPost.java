package com.danielcentore.scraper.parler.api.components;

import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.danielcentore.scraper.parler.PUtils;
import com.danielcentore.scraper.parler.db.ListToJsonConverter;
import com.danielcentore.scraper.parler.db.MapToJsonConverter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Parler Post.
 * 
 * Can be converted from Parler API JSON and to/from the database .
 *
 * @author Daniel Centore
 */
@Entity
@Table(name = "posts")
public class ParlerPost {

    String parlerId;

    // Map of username to parlerId
    // e.g.:
    // "@": {"greggjarrett": "7536fd1d5c3547fa883fb19ea3cbf244"},
    @JsonProperty("@")
    Map<String, String> mentions;

    @JsonProperty("article")
    Boolean article;

    // e.g. "@greggjarrett - Ex-intel Officials Defend ‘Russian disinformation’
    // Assessment of Hunter Biden Report"
    @JsonProperty("body")
    String body;

    // e.g. "20201223150519"
    @JsonProperty("createdAt")
    String createdAt;

    // e.g. "4b8638eca2db4b17b3a7203e40ac8ce7"
    @JsonProperty("creator")
    String creatorId;

    // Unclear what this means
    // e.g. "0"
    @JsonProperty("depth")
    String depth;

    @JsonProperty("depthRaw")
    Long depthRaw;

    // e.g. ["parler", "parlerusa"]
    @JsonProperty("hashtags")
    List<String> hashtags;

    // These are linkIds for ParlerLinks
    // e.g. ["tzQpI"],
    @JsonProperty("links")
    List<String> linkIds;

    // This is the id of the parent post to this post
    // An example post which has a parent:
    // https://parler.com/post/a730ccce12fe4ed6ae9419ee79589405
    // The parent post: https://parler.com/post/e37160b20b3e44ac9f04a0341138abcb
    @JsonProperty("parent")
    String parentId;

    @JsonProperty("preview")
    String preview;

    @JsonProperty("sensitive")
    Boolean sensitive;

    @JsonProperty("state")
    Integer state;

    @JsonProperty("shareLink")
    String shareLink;
    
    @JsonProperty("trolling")
    Boolean trolling;

    // If this is a share of a share of a share, then the root is the very first
    // post
    // Example: https://parler.com/post/fa5e09d8ea60445ea5b9e07115bb6deb
    // ID: fa5e09d8ea60445ea5b9e07115bb6deb
    // Parent: 66e260cf4f9c46ba9bd77b0a505f491c
    // Root: 761bc451f36c453c86bf3df2deb49706
    @JsonProperty("root")
    String rootId;

    // == Weird Parler Numbers (e.g. 2.3m) == //
    Long comments;
    Long impressions;
    Long reposts;
    Long upvotes;

    public ParlerPost(@JsonProperty("id") String idA,
            @JsonProperty("_id") String idB,
            @JsonProperty("comments") String comments,
            @JsonProperty("impressions") String impressions,
            @JsonProperty("reposts") String reposts,
            @JsonProperty("upvotes") String upvotes) {
        this.parlerId = (idA != null && !idA.isEmpty()) ? idA : idB;

        this.comments = PUtils.deparlify(comments);
        this.impressions = PUtils.deparlify(impressions);
        this.reposts = PUtils.deparlify(reposts);
        this.upvotes = PUtils.deparlify(upvotes);
    }

    public ParlerPost() {
        // Needed for Hibernate to load
    }

    @Id
    @Column(name = "id")
    public String getParlerId() {
        return parlerId;
    }

    public void setParlerId(String parlerId) {
        this.parlerId = parlerId;
    }

    @Column(name = "mentions")
    @Convert(converter = MapToJsonConverter.class)
    public Map<String, String> getMentions() {
        return mentions;
    }

    public void setMentions(Map<String, String> mentions) {
        this.mentions = mentions;
    }

    @Column(name = "article")
    public Boolean getArticle() {
        return article;
    }

    public void setArticle(Boolean article) {
        this.article = article;
    }

    @Column(name = "body")
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Column(name = "created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Column(name = "creator_id")
    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    @Column(name = "depth")
    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    @Column(name = "depth_raw")
    public Long getDepthRaw() {
        return depthRaw;
    }

    public void setDepthRaw(Long depthRaw) {
        this.depthRaw = depthRaw;
    }

    @Column(name = "hashtags")
    @Convert(converter = ListToJsonConverter.class)
    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    @Column(name = "link_ids")
    @Convert(converter = ListToJsonConverter.class)
    public List<String> getLinkIds() {
        return linkIds;
    }

    public void setLinkIds(List<String> linkIds) {
        this.linkIds = linkIds;
    }

    @Column(name = "parent_id")
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Column(name = "preview")
    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    @Column(name = "sensitive")
    public Boolean getSensitive() {
        return sensitive;
    }

    public void setSensitive(Boolean sensitive) {
        this.sensitive = sensitive;
    }

    @Column(name = "state")
    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    @Column(name = "share_link")
    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    @Column(name = "comments")
    public Long getComments() {
        return comments;
    }

    public void setComments(Long comments) {
        this.comments = comments;
    }

    @Column(name = "impressions")
    public Long getImpressions() {
        return impressions;
    }

    public void setImpressions(Long impressions) {
        this.impressions = impressions;
    }

    @Column(name = "reposts")
    public Long getReposts() {
        return reposts;
    }

    public void setReposts(Long reposts) {
        this.reposts = reposts;
    }

    @Column(name = "root_id")
    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    @Column(name = "upvotes")
    public Long getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Long upvotes) {
        this.upvotes = upvotes;
    }
    
    @Column(name = "trolling")
    public boolean getTrolling() {
        return trolling;
    }

    public void setTrolling(boolean trolling) {
        this.trolling = trolling;
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
        ParlerPost other = (ParlerPost) obj;
        if (parlerId == null) {
            if (other.parlerId != null)
                return false;
        } else if (!parlerId.equals(other.parlerId))
            return false;
        return true;
    }

}
