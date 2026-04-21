package com.emergent.codeide.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergent.codeide.ui.theme.*
import com.emergent.codeide.util.SyntaxHighlighter

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    lang: String,
    fontSize: Float,
    showSoftKeyboard: Boolean,
    syntaxOn: Boolean,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Hide soft-keyboard whenever user taps / focuses if disabled in settings
    LaunchedEffect(showSoftKeyboard) {
        if (!showSoftKeyboard) keyboardController?.hide()
    }

    val transformation = remember(lang, syntaxOn) {
        if (!syntaxOn) VisualTransformation.None
        else VisualTransformation { text ->
            val annotated = SyntaxHighlighter.highlight(text.text, lang)
            TransformedText(annotated, OffsetMapping.Identity)
        }
    }

    val style = TextStyle(
        color = VsText,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
    )

    Row(modifier.background(VsBg).fillMaxSize()) {
        LineGutter(lineCount = value.text.count { it == '\n' } + 1, fontSize = fontSize)
        Box(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { v ->
                    onValueChange(v)
                    if (!showSoftKeyboard) keyboardController?.hide()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                textStyle = style,
                visualTransformation = transformation,
                cursorBrush = SolidColor(VsAccent),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    imeAction = ImeAction.Default,
                    keyboardType = if (showSoftKeyboard) KeyboardType.Ascii else KeyboardType.Text
                ),
                // When showSoftKeyboard=false we intercept via LaunchedEffect & hide IME.
            )
        }
    }

    // Track focus/touch to hide keyboard promptly when disabled
    LaunchedEffect(Unit) {
        if (!showSoftKeyboard) keyboardController?.hide()
    }
}

@Composable
private fun LineGutter(lineCount: Int, fontSize: Float) {
    val width = (30 + (lineCount.toString().length * 8)).dp
    Column(
        Modifier
            .fillMaxHeight()
            .width(width)
            .background(VsPanel)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.End
    ) {
        for (i in 1..lineCount) {
            androidx.compose.material3.Text(
                text = i.toString(),
                color = VsTextMuted,
                fontSize = fontSize.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.height((fontSize * 1.45f).dp)
            )
        }
    }
}
