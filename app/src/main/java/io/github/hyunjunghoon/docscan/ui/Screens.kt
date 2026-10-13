package io.github.hyunjunghoon.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.hyunjunghoon.docscan.R
import io.github.hyunjunghoon.docscan.ads.BannerAd
import io.github.hyunjunghoon.docscan.data.Doc
import io.github.hyunjunghoon.docscan.data.ExportFormat
import io.github.hyunjunghoon.docscan.data.ExportResult
import java.io.File
import java.text.DateFormat
import java.util.Date

private val LIMIT_PRESETS = listOf(50, 100, 200, 300, 500, 1000, 2000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    docs: List<Doc>,
    busy: Boolean,
    snackbar: SnackbarHostState,
    showPrivacy: Boolean,
    onPrivacy: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onOpen: (String) -> Unit,
    onScan: () -> Unit,
    onPhotos: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_privacy_policy)) },
                            onClick = { menu = false; onPrivacyPolicy() },
                        )
                        if (showPrivacy) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_privacy)) },
                                onClick = { menu = false; onPrivacy() },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = onScan, enabled = !busy, modifier = Modifier.weight(1f).height(56.dp)) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_scan))
                    }
                    FilledTonalButton(onClick = onPhotos, enabled = !busy, modifier = Modifier.weight(1f).height(56.dp)) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_photos))
                    }
                }
                Spacer(Modifier.height(12.dp))
                BannerAd()
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (docs.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.home_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(docs, key = { it.id }) { doc -> DocRow(doc) { onOpen(doc.id) } }
                }
            }
        }
    }
}

@Composable
private fun DocRow(doc: Doc, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(doc.pages.firstOrNull(), Modifier.size(width = 56.dp, height = 76.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val date = remember(doc.createdAt) {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(doc.createdAt))
                }
                Text(
                    pluralStringResource(R.plurals.pages_count, doc.pages.size, doc.pages.size) + " · " + date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocScreen(
    doc: Doc,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDeletePage: (File) -> Unit,
    onAddScan: () -> Unit,
    onAddPhotos: () -> Unit,
    onExport: () -> Unit,
    busy: Boolean = false,
) {
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pageToDeleteName by rememberSaveable { mutableStateOf<String?>(null) }
    var addMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = { Text(doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = { renaming = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_rename))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { addMenu = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_pages))
                    }
                    DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_scan)) },
                            leadingIcon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null) },
                            onClick = { addMenu = false; onAddScan() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_photos)) },
                            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                            onClick = { addMenu = false; onAddPhotos() },
                        )
                    }
                }
                Button(onClick = onExport, enabled = !busy, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Filled.IosShare, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_export))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(doc.pages, key = { _, f -> f.name }) { i, file ->
                Box {
                    Thumbnail(file, Modifier.fillMaxWidth().aspectRatio(0.72f))
                    Text(
                        stringResource(R.string.page_n, i + 1),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color(0x99000000), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    IconButton(
                        onClick = { pageToDeleteName = file.name },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(36.dp)
                            .background(Color(0x99000000), RoundedCornerShape(18.dp)),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_delete), tint = Color.White)
                    }
                }
            }
        }
        }
    }

    if (renaming) {
        var name by rememberSaveable { mutableStateOf(doc.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.name_label)) },
                )
            },
            confirmButton = {
                TextButton(onClick = { renaming = false; onRename(name) }, enabled = name.isNotBlank()) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.delete_doc_title), onDismiss = { confirmDelete = false }) {
            confirmDelete = false
            onDelete()
        }
    }
    doc.pages.firstOrNull { it.name == pageToDeleteName }?.let { file ->
        ConfirmDialog(stringResource(R.string.delete_page_title), onDismiss = { pageToDeleteName = null }) {
            pageToDeleteName = null
            onDeletePage(file)
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportSheet(
    initialFormat: ExportFormat,
    initialLimitKb: Int?,
    onDismiss: () -> Unit,
    onCreate: (ExportFormat, Int?) -> Unit,
) {
    var format by rememberSaveable { mutableStateOf(initialFormat) }
    var limit by rememberSaveable { mutableStateOf(initialLimitKb) }
    var customOpen by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.export_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.format_label), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = format == ExportFormat.PDF, onClick = { format = ExportFormat.PDF }, label = { Text("PDF") })
                FilterChip(selected = format == ExportFormat.JPG, onClick = { format = ExportFormat.JPG }, label = { Text("JPG") })
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.size_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = limit == null, onClick = { limit = null }, label = { Text(stringResource(R.string.size_original)) })
                LIMIT_PRESETS.forEach { kb ->
                    FilterChip(selected = limit == kb, onClick = { limit = kb }, label = { Text("≤ " + formatLimit(kb)) })
                }
                val custom = limit?.takeIf { it !in LIMIT_PRESETS }
                FilterChip(
                    selected = custom != null,
                    onClick = { customOpen = true },
                    label = { Text(custom?.let { "≤ " + formatLimit(it) } ?: stringResource(R.string.size_custom)) },
                )
            }
            if (format == ExportFormat.JPG && limit != null) {
                Text(
                    stringResource(R.string.size_per_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onCreate(format, limit) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.export_create))
            }
        }
    }

    if (customOpen) {
        var text by rememberSaveable { mutableStateOf("") }
        val value = text.toIntOrNull()?.takeIf { it in 5..50_000 }
        AlertDialog(
            onDismissRequest = { customOpen = false },
            title = { Text(stringResource(R.string.custom_kb_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(5) },
                    singleLine = true,
                    suffix = { Text("KB") },
                    placeholder = { Text(stringResource(R.string.custom_kb_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = { limit = value; customOpen = false }, enabled = value != null) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { customOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun ExportProgressDialog(done: Int, total: Int) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.export_working, (done + 1).coerceAtMost(total), total))
            }
        },
    )
}

@Composable
fun ExportResultDialog(result: ExportResult, onShare: () -> Unit, onSave: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.export_done_title)) },
        text = {
            Column {
                val size = formatSize(result.totalBytes)
                val limitText = result.limitBytes?.let { formatLimit((it / 1000).toInt()) }
                Text(
                    pluralStringResource(R.plurals.files_count, result.files.size, result.files.size) + " · " +
                        if (limitText != null) stringResource(R.string.export_size_with_limit, size, limitText) else size,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!result.reached && limitText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.export_not_reached, limitText),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSave) { Text(stringResource(R.string.action_save)) }
                Button(onClick = onShare) { Text(stringResource(R.string.action_share)) }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}
