package com.example.navigationtoyproject

import android.app.Application
import com.kakaomobility.knsdk.KNSDK

class ServiceApplication: Application()  {
    override fun onCreate() {
        super.onCreate()
        // KNSDK 등록
        KNSDK.install(this, "$filesDir/files")
    }
}