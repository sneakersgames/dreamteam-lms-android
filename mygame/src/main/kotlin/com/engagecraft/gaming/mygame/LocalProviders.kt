package com.engagecraft.gaming.mygame

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.engagecraft.gaming.core.lib.Gaming

internal data class Params(
    val gameId: String = "",
)

internal val LocalParams = staticCompositionLocalOf { Params() }

@Composable
internal fun LocalProviders(
    data: Bundle?,
    component: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalParams provides Params(
            gameId = data?.getString(Gaming.PROP_GAME_ID) ?: "mygame"
        )
    ) {
        component()
    }
}