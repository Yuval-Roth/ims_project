package com.imsproject.watch.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.COLUMN_PADDING
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.SCREEN_RADIUS
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
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                Spacer(modifier = Modifier.height((SCREEN_RADIUS *0.05f).dp))
                val red = remember { Color(0xFFF14141) }
                Box(
                    modifier = Modifier
                        .background(color = red)
                        .fillMaxWidth()
                        .fillMaxHeight(0.10f)
                    ,
                    contentAlignment = Alignment.Center,
                ){
                    RTLText(
                        text = "שגיאה",
                        style = textStyle.copy(color = Color.Black),
                    )
                }
                Spacer(modifier = Modifier.height((SCREEN_RADIUS *0.04f).dp))
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(start= COLUMN_PADDING,end= COLUMN_PADDING)
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .verticalColumnScrollbar(scrollState, endPadding = -SCREEN_RADIUS *0.04f)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        text = error,
                        style = textStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(modifier = Modifier.height((SCREEN_RADIUS *0.04f).dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .fillMaxSize()
                    ,
                    shape = RectangleShape,
                ) {
                    RTLText(
                        text = "המשך",
                        style = textStyle.copy(color = Color.Black, letterSpacing = 1.sp),
                    )
                }
            }
        }
    }
}

@Composable
fun Modifier.verticalColumnScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    showScrollBarTrack: Boolean = true,
    scrollBarTrackColor: Color = Color.Gray,
    scrollBarColor: Color = LIGHT_BLUE_COLOR,
    scrollBarCornerRadius: Float = 4f,
    endPadding: Float = 0f
): Modifier {
    return drawWithContent {
        // Draw the column's content
        drawContent()
        // Dimensions and calculations
        val viewportHeight = this.size.height
        val totalContentHeight = scrollState.maxValue.toFloat() + viewportHeight
        if(totalContentHeight <= viewportHeight) return@drawWithContent
        val scrollValue = scrollState.value.toFloat()
        // Compute scrollbar height and position
        val scrollBarHeight =
            (viewportHeight / totalContentHeight) * viewportHeight
        val scrollBarStartOffset =
            (scrollValue / totalContentHeight) * viewportHeight
        // Draw the track (optional)
        if (showScrollBarTrack) {
            drawRoundRect(
                cornerRadius = CornerRadius(scrollBarCornerRadius),
                color = scrollBarTrackColor,
                topLeft = Offset(this.size.width - endPadding, 0f),
                size = Size(width.toPx(), viewportHeight),
            )
        }
        // Draw the scrollbar
        drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = scrollBarColor,
            topLeft = Offset(this.size.width - endPadding, scrollBarStartOffset),
            size = Size(width.toPx(), scrollBarHeight)
        )
    }
}

@Composable
fun RTLText(text: String, modifier: Modifier = Modifier, style: TextStyle = textStyle){
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            modifier = modifier,
            text = text,
            textAlign = TextAlign.Center,
            style = style.copy(textDirection = TextDirection.Rtl)
        )
    }
}

@Composable
fun RTLText(text: AnnotatedString, modifier: Modifier = Modifier, style: TextStyle = textStyle){
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            modifier = modifier,
            text = text,
            textAlign = TextAlign.Center,
            style = style.copy(textDirection = TextDirection.Rtl)
        )
    }
}

@Composable
fun BlankScreen() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DARK_BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ){

        }
    }
}

@Composable
fun LoadingScreen(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DARK_BACKGROUND_COLOR),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                strokeWidth = (SCREEN_RADIUS * 0.04f).dp,
                modifier = Modifier.fillMaxSize(0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            RTLText(text)
        }
    }
}

@Composable
fun FloatingLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            strokeWidth = (SCREEN_RADIUS * 0.04f).dp,
            modifier = Modifier.fillMaxSize(0.4f)
        )
    }
}