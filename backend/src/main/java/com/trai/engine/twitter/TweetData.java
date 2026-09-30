package com.trai.engine.twitter;

/**
 * Represents a single post fetched from X (formerly Twitter) API v2.
 */
public class TweetData {

    private String id;
    private String text;
    private String authorUsername;
    private boolean authorVerified;
    private int authorFollowers;
    private int retweetCount;
    private int likeCount;
    private String country;
    private String lang;
    private String createdAt;
    private double viralityWeight;   // computed from verification + engagement + authorityScore
    private double authorityScore;   // X user authority score (0–100) from XUserRankingService

    public String getId()              { return id; }
    public String getText()            { return text; }
    public String getAuthorUsername()  { return authorUsername; }
    public boolean isAuthorVerified()  { return authorVerified; }
    public int getAuthorFollowers()    { return authorFollowers; }
    public int getRetweetCount()       { return retweetCount; }
    public int getLikeCount()          { return likeCount; }
    public String getCountry()         { return country; }
    public String getLang()            { return lang; }
    public String getCreatedAt()       { return createdAt; }
    public double getViralityWeight()  { return viralityWeight; }
    public double getAuthorityScore()  { return authorityScore; }

    public void setId(String v)              { this.id = v; }
    public void setText(String v)            { this.text = v; }
    public void setAuthorUsername(String v)  { this.authorUsername = v; }
    public void setAuthorVerified(boolean v) { this.authorVerified = v; }
    public void setAuthorFollowers(int v)    { this.authorFollowers = v; }
    public void setRetweetCount(int v)       { this.retweetCount = v; }
    public void setLikeCount(int v)          { this.likeCount = v; }
    public void setCountry(String v)         { this.country = v; }
    public void setLang(String v)            { this.lang = v; }
    public void setCreatedAt(String v)       { this.createdAt = v; }
    public void setViralityWeight(double v)  { this.viralityWeight = v; }
    public void setAuthorityScore(double v)  { this.authorityScore = v; }
}
