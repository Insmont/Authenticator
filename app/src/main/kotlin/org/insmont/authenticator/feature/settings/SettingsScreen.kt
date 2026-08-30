package org.insmont.authenticator.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.insmont.authenticator.MainActivityViewModel
import org.insmont.authenticator.R
import org.insmont.authenticator.core.designsystem.component.AuthenticatorClickableItem
import org.insmont.authenticator.core.designsystem.component.AuthenticatorLoadingIndicator
import org.insmont.authenticator.core.designsystem.component.AuthenticatorPasswordDialog
import org.insmont.authenticator.core.designsystem.component.AuthenticatorScaffold
import org.insmont.authenticator.core.designsystem.component.AuthenticatorSelectionDialog
import org.insmont.authenticator.core.designsystem.component.AuthenticatorSwitchItem
import org.insmont.authenticator.core.designsystem.theme.AuthenticatorTheme
import org.insmont.authenticator.core.model.ThemeConfig
import org.insmont.authenticator.core.model.UserPreferences
import org.insmont.authenticator.isBiometricAvailable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    mainViewModel: MainActivityViewModel = koinViewModel(),
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingEvent by remember { mutableStateOf<SettingsEvent?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            pendingEvent = event
        }
    }

    pendingEvent?.let { event ->
        val message = when (event) {
            SettingsEvent.ExportSuccess -> {
                stringResource(R.string.export_success)
            }

            is SettingsEvent.ExportFailed -> {
                stringResource(R.string.export_failed, event.message)
            }

            is SettingsEvent.ImportSuccess -> {
                stringResource(R.string.import_success, event.count)
            }

            SettingsEvent.InvalidPassword -> {
                stringResource(R.string.invalid_password)
            }

            is SettingsEvent.ImportFailed -> {
                stringResource(R.string.import_failed, event.message)
            }
        }
        LaunchedEffect(event) {
            snackbarHostState.showSnackbar(message)
            pendingEvent = null
        }
    }

    SettingsContent(
        settingsUiState = settingsUiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onChangeDynamicColorPreference = viewModel::updateDynamicColorPreference,
        onChangeThemeConfig = viewModel::updateThemeConfig,
        onChangeHapticFeedbackPreference = viewModel::updateHapticFeedbackPreference,
        onChangeAppLockPreference = { enabled ->
            viewModel.updateAppLockPreference(enabled)
            if (enabled) {
                mainViewModel.unlockApp()
            }
        },
        onExportData = viewModel::exportData,
        onImportData = viewModel::importData,
        onViewSource = viewModel::viewSource,
        onChangeLanguage = viewModel::updateLanguage,
        modifier = modifier
    )
}

@Composable
fun SettingsContent(
    settingsUiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onChangeDynamicColorPreference: (Boolean) -> Unit,
    onChangeThemeConfig: (ThemeConfig) -> Unit,
    onChangeHapticFeedbackPreference: (Boolean) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onChangeAppLockPreference: (Boolean) -> Unit,
    onExportData: (Uri, String) -> Unit,
    onImportData: (Uri, String) -> Unit,
    onViewSource: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    AuthenticatorScaffold(
        title = stringResource(R.string.settings),
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) { innerPadding ->
        when (settingsUiState) {
            is SettingsUiState.Loading -> {
                AuthenticatorLoadingIndicator(modifier = Modifier.padding(innerPadding))
            }

            is SettingsUiState.Success -> {
                SettingsPanel(
                    settings = settingsUiState.settings,
                    language = settingsUiState.language,
                    onChangeDynamicColorPreference = onChangeDynamicColorPreference,
                    onChangeThemeConfig = onChangeThemeConfig,
                    onChangeHapticFeedbackPreference = onChangeHapticFeedbackPreference,
                    onChangeLanguage = onChangeLanguage,
                    onChangeAppLockPreference = onChangeAppLockPreference,
                    onExportData = onExportData,
                    onImportData = onImportData,
                    onViewSource = onViewSource,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    settings: UserPreferences,
    language: String,
    onChangeDynamicColorPreference: (Boolean) -> Unit,
    onChangeThemeConfig: (ThemeConfig) -> Unit,
    onChangeHapticFeedbackPreference: (Boolean) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onChangeAppLockPreference: (Boolean) -> Unit,
    onExportData: (Uri, String) -> Unit,
    onImportData: (Uri, String) -> Unit,
    onViewSource: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPassword by remember { mutableStateOf("") }

    val currentSettings by rememberUpdatedState(settings)
    val currentOnChangeAppLockPreference by rememberUpdatedState(onChangeAppLockPreference)

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = remember {
            object : ActivityResultContracts.CreateDocument("application/octet-stream") {
                override fun createIntent(context: Context, input: String): Intent {
                    return super.createIntent(context, input).apply {
                        val documentsUri = DocumentsContract.buildDocumentUri(
                            "com.android.externalstorage.documents",
                            "primary:Documents"
                        )
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentsUri)
                    }
                }
            }
        }
    ) { uri ->
        if (uri != null) {
            onExportData(uri, pendingPassword)
            pendingPassword = ""
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            showImportPasswordDialog = true
        }
    }

    val exportAuthLauncher = rememberAuthenticationLauncher { result ->
        if (result is AuthenticationResult.Success) {
            showExportPasswordDialog = true
        }
    }

    val lockAuthLauncher = rememberAuthenticationLauncher { result ->
        if (result is AuthenticationResult.Success) {
            currentOnChangeAppLockPreference(!currentSettings.appLockEnabled)
        }
    }

    val authTitle = stringResource(R.string.biometric_prompt_title)
    val appName = stringResource(R.string.app_name)
    val authSubtitle = stringResource(R.string.biometric_prompt_subtitle, appName)

    val baseAuthRequest = remember(key1 = authTitle, key2 = authSubtitle) {
        AuthenticationRequest.biometricRequest(
            title = authTitle,
            AuthenticationRequest.Biometric.Fallback.DeviceCredential
        ) {
            setSubtitle(authSubtitle)
        }
    }

    val exportAuthTitle = stringResource(R.string.biometric_export_title)
    val exportAuthRequest = remember(exportAuthTitle) {
        AuthenticationRequest.biometricRequest(
            title = exportAuthTitle,
            AuthenticationRequest.Biometric.Fallback.DeviceCredential
        ) {}
    }

    val themeOptions = remember {
        listOf(
            ThemeConfig.FOLLOW_SYSTEM to R.string.theme_follow_system,
            ThemeConfig.LIGHT to R.string.theme_light,
            ThemeConfig.DARK to R.string.theme_dark
        )
    }
    val themeLabel =
        stringResource(themeOptions.find { it.first == settings.themeConfig }?.second ?: R.string.theme_follow_system)

    val languages = remember {
        listOf(
            "" to R.string.language_default,
            "en" to R.string.language_en,
            "zh-Hans" to R.string.language_zh_hans
        )
    }
    val languageLabel = stringResource(languages.find { it.first == language }?.second ?: R.string.language_default)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsHeader(stringResource(R.string.personalization))
            AuthenticatorSwitchItem(
                title = stringResource(R.string.dynamic_color),
                supportingText = stringResource(R.string.dynamic_color_desc),
                checked = settings.dynamicColorEnabled,
                onCheckedChange = onChangeDynamicColorPreference,
                leadingIcon = Icons.Rounded.Palette
            )
            AuthenticatorClickableItem(
                title = stringResource(R.string.theme),
                supportingText = themeLabel,
                onClick = { showThemeDialog = true },
                leadingIcon = Icons.Rounded.BrightnessMedium
            )
            AuthenticatorSwitchItem(
                title = stringResource(R.string.haptic_feedback),
                supportingText = stringResource(R.string.haptic_feedback_desc),
                checked = settings.hapticFeedbackEnabled,
                onCheckedChange = onChangeHapticFeedbackPreference,
                hapticFeedbackEnabled = true,
                leadingIcon = Icons.Rounded.Vibration
            )
            AuthenticatorClickableItem(
                title = stringResource(R.string.language),
                supportingText = languageLabel,
                onClick = { showLanguageDialog = true },
                leadingIcon = Icons.Rounded.Language
            )
        }

        item {
            SettingsHeader(stringResource(R.string.security_backup))
            AuthenticatorSwitchItem(
                title = stringResource(R.string.app_lock),
                supportingText = stringResource(R.string.app_lock_desc),
                checked = settings.appLockEnabled,
                onCheckedChange = {
                    if (context.isBiometricAvailable) {
                        lockAuthLauncher.launch(baseAuthRequest)
                    } else {
                        onChangeAppLockPreference(!settings.appLockEnabled)
                    }
                },
                leadingIcon = Icons.Rounded.Security
            )
            AuthenticatorClickableItem(
                title = stringResource(R.string.export_data),
                supportingText = stringResource(R.string.export_data_desc),
                onClick = {
                    if (context.isBiometricAvailable) {
                        exportAuthLauncher.launch(exportAuthRequest)
                    } else {
                        showExportPasswordDialog = true
                    }
                },
                leadingIcon = Icons.Rounded.Backup
            )
            AuthenticatorClickableItem(
                title = stringResource(R.string.import_data),
                supportingText = stringResource(R.string.import_data_desc),
                onClick = {
                    openDocumentLauncher.launch(arrayOf("*/*"))
                },
                leadingIcon = Icons.Rounded.SettingsBackupRestore
            )
        }

        item {
            SettingsHeader(stringResource(R.string.about))
            AuthenticatorClickableItem(
                title = stringResource(R.string.view_source),
                onClick = { onViewSource(context) },
                leadingIcon = Icons.Rounded.Code
            )
        }

        item {
            AppInfoFooter()
        }
    }

    if (showThemeDialog) {
        AuthenticatorSelectionDialog(
            onDismiss = { showThemeDialog = false },
            title = stringResource(R.string.theme),
            items = themeOptions.map { it.first to stringResource(it.second) },
            selectedItem = settings.themeConfig,
            onItemSelected = {
                onChangeThemeConfig(it)
                showThemeDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        AuthenticatorSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            title = stringResource(R.string.language),
            items = languages.map { it.first to stringResource(it.second) },
            selectedItem = language,
            onItemSelected = {
                onChangeLanguage(it)
                showLanguageDialog = false
            }
        )
    }

    if (showExportPasswordDialog) {
        AuthenticatorPasswordDialog(
            onDismiss = { showExportPasswordDialog = false },
            onConfirm = { password ->
                pendingPassword = password
                val timestamp = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .format(LocalDateTime.Format {
                        year()
                        monthNumber()
                        day()
                        hour()
                        minute()
                        second()
                    })
                createDocumentLauncher.launch("IA_$timestamp.authbak")
                showExportPasswordDialog = false
            },
            title = stringResource(R.string.export_password_title),
            description = stringResource(R.string.export_password_desc)
        )
    }

    if (showImportPasswordDialog) {
        AuthenticatorPasswordDialog(
            onDismiss = { showImportPasswordDialog = false },
            onConfirm = { password ->
                pendingUri?.let { onImportData(it, password) }
                showImportPasswordDialog = false
            },
            title = stringResource(R.string.import_password_title),
            description = stringResource(R.string.import_password_desc)
        )
    }
}

@Composable
private fun AppInfoFooter() {
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: ""
    val versionCode = packageInfo?.longVersionCode ?: 0
    val appName = stringResource(R.string.app_name)

    Text(
        text = stringResource(
            R.string.app_info_footer,
            appName,
            versionName,
            versionCode
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    )
}

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.primary
        )
    )
}

@PreviewScreenSizes
@Composable
private fun SettingsScreenPreview() {
    AuthenticatorTheme {
        SettingsContent(
            settingsUiState = SettingsUiState.Success(
                settings = UserPreferences(
                    themeConfig = ThemeConfig.FOLLOW_SYSTEM,
                    dynamicColorEnabled = true,
                    hapticFeedbackEnabled = true,
                    appLockEnabled = false
                ),
                language = ""
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onChangeThemeConfig = {},
            onChangeDynamicColorPreference = {},
            onChangeHapticFeedbackPreference = {},
            onChangeAppLockPreference = {},
            onExportData = { _, _ -> },
            onImportData = { _, _ -> },
            onViewSource = {},
            onChangeLanguage = {}
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsScreenLoadingPreview() {
    AuthenticatorTheme {
        SettingsContent(
            settingsUiState = SettingsUiState.Loading,
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onChangeThemeConfig = {},
            onChangeDynamicColorPreference = {},
            onChangeHapticFeedbackPreference = {},
            onChangeAppLockPreference = {},
            onExportData = { _, _ -> },
            onImportData = { _, _ -> },
            onViewSource = {},
            onChangeLanguage = {}
        )
    }
}