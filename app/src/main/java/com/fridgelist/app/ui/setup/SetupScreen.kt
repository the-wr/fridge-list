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
            SetupStep.INITIAL_POPULATION -> ConfigureStep(
                columns = uiState.gridColumns,
                rows = uiState.gridRows,
                isLoading = uiState.isLoading,
                hasExistingGrid = uiState.hasExistingGrid,
                listName = uiState.selectedListName ?: "",
                onBack = { viewModel.goBack() },
                onFinish = { cols, rows, choice ->
                    viewModel.setGridDimensions(cols, rows)
                    when (choice) {
                        PopulationChoice.KEEP_CURRENT -> viewModel.populateKeepCurrent(onSetupComplete)
                        PopulationChoice.DEFAULT -> viewModel.populateDefault(onSetupComplete)
                        PopulationChoice.FROM_LIST -> viewModel.populateFromList(onSetupComplete)
                        PopulationChoice.EMPTY -> viewModel.populateEmpty(onSetupComplete)
                    }
                }
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
        Spacer(Modifier.height(4.dp))
        Text(
            "FridgeList doesn\u2019t store your shopping list \u2014 it connects to one you already " +
                "have. To get started, you\u2019ll need an account with one of the supported providers below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        Button(onClick = { onProviderSelected(ProviderType.TODOIST) }, modifier = Modifier.fillMaxWidth()) {
            Text("Todoist")
        }
        Button(onClick = { onProviderSelected(ProviderType.MICROSOFT_TODO) }, modifier = Modifier.fillMaxWidth()) {
            Text("Microsoft To Do")
        }
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Google Tasks (coming soon)")
        }
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("TickTick (coming soon)")
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
    var isLaunching by remember { mutableStateOf(false) }

    // If the OAuth flow returns an error or is cancelled, re-show the button.
    LaunchedEffect(error) { if (error != null) isLaunching = false }

    // Defer the actual Intent launch until after the spinner frame is committed.
    // Setting isLaunching = true and calling startActivity in the same click handler
    // is a race: startActivity wins and the spinner never renders.
    LaunchedEffect(isLaunching) {
        if (isLaunching) {
            withFrameNanos { } // wait for Compose to commit the spinner frame
            onAuthorize()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Connect ${provider.displayName()}", style = MaterialTheme.typography.headlineMedium)

        if (isLoading || isLaunching) {
            CircularProgressIndicator()
            Text(
                if (isLoading) "Completing sign-in\u2026" else "Opening sign-in page\u2026",
                style = MaterialTheme.typography.bodyMedium,
            )
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
                onClick = { isLaunching = true },
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
        Text("Select a list to use as the shopping list", style = MaterialTheme.typography.headlineMedium)
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

// ---------------------------------------------------------------------------
// Combined grid + contents step
// ---------------------------------------------------------------------------

private enum class PopulationChoice { KEEP_CURRENT, DEFAULT, FROM_LIST, EMPTY }

@Composable
private fun ConfigureStep(
    columns: Int,
    rows: Int,
    isLoading: Boolean,
    hasExistingGrid: Boolean,
    listName: String,
    onBack: () -> Unit,
    onFinish: (cols: Int, rows: Int, choice: PopulationChoice) -> Unit,
) {
    if (isLoading) {
        CircularProgressIndicator()
        return
    }

    var cols by remember { mutableStateOf(columns) }
    var rowCount by remember { mutableStateOf(rows) }
    val defaultChoice = if (hasExistingGrid) PopulationChoice.KEEP_CURRENT else PopulationChoice.DEFAULT
    var selected by remember { mutableStateOf(defaultChoice) }

    Column(
        modifier = Modifier
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Grid dimensions", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

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

        Spacer(Modifier.height(16.dp))
        Text("Start with\u2026", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (hasExistingGrid) {
            PopulationOption(
                label = "Keep current grid",
                hint = null,
                selected = selected == PopulationChoice.KEEP_CURRENT,
                onClick = { selected = PopulationChoice.KEEP_CURRENT },
            )
        }
        PopulationOption(
            label = "Default grid",
            hint = "Start with most common groceries; you can change them later",
            selected = selected == PopulationChoice.DEFAULT,
            onClick = { selected = PopulationChoice.DEFAULT },
        )
        PopulationOption(
            label = "Import from the \u201c$listName\u201d list",
            hint = "Populate the grid based on the contents of your list",
            selected = selected == PopulationChoice.FROM_LIST,
            onClick = { selected = PopulationChoice.FROM_LIST },
        )
        PopulationOption(
            label = "Empty grid",
            hint = null,
            selected = selected == PopulationChoice.EMPTY,
            onClick = { selected = PopulationChoice.EMPTY },
        )

        SetupNavRow(
            onBack = onBack,
            onNext = { onFinish(cols, rowCount, selected) },
            nextLabel = "Finish",
        )
    }
}

@Composable
private fun PopulationOption(
    label: String,
    hint: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
