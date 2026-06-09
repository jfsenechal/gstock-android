package be.marche.gstock.data.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import be.marche.gstock.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** How the app resolves its light/dark color scheme. */
enum class ThemeMode(@param:StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

/**
 * Persists the user's preferred [ThemeMode] in SharedPreferences and exposes it as a [StateFlow] so
 * the theme reacts immediately when it changes (no restart needed).
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(read())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    private fun read(): ThemeMode =
        prefs.getString(KEY_THEME_MODE, null)
            ?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
