package org.insmont.authenticator.feature.addaccount

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import org.insmont.authenticator.R
import org.insmont.authenticator.core.designsystem.component.AuthenticatorAccountDialog
import org.insmont.authenticator.core.designsystem.component.AuthenticatorIconButton
import org.insmont.authenticator.core.designsystem.component.AuthenticatorScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddAccountScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddAccountViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var shouldShowRationale by remember {
        mutableStateOf(
            context.findActivity()?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
        )
    }

    var hasAttemptedAutoRequest by remember { mutableStateOf(false) }
    var resumeCount by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            shouldShowRationale = !granted && (context.findActivity()?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false)
            hasAttemptedAutoRequest = true
        }
    )

    LifecycleResumeEffect(Unit) {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        hasCameraPermission = status == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            shouldShowRationale = context.findActivity()?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
        }
        resumeCount++
        onPauseOrDispose { }
    }

    LaunchedEffect(hasCameraPermission, shouldShowRationale, resumeCount) {
        if (!hasCameraPermission && !shouldShowRationale && !hasAttemptedAutoRequest) {
            hasAttemptedAutoRequest = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBackClick()
            viewModel.resetSuccess()
        }
    }

    AddAccountContent(
        uiState = uiState,
        onBackClick = onBackClick,
        hasCameraPermission = hasCameraPermission,
        shouldShowRationale = shouldShowRationale,
        onScanResult = viewModel::onScanResult,
        onShowManualAdd = { viewModel.onShowManualAdd(true) },
        onManualAddDismiss = { viewModel.onShowManualAdd(false) },
        onManualAdd = viewModel::onManualAdd,
        onRequestPermission = {
            hasAttemptedAutoRequest = false
            launcher.launch(Manifest.permission.CAMERA)
        },
        onOpenSettingsClick = {
            hasAttemptedAutoRequest = false
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        },
        modifier = modifier
    )
}

@Composable
fun AddAccountContent(
    uiState: AddAccountUiState,
    onBackClick: () -> Unit,
    hasCameraPermission: Boolean,
    shouldShowRationale: Boolean,
    onScanResult: (String) -> Unit,
    onShowManualAdd: () -> Unit,
    onManualAddDismiss: () -> Unit,
    onManualAdd: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    AuthenticatorScaffold(
        title = stringResource(R.string.add_account),
        onBackClick = onBackClick,
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_account),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    AuthenticatorIconButton(
                        onClick = onBackClick,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        tooltipText = stringResource(R.string.back),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                ScannerView(onScanResult = onScanResult)

                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(innerPadding)
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.scan_hint),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(Modifier.height(24.dp))
                            ManualAddButton(
                                onShowManualAdd = onShowManualAdd,
                                modifier = Modifier.width(240.dp)
                            )
                        }

                        ScanOverlay(
                            showHint = false,
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    ScanOverlay(showHint = true)
                    ManualAddButton(
                        onShowManualAdd = onShowManualAdd,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(innerPadding)
                            .padding(bottom = 32.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    )
                }
            } else {
                PermissionDenied(
                    shouldShowRationale = shouldShowRationale,
                    onRequestPermission = onRequestPermission,
                    onOpenSettingsClick = onOpenSettingsClick,
                    onShowManualAdd = onShowManualAdd,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }

        if (uiState.showManualAdd) {
            AuthenticatorAccountDialog(
                onDismiss = onManualAddDismiss,
                onConfirm = onManualAdd,
                title = stringResource(R.string.enter_key_manually),
                confirmButtonText = stringResource(R.string.add),
                issuerState = uiState.issuerState,
                accountState = uiState.accountNameState,
                secretKeyState = uiState.secretKeyState
            )
        }
    }
}

@Composable
private fun ScannerView(onScanResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(
                    listOf(barcodeScanner),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context)
                ) { result ->
                    val barcode = result.getValue(barcodeScanner)
                    barcode?.firstOrNull()?.rawValue?.let {
                        onScanResult(it)
                    }
                }
            )
        }
    }

    LaunchedEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
    }

    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScanOverlay(
    modifier: Modifier = Modifier,
    showHint: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val overlayColor = Color.Black.copy(alpha = 0.6f)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanBoxSize = 250.dp.toPx()
            val finalSize = minOf(a = scanBoxSize, b = size.width * 0.8f, c = size.height * 0.7f)
            val left = (size.width - finalSize) / 2
            val top = (size.height - finalSize) / 2
            val rect = Rect(Offset(x = left, y = top), Size(width = finalSize, height = finalSize))

            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                )
            }

            clipPath(path, clipOp = ClipOp.Difference) {
                drawRect(overlayColor)
            }
        }

        if (showHint) {
            val hintTopPadding = if (isLandscape) 0.dp else 320.dp
            val hintBottomPadding = if (isLandscape) 80.dp else 0.dp

            Text(
                text = stringResource(R.string.scan_hint),
                modifier = Modifier
                    .align(if (isLandscape) Alignment.BottomCenter else Alignment.Center)
                    .padding(top = hintTopPadding, bottom = hintBottomPadding),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun PermissionDenied(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onShowManualAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.camera_permission_denied),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                textAlign = TextAlign.Center
            )
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (shouldShowRationale) {
                    onRequestPermission()
                } else {
                    onOpenSettingsClick()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(if (shouldShowRationale) R.string.grant_permission else R.string.open_settings),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.height(16.dp))
        ManualAddButton(
            onShowManualAdd = onShowManualAdd,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ManualAddButton(
    onShowManualAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onShowManualAdd,
        modifier = modifier
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White
        )
    ) {
        Text(
            text = stringResource(R.string.enter_key_manually),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}


private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}