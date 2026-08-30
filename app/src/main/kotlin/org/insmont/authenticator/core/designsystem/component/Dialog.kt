package org.insmont.authenticator.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.insmont.authenticator.R

@Composable
fun AuthenticatorAlertDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = onDismiss,
    title: String? = null,
    text: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmButtonText: String? = null,
    dismissButtonText: String? = null,
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary,
    confirmButtonEnabled: Boolean = true,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton ?: {
            if (confirmButtonText != null && onConfirm != null) {
                TextButton(
                    onClick = onConfirm,
                    enabled = confirmButtonEnabled
                ) {
                    Text(
                        text = confirmButtonText,
                        color = confirmButtonColor
                    )
                }
            }
        },
        modifier = modifier,
        dismissButton = dismissButton ?: dismissButtonText?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(it)
                }
            }
        },
        title = title?.let {
            {
                Text(it)
            }
        },
        text = content ?: text?.let {
            {
                Text(it)
            }
        }
    )
}

@Composable
fun <T> AuthenticatorSelectionDialog(
    onDismiss: () -> Unit,
    title: String,
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthenticatorAlertDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        title = title,
        confirmButtonText = stringResource(R.string.confirm),
        onConfirm = onDismiss,
    ) {
        Column(Modifier.selectableGroup()) {
            items.forEach { (value, label) ->
                SelectionOption(
                    text = label,
                    selected = value == selectedItem,
                    onClick = { onItemSelected(value) }
                )
            }
        }
    }
}

@Composable
private fun SelectionOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun AuthenticatorAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    confirmButtonText: String,
    issuerState: TextFieldState,
    accountState: TextFieldState,
    modifier: Modifier = Modifier,
    secretKeyState: TextFieldState? = null,
    isSecretReadOnly: Boolean = false,
    confirmButtonEnabled: Boolean? = null,
    secretTip: String? = null,
    secretOutputTransformation: OutputTransformation? = null,
    secretTrailingIcon: @Composable (() -> Unit)? = null,
) {
    val secretKeyTransformation = remember {
        InputTransformation {
            val currentText = asCharSequence().toString()
            val filteredText = currentText.uppercase(Locale.ROOT)
                .filter { it in 'A'..'Z' || it in '2'..'7' }

            if (currentText != filteredText) {
                replace(start = 0, end = length, text = filteredText)
            }
        }
    }

    AuthenticatorAlertDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        title = title,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmButtonEnabled ?: (
                        issuerState.text.isNotBlank() && accountState.text.isNotBlank() &&
                                (secretKeyState == null || isSecretReadOnly || secretKeyState.text.isNotBlank())
                        )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButtonText = stringResource(R.string.cancel),
    ) {
        val focusManager = LocalFocusManager.current
        val (issuerFocus, accountFocus, secretFocus) = remember { FocusRequester.createRefs() }

        LaunchedEffect(Unit) {
            if (issuerState.text.isEmpty()) {
                issuerFocus.requestFocus()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                state = issuerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(issuerFocus),
                label = { Text(stringResource(R.string.issuer)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                onKeyboardAction = { accountFocus.requestFocus() }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                state = accountState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(accountFocus),
                label = { Text(stringResource(R.string.account)) },
                keyboardOptions = KeyboardOptions(imeAction = if (secretKeyState != null && !isSecretReadOnly) ImeAction.Next else ImeAction.Done),
                onKeyboardAction = {
                    if (secretKeyState != null && !isSecretReadOnly) secretFocus.requestFocus() else focusManager.clearFocus()
                }
            )

            if (secretKeyState != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    state = secretKeyState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(secretFocus),
                    label = { Text(stringResource(R.string.secret_key)) },
                    readOnly = isSecretReadOnly,
                    trailingIcon = secretTrailingIcon,
                    outputTransformation = secretOutputTransformation,
                    inputTransformation = if (isSecretReadOnly) null else secretKeyTransformation,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Ascii
                    ),
                    onKeyboardAction = {
                        focusManager.clearFocus()
                    }
                )
            }

            if (secretTip != null && secretKeyState != null) {
                Text(
                    text = secretTip,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }
}

@Composable
fun AuthenticatorPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }

    AuthenticatorAlertDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        title = title,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButtonText = stringResource(R.string.cancel),
    ) {
        Column {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        }
    }
}