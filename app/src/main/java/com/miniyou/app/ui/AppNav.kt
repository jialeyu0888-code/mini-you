package com.miniyou.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.miniyou.app.R
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miniyou.app.ui.belly.BellyEditScreen
import com.miniyou.app.ui.belly.BellyScreen
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
import com.miniyou.app.ui.knowledge.MyRemedyEditScreen
import com.miniyou.app.ui.knowledge.RemedyDetailScreen
import com.miniyou.app.ui.onboarding.OnboardingScreen
import com.miniyou.app.ui.settings.SettingsScreen
import com.miniyou.app.ui.shopping.ShoppingDetailScreen
import com.miniyou.app.ui.shopping.ShoppingEditScreen
import com.miniyou.app.ui.shopping.ShoppingScreen
import com.miniyou.app.ui.theme.LocalWindowLayout

object Routes {
    const val HOME = "home"
    const val CHECKUPS = "checkups"
    const val DIARY = "diary"
    const val SHOPPING = "shopping"
    const val KNOWLEDGE = "knowledge"
    const val SETTINGS = "settings"
    const val CHECKIN = "checkin"
    const val SCHEDULE = "schedule"
    const val BELLY = "belly"

    fun checkupEdit(id: Long = -1) = "checkup/edit?id=$id"
    fun checkupDetail(id: Long) = "checkup/$id"
    fun diaryEdit(id: Long = -1, date: Long = -1) = "diary/edit?id=$id&date=$date"
    fun shoppingDetail(id: Long) = "shopping/view/$id"
    fun shoppingEdit(id: Long = -1, status: Int = 0, ai: Boolean = false) = "shopping/edit?id=$id&status=$status&ai=$ai"
    fun knowledge(tab: Int) = "knowledge?tab=$tab"
    fun bellyEdit(id: Long = -1) = "belly/edit?id=$id"
    fun myRemedyEdit(id: Long = -1, remedyId: String = "") = "myremedy/edit?id=$id&remedy=$remedyId"
    fun remedy(id: String) = "remedy/$id"
    fun lesson(id: String) = "lesson/$id"
}

private data class Tab(val route: String, val label: String, val icon: Int) {
    fun matches(current: String) = current == route || current.startsWith("$route?")
}

private val tabRoutes = listOf(Routes.HOME, Routes.CHECKUPS, Routes.DIARY, Routes.SHOPPING, Routes.KNOWLEDGE)

/**
 * 切换底部/侧边导航的一级页面。
 * 首页是返回栈的根：回首页时直接弹出其上的页面（保存它们的状态以便再次切回），
 * 而不是 navigate + restoreState——否则非 tab 方式压在首页上的页面会被当成首页的状态恢复回来。
 */
fun NavController.switchTab(route: String) {
    val base = route.substringBefore('?')
    if (base == Routes.HOME) {
        if (!popBackStack(Routes.HOME, inclusive = false, saveState = true)) {
            navigate(Routes.HOME) { popUpTo(graph.id) { inclusive = true } }
        }
        return
    }
    val explicitArgs = route.contains('?')
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = !explicitArgs }
        launchSingleTop = true
        restoreState = !explicitArgs
    }
}

/** 首页等处的入口：一级页面按 tab 切换，其余页面正常压栈。 */
fun NavController.open(route: String) {
    if (route.substringBefore('?') in tabRoutes) switchTab(route) else navigate(route)
}

private val tabs = listOf(
    Tab(Routes.HOME, "首页", R.drawable.home_tab_home),
    Tab(Routes.CHECKUPS, "产检", R.drawable.home_tab_checkup),
    Tab(Routes.DIARY, "日记", R.drawable.home_tab_diary),
    Tab(Routes.SHOPPING, "购物", R.drawable.home_tab_shopping),
    Tab(Routes.KNOWLEDGE, "百科", R.drawable.home_tab_knowledge),
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
    val settings by app().settings.state.collectAsState()
    if (!settings.onboarded) {
        OnboardingScreen(onDone = {})
        return
    }
    val nav = rememberNavController()
    val layout = LocalWindowLayout.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val topLevel = tabs.any { it.matches(route) }
    val showRail = layout.expanded && route.isNotEmpty()
    val showBottom = !layout.expanded && topLevel

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottom) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = tab.matches(route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Image(painterResource(tab.icon), null, Modifier.size(26.dp)) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.ui.graphics.Color(0xFFF8D0D8)),
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
                        onClick = { nav.navigate(Routes.CHECKIN) { launchSingleTop = true } },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ) { Text("✅") }
                    Spacer(Modifier.height(20.dp))
                    Column {
                        tabs.forEach { tab ->
                            val selected = tab.matches(route)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { nav.switchTab(tab.route) },
                                icon = { Image(painterResource(tab.icon), null, Modifier.size(26.dp)) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(indicatorColor = androidx.compose.ui.graphics.Color(0xFFF8D0D8)),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            CompositionLocalProvider(LocalBottomBarShown provides showBottom) {
                AppNavHost(nav, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, modifier: Modifier) {
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(nav, startDestination = Routes.HOME, modifier = modifier) {
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
        composable("shopping/view/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            ShoppingDetailScreen(it.arguments?.getLong("id") ?: -1L, nav)
        }
        composable(
            "shopping/edit?id={id}&status={status}&ai={ai}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("status") { type = NavType.IntType; defaultValue = 0 },
                navArgument("ai") { type = NavType.BoolType; defaultValue = false },
            ),
        ) {
            ShoppingEditScreen(
                it.arguments?.getLong("id") ?: -1L, it.arguments?.getInt("status") ?: 0,
                autoAi = it.arguments?.getBoolean("ai") ?: false, onDone = back,
            )
        }
        composable(Routes.BELLY) { BellyScreen(nav) }
        composable(
            "belly/edit?id={id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) { BellyEditScreen(it.arguments?.getLong("id") ?: -1L, onDone = back) }
        composable(
            "myremedy/edit?id={id}&remedy={remedy}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("remedy") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { MyRemedyEditScreen(it.arguments?.getLong("id") ?: -1L, it.arguments?.getString("remedy").orEmpty(), onDone = back) }
        composable(Routes.CHECKIN) { CheckinScreen(onBack = back) }
        composable(
            "knowledge?tab={tab}",
            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 }),
        ) { KnowledgeScreen(nav, it.arguments?.getInt("tab") ?: 0) }
        composable("remedy/{id}") { RemedyDetailScreen(it.arguments?.getString("id").orEmpty(), nav, onBack = back) }
        composable("lesson/{id}") { LessonDetailScreen(it.arguments?.getString("id").orEmpty(), onBack = back) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
    }
}
