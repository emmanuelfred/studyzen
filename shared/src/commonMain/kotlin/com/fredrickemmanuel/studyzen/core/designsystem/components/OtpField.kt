package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * One-time-code input shown as separate boxes. A single invisible text field sits on top of the
 * boxes, so typing, deleting, pasting and the numeric keyboard all work normally.
 */
@Composable
fun OtpField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // May be refused (e.g. browsers without a user gesture); the user can still tap the boxes.
        runCatching { focusRequester.requestFocus() }
    }

    Box(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(length) { index ->
                val digit = value.getOrNull(index)?.toString().orEmpty()
                val isActive = focused && index == value.length.coerceAtMost(length - 1)
                val borderColor = when {
                    isError -> colors.error
                    isActive -> colors.primary
                    else -> colors.outlineVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.85f)
                        .background(colors.surface, shape)
                        .border(
                            width = if (isActive || isError) 2.dp else 1.dp,
                            color = borderColor,
                            shape = shape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                    )
                }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(length)) },
            modifier = Modifier
                .matchParentSize()
                .alpha(0f)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(Color.Transparent),
        )
    }
}
