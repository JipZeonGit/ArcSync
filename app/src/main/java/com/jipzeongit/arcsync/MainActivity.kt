package com.jipzeongit.arcsync

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.data.SettingsRepository
import com.jipzeongit.arcsync.ui.AppRoot
import com.jipzeongit.arcsync.ui.theme.ArcSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(this)

        setContent {
            val themeMode = settingsRepository.themeModeFlow
                .collectAsStateWithLifecycle(initialValue = SettingsRepository.ThemeMode.SYSTEM)
            val themeStyle = settingsRepository.themeStyleFlow
                .collectAsStateWithLifecycle(initialValue = SettingsRepository.ThemeStyle.DYNAMIC)
            val useAmoled = settingsRepository.amoledEnabledFlow
                .collectAsStateWithLifecycle(initialValue = false)

            ArcSyncTheme(
                mode = themeMode.value,
                themeStyle = themeStyle.value,
                useAmoled = useAmoled.value
            ) {
                AppRoot(settingsRepository)
            }
        }
    }
}
