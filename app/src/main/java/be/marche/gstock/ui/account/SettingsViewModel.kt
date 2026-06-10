package be.marche.gstock.ui.account

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import be.marche.gstock.data.settings.AppLanguage
import be.marche.gstock.data.settings.ThemeMode
import be.marche.gstock.data.settings.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the persisted [ThemeMode] and the per-app language, and lets the UI change them. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    fun setThemeMode(mode: ThemeMode) = themePreferences.setThemeMode(mode)

    /** The current per-app language; [AppLanguage.SYSTEM] when no override is set. */
    fun currentLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val language = locales[0]?.language
        return AppLanguage.entries.firstOrNull { it.tag == language } ?: AppLanguage.SYSTEM
    }

    /** Applies [language] as the per-app locale; AppCompat recreates the activity to take effect. */
    fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            if (language == AppLanguage.SYSTEM) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.tag)
            },
        )
    }
}
