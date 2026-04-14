package com.engagecraft.gaming.mygame

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.mygame.game.Navigation

@Keep
@Composable
fun Game(data: Bundle? = null) {
    Navigation(
        gameId = data?.getString(Gaming.PROP_GAME_ID) ?: "",
        initDeepLink = data?.getBundle(Gaming.PROP_DATA)?.getString(Gaming.PROP_LINK),
        openedFromCard = data?.getBundle(Gaming.PROP_DATA)?.getBoolean("fromCard") == true,
    )
}

@Preview
@Composable
internal fun GamePrev() {
    Game()
}