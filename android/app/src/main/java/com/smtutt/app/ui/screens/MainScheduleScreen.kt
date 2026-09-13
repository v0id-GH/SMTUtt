package com.smtutt.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smtutt.app.data.local.FavoriteEntity
import com.smtutt.app.data.model.Lesson
import com.smtutt.app.data.model.ScheduleResponse
import com.smtutt.app.data.parser.SmtuWebParser
import com.smtutt.app.data.repository.Resource
import com.smtutt.app.ui.theme.*
import com.smtutt.app.ui.viewmodel.ScheduleUiState
import com.smtutt.app.ui.viewmodel.ScheduleViewModel
import com.smtutt.app.ui.viewmodel.WeekFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScheduleScreen(
    viewModel: ScheduleViewModel,
    onNavigateToGroups: () -> Unit,
    onNavigateToTeachers: () -> Unit,
    onNavigateToTeacherSchedule: (teacherId: String, teacherName: String) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState(initial = emptyList())
    val userMessage by viewModel.userMessage.collectAsState()
    val colors = LocalAppColors.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show toast / snackbar on userMessage
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = colors.surface
            ) {
                ScheduleDrawerContent(
                    favorites = favorites,
                    currentId = uiState.id,
                    currentWeek = uiState.detectedCurrentWeek,
                    onSelectFavorite = { fav ->
                        viewModel.loadSchedule(fav.id, fav.isTeacher)
                        scope.launch { drawerState.close() }
                    },
                    onOpenGroups = {
                        scope.launch { drawerState.close() }
                        onNavigateToGroups()
                    },
                    onOpenTeachers = {
                        scope.launch { drawerState.close() }
                        onNavigateToTeachers()
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Меню выбора расписания",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = if (uiState.title.isNotBlank()) uiState.title else "Расписание СПбГМТУ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.detectedCurrentWeek != WeekFilter.ALL) {
                                    Text(
                                        text = "Сейчас: ${uiState.detectedCurrentWeek.title} нед. • ",
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (uiState.isOfflineAvailable) {
                                    Text(
                                        text = "Офлайн-кэш",
                                        color = colors.practiceText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Онлайн",
                                        color = colors.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (uiState.id.isNotBlank()) {
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Избранное",
                                    tint = if (uiState.isFavorite) colors.primary else colors.textSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.saveOffline() }) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Сохранить офлайн",
                                    tint = colors.textSecondary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.loadSchedule(uiState.id, uiState.isTeacher, forceRefresh = true)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Обновить",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(innerPadding)
            ) {
                if (uiState.id.isBlank()) {
                    EmptySelectionCard(onSelectGroupClick = onNavigateToGroups)
                } else {
                    when (val resource = uiState.scheduleResource) {
                        is Resource.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colors.primary)
                            }
                        }
                        is Resource.Error -> {
                            ErrorCard(
                                message = resource.message,
                                onRetry = { viewModel.loadSchedule(uiState.id, uiState.isTeacher, forceRefresh = true) }
                            )
                        }
                        is Resource.Success -> {
                            ScheduleContent(
                                schedule = resource.data,
                                uiState = uiState,
                                onSelectDay = { viewModel.selectDay(it) },
                                onSelectWeek = { viewModel.setWeekFilter(it) },
                                onTeacherClick = onNavigateToTeacherSchedule
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleDrawerContent(
    favorites: List<FavoriteEntity>,
    currentId: String,
    currentWeek: WeekFilter,
    onSelectFavorite: (FavoriteEntity) -> Unit,
    onOpenGroups: () -> Unit,
    onOpenTeachers: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val currentThemeMode by ThemePreferences.themeModeState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(vertical = 16.dp)
    ) {
        // Drawer Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "СПбГМТУ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Выбор расписания",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current week badge
            if (currentWeek != WeekFilter.ALL) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentWeek == WeekFilter.UPPER) colors.upperWeekBg else colors.lowerWeekBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "● Сейчас идёт ${currentWeek.title.lowercase()} неделя",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentWeek == WeekFilter.UPPER) colors.upperWeekText else colors.lowerWeekText
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 10.dp), color = colors.border)

        // Section: Saved Schedules
        Text(
            text = "МОИ РАСПИСАНИЯ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Нет сохранённых расписаний.\nДобавьте группу через кнопку ниже.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                items(favorites) { item ->
                    val isSelected = item.id == currentId
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (item.isTeacher) Icons.Filled.Person else Icons.Filled.Group,
                                contentDescription = null,
                                tint = if (isSelected) colors.primary else colors.textSecondary
                            )
                        },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) colors.primary else colors.textPrimary,
                                    maxLines = 1
                                )
                                if (item.isDefault) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Основная",
                                        tint = Color(0xFFEAB308),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = { onSelectFavorite(item) },
                        shape = RoundedCornerShape(10.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = colors.primaryContainer.copy(alpha = 0.35f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = colors.border)

        // Theme Switcher Button in Drawer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { showThemeDialog = true },
            color = colors.surfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (currentThemeMode) {
                            AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                            AppThemeMode.LIGHT -> Icons.Filled.LightMode
                            AppThemeMode.DARK -> Icons.Filled.DarkMode
                        },
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Тема оформления",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = currentThemeMode.title,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action buttons
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            OutlinedButton(
                onClick = onOpenGroups,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выбрать другую группу", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onOpenTeachers,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Filled.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Найти преподавателя", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Все настройки", fontSize = 13.sp)
            }
        }

        // Theme Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Выберите тему оформления", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppThemeMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        ThemePreferences.setThemeMode(context, mode)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentThemeMode == mode,
                                    onClick = {
                                        ThemePreferences.setThemeMode(context, mode)
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mode.title,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Закрыть", color = colors.primary)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun ScheduleContent(
    schedule: ScheduleResponse,
    uiState: ScheduleUiState,
    onSelectDay: (Int) -> Unit,
    onSelectWeek: (WeekFilter) -> Unit,
    onTeacherClick: (String, String) -> Unit
) {
    val colors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Week filter selector
        WeekSelector(
            currentFilter = uiState.weekFilter,
            detectedCurrentWeek = uiState.detectedCurrentWeek,
            onSelectFilter = onSelectWeek
        )

        if (schedule.days.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Расписание не найдено для выбранного объекта", color = colors.textSecondary)
            }
            return
        }

        // Days tabs
        val days = schedule.days
        val activeIndex = uiState.selectedDayIndex.coerceIn(0, days.size - 1)

        ScrollableTabRow(
            selectedTabIndex = activeIndex,
            edgePadding = 16.dp,
            containerColor = colors.surface,
            contentColor = colors.primary
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = activeIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { onSelectDay(index) },
                    text = {
                        Text(
                            text = day.dayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isSelected) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        // Selected Day Lessons
        val currentDay = days[activeIndex]
        val filteredLessons = currentDay.lessons.filter { lesson ->
            when (uiState.weekFilter) {
                WeekFilter.ALL -> true
                WeekFilter.UPPER -> lesson.weekType.contains("верхняя", ignoreCase = true) || lesson.weekType.contains("обе", ignoreCase = true) || lesson.weekType.isBlank()
                WeekFilter.LOWER -> lesson.weekType.contains("нижняя", ignoreCase = true) || lesson.weekType.contains("обе", ignoreCase = true) || lesson.weekType.isBlank()
            }
        }

        if (filteredLessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.EventBusy,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "В этот день пар нет (${uiState.weekFilter.title.lowercase()} неделя)",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLessons) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        isTeacherView = schedule.isTeacher,
                        onTeacherClick = onTeacherClick
                    )
                }
            }
        }
    }
}

@Composable
fun WeekSelector(
    currentFilter: WeekFilter,
    detectedCurrentWeek: WeekFilter,
    onSelectFilter: (WeekFilter) -> Unit
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeekFilter.values().forEach { filter ->
            val isSelected = currentFilter == filter
            val isCurrentWeek = filter == detectedCurrentWeek && filter != WeekFilter.ALL

            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(filter) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = filter.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isCurrentWeek) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• сейчас",
                                fontSize = 10.sp,
                                color = if (isSelected) colors.primary else colors.lowerWeekText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primaryContainer.copy(alpha = 0.5f),
                    selectedLabelColor = colors.textPrimary,
                    containerColor = colors.surface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) colors.primary else colors.border
                )
            )
        }
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    isTeacherView: Boolean,
    onTeacherClick: (String, String) -> Unit
) {
    val colors = LocalAppColors.current

    val cleanSubject = remember(lesson.subject) {
        SmtuWebParser.cleanSubject(lesson.subject).ifBlank { lesson.subject }
    }
    val effectiveLessonType = remember(lesson.lessonType, lesson.subject) {
        lesson.lessonType ?: SmtuWebParser.extractLessonType(lesson.subject)
    }
    val effectiveSubgroup = remember(lesson.subgroup, lesson.subject) {
        lesson.subgroup ?: SmtuWebParser.extractSubgroup(lesson.subject)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: Time & Week badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(colors.timeBg, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.timeBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = colors.timeText
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = lesson.time,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.timeText
                    )
                }

                // Week badge
                if (lesson.weekType.isNotBlank()) {
                    val isUpper = lesson.weekType.contains("верхняя", ignoreCase = true)
                    Text(
                        text = lesson.weekType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUpper) colors.upperWeekText else colors.lowerWeekText,
                        modifier = Modifier
                            .background(
                                if (isUpper) colors.upperWeekBg else colors.lowerWeekBg,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject Title (Cleaned from lesson type suffix)
            Text(
                text = cleanSubject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 16.sp
            )

            // Lesson Type & Subgroup Badges
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                effectiveLessonType?.let { type ->
                    val (textColor, bgColor, borderColor) = when {
                        type.contains("Лекция", ignoreCase = true) -> Triple(colors.lectureText, colors.lectureBg, colors.lectureBorder)
                        type.contains("Практик", ignoreCase = true) -> Triple(colors.practiceText, colors.practiceBg, colors.practiceBorder)
                        type.contains("Лаборатор", ignoreCase = true) -> Triple(colors.labText, colors.labBg, colors.labBorder)
                        else -> Triple(colors.otherText, colors.otherBg, colors.otherBorder)
                    }
                    Text(
                        text = type,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(bgColor, shape = RoundedCornerShape(6.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                effectiveSubgroup?.let { sg ->
                    Text(
                        text = sg,
                        color = colors.subgroupText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(colors.subgroupBg, shape = RoundedCornerShape(6.dp))
                            .border(1.dp, colors.subgroupBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = colors.border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Classroom / Campus
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (lesson.classroom.isNotBlank()) lesson.classroom else "Аудитория не указана",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
            }

            // Teacher or Group link
            if (!isTeacherView && lesson.teacherName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTeacherClick(lesson.teacherId, lesson.teacherName) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lesson.teacherName,
                        fontSize = 13.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (isTeacherView && lesson.group.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Группа: ${lesson.group}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }

            // Dates range if present
            if (lesson.dates.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = colors.textMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lesson.dates,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySelectionCard(onSelectGroupClick: () -> Unit) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colors.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Выберите учебную группу",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Откройте боковое меню слева (≡) или нажмите кнопку ниже, чтобы найти свою группу.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSelectGroupClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать группу")
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Повторить")
            }
        }
    }
}
