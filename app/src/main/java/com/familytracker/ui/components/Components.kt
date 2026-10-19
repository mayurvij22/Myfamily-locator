package com.familytracker.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familytracker.ui.theme.AlertRed
import com.familytracker.ui.theme.Black
import com.familytracker.ui.theme.White
import com.familytracker.util.Format

val CardShape = RoundedCornerShape(12.dp)
val SheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

/** Full-width 56 dp black button (inverted in dark mode). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = CardShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.7f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = contentColor, strokeWidth = 2.dp)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Large grey borderless input, like a phone-number entry field. */
@Composable
fun GreyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    imeAction: ImeAction = ImeAction.Next,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = textStyle) },
        singleLine = true,
        textStyle = textStyle,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth().height(60.dp),
    )
}

/** Black circle with white initials; used for markers, chips and rows. */
@Composable
fun Avatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Boolean = false,
    // Pure black disappears on the dark sheet, so lift it slightly there.
    color: Color = if (isSystemInDarkTheme() && !ring) Color(0xFF3A3A3A) else Black,
) {
    Surface(
        shape = CircleShape,
        color = color,
        modifier = modifier
            .size(size)
            .then(if (ring) Modifier.border(2.dp, White, CircleShape) else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = Format.initials(name),
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp,
                fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
            )
        }
    }
}

/** Small red pill, e.g. "At same place 3 h 10 min". */
@Composable
fun AlertTag(text: String, modifier: Modifier = Modifier) {
    Surface(color = AlertRed, shape = RoundedCornerShape(4.dp), modifier = modifier) {
        Text(
            text = text,
            color = White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}
