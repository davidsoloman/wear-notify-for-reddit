package com.emmaguy.todayilearned;

import com.google.firebase.crash.FirebaseCrash;

import timber.log.Timber;

/**
 * A logging implementation which reports exceptions to Firebase
 */
public class FirebaseTree extends Timber.DebugTree {
    @Override public void i(String message, Object... args) {
        FirebaseCrash.log(String.format(message, args));
    }

    @Override public void i(Throwable t, String message, Object... args) {
        FirebaseCrash.report(t);
    }

    @Override public void w(String message, Object... args) {
        FirebaseCrash.log(String.format(message, args));
    }

    @Override public void w(Throwable t, String message, Object... args) {
        FirebaseCrash.report(t);
    }

    @Override public void e(String message, Object... args) {
        FirebaseCrash.log(String.format(message, args));
    }

    @Override public void e(Throwable t, String message, Object... args) {
        FirebaseCrash.report(t);
    }
}
