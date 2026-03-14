package com.jipzeongit.arcsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jipzeongit.arcsync.data.SettingsRepository
import com.jipzeongit.arcsync.ui.AppRoot
import com.jipzeongit.arcsync.ui.theme.ArcSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(this)

        setContent {
            val themeModeState = settingsRepository.themeModeFlow.collectAsStateWithLifecycle(initialValue = SettingsRepository.ThemeMode.SYSTEM)
            ArcSyncTheme(themeModeState.value) {
                AppRoot(settingsRepository)
            }
        }
    }
}
