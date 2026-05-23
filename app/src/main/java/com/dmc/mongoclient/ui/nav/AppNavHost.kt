package com.dmc.mongoclient.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dmc.mongoclient.ui.browse.BrowseScreen
import com.dmc.mongoclient.ui.common.AdaptiveScreenContainer
import com.dmc.mongoclient.ui.connections.ConnectionEditScreen
import com.dmc.mongoclient.ui.connections.ConnectionListScreen
import com.dmc.mongoclient.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Route.CONNECTIONS) {
        composable(Route.CONNECTIONS) {
            AdaptiveScreenContainer {
                ConnectionListScreen(
                    onAddConnection = { nav.navigate(Route.connectionEdit(null)) },
                    onEditConnection = { id -> nav.navigate(Route.connectionEdit(id)) },
                    onConnected = { nav.navigate(Route.BROWSE) },
                    onOpenSettings = { nav.navigate(Route.SETTINGS) },
                )
            }
        }

        composable(Route.SETTINGS) {
            AdaptiveScreenContainer {
                SettingsScreen(onClose = { nav.popBackStack() })
            }
        }

        composable(
            route = "${Route.CONNECTION_EDIT}/{${Route.CONNECTION_EDIT_ARG}}",
            arguments = listOf(
                navArgument(Route.CONNECTION_EDIT_ARG) {
                    type = NavType.StringType
                    defaultValue = "new"
                },
            ),
        ) {
            // The route arg is a string ("new" or a numeric id) — the
            // ViewModel reads it from SavedStateHandle.
            AdaptiveScreenContainer {
                ConnectionEditScreen(onClose = { nav.popBackStack() })
            }
        }

        composable(Route.BROWSE) {
            BrowseScreen(
                onDisconnected = { nav.popBackStack(Route.CONNECTIONS, inclusive = false) },
            )
        }
    }
}
