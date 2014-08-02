package com.emmaguy.todayilearned.background;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.emmaguy.todayilearned.SettingsActivity;
import com.emmaguy.todayilearned.Utils;
import com.emmaguy.todayilearned.data.Listing;
import com.emmaguy.todayilearned.data.Post;
import com.emmaguy.todayilearned.data.Reddit;
import com.emmaguy.todayilearned.sharedlib.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RetrieveService extends WakefulIntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint("http://www.reddit.com/")
            .setConverter(new GsonConverter(new GsonBuilder().registerTypeAdapter(Listing.class, new Listing.ListingJsonDeserializer()).create()))
            .build();

    private final Reddit mRedditEndpoint = restAdapter.create(Reddit.class);
    private final ArrayList<String> mRedditPosts = new ArrayList<String>();
    private final ArrayList<String> mRedditPostSubreddits = new ArrayList<String>();

    private GoogleApiClient mGoogleApiClient;

    private long mLatestCreatedUtc = 0;

    public RetrieveService() {
        super("RetrieveService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        connectToWearable();
        retrieveLatestPostsFromReddit();
    }

    private void connectToWearable() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    private void retrieveLatestPostsFromReddit() {
        mRedditEndpoint.latestTILs(getSubreddit(), getSortType(), getNumberToRequest())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Func1<Listing, Observable<Post>>() {
                    @Override
                    public Observable<Post> call(Listing listing) {
                        return Observable.from(listing.getPosts());
                    }
                })
                .subscribe(new Action1<Post>() {
                    @Override
                    public void call(Post post) {
                        if (postIsNewerThanPreviouslyRetrievedPosts(post)) {
                            mRedditPosts.add(post.getTitle());
                            mRedditPostSubreddits.add(post.getSubreddit());

                            if (post.getCreatedUtc() > mLatestCreatedUtc) {
                                mLatestCreatedUtc = post.getCreatedUtc();
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("RedditWearApp", "Failed to retrieve latest posts: " + throwable.getLocalizedMessage(), throwable);
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        if (mRedditPosts.size() > 0) {
                            if (mLatestCreatedUtc > 0) {
                                storeNewCreatedUtc(mLatestCreatedUtc);
                            }

                            sendNewPostsData();
                        }
                    }
                });
    }

    private boolean postIsNewerThanPreviouslyRetrievedPosts(Post post) {
        return post.getCreatedUtc() > getCreatedUtcOfPosts();
    }

    private void storeNewCreatedUtc(long createdAtUtc) {
        getSharedPreferences().edit().putLong(SettingsActivity.PREFS_CREATED_UTC, createdAtUtc).apply();
    }

    private void sendNewPostsData() {
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest mapRequest = PutDataMapRequest.create(Constants.PATH_REDDIT_POSTS);
            mapRequest.getDataMap().putStringArrayList(Constants.KEY_REDDIT_POSTS, mRedditPosts);
            mapRequest.getDataMap().putStringArrayList(Constants.KEY_POST_SUBREDDITS, mRedditPostSubreddits);

            Wearable.DataApi.putDataItem(mGoogleApiClient, mapRequest.asPutDataRequest())
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            mRedditPosts.clear();

                            if (mGoogleApiClient.isConnected()) {
                                mGoogleApiClient.disconnect();
                            }
                        }
                    });
        }
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    private int getNumberToRequest() {
        return Integer.parseInt(getSharedPreferences().getString(SettingsActivity.PREFS_NUMBER_TO_RETRIEVE, "5"));
    }

    private long getCreatedUtcOfPosts() {
        return getSharedPreferences().getLong(SettingsActivity.PREFS_CREATED_UTC, 0);
    }

    private String getSortType() {
        return getSharedPreferences().getString(SettingsActivity.PREFS_SORT_ORDER, "new");
    }

    private String getSubreddit() {
        return TextUtils.join("+", Utils.selectedSubReddits(getApplicationContext()));
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}