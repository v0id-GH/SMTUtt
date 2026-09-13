package com.smtutt.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smtutt.app.ui.screens.FavoritesScreen
import com.smtutt.app.ui.screens.GroupSelectScreen
import com.smtutt.app.ui.screens.MainScheduleScreen
import com.smtutt.app.ui.screens.SettingsScreen
import com.smtutt.app.ui.screens.TeacherSearchScreen
import com.smtutt.app.ui.theme.LocalAppColors
import com.smtutt.app.ui.theme.SmtuTheme
import com.smtutt.app.ui.theme.ThemePreferences
import com.smtutt.app.ui.viewmodel.ScheduleViewModel
import com.smtutt.app.ui.viewmodel.TeacherSearchViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Schedule : Screen("schedule", "Расписание", Icons.Filled.CalendarToday)
    object Groups : Screen("groups", "Группы", Icons.Filled.School)
    object Teachers : Screen("teachers", "Персоналии", Icons.Filled.PersonSearch)
    object Favorites : Screen("favorites", "Избранное", Icons.Filled.Bookmark)
    object Settings : Screen("settings", "Настройки", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SmtuApplication
        val repository = app.repository
        ThemePreferences.init(this)

        setContent {
            val themeMode by ThemePreferences.themeModeState.collectAsState()
            SmtuTheme(themeMode = themeMode) {
                val colors = LocalAppColors.current
                val navController = rememberNavController()
                val scheduleViewModel: ScheduleViewModel = viewModel {
                    ScheduleViewModel(app)
                }
                val teacherSearchViewModel: TeacherSearchViewModel = viewModel {
                    TeacherSearchViewModel(repository)
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val context = LocalContext.current
                val activity = context as? Activity
                var lastBackPressTime by remember { mutableStateOf(0L) }

                BackHandler {
                    if (currentRoute != Screen.Schedule.route) {
                        val popped = navController.popBackStack(Screen.Schedule.route, inclusive = false)
                        if (!popped) {
                            navController.navigate(Screen.Schedule.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        scheduleViewModel.returnToDefaultOrLastFavorite()
                        lastBackPressTime = 0L
                    } else if (!scheduleViewModel.isViewingDefaultOrFavorite()) {
                        scheduleViewModel.returnToDefaultOrLastFavorite()
                        lastBackPressTime = 0L
                    } else {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000L) {
                            activity?.finish()
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(context, "Нажмите назад ещё раз, чтобы выйти", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = colors.surface
                        ) {
                            val items = listOf(
                                Screen.Schedule,
                                Screen.Groups,
                                Screen.Teachers,
                                Screen.Favorites,
                                Screen.Settings
                            )
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title, fontSize = 10.sp, maxLines = 1) },
                                    selected = currentRoute == screen.route,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.primary,
                                        selectedTextColor = colors.primary,
                                        indicatorColor = colors.primaryContainer.copy(alpha = 0.35f),
                                        unselectedIconColor = colors.textSecondary,
                                        unselectedTextColor = colors.textSecondary
                                    ),
                                    onClick = {
                                        if (screen == Screen.Schedule) {
                                            if (currentRoute != Screen.Schedule.route) {
                                                val popped =
                                                    navController.popBackStack(Screen.Schedule.route, inclusive = false)
                                                if (!popped) {
                                                    navController.navigate(Screen.Schedule.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        } else {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Schedule.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(Screen.Schedule.route) {
                            MainScheduleScreen(
                                viewModel = scheduleViewModel,
                                onNavigateToGroups = {
                                    navController.navigate(Screen.Groups.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToTeachers = {
                                    navController.navigate(Screen.Teachers.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToTeacherSchedule = { teacherId, _ ->
                                    scheduleViewModel.loadSchedule(teacherId, isTeacher = true)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(Screen.Groups.route) {
                            GroupSelectScreen(
                                repository = repository,
                                onGroupSelected = { groupId, groupName ->
                                    scheduleViewModel.setDefaultGroup(groupId, groupName)
                                    navController.navigate(Screen.Schedule.route) {
                                        popUpTo(Screen.Schedule.route) { inclusive = true }
                                    }
                                },
                                onNavigateBack = {
                                    val popped = navController.popBackStack(Screen.Schedule.route, inclusive = false)
                                    if (!popped) {
                                        navController.navigate(Screen.Schedule.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    scheduleViewModel.returnToDefaultOrLastFavorite()
                                }
                            )
                        }

                        composable(Screen.Teachers.route) {
                            TeacherSearchScreen(
                                viewModel = teacherSearchViewModel,
                                onTeacherSelected = { teacherId, teacherName ->
                                    scheduleViewModel.loadSchedule(teacherId, isTeacher = true)
                                    navController.navigate(Screen.Schedule.route)
                                }
                            )
                        }

                        composable(Screen.Favorites.route) {
                            FavoritesScreen(
                                repository = repository,
                                onItemSelected = { id, isTeacher ->
                                    scheduleViewModel.loadSchedule(id, isTeacher)
                                    navController.navigate(Screen.Schedule.route)
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                repository = repository
                            )
                        }
                    }
                }
            }
        }
    }
}
