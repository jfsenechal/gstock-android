package be.marche.gstock.ui.account

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.marche.gstock.R
import be.marche.gstock.data.settings.AppLanguage
import be.marche.gstock.data.settings.ThemeMode

@Composable
fun AccountScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    // Changing the language recreates the activity, so the initial read reflects the active locale.
    var language by remember { mutableStateOf(viewModel.currentLanguage()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SettingsSection(
            titleRes = R.string.account_appearance,
            options = ThemeMode.entries,
            selected = themeMode,
            labelOf = { it.labelRes },
            onSelect = viewModel::setThemeMode,
        )

        SettingsSection(
            titleRes = R.string.account_language,
            options = AppLanguage.entries,
            selected = language,
            labelOf = { it.labelRes },
            onSelect = {
                language = it
                viewModel.setLanguage(it)
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** A titled, single-choice radio group used for the Account settings sections. */
@Composable
private fun <T> SettingsSection(
    @StringRes titleRes: Int,
    options: List<T>,
    selected: T,
    labelOf: (T) -> Int,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(Modifier.selectableGroup()) {
            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == selected,
                        onClick = null,
                    )
                    Text(
                        stringResource(labelOf(option)),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }
}
