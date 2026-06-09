package be.marche.gstock.ui.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.marche.gstock.R
import be.marche.gstock.data.local.entity.ToolEntity
import be.marche.gstock.ui.common.LoadingBox
import be.marche.gstock.ui.common.MessageBox
import be.marche.gstock.ui.common.SearchField

@Composable
fun ToolsScreen(viewModel: ToolsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = stringResource(R.string.search_tools),
            onSearch = viewModel::refresh,
        )
        FilterChip(
            selected = state.onlyAvailable,
            onClick = viewModel::toggleOnlyAvailable,
            label = { Text(stringResource(R.string.tools_available_only)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.tools.isEmpty() -> LoadingBox()
                state.error != null && state.tools.isEmpty() ->
                    MessageBox(state.error!!, onRetry = viewModel::refresh)
                state.tools.isEmpty() -> MessageBox(stringResource(R.string.tools_empty))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.tools, key = { it.id }) { tool ->
                        ToolRow(tool)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRow(tool: ToolEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            if (tool.isAvailable) stringResource(R.string.tool_available)
                            else (tool.status ?: stringResource(R.string.tool_unavailable)),
                        )
                    },
                )
            }
            listOfNotNull(tool.manufacturer, tool.model)
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyMedium) }
            tool.categoryName?.let {
                Text(it, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
