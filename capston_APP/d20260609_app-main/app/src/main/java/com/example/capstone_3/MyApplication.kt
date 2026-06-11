package com.example.capstone_3

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.iamport.sdk.domain.core.Iamport

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Iamport.create(this)
        initEdutrackFirebase()
    }

    private fun initEdutrackFirebase() {
        if (FirebaseApp.getApps(this).none { it.name == "edutrack" }) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyDzi7vvF4A5E_Mg9SMJrZZ8Z7RMCxgLcc4")
                .setProjectId("edutrack-bfd57")
                .setApplicationId("1:000000000000:android:0000000000000000")
                .build()
            FirebaseApp.initializeApp(this, options, "edutrack")
        }
    }
}
