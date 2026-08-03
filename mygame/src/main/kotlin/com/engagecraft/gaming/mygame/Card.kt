package com.engagecraft.gaming.mygame

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingAuthManager
import com.engagecraft.gaming.core.lib.GamingEvent
import com.engagecraft.gaming.core.lib.GamingEvent.Companion.asState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Keep
@Composable
fun Card(data: Bundle? = null) {
    val gameId = data?.getString(Gaming.PROP_GAME_ID) ?: ""

    val user = GamingAuthManager.getUser().observeAsState()

    val refresh by GamingEvent.onRefresh().asState()
    var onRefresh by remember(refresh) { mutableStateOf(refresh != null) }
    LaunchedEffect(onRefresh) {
        if (onRefresh) {
            delay(1.seconds)
            onRefresh = false
        }
    }

    Card {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp)
        ) {
            Text(text = stringResource(R.string.mygame_card_game, gameId))
            Text(text = user.value?.username ?: "")
            Text(
                text = stringResource(
                    R.string.mygame_card_data,
                    data?.getBundle(Gaming.PROP_DATA)?.let { "\n$it" } ?: ""
                ),
                textAlign = TextAlign.Center
            )
            Button(onClick = {
                Gaming.open(gameId, bundleOf("fromCard" to true))
            }) {
                Text(text = stringResource(R.string.mygame_card_open_game))
            }
        }
    }
}

@Preview
@Composable
fun CardPreview() {
    Card()
}