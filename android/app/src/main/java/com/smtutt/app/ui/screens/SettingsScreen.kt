package com.smtutt.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smtutt.app.data.repository.ScheduleRepository
import com.smtutt.app.ui.theme.AppThemeMode
import com.smtutt.app.ui.theme.LocalAppColors
import com.smtutt.app.ui.theme.ThemePreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: ScheduleRepository
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val currentThemeMode by ThemePreferences.themeModeState.collectAsState()
    val autoRefreshEnabled by ThemePreferences.autoRefreshState.collectAsState()
    val scope = rememberCoroutineScope()
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Theme Settings Section
            SettingsSectionHeader(
                icon = Icons.Filled.Palette,
                title = "Тема оформления"
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Выберите внешний вид приложения в стиле редактора VS Code:",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    AppThemeOptionItem(
                        title = "Системная (автоматически)",
                        subtitle = "Следовать теме Android (автоматическое переключение)",
                        icon = Icons.Filled.BrightnessAuto,
                        isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                        onClick = { ThemePreferences.setThemeMode(context, AppThemeMode.SYSTEM) }
                    )

                    AppThemeOptionItem(
                        title = "Тёмная тема (VS Code Dark+)",
                        subtitle = "Тёмный фон #1E1E1E, синтаксическая подсветка",
                        icon = Icons.Filled.DarkMode,
                        isSelected = currentThemeMode == AppThemeMode.DARK,
                        onClick = { ThemePreferences.setThemeMode(context, AppThemeMode.DARK) }
                    )

                    AppThemeOptionItem(
                        title = "Светлая тема (VS Code Light+)",
                        subtitle = "Светлый редактор #F3F3F3, контрастный строгий стиль",
                        icon = Icons.Filled.LightMode,
                        isSelected = currentThemeMode == AppThemeMode.LIGHT,
                        onClick = { ThemePreferences.setThemeMode(context, AppThemeMode.LIGHT) }
                    )
                }
            }

            // 2. Sync & Schedule Section
            SettingsSectionHeader(
                icon = Icons.Filled.Sync,
                title = "Синхронизация и расписание"
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Toggle row for auto-refresh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudSync,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Автообновление при запуске",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (autoRefreshEnabled)
                                        "Включено: приложение проверяет изменения на smtu.ru при каждом старте"
                                    else
                                        "Выключено: используется только сохранённая оффлайн-версия",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoRefreshEnabled,
                            onCheckedChange = { isChecked ->
                                ThemePreferences.setAutoRefreshEnabled(context, isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.surface,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    Divider(color = colors.border)

                    InfoSettingRow(
                        icon = Icons.Filled.CalendarMonth,
                        title = "Определение недели (Верхняя / Нижняя)",
                        description = "Чётность недели считывается напрямую с сайта вуза и переключается автоматически каждый понедельник в 00:00 (МСК)."
                    )
                }
            }

            // 3. Storage & Cache Section
            SettingsSectionHeader(
                icon = Icons.Filled.Storage,
                title = "Данные и память"
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Очистить локальный кэш",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Удалить сохранённые копии расписаний для освобождения места",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { showClearCacheDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Очистить", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. About App Section
            SettingsSectionHeader(
                icon = Icons.Filled.Info,
                title = "О приложении"
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Расписание СПбГМТУ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Корабелка • Версия 1.1.0",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "100% автономное приложение: прямой парсинг расписания сайта smtu.ru, локальная база Room SQLite и поддержка оффлайн-доступа.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Confirmation Dialog for Cache Clear
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = {
                    Text(
                        text = "Очистить кэш расписаний?",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Все сохранённые для оффлайн-доступа расписания будут удалены. Избранные группы останутся в списке.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearCacheDialog = false
                            scope.launch {
                                repository.clearCache()
                                Toast.makeText(context, "Кэш расписаний успешно очищен", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("Отмена", color = colors.primary)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun AppThemeOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) colors.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.primary else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) colors.primary else colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isSelected) colors.primary else colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
            )
        }
    }
}

@Composable
private fun InfoSettingRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
