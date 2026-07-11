package com.coolventory.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.ui.nav.Routes
import com.coolventory.app.ui.screens.AllProductsScreen
import com.coolventory.app.ui.screens.BuyAgainScreen
import com.coolventory.app.ui.screens.ExpiryReviewScreen
import com.coolventory.app.ui.screens.HistoryDetailScreen
import com.coolventory.app.ui.screens.HistoryScreen
import com.coolventory.app.ui.screens.OnboardingScreen
import com.coolventory.app.ui.screens.ProductDetailScreen
import com.coolventory.app.ui.screens.ProductFormScreen
import com.coolventory.app.ui.screens.SearchScreen
import com.coolventory.app.ui.screens.SettingsScreen
import com.coolventory.app.ui.screens.ShelfDashboardScreen
import com.coolventory.app.ui.screens.ShelfManagementScreen
import com.coolventory.app.ui.screens.StatisticsScreen
import com.coolventory.app.ui.vm.MainViewModel

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.DASHBOARD, "Storage", Icons.Filled.Kitchen),
    BottomItem(Routes.SEARCH, "Search", Icons.Filled.Search),
    BottomItem(Routes.HISTORY, "History", Icons.Filled.History),
    BottomItem(Routes.BUY_AGAIN, "Buy Again", Icons.Filled.ShoppingBag),
    BottomItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun CoolventoryApp(
    viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Decide start destination once, after data has loaded.
    val startDestination = remember(state.settings.onboardingCompleted) {
        if (state.settings.onboardingCompleted) Routes.DASHBOARD else Routes.ONBOARDING
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            appGraph(navController, viewModel)
        }
    }
}

private fun NavGraphBuilder.appGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
) {
    composable(Routes.ONBOARDING) {
        OnboardingScreen(
            onAddFirst = {
                viewModel.completeOnboarding()
                navController.navigateTopLevel(Routes.DASHBOARD)
                navController.navigate(Routes.addProduct())
            },
            onExplore = {
                viewModel.completeOnboarding()
                navController.navigateTopLevel(Routes.DASHBOARD)
            },
        )
    }

    composable(Routes.DASHBOARD) {
        ShelfDashboardScreen(
            viewModel = viewModel,
            onOpenProduct = { navController.navigate(Routes.productDetail(it)) },
            onAddProduct = { location, shelfId -> navController.navigate(Routes.addProduct(location, shelfId)) },
            onOpenReview = { navController.navigate(Routes.EXPIRY_REVIEW) },
            onOpenAllProducts = { navController.navigate(Routes.ALL_PRODUCTS) },
            onOpenSearch = { navController.navigateBottom(Routes.SEARCH) },
            onOpenBuyAgain = { navController.navigateBottom(Routes.BUY_AGAIN) },
        )
    }

    composable(Routes.SEARCH) {
        SearchScreen(
            viewModel = viewModel,
            onOpenProduct = { navController.navigate(Routes.productDetail(it)) },
        )
    }

    composable(Routes.HISTORY) {
        HistoryScreen(
            viewModel = viewModel,
            onOpenEvent = { navController.navigate(Routes.historyDetail(it)) },
        )
    }

    composable(Routes.BUY_AGAIN) {
        BuyAgainScreen(viewModel = viewModel)
    }

    composable(Routes.SETTINGS) {
        SettingsScreen(
            viewModel = viewModel,
            onOpenShelfManagement = { navController.navigate(Routes.SHELF_MANAGEMENT) },
            onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
            onShowOnboarding = { navController.navigate(Routes.ONBOARDING) },
        )
    }

    composable(Routes.ALL_PRODUCTS) {
        AllProductsScreen(
            viewModel = viewModel,
            onOpenProduct = { navController.navigate(Routes.productDetail(it)) },
            onAddProduct = { navController.navigate(Routes.addProduct()) },
            onBack = { navController.popBackStack() },
        )
    }

    composable(Routes.EXPIRY_REVIEW) {
        ExpiryReviewScreen(
            viewModel = viewModel,
            onOpenProduct = { navController.navigate(Routes.productDetail(it)) },
            onBack = { navController.popBackStack() },
        )
    }

    composable(Routes.STATISTICS) {
        StatisticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
    }

    composable(Routes.SHELF_MANAGEMENT) {
        ShelfManagementScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
    }

    composable(
        route = Routes.ADD_PRODUCT_PATTERN,
        arguments = listOf(
            navArgument(Routes.ARG_LOCATION) { type = NavType.StringType; defaultValue = "" },
            navArgument(Routes.ARG_SHELF_ID) { type = NavType.StringType; defaultValue = "" },
        ),
    ) { entry ->
        val locationArg = entry.arguments?.getString(Routes.ARG_LOCATION).orEmpty()
        val shelfArg = entry.arguments?.getString(Routes.ARG_SHELF_ID).orEmpty()
        val presetLocation = StorageLocation.entries.firstOrNull { it.name == locationArg }
        ProductFormScreen(
            viewModel = viewModel,
            productId = null,
            presetLocation = presetLocation,
            presetShelfId = shelfArg.ifBlank { null },
            onDone = { navController.popBackStack() },
            onCancel = { navController.popBackStack() },
        )
    }

    composable(
        route = Routes.EDIT_PRODUCT_PATTERN,
        arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.StringType }),
    ) { entry ->
        val productId = entry.arguments?.getString(Routes.ARG_PRODUCT_ID)
        ProductFormScreen(
            viewModel = viewModel,
            productId = productId,
            presetLocation = null,
            presetShelfId = null,
            onDone = { navController.popBackStack() },
            onCancel = { navController.popBackStack() },
        )
    }

    composable(
        route = Routes.PRODUCT_DETAIL_PATTERN,
        arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.StringType }),
    ) { entry ->
        val productId = entry.arguments?.getString(Routes.ARG_PRODUCT_ID)
        ProductDetailScreen(
            viewModel = viewModel,
            productId = productId,
            onEdit = { navController.navigate(Routes.editProduct(it)) },
            onBack = { navController.popBackStack() },
            onDeleted = { navController.popBackStack() },
            onOpenEvent = { navController.navigate(Routes.historyDetail(it)) },
        )
    }

    composable(
        route = Routes.HISTORY_DETAIL_PATTERN,
        arguments = listOf(navArgument(Routes.ARG_EVENT_ID) { type = NavType.StringType }),
    ) { entry ->
        val eventId = entry.arguments?.getString(Routes.ARG_EVENT_ID)
        HistoryDetailScreen(
            viewModel = viewModel,
            eventId = eventId,
            onBack = { navController.popBackStack() },
            onOpenProduct = { navController.navigate(Routes.productDetail(it)) },
        )
    }
}

/** Navigate to a top-level destination clearing the onboarding entry cleanly. */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true; saveState = false }
        launchSingleTop = true
    }
}

/** Navigate to a bottom-bar destination, matching the bottom bar's own behavior. */
private fun NavHostController.navigateBottom(route: String) {
    navigate(route) {
        popUpTo(Routes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
