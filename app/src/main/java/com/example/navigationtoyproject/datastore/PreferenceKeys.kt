package com.example.navigationtoyproject.datastore

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val MAP_VERSION_KEY = stringPreferencesKey("map_version")
    val REQUEST_FLAG_KEY = intPreferencesKey("request_flag") // New key for request flag
}