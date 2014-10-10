package com.emmaguy.todayilearned.sharedlib;

public class Post {
    private final String mSubreddit;
    private final String mTitle;
    private final String mDescription;
    private final String mFullname;
    private final String mPermalink;
    private final long mCreatedUtc;

    public Post(String title, String subreddit, String selftext, String fullname, String permalink, long createdUtc) {
        mTitle = title;
        mDescription = selftext;
        mFullname = fullname;
        mPermalink = permalink;
        mCreatedUtc = createdUtc;
        mSubreddit = String.format("/r/%s", subreddit);
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSubreddit() {
        return mSubreddit;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getFullname() {
        return mFullname;
    }

    public long getCreatedUtc() {
        return mCreatedUtc;
    }

    public String getPermalink() {
        return mPermalink;
    }
}