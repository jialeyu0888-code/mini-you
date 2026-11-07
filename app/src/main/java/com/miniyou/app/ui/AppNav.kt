package com.miniyou.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miniyou.app.ui.checkin.CheckinScreen
import com.miniyou.app.ui.checkup.CheckupDetailScreen
import com.miniyou.app.ui.checkup.CheckupEditScreen
import com.miniyou.app.ui.checkup.CheckupListScreen
import com.miniyou.app.ui.checkup.ScheduleScreen
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.diary.DiaryEditScreen
import com.miniyou.app.ui.diary.DiaryScreen
import com.miniyou.app.ui.home.HomeScreen
import com.miniyou.app.ui.knowledge.KnowledgeScreen
import com.miniyou.app.ui.knowledge.LessonDetailScreen
import com.miniyou.app.ui.knowledge.RemedyDetailScreen
import com.miniyou.app.ui.onboarding.OnboardingScreen
import com.miniyou.app.ui.settings.SettingsScreen
import com.miniyou.app.ui.shopping.ShoppingEditScreen
import com.miniyou.app.ui.shopping.ShoppingScreen
import com.miniyou.app.ui.theme.LocalWindowLayout

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CHECKUPS = "checkups"
    const val DIARY = "diary"
    const val SHOPPING = "shopping"
    const val KNOWLEDGE = "knowledge"
    const val SETTINGS = "settings"
    const val CHECKIN = "checkin"
    const val SCHEDULE = "schedule"

    fun checkupEdit(id: Long = -1) = "checkup/edit?id=$id"
    fun checkupDetail(id: Long) = "checkup/$id"
    fun diaryEdit(id: Long = -1, date: Long = -1) = "diary/edit?id=$id&date=$date"
    fun shoppingEdit(id: Long = -1, status: Int = 0) = "shopping/edit?id=$id&status=$status"
    fun knowledge(tab: Int) = "knowledge?tab=$tab"
    fun remedy(id: String) = "remedy/$id"
    fun lesson(id: String) = "lesson/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    fun matches(current: String) = current == route || current.startsWith("$route?")
}

private val tabs = listOf(
    Tab(Routes.HOME, "首页", Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.CHECKUPS, "产检", Icons.AutoMirrored.Outlined.Assignment, Icons.AutoMirrored.Outlined.Assignment),
    Tab(Routes.DIARY, "日记", Icons.Outlined.AutoStories, Icons.Outlined.AutoStories),
    Tab(Routes.SHOPPING, "购物", Icons.Outlined.ShoppingBag, Icons.Outlined.ShoppingBag),
    Tab(Routes.KNOWLEDGE, "百科", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
)

/** 是否由外层底部导航栏负责底部系统栏留白。 */
val LocalBottomBarShown = compositionLocalOf { false }

/** 各页面统一使用的 Scaffold，按导航形态处理系统栏留白。 */
@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val insets = if (LocalBottomBarShown.current) {
        WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else WindowInsets.systemBars
    Scaffold(
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        contentWindowInsets = insets,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val settings = app().settings
    val layout = LocalWindowLayout.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val topLevel = tabs.any { it.matches(route) }
    val showRail = layout.expanded && route != Routes.ONBOARDING && route.isNotEmpty()
    val showBottom = !layout.expanded && topLevel

    fun go(tab: Tab) {
        nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottom) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = tab.matches(route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { go(tab) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                }
            }
        },
    ) { inner ->
        Row(Modifier.fillMaxSize().padding(inner)) {
            if (showRail) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    Spacer(Modifier.height(12.dp))
                    FloatingActionButton(
                        onClick = { nav.navigate(Routes.CHECKIN) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ) { Text("✅") }
                    Spacer(Modifier.height(20.dp))
                    Column {
                        tabs.forEach { tab ->
                            val selected = tab.matches(route)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { go(tab) },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            CompositionLocalProvider(LocalBottomBarShown provides showBottom) {
                AppNavHost(nav, if (settings.current.onboarded) Routes.HOME else Routes.ONBOARDING, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, start: String, modifier: Modifier) {
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(nav, startDestination = start, modifier = modifier) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.CHECKUPS) { CheckupListScreen(nav) }
        composable(
            "checkup/edit?id={id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) { CheckupEditScreen(it.arguments?.getLong("id") ?: -1L, onDone = back) }
        composable("checkup/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            CheckupDetailScreen(it.arguments?.getLong("id") ?: -1L, nav)
        }
        composable(Routes.SCHEDULE) { ScheduleScreen(onBack = back) }
        composable(Routes.DIARY) { DiaryScreen(nav) }
        composable(
            "diary/edit?id={id}&date={date}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("date") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { DiaryEditScreen(it.arguments?.getLong("id") ?: -1L, it.arguments?.getLong("date") ?: -1L, onDone = back) }
        composable(Routes.SHOPPING) { ShoppingScreen(nav) }
        composable(
            "shopping/edit?id={id}&status={status}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("status") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { ShoppingEditScreen(it.arguments?.getLong("id") ?: -1L, it.arguments?.getInt("status") ?: 0, onDone = back) }
        composable(Routes.CHECKIN) { CheckinScreen(onBack = back) }
        composable(
            "knowledge?tab={tab}",
            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 }),
        ) { KnowledgeScreen(nav, it.arguments?.getInt("tab") ?: 0) }
        composable("remedy/{id}") { RemedyDetailScreen(it.arguments?.getString("id").orEmpty(), onBack = back) }
        composable("lesson/{id}") { LessonDetailScreen(it.arguments?.getString("id").orEmpty(), onBack = back) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
    }
}
