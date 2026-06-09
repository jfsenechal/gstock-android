package be.marche.gstock.ui.account

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import be.marche.gstock.data.settings.AppLanguage
import be.marche.gstock.data.settings.ThemeMode
import be.marche.gstock.data.settings.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the persisted [ThemeMode] and the per-app language, and lets the UI change them. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    fun setThemeMode(mode: ThemeMode) = themePreferences.setThemeMode(mode)

    private val localeManager: LocaleManager
        get() = context.getSystemService(LocaleManager::class.java)

    /** The current per-app language; [AppLanguage.SYSTEM] when no override is set. */
    fun currentLanguage(): AppLanguage {
        val locales = localeManager.applicationLocales
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val language = locales[0]?.language
        return AppLanguage.entries.firstOrNull { it.tag == language } ?: AppLanguage.SYSTEM
    }

    /** Applies [language] as the per-app locale; the system recreates the activity to take effect. */
    fun setLanguage(language: AppLanguage) {
        localeManager.applicationLocales = if (language == AppLanguage.SYSTEM) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(language.tag)
        }
    }
}
