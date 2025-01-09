package com.example.navigationtoyproject

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.kakaomobility.knsdk.KNSDK

private const val USER_PREFERENCES_NAME = "user_preferences"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_PREFERENCES_NAME)

class ServiceApplication: Application()  {
    override fun onCreate() {
        super.onCreate()
        // KNSDK 등록
        KNSDK.install(this, "$filesDir/files")
    }
}