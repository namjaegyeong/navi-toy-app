package com.example.navigationtoyproject.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

object DataStoreManager {
    // Extension property to create DataStore
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
}