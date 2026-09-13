package com.smtutt.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smtutt.app.data.model.FacultyInfo
import com.smtutt.app.data.model.GroupInfo
import com.smtutt.app.data.repository.ScheduleRepository
import com.smtutt.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectScreen(
    repository: ScheduleRepository,
    onGroupSelected: (groupId: String, groupName: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    var faculties by remember { mutableStateOf<List<FacultyInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            faculties = repository.getFaculties()
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Ошибка загрузки списка групп"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Выбор группы",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск группы (например, 10274)", color = colors.textSecondary) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = colors.primary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Очистить", tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        faculties = repository.getFaculties(forceRefresh = true)
                                        isLoading = false
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Повторить")
                        }
                    }
                }
            } else {
                // If search query active, flatten groups and filter
                if (searchQuery.isNotBlank()) {
                    val filteredGroups = remember(searchQuery, faculties) {
                        val q = searchQuery.trim().lowercase()
                        faculties.flatMap { f ->
                            f.courses.flatMap { c ->
                                c.groups.map { g -> Triple(g, f.faculty, c.course) }
                            }
                        }.filter { it.first.name.lowercase().contains(q) }
                    }

                    if (filteredGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Группа '$searchQuery' не найдена", color = colors.textSecondary)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGroups) { (group, fac, course) ->
                                GroupItemCard(
                                    group = group,
                                    subtitle = "$fac • $course",
                                    onClick = { onGroupSelected(group.id, group.name) }
                                )
                            }
                        }
                    }
                } else {
                    // Faculty Accordion / List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(faculties) { faculty ->
                            FacultyCard(
                                faculty = faculty,
                                onGroupSelected = onGroupSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FacultyCard(
    faculty: FacultyInfo,
    onGroupSelected: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = faculty.faculty,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.textPrimary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = colors.border)
                Spacer(modifier = Modifier.height(8.dp))

                faculty.courses.forEach { course ->
                    Text(
                        text = course.course,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Flow of groups
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        course.groups.chunked(3).forEach { rowGroups ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowGroups.forEach { grp ->
                                    OutlinedButton(
                                        onClick = { onGroupSelected(grp.id, grp.name) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = colors.textPrimary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = grp.name, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                                // Fill remainder if less than 3
                                repeat(3 - rowGroups.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun GroupItemCard(
    group: GroupInfo,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Группа ${group.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary
            )
        }
    }
}

