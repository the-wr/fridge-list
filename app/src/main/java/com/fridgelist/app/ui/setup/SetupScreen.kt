package com.fridgelist.app.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridgelist.app.data.model.ProviderType

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Hoisted to the top level so the registration is stable for the full lifetime of this
    // screen — if the launcher lived inside the AUTHENTICATE branch of the when() block,
    // a recomposition triggered by any state change (e.g. isAuthLoading flipping to true)
    // could re-register it mid-flight and orphan the pending AppAuth result.
    val oauthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleOAuthResult(result.data)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState.step) {
            SetupStep.CHOOSE_PROVIDER -> ChooseProviderStep(
                onProviderSelected = { viewModel.selectProvider(it) }
            )
            SetupStep.AUTHENTICATE -> AuthStep(
                provider = uiState.selectedProvider!!,
                isLoading = uiState.isAuthLoading,
                error = uiState.authError,
                onBack = { viewModel.goBack() },
                onAuthorize = {
                    val intent = viewModel.buildAuthIntent(uiState.selectedProvider!!)
                    intent?.let { oauthLauncher.launch(it) }
                },
            )
            SetupStep.SELECT_LIST -> SelectListStep(
                lists = uiState.availableLists,
                isLoading = uiState.isLoading,
                onBack = { viewModel.goBack() },
                onListSelected = { id, name -> viewModel.selectList(id, name) },
                onCreateList = { name -> viewModel.createAndSelectList(name) },
            )
            SetupStep.SET_GRID -> SetGridStep(
                columns = uiState.gridColumns,
                rows = uiState.gridRows,
                isLandscape = uiState.isLandscape,
                onBack = { viewModel.goBack() },
                onConfirm = { cols, rows, landscape ->
                    viewModel.setGridDimensions(cols, rows, landscape)
                    viewModel.proceedToPopulation()
                }
            )
            SetupStep.INITIAL_POPULATION -> PopulationStep(
                isLoading = uiState.isLoading,
                hasExistingGrid = uiState.hasExistingGrid,
                onBack = { viewModel.goBack() },
                onKeepCurrent = { viewModel.populateKeepCurrent(onSetupComplete) },
                onDefault = { viewModel.populateDefault(onSetupComplete) },
                onFromList = { viewModel.populateFromList(onSetupComplete) },
                onEmpty = { viewModel.populateEmpty(onSetupComplete) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared navigation row
// ---------------------------------------------------------------------------

@Composable
private fun SetupNavRow(
    onBack: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    nextLabel: String = "Next"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = when {
            onBack != null && onNext != null -> Arrangement.SpaceBetween
            onNext != null -> Arrangement.End
            else -> Arrangement.Start
        }
    ) {
        if (onBack != null) {
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        if (onNext != null) {
            Button(onClick = onNext, enabled = nextEnabled) { Text(nextLabel) }
        }
    }
}

// ---------------------------------------------------------------------------
// Step composables
// ---------------------------------------------------------------------------

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
private fun AuthStep(
    provider: ProviderType,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onAuthorize: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Connect ${provider.displayName()}", style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator()
            Text("Completing sign-in\u2026", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "You\u2019ll be taken to ${provider.displayName()} to sign in, then returned here automatically.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (error != null) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onAuthorize,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign in with ${provider.displayName()}")
            }
            SetupNavRow(onBack = onBack)
        }
    }
}

private fun ProviderType.displayName() = when (this) {
    ProviderType.TODOIST -> "Todoist"
    ProviderType.MICROSOFT_TODO -> "Microsoft To Do"
    ProviderType.GOOGLE_TASKS -> "Google Tasks"
    ProviderType.TICKTICK -> "TickTick"
}

@Composable
private fun SelectListStep(
    lists: List<ProviderListInfo>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onListSelected: (String, String) -> Unit,
    onCreateList: (String) -> Unit,
) {
    if (isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading lists\u2026", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var selectedId by remember { mutableStateOf<String?>(null) }
    var createNew by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("Shopping list") }

    Column(
        modifier = Modifier
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Select a list", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        lists.forEach { list ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedId = list.id
                        createNew = false
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedId == list.id,
                    onClick = {
                        selectedId = list.id
                        createNew = false
                    }
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(list.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${list.totalTasks} task${if (list.totalTasks == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    createNew = true
                    selectedId = null
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = createNew,
                onClick = {
                    createNew = true
                    selectedId = null
                }
            )
            Spacer(Modifier.width(8.dp))
            Text("Create a new list", style = MaterialTheme.typography.bodyLarge)
        }

        AnimatedVisibility(visible = createNew) {
            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                label = { Text("List name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 4.dp, bottom = 8.dp),
            )
        }

        SetupNavRow(
            onBack = onBack,
            onNext = {
                if (createNew) {
                    onCreateList(newListName)
                } else {
                    val id = selectedId!!
                    val name = lists.find { it.id == id }!!.name
                    onListSelected(id, name)
                }
            },
            nextEnabled = createNew || selectedId != null,
        )
    }
}

@Composable
private fun SetGridStep(
    columns: Int,
    rows: Int,
    isLandscape: Boolean,
    onBack: () -> Unit,
    onConfirm: (Int, Int, Boolean) -> Unit
) {
    var cols by remember { mutableStateOf(columns) }
    var rowCount by remember { mutableStateOf(rows) }

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
                valueRange = 3f..15f,
                steps = 11,
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

        SetupNavRow(
            onBack = onBack,
            onNext = { onConfirm(cols, rowCount, isLandscape) }
        )
    }
}

@Composable
private fun PopulationStep(
    isLoading: Boolean,
    hasExistingGrid: Boolean,
    onBack: () -> Unit,
    onKeepCurrent: () -> Unit,
    onDefault: () -> Unit,
    onFromList: () -> Unit,
    onEmpty: () -> Unit
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
        Text("Start with\u2026", style = MaterialTheme.typography.headlineMedium)

        if (hasExistingGrid) {
            Button(onClick = onKeepCurrent, modifier = Modifier.fillMaxWidth()) {
                Text("Keep current grid")
            }
        }
        Button(
            onClick = onDefault,
            modifier = Modifier.fillMaxWidth(),
            colors = if (hasExistingGrid)
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            else ButtonDefaults.buttonColors()
        ) {
            Text(if (hasExistingGrid) "Default grid" else "Default grid (recommended)")
        }
        OutlinedButton(onClick = onFromList, modifier = Modifier.fillMaxWidth()) {
            Text("Import from my list")
        }
        OutlinedButton(onClick = onEmpty, modifier = Modifier.fillMaxWidth()) {
            Text("Empty grid")
        }

        SetupNavRow(onBack = onBack)
    }
}
