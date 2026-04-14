package com.engagecraft.gaming.mygame

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingAuthManager

@Composable
internal fun GameHome(data: Bundle? = null) {
    val gameId = data?.getString(Gaming.PROP_GAME_ID) ?: ""

    val user = GamingAuthManager.getUser().observeAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(R.string.mygame_game_demo))
            Text(text = user.value?.username ?: "")

            Text(text = if (data?.getBundle(Gaming.PROP_DATA)?.getBoolean("fromCard") == true) "fromCard" else "notFromCard")

            if (user.value?.anonymous != false) {
                Button(onClick = {
                    Gaming.login(gameId)
                }) {
                    Text(text = stringResource(R.string.mygame_login))
                }
            }

            if (user.value?.anonymous == false) {
                Button(onClick = {
                    Gaming.openProfile()
                }) {
                    Text(text = stringResource(R.string.mygame_open_profile))
                }
            }

            Button(onClick = {
                Gaming.close()
            }) {
                Text(text = stringResource(R.string.mygame_close_game_demo))
            }
        }
    }
}