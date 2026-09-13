package com.smtutt.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smtutt.app.data.model.TeacherSearchResult
import com.smtutt.app.ui.theme.LocalAppColors
import com.smtutt.app.ui.viewmodel.TeacherSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSearchScreen(
    viewModel: TeacherSearchViewModel,
    onTeacherSelected: (teacherId: String, teacherName: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Поиск преподавателя",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
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
            // Search Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Фамилия (например, Альбаев)", color = colors.textSecondary) },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = colors.primary) },
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
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.search() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Поиск")
                }
            }

            uiState.errorMessage?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (uiState.hasSearched && uiState.results.isEmpty() && uiState.errorMessage == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Преподаватели по запросу '${uiState.query}' не найдены",
                        color = colors.textSecondary
                    )
                }
            } else if (!uiState.hasSearched && uiState.results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Введите фамилию преподавателя для поиска",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                ) {
                    items(uiState.results) { teacher ->
                        TeacherItemCard(
                            teacher = teacher,
                            onClick = { onTeacherSelected(teacher.id, teacher.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherItemCard(
    teacher: TeacherSearchResult,
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = teacher.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "ID: ${teacher.id}",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary
            )
        }
    }
}

