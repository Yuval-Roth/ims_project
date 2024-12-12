package com.imsproject.watch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ===================== Colors ===================== |
val DARK_BACKGROUND_COLOR = Color(0xFF333842)
val LIGHT_BLUE_COLOR = Color(0xFFACC7F6)
val VIVID_ORANGE_COLOR = Color(0xFFFF5722)
val GRAY_COLOR = Color(0xFFDEDBDB)
val GREEN_COLOR = Color(0xFF4EFF00)
val RED_COLOR = Color(0xFFFF0000)
// ================================================== |

// ================= Water Ripples ================== |
var WATER_RIPPLES_BUTTON_SIZE = -1
var RIPPLE_MAX_SIZE = -1
const val WATER_RIPPLES_ANIMATION_DURATION = 2000
// ================================================== |

var SCREEN_WIDTH : Int = -1
var SCREEN_HEIGHT : Int = -1
var TEXT_SIZE = 0.sp
var DISMISS_BUTTON_SPACING = 0.dp
var READY_BUTTON_SPACING = 0.dp
var ERROR_TEXT_PADDING = 0.dp
var CONNECTING_SCREEN_CIRCLE_SIZE = 0.dp
var CONNECTING_SCREEN_STROKE_WIDTH = 0.dp
var textStyle : TextStyle = TextStyle()

// called from MainActivity.kt in onCreate()
fun initGlobalValues(screenWidth : Int, screenHeight : Int){
    SCREEN_WIDTH = screenWidth
    SCREEN_HEIGHT = screenHeight

    WATER_RIPPLES_BUTTON_SIZE = SCREEN_WIDTH / 6

    RIPPLE_MAX_SIZE = SCREEN_WIDTH / 2

    TEXT_SIZE = if(SCREEN_WIDTH > 400) 14.sp else 12.sp

    DISMISS_BUTTON_SPACING = if(SCREEN_WIDTH > 400) 30.dp else 15.dp

    READY_BUTTON_SPACING = if(SCREEN_WIDTH > 400) 20.dp else 10.dp

    ERROR_TEXT_PADDING = if(SCREEN_WIDTH > 400) 20.dp else 10.dp

    CONNECTING_SCREEN_CIRCLE_SIZE = if(SCREEN_WIDTH > 400) 75.dp else 65.dp

    CONNECTING_SCREEN_STROKE_WIDTH = if(SCREEN_WIDTH > 400) 10.dp else 8.dp

    textStyle = TextStyle(
        color = Color.White,
        fontSize = TEXT_SIZE,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Ltr
    )
}


