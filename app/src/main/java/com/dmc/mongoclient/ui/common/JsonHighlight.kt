package com.dmc.mongoclient.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.dmc.mongoclient.data.mongo.JsonTokenKind
import com.dmc.mongoclient.data.mongo.tokenizeJson
import com.dmc.mongoclient.ui.theme.JsonSyntaxColors
import com.dmc.mongoclient.ui.theme.rememberJsonSyntaxColors

/** Builds an AnnotatedString with per-token colour spans. Memoised on text + colours. */
@Composable
fun highlightedJson(text: String): AnnotatedString {
    val colors = rememberJsonSyntaxColors()
    return remember(text, colors) { applyHighlighting(text, colors) }
}

/** VisualTransformation that highlights without changing offsets — safe for editable fields. */
@Composable
fun rememberJsonHighlightTransformation(): VisualTransformation {
    val colors = rememberJsonSyntaxColors()
    return remember(colors) { JsonHighlightTransformation(colors) }
}

private class JsonHighlightTransformation(
    private val colors: JsonSyntaxColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(applyHighlighting(text.text, colors), OffsetMapping.Identity)
}

private fun applyHighlighting(text: String, colors: JsonSyntaxColors): AnnotatedString =
    buildAnnotatedString {
        append(text)
        tokenizeJson(text).forEach { token ->
            val color = when (token.kind) {
                JsonTokenKind.KEY -> colors.key
                JsonTokenKind.STRING -> colors.string
                JsonTokenKind.NUMBER -> colors.number
                JsonTokenKind.KEYWORD -> colors.keyword
            }
            addStyle(SpanStyle(color = color), token.start, token.end)
        }
    }
