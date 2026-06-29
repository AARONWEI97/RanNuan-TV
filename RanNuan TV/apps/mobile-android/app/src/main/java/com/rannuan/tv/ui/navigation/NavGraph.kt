package com.rannuan.tv.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.ui.screens.category.CategoryScreen
import com.rannuan.tv.ui.screens.category.prefetchCategoryFirstPages
import com.rannuan.tv.ui.screens.detail.DetailScreen
import com.rannuan.tv.ui.screens.home.HomeScreen
import com.rannuan.tv.ui.screens.player.PlayerScreen
import com.rannuan.tv.ui.screens.profile.ProfileScreen
import com.rannuan.tv.ui.screens.search.SearchScreen
import com.rannuan.tv.ui.theme.Brand400
import com.rannuan.tv.ui.theme.Zinc300
import com.rannuan.tv.ui.theme.Zinc500
import com.rannuan.tv.ui.theme.Zinc950

const val API_HOST = "8.156.83.59"

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Outlined.Home)
    data object Category : Screen("category/movie", "分类", Icons.Outlined.GridView)
    data object Search : Screen("search", "搜索", Icons.Outlined.Search)
    data object Profile : Screen("profile", "我的", Icons.Outlined.Person)

    companion object {
        // 直接列举所有 Tab，避免运行时 Kotlin 反射（sealedSubclasses 需要 kotlin-reflect）。
        // App 未引入 kotlin-reflect，反射会导致进入首页时 KotlinReflectionNotSupportedError 闪退。
        val entries get() = listOf(Home, Category, Search, Profile)
    }
}

sealed class DetailRoute(val route: String) {
    data object MultiSource : DetailRoute("detail/source/{name}?keys={keys}")
    data object SingleSource : DetailRoute("detail/{siteKey}/{id}?name={name}")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost(api: RanNuanApi) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf("home", "category", "search", "profile")
    // route 是模式串(如 "category/{type}")，不能用精确匹配，用前缀匹配
    val showBottomBar = currentRoute?.let { route ->
        bottomBarRoutes.any { route.startsWith(it) }
    } ?: false

    LaunchedEffect(Unit) {
        prefetchCategoryFirstPages(api)
    }

    Scaffold(
        containerColor = Zinc950,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Zinc950,
                    contentColor = Zinc300,
                    tonalElevation = 0.dp
                ) {
                    Screen.entries.forEach { screen ->
                        val routeBase = when (screen) {
                            Screen.Category -> "category"
                            else -> screen.route
                        }
                        val selected = currentRoute?.startsWith(routeBase) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (selected) return@NavigationBarItem
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Brand400,
                                selectedTextColor = Brand400,
                                indicatorColor = Brand400.copy(alpha = 0.15f),
                                unselectedIconColor = Zinc500,
                                unselectedTextColor = Zinc500
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(api = api, onNavigate = { navController.navigate(it) })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onNavigate = { navController.navigate(it) })
            }
            composable(
                "search?wd={wd}",
                arguments = listOf(navArgument("wd") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val wd = backStackEntry.arguments?.getString("wd") ?: ""
                SearchScreen(initialQuery = wd, api = api, onNavigate = { navController.navigate(it) })
            }
            composable(
                "category/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "movie"
                CategoryScreen(type = type, api = api, onNavigate = { navController.navigate(it) })
            }
            composable(
                DetailRoute.SingleSource.route,
                arguments = listOf(
                    navArgument("siteKey") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val name = backStackEntry.arguments?.getString("name") ?: ""
                DetailScreen(
                    siteKey = siteKey, id = id, name = name,
                    api = api,
                    onBack = { navController.popBackStack() },
                    onPlay = { siteKey2, id2, src, ep, playName, playKeys ->
                        val encodedName = java.net.URLEncoder.encode(playName, "UTF-8")
                        val encodedKeys = java.net.URLEncoder.encode(playKeys, "UTF-8")
                        navController.navigate("player/$siteKey2/$id2?src=$src&ep=$ep&name=$encodedName&keys=$encodedKeys")
                    }
                )
            }
            composable(
                DetailRoute.MultiSource.route,
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType },
                    navArgument("keys") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val keys = backStackEntry.arguments?.getString("keys") ?: ""
                DetailScreen(
                    name = name, keys = keys,
                    api = api,
                    onBack = { navController.popBackStack() },
                    onPlay = { siteKey2, id2, src, ep, playName, playKeys ->
                        val encodedName = java.net.URLEncoder.encode(playName, "UTF-8")
                        val encodedKeys = java.net.URLEncoder.encode(playKeys, "UTF-8")
                        navController.navigate("player/$siteKey2/$id2?src=$src&ep=$ep&name=$encodedName&keys=$encodedKeys")
                    }
                )
            }
            composable(
                "player/{siteKey}/{id}?src={src}&ep={ep}&pos={pos}&name={name}&keys={keys}",
                arguments = listOf(
                    navArgument("siteKey") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                    navArgument("src") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("ep") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("pos") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("keys") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val src = backStackEntry.arguments?.getInt("src") ?: 0
                val ep = backStackEntry.arguments?.getInt("ep") ?: 0
                val pos = backStackEntry.arguments?.getLong("pos") ?: 0L
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val keys = backStackEntry.arguments?.getString("keys") ?: ""
                PlayerScreen(
                    siteKey = siteKey, id = id, sourceIdx = src, epIdx = ep, resumePos = pos,
                    name = name, keys = keys,
                    api = api,
                    onBack = { navController.popBackStack() },
                    onNavigate = { navController.navigate(it) }
                )
            }
        }
    }
}
