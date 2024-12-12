package com.imsproject.watch.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.DISMISS_BUTTON_SPACING
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.ERROR_TEXT_PADDING
import com.imsproject.watch.TEXT_SIZE
import com.imsproject.watch.textStyle

@Composable
fun ErrorScreen(error: String, onDismiss: () -> Unit) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DARK_BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(30.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState(0)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = "ERROR",
                    style = TextStyle(color = Color.White, fontSize = TEXT_SIZE, textAlign = TextAlign.Center, textDecoration = TextDecoration.Underline, letterSpacing = 1.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(top = ERROR_TEXT_PADDING)
                )
                Spacer(modifier = Modifier.height(5.dp))
                BasicText(
                    text = error,
                    style = textStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(DISMISS_BUTTON_SPACING))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    BasicText(
                        text = "Dismiss",
                        style = textStyle
                    )
                }
            }
        }
    }
}