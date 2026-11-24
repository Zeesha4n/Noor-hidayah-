package com.noorehidayah.quran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.ui.i18n.AppStrings
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoorHidayahApp(vm: MainViewModel) {
    val prefs by vm.preferences.collectAsState()
    val uiState by vm.uiState.collectAsState()
    val strings = remember(prefs.language) { AppStrings.forLanguage(prefs.language) }
    var activeTab by remember { mutableIntStateOf(0) }

    NoorHidayahTheme(darkTheme = prefs.darkMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.appTitle, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        // Quick cycle language right from the Top Bar
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
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text(strings.tabSearch) }
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
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        label = { Text(strings.tabTools) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
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
                } else {
                    when (activeTab) {
                        0 -> HomeSearchScreen(
                            uiState = uiState,
                            prefs = prefs,
                            strings = strings,
                            onQueryChange = vm::onSearchQueryChange,
                            onOpenSurah = { surahNum ->
                                vm.openSurah(surahNum)
                                activeTab = 1
                            },
                            onToggleBookmark = vm::toggleBookmark
                        )
                        1 -> QuranScreen(
                            uiState = uiState,
                            prefs = prefs,
                            strings = strings,
                            onOpenSurah = vm::openSurah,
                            onCloseReader = vm::closeSurahReader,
                            onToggleBookmark = vm::toggleBookmark
                        )
                        2 -> ToolsScreen(
                            duas = uiState.duas,
                            prefs = prefs,
                            strings = strings,
                            onIncrementTasbih = vm::incrementTasbih,
                            onResetTasbih = vm::resetTasbih,
                            onSelectTasbihPreset = vm::setTasbihPreset,
                            onSelectCity = vm::setCityIndex,
                            onSelectAsrMethod = vm::setAsrMethod
                        )
                        3 -> SettingsScreen(
                            uiState = uiState,
                            prefs = prefs,
                            strings = strings,
                            onLanguageChange = vm::setLanguage,
                            onFiqhChange = vm::setDefaultFiqh,
                            onArabicSizeChange = vm::setArabicFontSize,
                            onDarkModeChange = vm::setDarkMode,
                            onPrayerMethodChange = vm::setPrayerMethod,
                            onImportPack = vm::importPack
                        )
                    }
                }
            }
        }
    }
}
