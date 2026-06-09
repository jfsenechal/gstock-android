package be.marche.gstock.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import be.marche.gstock.data.auth.AuthState
import be.marche.gstock.ui.auth.AuthViewModel
import be.marche.gstock.ui.auth.LoginScreen
import be.marche.gstock.ui.account.AccountScreen
import be.marche.gstock.ui.catalog.CatalogScreen
import be.marche.gstock.ui.checkout.CheckoutScreen
import be.marche.gstock.ui.checkouts.CheckoutsScreen
import be.marche.gstock.ui.common.LoadingBox

@Composable
fun GstockApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Loading -> LoadingBox()
        AuthState.Unauthenticated -> LoginScreen()
        is AuthState.Authenticated -> MainScaffold(onLogout = authViewModel::logout)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val title = Destination.entries
        .firstOrNull { dest -> currentRoute?.hierarchy?.any { it.route == dest.route } == true }
        ?.label ?: "Gstock"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Checkouts.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Checkout.route) {
                CheckoutScreen(
                    onFinished = {
                        navController.navigate(Destination.Checkouts.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Destination.Catalog.route) { CatalogScreen() }
            composable(Destination.Checkouts.route) { CheckoutsScreen() }
            composable(Destination.Account.route) { AccountScreen() }
        }
    }
}
