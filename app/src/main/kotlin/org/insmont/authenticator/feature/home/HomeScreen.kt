package org.insmont.authenticator.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.insmont.authenticator.R
import org.insmont.authenticator.core.designsystem.component.AuthenticatorAccountCard
import org.insmont.authenticator.core.designsystem.component.AuthenticatorAccountDialog
import org.insmont.authenticator.core.designsystem.component.AuthenticatorAlertDialog
import org.insmont.authenticator.core.designsystem.component.AuthenticatorIconButton
import org.insmont.authenticator.core.designsystem.component.AuthenticatorScaffold
import org.insmont.authenticator.core.model.Account
import org.insmont.authenticator.core.util.OtpUtils
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeState = produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000.milliseconds)
        }
    }
    val currentTimeProvider = remember { { timeState.value } }

    HomeContent(
        uiState = uiState,
        currentTimeProvider = currentTimeProvider,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onDeleteAccount = viewModel::onDeleteAccount,
        onEditAccount = viewModel::onEditAccount,
        onUpdateAccount = viewModel::onUpdateAccount,
        onDecryptSecret = viewModel::decryptSecret,
        onDismissEdit = viewModel::dismissEdit,
        editIssuerState = viewModel.editIssuerState,
        editAccountNameState = viewModel.editAccountNameState,
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelected = viewModel::deleteSelectedAccounts,
        onAddClick = onAddClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier
    )
}

@OptIn(FlowPreview::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    currentTimeProvider: () -> Long,
    onSearchQueryChanged: (String) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onEditAccount: (Account) -> Unit,
    onUpdateAccount: () -> Unit,
    onDecryptSecret: (ByteArray) -> ByteArray,
    onDismissEdit: () -> Unit,
    editIssuerState: TextFieldState,
    editAccountNameState: TextFieldState,
    onToggleSelection: (Account) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isSearchingLocally by remember { mutableStateOf(false) }

    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text }
            .collect {
                if (it.isNotEmpty()) isSearchingLocally = true
            }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text }
            .debounce(300.milliseconds)
            .distinctUntilChanged()
            .collect {
                onSearchQueryChanged(it.toString())
                isSearchingLocally = false
            }
    }

    if (uiState.isSearching) {
        BackHandler {
            textFieldState.clearText()
            onSearchQueryChanged("")
            isSearchingLocally = false
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    if (uiState.isInSelectionMode) {
        BackHandler(onBack = onClearSelection)
    }

    if (accountToDelete != null) {
        AuthenticatorAlertDialog(
            onDismiss = { accountToDelete = null },
            onConfirm = {
                accountToDelete?.let(onDeleteAccount)
                accountToDelete = null
            },
            title = stringResource(R.string.delete_account_confirmation_title),
            text = stringResource(R.string.delete_account_confirmation_desc),
            confirmButtonText = stringResource(R.string.delete),
            dismissButtonText = stringResource(R.string.cancel),
            confirmButtonColor = MaterialTheme.colorScheme.error
        )
    }

    if (showDeleteSelectedConfirm) {
        AuthenticatorAlertDialog(
            onDismiss = { showDeleteSelectedConfirm = false },
            onConfirm = {
                onDeleteSelected()
                showDeleteSelectedConfirm = false
            },
            title = stringResource(R.string.delete_selected_confirmation_title),
            text = stringResource(
                R.string.delete_selected_confirmation_desc,
                uiState.selectedAccounts.size
            ),
            confirmButtonText = stringResource(R.string.delete),
            dismissButtonText = stringResource(R.string.cancel),
            confirmButtonColor = MaterialTheme.colorScheme.error
        )
    }

    if (uiState.accountToEdit != null) {
        EditAccountDialog(
            onDismiss = onDismissEdit,
            onSave = onUpdateAccount,
            issuerState = editIssuerState,
            accountState = editAccountNameState,
            canSave = uiState.canSaveEdit
        )
    }

    AuthenticatorScaffold(
        title = if (uiState.isInSelectionMode) {
            stringResource(R.string.selected_count, uiState.selectedAccounts.size)
        } else {
            stringResource(R.string.app_name)
        },
        modifier = modifier,
        navigationIcon = {
            if (uiState.isInSelectionMode) {
                AuthenticatorIconButton(
                    onClick = onClearSelection,
                    icon = Icons.Rounded.Close,
                    tooltipText = stringResource(R.string.clear_selection)
                )
            }
        },
        actions = {
            if (uiState.isInSelectionMode) {
                val allSelected = uiState.selectedAccounts.size == uiState.accounts.size
                AuthenticatorIconButton(
                    onClick = onSelectAll,
                    icon = Icons.Rounded.SelectAll,
                    tooltipText = stringResource(if (allSelected) R.string.deselect_all else R.string.select_all)
                )
                AuthenticatorIconButton(
                    onClick = { showDeleteSelectedConfirm = true },
                    icon = Icons.Rounded.Delete,
                    tooltipText = stringResource(R.string.delete)
                )
            } else {
                AuthenticatorIconButton(
                    onClick = onAddClick,
                    icon = Icons.Rounded.Add,
                    tooltipText = stringResource(R.string.add_account)
                )
                AuthenticatorIconButton(
                    onClick = onSettingsClick,
                    icon = Icons.Rounded.Settings,
                    tooltipText = stringResource(R.string.settings)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Surface(
                shape = SearchBarDefaults.inputFieldShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    SearchBarDefaults.InputField(
                        textFieldState = textFieldState,
                        searchBarState = searchBarState,
                        onSearch = { keyboardController?.hide() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_accounts)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (textFieldState.text.isNotEmpty()) {
                                AuthenticatorIconButton(
                                    onClick = { textFieldState.clearText() },
                                    icon = Icons.Rounded.Clear,
                                    tooltipText = stringResource(R.string.clear)
                                )
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = isSearchingLocally,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    ) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }
            }

            if (!uiState.isSecurityReliable) {
                SecurityWarningBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            if (!uiState.isSearching && uiState.accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_accounts_hint),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (uiState.isSearching) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.search_results),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (uiState.isSearching && uiState.accounts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.search_no_results),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.accounts,
                            key = { it.id }
                        ) { account ->
                            AccountItem(
                                account = account,
                                currentTimeProvider = currentTimeProvider,
                                onDecryptSecret = onDecryptSecret,
                                onEditAccount = onEditAccount,
                                onDeleteAccount = { accountToDelete = account },
                                isSelected = uiState.selectedAccounts.contains(account),
                                isSelectionMode = uiState.isInSelectionMode,
                                onToggleSelection = onToggleSelection,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountItem(
    account: Account,
    currentTimeProvider: () -> Long,
    onDecryptSecret: (ByteArray) -> ByteArray,
    onEditAccount: (Account) -> Unit,
    onDeleteAccount: () -> Unit,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTime = currentTimeProvider()
    val otpCode = remember(key1 = account, key2 = currentTime / 1000 / account.period) {
        OtpUtils.generateTotp(
            secret = account.secret,
            decrypt = onDecryptSecret,
            currentTime = currentTime,
            period = account.period,
            digits = account.digits,
            algorithm = account.algorithm
        )
    }
    AuthenticatorAccountCard(
        account = account,
        otpCode = otpCode,
        progress = OtpUtils.calculateProgress(currentTime, account.period),
        remainingSeconds = OtpUtils.calculateRemainingSeconds(
            currentTime,
            account.period
        ),
        onEdit = { onEditAccount(account) },
        onDelete = onDeleteAccount,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        onClick = {
            if (isSelectionMode) {
                onToggleSelection(account)
            }
        },
        onLongClick = {
            if (!isSelectionMode) {
                onToggleSelection(account)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun EditAccountDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    issuerState: TextFieldState,
    accountState: TextFieldState,
    canSave: Boolean
) {
    AuthenticatorAccountDialog(
        onDismiss = onDismiss,
        onConfirm = onSave,
        title = stringResource(R.string.edit_account),
        confirmButtonText = stringResource(R.string.save),
        issuerState = issuerState,
        accountState = accountState,
        confirmButtonEnabled = canSave,
    )
}

@Composable
private fun SecurityWarningBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = stringResource(R.string.security_warning_title),
                    modifier = Modifier.padding(start = 32.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = stringResource(R.string.security_warning_desc),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun HomeContentPreview() {
    HomeContent(
        uiState = HomeUiState(
            accounts = listOf(
                Account(issuer = "Google", accountName = "user@gmail.com", secret = byteArrayOf()),
                Account(issuer = "GitHub", accountName = "dev-git", secret = byteArrayOf()),
            ),
            isSecurityReliable = false
        ),
        currentTimeProvider = { System.currentTimeMillis() },
        onSearchQueryChanged = {},
        onDeleteAccount = {},
        onEditAccount = {},
        onUpdateAccount = {},
        onDecryptSecret = { it },
        onDismissEdit = {},
        editIssuerState = TextFieldState(""),
        editAccountNameState = TextFieldState(""),
        onToggleSelection = {},
        onSelectAll = {},
        onClearSelection = {},
        onDeleteSelected = {},
        onAddClick = {},
        onSettingsClick = {}
    )
}