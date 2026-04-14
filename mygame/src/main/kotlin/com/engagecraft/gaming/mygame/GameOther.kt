package com.engagecraft.gaming.mygame

import android.graphics.Canvas
import android.widget.LinearLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingAuthManager

@Composable
internal fun GameOther(
    viewModel: DemoViewModel = hiltViewModel(),
) {
    val gameId = LocalParams.current.gameId

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            AndroidView(factory = { ctx ->
                LinearLayout(ctx).apply {
                    addView(ComposeView(ctx).apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.LightGray,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val bitmap = createBitmap(width, height)
                                        val canvas = Canvas(bitmap)
                                        draw(canvas)

                                        Gaming.shareImage(
                                            context = ctx,
                                            bitmap = bitmap,
                                            key = "card",
                                        )
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 48.dp,
                                            horizontal = 16.dp,
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.mygame_outline_star_24),
                                        contentDescription = null,
                                        tint = Color.Blue,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Click to share",
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    })
                }
            }, update = {

            })
            if (GamingAuthManager.isLoggedIn) {
                Spacer(modifier = Modifier.size(16.dp))
                Button(onClick = { viewModel.checkNotifications(gameId) }) {
                    Text(text = "Check notifications")
                }
                if (viewModel.notificationsChecked) {
                    Text(text = buildAnnotatedString {
                        append("Notifications enabled: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(viewModel.isNotificationsEnabled.toString())
                        }
                    })
                    if (!viewModel.isNotificationsEnabled) {
                        Spacer(modifier = Modifier.size(16.dp))
                        Button(onClick = { viewModel.enableNotifications(gameId) }) {
                            Text(text = "Enable notifications")
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    Card {
                        Column {
                            viewModel.notificationChannels.forEach {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = it.name)
                                    Switch(
                                        colors = SwitchDefaults.colors(),
                                        checked = it.isEnabled,
                                        onCheckedChange = { isChecked ->
                                            viewModel.setupChannel(gameId, it, isChecked)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}