package be.marche.gstock.data.settings

import androidx.annotation.StringRes
import be.marche.gstock.R

/**
 * Languages the user can pick in-app. [SYSTEM] clears the per-app override so the app follows the
 * device language; the others force a specific locale. [tag] is the BCP-47 language tag passed to
 * `AppCompatDelegate.setApplicationLocales` (empty for [SYSTEM]). Keep in sync with `res/xml/locales_config.xml`.
 */
enum class AppLanguage(val tag: String, @param:StringRes val labelRes: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    FRENCH("fr", R.string.language_french),
}
