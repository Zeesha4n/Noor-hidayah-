package com.noorehidayah.quran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.data.model.FiqhSchool
import com.noorehidayah.quran.ui.i18n.AppStrings
import com.noorehidayah.quran.ui.screens.ChatAssistantScreen
import com.noorehidayah.quran.ui.screens.HadithAndDuasScreen
import com.noorehidayah.quran.ui.screens.HomeSearchScreen
import com.noorehidayah.quran.ui.screens.QuranScreen
import com.noorehidayah.quran.ui.screens.SettingsScreen
import com.noorehidayah.quran.ui.screens.ToolsScreen
import com.noorehidayah.quran.ui.theme.NoorHidayahTheme
import com.noorehidayah.quran.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            NoorHidayahApp(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoorHidayahApp(vm: MainViewModel) {
    val prefs by vm.preferences.collectAsState()
    val uiState by vm.uiState.collectAsState()
    val strings = remember(prefs.language) { AppStrings.forLanguage(prefs.language) }
    var activeTab by remember { mutableIntStateOf(0) }
    var showChat by remember { mutableStateOf(false) }

    NoorHidayahTheme(darkTheme = prefs.darkMode) {
        if (!prefs.onboardingCompleted) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Assalamu Alaikum • Noor-e-Hidayah",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "100% Offline Quran, Multi-Fiqh Tafseer, Hadith, Duas, Namaz Waqt, Qibla Compass & Hijri Calendar.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("Apni Zubaan (Language) Chunein:", fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = prefs.language == lang,
                                    onClick = { vm.setLanguage(lang) },
                                    label = { Text(lang.displayName) }
                                )
                            }
                        }
                        Text("Default Fiqh Chunein:", fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FiqhSchool.entries.forEach { fiqh ->
                                FilterChip(
                                    selected = prefs.defaultFiqh == fiqh,
                                    onClick = { vm.setDefaultFiqh(fiqh) },
                                    label = { Text(fiqh.displayName) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = vm::completeOnboarding,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Shuru Karein", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return@NoorHidayahTheme
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.appTitle, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        IconButton(onClick = { showChat = !showChat }) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Noor Guide Chat",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        TextButton(onClick = {
                            val entries = AppLanguage.entries
                            val next = entries[(prefs.language.ordinal + 1) % entries.size]
                            vm.setLanguage(next)
                        }) {
                            Text(
                                text = prefs.language.shortLabel,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        label = { Text(strings.tabQuran) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text("Hadith") }
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        label = { Text(strings.tabTools) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(strings.tabSettings) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!uiState.isReady) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.startupError != null) {
                    Text(
                        text = "Startup Error:\n${uiState.startupError}",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (showChat) {
                    ChatAssistantScreen(
                        repository = vm.repo,
                        surahs = uiState.surahs,
                        prefs = prefs,
                        onBack = { showChat = false }
                    )
                } else {
                    when (activeTab) {
                        0 -> HomeSearchScreen(
                            uiState = uiState,
                            prefs = prefs,
                            strings = strings,
                            onQueryChange = vm::onSearchQueryChange,
                            onCategoryChange = vm::onSearchCategoryChange,
                            onOpenSurah = { surahNum ->
                                vm.openSurah(surahNum)
                                activeTab = 1
                            },
                            onToggleBookmark = vm::toggleBookmark,
                            onToggleHadithBookmark = vm::toggleHadithBookmark
                        )
                        1 -> QuranScreen(
                            uiState = uiState,
                            prefs = prefs,
                            strings = strings,
                            onOpenSurah = vm::openSurah,
                            onCloseReader = vm::closeSurahReader,
                            onToggleBookmark = vm::toggleBookmark
                        )
                        2 -> HadithAndDuasScreen(
                            hadiths = uiState.hadiths,
                            duas = uiState.duas,
                            prefs = prefs,
                            onToggleHadithBookmark = vm::toggleHadithBookmark,
                            onToggleDuaBookmark = vm::toggleDuaBookmark
                        )
                        3 -> ToolsScreen(
                            prefs = prefs,
                            strings = strings,
                            onIncrementTasbih = vm::incrementTasbih,
                            onResetTasbih = vm::resetTasbih,
                            onToggleTasbihSound = vm::toggleTasbihSound,
                            onSelectTasbihPreset = vm::setTasbihPreset,
                            onSelectCity = vm::setCityIndex,
                            onSelectAsrMethod = vm::setAsrMethod
                        )
                        4 -> SettingsScreen(
                            uiState = uiState,
                            prefs = prefs,
                            strings = strings,
                            onLanguageChange = vm::setLanguage,
                            onFiqhChange = vm::setDefaultFiqh,
                            onArabicSizeChange = vm::setArabicFontSize,
                            onDarkModeChange = vm::setDarkMode,
                            onPrayerMethodChange = vm::setPrayerMethod,
                            onRemindersChange = vm::setRemindersEnabled,
                            onImportPack = vm::importPack
                        )
                    }
                }
            }
        }
    }
}
