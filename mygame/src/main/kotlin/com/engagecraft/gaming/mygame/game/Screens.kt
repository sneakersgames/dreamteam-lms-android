package com.engagecraft.gaming.mygame.game

import kotlinx.serialization.Serializable

internal sealed class Screens {
    @Serializable data object Home: Screens()
    @Serializable data object Other: Screens()
}
