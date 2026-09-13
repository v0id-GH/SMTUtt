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
import com.smtutt.app.data.local.FavoriteEntity
import com.smtutt.app.data.repository.ScheduleRepository
import com.smtutt.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    repository: ScheduleRepository,
    onItemSelected: (id: String, isTeacher: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val favorites by repository.getFavorites().collectAsState(initial = emptyList())
    val colors = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Сохранённые и Избранное",
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
            if (favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "У вас пока нет сохранённых расписаний",
                            color = colors.textSecondary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Нажмите значок закладки или скачивания на экране расписания",
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    items(favorites) { fav ->
                        FavoriteItemCard(
                            item = fav,
                            onClick = { onItemSelected(fav.id, fav.isTeacher) },
                            onDelete = {
                                coroutineScope.launch {
                                    repository.toggleFavorite(fav.id, fav.name, fav.isTeacher)
                                }
                            },
                            onSetDefault = {
                                coroutineScope.launch {
                                    repository.setDefaultGroup(fav.id, fav.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItemCard(
    item: FavoriteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
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
                    imageVector = if (item.isTeacher) Icons.Filled.Person else Icons.Filled.Group,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        if (item.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = colors.primaryContainer) {
                                Text(
                                    text = "Основная",
                                    color = colors.onPrimaryContainer,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = if (item.isTeacher) "Преподаватель" else "Группа",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.isTeacher && !item.isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(
                            imageVector = Icons.Filled.StarBorder,
                            contentDescription = "Сделать основной",
                            tint = colors.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

