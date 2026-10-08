package com.engagecraft.gaming.epllastmanstanding

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.livedata.observeAsState
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingAuthManager
import com.engagecraft.gaming.core.lib.GamingEvent
import com.engagecraft.gaming.core.lib.GamingEvent.Companion.asState
import com.engagecraft.gaming.epllastmanstanding.web.GameWebView
import com.engagecraft.gaming.epllastmanstanding.web.GameWebViewHost

/**
 * The game is a remote web app; this composable only hosts it and answers its questions about
 * the user, environment and consent through the `ghbridge` protocol.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Keep
@Composable
fun Game(data: Bundle? = null) {
    val context = LocalContext.current

    // The deep link the host opened the game with, reported to the web app as `launchUrl`.
    val launchUrl = remember(data) {
        data?.getBundle(Gaming.PROP_DATA)?.getString(Gaming.PROP_LINK)
    }

    val host = remember { GameWebViewHost(context, launchUrl) }

    val user by GamingAuthManager.getUser().observeAsState()
    val token by GamingAuthManager.getToken().observeAsState()
    val linkEvent by GamingEvent.onLink().asState()

    LaunchedEffect(host) { host.load() }

    // A changed user id means a login or logout; an unchanged one means the profile data moved.
    // Either way the web app wants a freshly built payload.
    LaunchedEffect(user, token) { host.onAuthChanged(user, token) }

    LaunchedEffect(linkEvent) {
        linkEvent?.link?.let { host.onDeepLink(it) }
    }

    DisposableEffect(host) {
        onDispose { host.destroy() }
    }

    // A swipe-to-open host menu competes with vertical scrolling in the WebView. The menu stays
    // reachable through the app bar button.
    DisposableEffect(Unit) {
        Gaming.setMenuGestured(false)
        onDispose { Gaming.setMenuGestured(true) }
    }

    // Back unwinds the game's own navigation first; only at its root does it leave the game.
    BackHandler { if (!host.goBack()) Gaming.close() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.epllastmanstanding_title)) },
                navigationIcon = {
                    IconButton(onClick = { Gaming.openMenu() }) {
                        Icon(
                            painter = painterResource(R.drawable.epllastmanstanding_outline_menu_24),
                            contentDescription = stringResource(R.string.epllastmanstanding_open_menu),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEB1701)),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Box(Modifier.padding(padding)) {
            GameWebView(host = host, modifier = Modifier.fillMaxSize())
        }
    }
}
