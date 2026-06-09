package be.marche.gstock.ui.account

import androidx.lifecycle.ViewModel
import be.marche.gstock.data.settings.ThemeMode
import be.marche.gstock.data.settings.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the persisted [ThemeMode] and lets the UI change it. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    fun setThemeMode(mode: ThemeMode) = themePreferences.setThemeMode(mode)
}
