package com.graceofgod.zstore.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.graceofgod.zstore.AppContainer
import com.graceofgod.zstore.data.Repository
import com.graceofgod.zstore.data.User
import com.graceofgod.zstore.install.InstallManager
import com.graceofgod.zstore.ui.theme.Z
import com.graceofgod.zstore.util.Pkg
import kotlinx.coroutines.flow.Flow

/** Ce dont les écrans ont besoin. */
data class Deps(
    val repo: Repository,
    val installer: InstallManager,
    val user: User?,
) {
    val isAdmin: Boolean get() = user?.isAdmin == true
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun ZStoreRoot(container: AppContainer, openRequests: Flow<String>) {
    val user by container.session.user.collectAsState()
    val deps = Deps(container.repository, container.installer, user)
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { container.repository.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) {
        container.repository.refreshMe()
        openRequests.collect { route -> nav.navigate(route) { launchSingleTop = true } }
    }

    val catalog by container.repository.catalog.collectAsState()
    val tick by container.installer.installedTick.collectAsState()
    val updateCount = remember(catalog, tick) {
        val installed = Pkg.installedMap(context, catalog.map { it.packageName })
        catalog.count { a -> a.owned && installed[a.packageName]?.let { (a.versionCode ?: 0L) > it } == true }
    }

    val tabs = buildList {
        add(Tab("store", "Store", Icons.Filled.Storefront))
        add(Tab("library", "Mes apps", Icons.Filled.Apps))
        if (deps.isAdmin) add(Tab("admin", "Admin", Icons.Filled.AdminPanelSettings))
        add(Tab("account", "Compte", Icons.Filled.Person))
    }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = tabs.any { it.route == current }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Z.Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Z.Surface) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = current == t.route,
                            onClick = { nav.goTab(t.route) },
                            icon = {
                                if (t.route == "library" && updateCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$updateCount") } }) { Icon(t.icon, null) }
                                } else Icon(t.icon, null)
                            },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Z.OnLime,
                                indicatorColor = Z.Lime,
                                selectedTextColor = Z.Text,
                                unselectedIconColor = Z.Muted,
                                unselectedTextColor = Z.Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "store",
            modifier = Modifier
                .padding(pad)
                .statusBarsPaddingIf(showBar)
                .imePadding(),
        ) {
            composable("store") { StoreScreen(deps, onOpenApp = { nav.navigate("app/$it") }) }
            composable("library") { LibraryScreen(deps, onOpenApp = { nav.navigate("app/$it") }) }
            composable("account") { AccountScreen(deps) }
            composable("login") {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                    Text("Connexion", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(vertical = 12.dp))
                    AuthForm(deps, onDone = { nav.popBackStack() })
                }
            }
            composable("app/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                val id = e.arguments?.getInt("id") ?: 0
                AppDetailScreen(
                    deps, id,
                    onBack = { nav.popBackStack() },
                    onLogin = { nav.navigate("login") },
                    onEdit = { nav.navigate("admin/app/$it") },
                )
            }

            // ---- Admin ----
            composable("admin") {
                if (!deps.isAdmin) {
                    CenterMessage("Accès réservé", "Connecte-toi avec le compte admin.")
                } else {
                    AdminScreen(
                        deps,
                        onOpenAppAdmin = { nav.navigate("admin/app/$it") },
                        onUpload = { nav.navigate("admin/upload") },
                        onNewBuild = { nav.navigate("admin/build/new") },
                        onOpenBuild = { nav.navigate("admin/build/$it") },
                        onNewAi = { nav.navigate("admin/ai/new") },
                        onOpenAiJob = { nav.navigate("admin/ai/$it") },
                    )
                }
            }
            composable(
                "admin/upload?appId={appId}",
                listOf(navArgument("appId") { type = NavType.IntType; defaultValue = -1 }),
            ) { e ->
                val appId = e.arguments?.getInt("appId")?.takeIf { it > 0 }
                UploadApkScreen(
                    deps, appId,
                    onBack = { nav.popBackStack() },
                    onDone = { id -> nav.navigate("admin/app/$id") { popUpTo("admin") } },
                )
            }
            composable(
                "admin/build/new?appId={appId}",
                listOf(navArgument("appId") { type = NavType.IntType; defaultValue = -1 }),
            ) { e ->
                val appId = e.arguments?.getInt("appId")?.takeIf { it > 0 }
                NewBuildScreen(
                    deps, appId,
                    onBack = { nav.popBackStack() },
                    onStarted = { id -> nav.navigate("admin/build/$id") { popUpTo("admin") } },
                )
            }
            composable("admin/build/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                val id = e.arguments?.getInt("id") ?: 0
                BuildDetailScreen(
                    deps, id,
                    onBack = { nav.popBackStack() },
                    onOpenApp = { nav.navigate("admin/app/$it") },
                    onRebuild = { appId -> nav.navigate("admin/build/new" + (appId?.let { "?appId=$it" } ?: "")) },
                    onFixWithAi = { bid -> nav.navigate("admin/ai/new?buildId=$bid") },
                )
            }
            composable("admin/app/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                val id = e.arguments?.getInt("id") ?: 0
                AdminAppEditScreen(
                    deps, id,
                    onBack = { nav.popBackStack() },
                    onNewApk = { nav.navigate("admin/upload?appId=$it") },
                    onNewBuild = { nav.navigate("admin/build/new?appId=$it") },
                    onDeleted = { nav.popBackStack("admin", inclusive = false) },
                    onEditWithAi = { nav.navigate("admin/ai/new?appId=$it") },
                )
            }
            composable(
                "admin/ai/new?appId={appId}&buildId={buildId}",
                listOf(
                    navArgument("appId") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("buildId") { type = NavType.IntType; defaultValue = -1 },
                ),
            ) { e ->
                AiNewScreen(
                    deps,
                    appId = e.arguments?.getInt("appId")?.takeIf { it > 0 },
                    buildId = e.arguments?.getInt("buildId")?.takeIf { it > 0 },
                    onBack = { nav.popBackStack() },
                    onStarted = { id -> nav.navigate("admin/ai/$id") { popUpTo("admin") } },
                )
            }
            composable("admin/ai/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                AiJobScreen(
                    deps, e.arguments?.getInt("id") ?: 0,
                    onBack = { nav.popBackStack() },
                    onOpenBuild = { nav.navigate("admin/build/$it") },
                    onFix = { bid -> nav.navigate("admin/ai/new?buildId=$bid") },
                )
            }
        }
    }
}

private fun Modifier.statusBarsPaddingIf(apply: Boolean): Modifier = if (apply) statusBarsPadding() else this

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
