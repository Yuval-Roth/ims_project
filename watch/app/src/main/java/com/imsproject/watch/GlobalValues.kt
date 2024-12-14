package com.imsproject.watch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imsproject.watch.SCREEN_HEIGHT

const val PACKAGE_PREFIX = "com.imsproject.watch"

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
var COLUMN_PADDING = 0.dp
var textStyle : TextStyle = TextStyle()

// called from MainActivity.kt in onCreate()
fun initGlobalValues(screenWidth : Int, screenHeight : Int){
    SCREEN_WIDTH = screenWidth
    SCREEN_HEIGHT = screenHeight

    WATER_RIPPLES_BUTTON_SIZE = SCREEN_WIDTH / 6

    RIPPLE_MAX_SIZE = SCREEN_WIDTH / 2

    TEXT_SIZE = (SCREEN_WIDTH * 0.03f).sp

    COLUMN_PADDING = (SCREEN_HEIGHT * 0.06f).dp

    textStyle = TextStyle(
        color = Color.White,
        fontSize = TEXT_SIZE,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Ltr
    )
}


