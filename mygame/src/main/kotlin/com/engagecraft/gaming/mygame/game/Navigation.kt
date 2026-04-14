package com.engagecraft.gaming.mygame.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingEvent
import com.engagecraft.gaming.core.lib.GamingEvent.Companion.asState
import com.engagecraft.gaming.core.lib.GamingUtil
import com.engagecraft.gaming.mygame.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Navigation(
    gameId: String,
    initDeepLink: String?,
    openedFromCard: Boolean,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val onLink by GamingEvent.onLink().asState()
    LaunchedEffect(onLink) {
        onLink?.link?.let {
            GamingUtil.log("EVENT deeplink $it")
            processDeepLink(navController, it)
        }
    }

    LaunchedEffect(initDeepLink) {
        initDeepLink?.let {
            GamingUtil.log("EVENT deeplink $initDeepLink")
            processDeepLink(navController, it)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.mygame_title)) },
                    navigationIcon = {
                        IconButton(onClick = { Gaming.openMenu() }) {
                            Icon(painter = painterResource(R.drawable.mygame_outline_menu_24), contentDescription = null)
                        }
                    },
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.mygame_outline_home_24),
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.mygame_tab_home)) },
                        selected = navBackStackEntry.isScreen(Screens.Home),
                        onClick = { openTab(navController, Screens.Home) }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.mygame_outline_list_24),
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.mygame_tab_other)) },
                        selected = navBackStackEntry.isScreen(Screens.Other),
                        onClick = { openTab(navController, Screens.Other) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Screens.Home) {
                composable<Screens.Home> {
                    GameHome(
                        gameId = gameId,
                        openedFromCard = openedFromCard,
                    )
                }
                composable<Screens.Other> {
                    GameOther()
                }
            }
        }
    }
}

internal fun processDeepLink(navController: NavController, link: String) {
    link.toUri().apply {
        if (lastPathSegment == "other") {
            navController.navigate(Screens.Other)
        }
    }
}

internal fun openTab(tabsNavController: NavController, route: Screens) {
    tabsNavController.navigate(route) {
        popUpTo(tabsNavController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun NavBackStackEntry?.isScreen(screen: Screens) =
    this?.destination?.hierarchy?.any { it.hasRoute(screen::class) } == true
