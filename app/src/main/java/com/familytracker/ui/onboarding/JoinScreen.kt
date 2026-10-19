package com.familytracker.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familytracker.R
import com.familytracker.ui.components.GreyTextField
import com.familytracker.ui.components.PrimaryButton
import java.security.SecureRandom

// No 0/O/1/I/L so codes are easy to read out loud.
private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
private const val NEW_CODE_LENGTH = 8

private fun generateCode(): String {
    val random = SecureRandom()
    return (1..NEW_CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
}

fun isValidCode(code: String) = code.length in 6..8 && code.all { it.isLetterOrDigit() }

@Composable
fun JoinScreen(
    initialName: String,
    joining: Boolean,
    error: String?,
    onBack: () -> Unit,
    onJoin: (name: String, code: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var code by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val nameError = name.isBlank()
    val codeError = !isValidCode(code)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.join_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.join_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            GreyTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                placeholder = stringResource(R.string.your_name),
            )
            if (showErrors && nameError) ErrorText(stringResource(R.string.error_name))
            Spacer(Modifier.height(16.dp))

            GreyTextField(
                value = code,
                onValueChange = { input -> code = input.filter { it.isLetterOrDigit() }.uppercase().take(8) },
                placeholder = stringResource(R.string.family_code),
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            )
            if (showErrors && codeError) ErrorText(stringResource(R.string.error_code))

            TextButton(onClick = { code = generateCode() }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Text(
                    "  " + stringResource(R.string.generate_code),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (error != null) ErrorText(error)
        }

        PrimaryButton(
            text = stringResource(R.string.join),
            loading = joining,
            onClick = {
                showErrors = true
                if (!nameError && !codeError) onJoin(name.trim(), code)
            },
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
    )
}
