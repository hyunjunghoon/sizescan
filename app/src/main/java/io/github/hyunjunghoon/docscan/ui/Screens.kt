package io.github.hyunjunghoon.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
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

private val LIMIT_PRESETS = listOf(10, 20, 50, 100, 200, 300, 500, 1000, 2000)

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
    onHelp: () -> Unit = {},
    onTheme: () -> Unit = {},
    onShareApp: () -> Unit = {},
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
                            text = { Text(stringResource(R.string.menu_help)) },
                            onClick = { menu = false; onHelp() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_theme)) },
                            onClick = { menu = false; onTheme() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_share_app)) },
                            onClick = { menu = false; onShareApp() },
                        )
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
                    // Same height for both buttons even when one label wraps to two lines.
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = onScan, enabled = !busy, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp)) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_scan), textAlign = TextAlign.Center)
                    }
                    FilledTonalButton(onClick = onPhotos, enabled = !busy, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp)) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_photos), textAlign = TextAlign.Center)
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
    onRotatePage: (File) -> Unit = {},
    onMovePage: (File, Int) -> Unit = { _, _ -> },
) {
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pageToDeleteName by rememberSaveable { mutableStateOf<String?>(null) }
    var addMenu by rememberSaveable { mutableStateOf(false) }
    var pageMenuName by rememberSaveable { mutableStateOf<String?>(null) }

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
                Modifier.navigationBarsPadding().fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    OutlinedButton(onClick = { addMenu = true }, enabled = !busy, modifier = Modifier.fillMaxSize().heightIn(min = 56.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_pages), textAlign = TextAlign.Center)
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
                Button(onClick = onExport, enabled = !busy, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp)) {
                    Icon(Icons.Filled.IosShare, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_export), textAlign = TextAlign.Center)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (doc.pages.isNotEmpty()) {
            // The page menu has no visible button, so say it exists.
            Text(
                stringResource(R.string.page_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
        LazyVerticalGrid(
            // 120dp keeps two columns on 320dp-wide phones instead of one oversized page.
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(doc.pages, key = { _, f -> f.name }) { i, file ->
                Box {
                    // Tap a page for rotate / reorder.
                    Thumbnail(
                        file,
                        Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !busy) { pageMenuName = file.name },
                    )
                    DropdownMenu(expanded = pageMenuName == file.name, onDismissRequest = { pageMenuName = null }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.page_rotate)) },
                            leadingIcon = { Icon(Icons.Filled.RotateRight, contentDescription = null) },
                            onClick = { pageMenuName = null; onRotatePage(file) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.page_move_earlier)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                            enabled = i > 0,
                            onClick = { pageMenuName = null; onMovePage(file, -1) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.page_move_later)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                            enabled = i < doc.pages.lastIndex,
                            onClick = { pageMenuName = null; onMovePage(file, 1) },
                        )
                    }
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
    initialMinKb: Int?,
    onDismiss: () -> Unit,
    onCreate: (ExportFormat, Int?, Int?) -> Unit,
) {
    var format by rememberSaveable { mutableStateOf(initialFormat) }
    var limit by rememberSaveable { mutableStateOf(initialLimitKb) }
    var minKb by rememberSaveable { mutableStateOf(initialMinKb?.takeIf { initialLimitKb != null }) }
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
                FilterChip(selected = limit == null, onClick = { limit = null; minKb = null }, label = { Text(stringResource(R.string.size_original)) })
                LIMIT_PRESETS.forEach { kb ->
                    FilterChip(selected = limit == kb && minKb == null, onClick = { limit = kb; minKb = null }, label = { Text("≤ " + formatLimit(kb)) })
                }
                val custom = limit?.takeIf { it !in LIMIT_PRESETS || minKb != null }
                FilterChip(
                    selected = custom != null,
                    onClick = { customOpen = true },
                    label = { Text(custom?.let { formatRange(minKb, it) } ?: stringResource(R.string.size_custom)) },
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
            Button(onClick = { onCreate(format, limit, minKb) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.export_create))
            }
        }
    }

    if (customOpen) {
        var minText by rememberSaveable { mutableStateOf(minKb?.toString() ?: "") }
        var maxText by rememberSaveable { mutableStateOf(limit?.toString() ?: "") }
        val max = maxText.toIntOrNull()?.takeIf { it in 5..50_000 }
        val min = minText.toIntOrNull()?.takeIf { it > 0 }
        // Matches Exporter: the minimum counts 1 KB as 1024 bytes, the maximum as 1000 bytes.
        val rangeBad = min != null && max != null && min * 1024L >= max * 1000L
        val valid = max != null && !rangeBad && (minText.isEmpty() || min != null)
        AlertDialog(
            onDismissRequest = { customOpen = false },
            title = { Text(stringResource(R.string.custom_kb_title)) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { v -> minText = v.filter { it.isDigit() }.take(5) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(R.string.min_kb_label)) },
                            suffix = { Text("KB") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = { v -> maxText = v.filter { it.isDigit() }.take(5) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(R.string.max_kb_label)) },
                            suffix = { Text("KB") },
                            placeholder = { Text(stringResource(R.string.custom_kb_hint)) },
                            isError = rangeBad,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(if (rangeBad) R.string.min_kb_error else R.string.min_kb_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rangeBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { limit = max; minKb = min; customOpen = false }, enabled = valid) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportResultDialog(
    result: ExportResult,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onOpen: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.export_done_title)) },
        text = {
            Column {
                val size = formatSize(result.totalBytes)
                val maxText = result.limitBytes?.let { formatLimit((it / 1000).toInt()) }
                val limitText = result.limitBytes?.let { max ->
                    result.minBytes?.let { formatRange((it / 1024).toInt(), (max / 1000).toInt()) } ?: maxText
                }
                Text(
                    pluralStringResource(R.plurals.files_count, result.files.size, result.files.size) + " · " +
                        if (limitText != null) stringResource(R.string.export_size_with_limit, size, limitText) else size,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!result.reached && limitText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.export_not_reached, maxText!!),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            // Wraps onto two lines on narrow screens or with long translations instead of squeezing the labels.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onOpen != null) TextButton(onClick = onOpen) { Text(stringResource(R.string.action_open)) }
                OutlinedButton(onClick = onSave) { Text(stringResource(R.string.action_save)) }
                Button(onClick = onShare) { Text(stringResource(R.string.action_share)) }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.help_step_1, R.string.help_step_2, R.string.help_step_3, R.string.help_step_4)
                    .forEachIndexed { i, res -> Text("${i + 1}. " + stringResource(res)) }
                Text(stringResource(R.string.help_privacy), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}

@Composable
fun ThemeDialog(current: ThemeMode, onPick: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val labels = mapOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_theme)) },
        text = {
            Column {
                labels.forEach { (mode, res) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = mode == current, role = Role.RadioButton, onClick = { onPick(mode) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(res))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
