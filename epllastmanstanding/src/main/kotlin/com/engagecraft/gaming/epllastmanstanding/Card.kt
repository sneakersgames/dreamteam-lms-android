package com.engagecraft.gaming.epllastmanstanding

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingAuthManager
import com.engagecraft.gaming.core.lib.model.isLoggedIn
import com.engagecraft.gaming.epllastmanstanding.web.GameConfig

/**
 * Placeholder featured card, ported from the iOS `FeaturedCard`.
 *
 * A card may never open the game itself, so the tap is routed through the host.
 */
@Keep
@Composable
fun Card(data: Bundle? = null) {
    val user by GamingAuthManager.getUser().observeAsState()
    val isLoggedIn = user?.isLoggedIn() == true

    Card(
        onClick = { Gaming.open(GameConfig.GAME_ID, data?.getBundle(Gaming.PROP_DATA)) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.epllastmanstanding_card_headline),
                style = MaterialTheme.typography.titleMedium,
            )
            if (isLoggedIn) {
                Text(
                    text = stringResource(
                        R.string.epllastmanstanding_card_username,
                        user?.username.orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.epllastmanstanding_card_user_id,
                        user?.id?.toString().orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
