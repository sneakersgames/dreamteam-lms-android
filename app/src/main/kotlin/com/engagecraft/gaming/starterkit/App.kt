package com.engagecraft.gaming.starterkit

import android.app.Application
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingConfig
import com.engagecraft.gaming.ui.shared.theme.dt.Domain
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate(){
        super.onCreate()
        Gaming.init(
            context = this,
            env = GamingConfig.config.env,
            domain = Domain,
        )
    }
}