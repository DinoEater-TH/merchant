package com.example.util

/**
 * Tracks whether the admin UI is currently visible to the user.
 *
 * Used to avoid double alerts: while the Activity is in the foreground the
 * ViewModel plays the alert, and while it is in the background the foreground
 * service plays it.
 */
object AppVisibility {
    @Volatile
    var isForeground: Boolean = false
}
