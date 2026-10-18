package io.github.hyunjunghoon.docscan.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.android.play.core.review.ReviewManagerFactory
import io.github.hyunjunghoon.docscan.ExportUi
import io.github.hyunjunghoon.docscan.MainViewModel
import io.github.hyunjunghoon.docscan.R
import io.github.hyunjunghoon.docscan.ads.Ads
import io.github.hyunjunghoon.docscan.data.ExportFormat

private const val PRIVACY_POLICY_URL = "https://hyunjunghoon.github.io/sizescan/privacy.html"

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val docs by vm.docs.collectAsStateWithLifecycle()
    val openId by vm.openDocId.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val export by vm.export.collectAsStateWithLifecycle()
    val privacyRequired by Ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val doc = docs.firstOrNull { it.id == openId }

    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            snackbar.showSnackbar(
                context.getString(m.res, *m.args.toTypedArray()),
                withDismissAction = m.long,
                duration = if (m.long) SnackbarDuration.Long else SnackbarDuration.Short,
            )
        }
    }

    // Which document new pages go into (null = new document). Saved across process death.
    var addTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val uris = GmsDocumentScanningResult.fromActivityResultIntent(r.data)?.pages?.map { it.imageUri }.orEmpty()
            if (uris.isNotEmpty()) vm.importImages(uris, addTarget, fromScanner = true)
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
        if (uris.isNotEmpty()) vm.importImages(uris, addTarget, fromScanner = false)
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let(vm::saveTo)
    }
    val saveJpg = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        uri?.let(vm::saveTo)
    }
    val saveFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(vm::saveToFolder)
    }

    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build(),
        )
    }
    val startScan: (String?) -> Unit = { target ->
        addTarget = target
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { scanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { vm.message(R.string.scan_unavailable) }
    }
    val pickPhotos: (String?) -> Unit = { target ->
        addTarget = target
        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    var showExportSheet by rememberSaveable { mutableStateOf(false) }

    if (doc != null) {
        BackHandler { vm.openDoc(null) }
        DocScreen(
            doc = doc,
            snackbar = snackbar,
            onBack = { vm.openDoc(null) },
            onRename = { vm.rename(doc.id, it) },
            onDelete = { vm.deleteDoc(doc.id) },
            onDeletePage = { vm.deletePage(doc.id, it) },
            onAddScan = { startScan(doc.id) },
            onAddPhotos = { pickPhotos(doc.id) },
            onExport = { showExportSheet = true },
            busy = busy,
        )
    } else {
        HomeScreen(
            docs = docs,
            busy = busy,
            snackbar = snackbar,
            showPrivacy = privacyRequired,
            onPrivacy = { Ads.showPrivacyOptions(activity) },
            onPrivacyPolicy = {
                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
            },
            onOpen = { vm.openDoc(it) },
            onScan = { startScan(null) },
            onPhotos = { pickPhotos(null) },
        )
    }

    if (showExportSheet && doc != null) {
        ExportSheet(
            initialFormat = vm.lastFormat,
            initialLimitKb = vm.lastLimitKb,
            onDismiss = { showExportSheet = false },
            onCreate = { format, kb ->
                showExportSheet = false
                vm.export(doc, format, kb)
            },
        )
    }

    when (val s = export) {
        is ExportUi.Running -> ExportProgressDialog(s.done, s.total)
        is ExportUi.Done -> {
            ExportResultDialog(
                result = s.result,
                onShare = { shareFiles(context, s.result.files, s.result.format) },
                onSave = {
                    val files = s.result.files
                    when {
                        files.size > 1 -> saveFolder.launch(null)
                        s.result.format == ExportFormat.PDF -> savePdf.launch(files[0].name)
                        else -> saveJpg.launch(files[0].name)
                    }
                },
                onClose = {
                    val showAd = s.showAd
                    if (vm.closeExport()) {
                        val manager = ReviewManagerFactory.create(activity)
                        manager.requestReviewFlow().addOnCompleteListener { task ->
                            if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
                        }
                    } else if (showAd) {
                        // Natural break: the user has finished with the file.
                        Ads.maybeShowInterstitial(activity)
                    }
                },
            )
        }
        ExportUi.Hidden -> Unit
    }
}
