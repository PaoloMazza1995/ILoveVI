package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/**
 * General-purpose labeled text field — [IloveViSearchField]'s exact visual shape (bg-800
 * background, border outline, radius-sm, 48dp minimum height) generalized for name/zone/
 * description/email/password fields, which `SearchField` can't be used for (no label, no password
 * masking, single line only). Introduced for the Map Editor Functional Analysis's sign-in dialog
 * and inline entry form (EDT-01, EDT-54), which today fall back to a raw, un-themed Material3
 * `OutlinedTextField` — this is the design-system home for that instead.
 *
 * [errorText], when non-null, is shown below in [IloveViColors.warningYellow] (e.g. "Name is
 * required" EDT-54, "Image too large" EDT-80's edge case) — a hint/validation message, not a hard
 * Material3 error color (this design system has none, see `Theme.kt`'s comment on why).
 */
@Composable
fun IloveViTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    errorText: String? = null,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = IloveViColors.ink)
    val visualTransformation =
        if (isPassword) PasswordVisualTransformation() else VisualTransformation.None

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = IloveViSpacing.space1),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(IloveViShapes.radiusSm)
                .background(IloveViColors.bg800, IloveViShapes.radiusSm)
                .border(1.dp, IloveViColors.border, IloveViShapes.radiusSm)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(text = placeholder, color = IloveViColors.inkMuted, style = textStyle)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(IloveViColors.ink),
                // [modifier] lands on the actual editable field, not the label/error wrapper above
                // — a caller tagging/sizing/focusing "the email field" means the input itself.
                modifier = modifier.fillMaxWidth(),
            )
        }
        errorText?.let {
            Text(
                text = it,
                color = IloveViColors.warningYellow,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = IloveViSpacing.space1),
            )
        }
    }
}

@Preview
@Composable
private fun IloveViTextFieldEmptyPreview() {
    IloveViTheme {
        IloveViTextField(
            value = "",
            onValueChange = {},
            label = "Name",
            placeholder = "e.g. Falcon .45"
        )
    }
}

@Preview
@Composable
private fun IloveViTextFieldPasswordPreview() {
    IloveViTheme {
        IloveViTextField(
            value = "hunter2",
            onValueChange = {},
            label = "Password",
            isPassword = true
        )
    }
}

@Preview
@Composable
private fun IloveViTextFieldErrorPreview() {
    IloveViTheme {
        IloveViTextField(
            value = "",
            onValueChange = {},
            label = "Name",
            errorText = "Name is required",
        )
    }
}

@Preview
@Composable
private fun IloveViTextFieldMultilinePreview() {
    IloveViTheme {
        IloveViTextField(
            value = "A compact .45 pistol favored by Vice City street crews.",
            onValueChange = {},
            label = "Description",
            singleLine = false,
        )
    }
}
