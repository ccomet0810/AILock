package com.ccomet.ailock.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.PandaOrange

@Composable
fun AilockOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        shape = AILockShape.control,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PandaOrange,
            unfocusedBorderColor = AppBorder,
            focusedContainerColor = AppSurface,
            unfocusedContainerColor = AppSurface,
        ),
    )
}
