package com.colman.triplens.data.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages seed data flag to ensure initial mock posts are only generated once.
 */
class SeedDataManager(context: Context) {

    companion object {
        private const val TAG = "SeedDataManager"
        private const val PREFS_NAME = "seed_data_prefs"
        private const val KEY_SEED_GENERATED = "seed_data_generated"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if seed data has been generated.
     */
    fun isSeedGenerated(): Boolean {
        val generated = prefs.getBoolean(KEY_SEED_GENERATED, false)
        Log.d(TAG, "isSeedGenerated: $generated")
        return generated
    }

    /**
     * Mark seed data as generated (persist flag).
     */
    fun markSeedGenerated() {
        Log.d(TAG, "Marking seed data as generated")
        prefs.edit().putBoolean(KEY_SEED_GENERATED, true).apply()
    }

    /**
     * Reset seed flag (for testing or if you want to regenerate).
     */
    fun resetSeedFlag() {
        Log.d(TAG, "Resetting seed flag")
        prefs.edit().putBoolean(KEY_SEED_GENERATED, false).apply()
    }
}

