package com.fridgelist.app.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridgelist.app.data.model.ProviderType

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState.step) {
            SetupStep.CHOOSE_PROVIDER -> ChooseProviderStep(
                onProviderSelected = { viewModel.selectProvider(it) }
            )
            SetupStep.AUTHENTICATE -> AuthStep(
                provider = uiState.selectedProvider!!,
                onAuthComplete = { token -> viewModel.onAuthComplete(token, null) }
            )
            SetupStep.SELECT_LIST -> SelectListStep(
                lists = uiState.availableLists,
                onListSelected = { id, name -> viewModel.selectList(id, name) }
            )
            SetupStep.SET_GRID -> SetGridStep(
                columns = uiState.gridColumns,
                rows = uiState.gridRows,
                isLandscape = uiState.isLandscape,
                onConfirm = { cols, rows, landscape ->
                    viewModel.setGridDimensions(cols, rows, landscape)
                    viewModel.proceedToPopulation()
                }
            )
            SetupStep.INITIAL_POPULATION -> PopulationStep(
                isLoading = uiState.isLoading,
                onEmpty = { viewModel.populateEmpty(onSetupComplete) },
                onDefault = { viewModel.populateDefault(onSetupComplete) },
                onFromList = { viewModel.populateFromList(onSetupComplete) }
            )
        }
    }
}

@Composable
private fun ChooseProviderStep(onProviderSelected: (ProviderType) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Welcome to FridgeList", style = MaterialTheme.typography.headlineLarge)
        Text("Choose your shopping list provider", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { onProviderSelected(ProviderType.TODOIST) }, modifier = Modifier.fillMaxWidth()) {
            Text("Todoist")
        }
        Button(onClick = { onProviderSelected(ProviderType.MICROSOFT_TODO) }, modifier = Modifier.fillMaxWidth()) {
            Text("Microsoft To Do")
        }
        Button(onClick = { onProviderSelected(ProviderType.GOOGLE_TASKS) }, modifier = Modifier.fillMaxWidth()) {
            Text("Google Tasks")
        }
        Button(onClick = { onProviderSelected(ProviderType.TICKTICK) }, modifier = Modifier.fillMaxWidth()) {
            Text("TickTick")
        }
    }
}

@Composable
private fun AuthStep(provider: ProviderType, onAuthComplete: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Connect ${provider.name}", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Enter your API token or complete OAuth authentication",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("API Token") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onAuthComplete(token) },
            enabled = token.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }
    }
}

@Composable
private fun SelectListStep(
    lists: List<Pair<String, String>>,
    onListSelected: (String, String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Select a list", style = MaterialTheme.typography.headlineMedium)
        lists.forEach { (id, name) ->
            OutlinedButton(
                onClick = { onListSelected(id, name) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(name)
            }
        }
    }
}

@Composable
private fun SetGridStep(
    columns: Int,
    rows: Int,
    isLandscape: Boolean,
    onConfirm: (Int, Int, Boolean) -> Unit
) {
    var cols by remember { mutableStateOf(columns) }
    var rowCount by remember { mutableStateOf(rows) }
    var landscape by remember { mutableStateOf(isLandscape) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Grid dimensions", style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Columns: $cols", modifier = Modifier.width(120.dp))
            Slider(
                value = cols.toFloat(),
                onValueChange = { cols = it.toInt() },
                valueRange = 3f..10f,
                steps = 6,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rows: $rowCount", modifier = Modifier.width(120.dp))
            Slider(
                value = rowCount.toFloat(),
                onValueChange = { rowCount = it.toInt() },
                valueRange = 3f..15f,
                steps = 11,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Orientation:")
            Spacer(Modifier.width(16.dp))
            Column(Modifier.selectableGroup()) {
                Row(
                    Modifier.selectable(
                        selected = landscape,
                        onClick = { landscape = true },
                        role = Role.RadioButton
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = landscape, onClick = null)
                    Text("Landscape")
                }
                Row(
                    Modifier.selectable(
                        selected = !landscape,
                        onClick = { landscape = false },
                        role = Role.RadioButton
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !landscape, onClick = null)
                    Text("Portrait")
                }
            }
        }

        Button(
            onClick = { onConfirm(cols, rowCount, landscape) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}

@Composable
private fun PopulationStep(
    isLoading: Boolean,
    onEmpty: () -> Unit,
    onDefault: () -> Unit,
    onFromList: () -> Unit
) {
    if (isLoading) {
        CircularProgressIndicator()
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Start with…", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = onDefault, modifier = Modifier.fillMaxWidth()) {
            Text("Default grid (recommended)")
        }
        OutlinedButton(onClick = onFromList, modifier = Modifier.fillMaxWidth()) {
            Text("Import from my list")
        }
        OutlinedButton(onClick = onEmpty, modifier = Modifier.fillMaxWidth()) {
            Text("Empty grid")
        }
    }
}
