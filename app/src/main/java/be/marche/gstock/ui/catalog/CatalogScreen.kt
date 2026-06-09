package be.marche.gstock.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import be.marche.gstock.R
import be.marche.gstock.ui.tools.ToolsScreen
import be.marche.gstock.ui.workers.WorkersScreen

/** Groups the Workers and Tools lists behind a single bottom-bar destination, switched via tabs. */
@Composable
fun CatalogScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(R.string.catalog_tab_workers, R.string.catalog_tab_tools)

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(stringResource(title)) },
                )
            }
        }
        when (selectedTab) {
            0 -> WorkersScreen()
            else -> ToolsScreen()
        }
    }
}
