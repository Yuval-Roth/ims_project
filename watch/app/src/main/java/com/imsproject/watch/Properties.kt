package com.imsproject.watch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val PACKAGE_PREFIX = "com.imsproject.watch"

// ============== Screen size related =============== |
var SCREEN_WIDTH : Int = -1
var SCREEN_HEIGHT : Int = -1
var TEXT_SIZE = 0.sp
var COLUMN_PADDING = 0.dp
var textStyle : TextStyle = TextStyle()
// ================================================== |

// ===================== Colors ===================== |
val DARK_BACKGROUND_COLOR = Color(0xFF333842)
val LIGHT_BLUE_COLOR = Color(0xFFACC7F6)
val VIVID_ORANGE_COLOR = Color(0xFFFF5722)
val GRAY_COLOR = Color(0xFFDEDBDB)
val GREEN_COLOR = Color(0xFF4EFF00)
val RED_COLOR = Color(0xFFFF0000)
val GLOWING_YELLOW_COLOR = Color(0xFFFFA500)
val LIGHT_GRAY_COLOR = Color(0xFFD5D5D5)
// ================================================== |

// ================= Water Ripples ================== |
var WATER_RIPPLES_BUTTON_SIZE = -1
var RIPPLE_MAX_SIZE = -1
const val WATER_RIPPLES_ANIMATION_DURATION = 2000
// ================================================== |

// ================= Wine Glasses =================== |

// general
var SCREEN_CENTER = Offset(0f,0f)
const val MARKER_FADE_DURATION = 500
const val UNDEFINED_ANGLE = 600f
const val ARC_DEFAULT_ALPHA = 0.8f
const val MAX_ANGLE_SKEW = 60f
const val MIN_ANGLE_SKEW = 15f

// my arc
const val MY_STROKE_WIDTH = 30
const val MY_SWEEP_ANGLE = 30f
var MY_RADIUS_OUTER_EDGE = 0f
var MY_RADIUS_INNER_EDGE = 0f
var MY_ARC_TOP_LEFT = Offset(0f,0f)
var MY_ARC_SIZE = Size(0f,0f)

// opponent arc
const val OPPONENT_STROKE_WIDTH = 30 / 4
const val OPPONENT_SWEEP_ANGLE = 60f
var OPPONENT_RADIUS_OUTER_EDGE = 0f
var OPPONENT_ARC_TOP_LEFT = Offset(0f,0f)
var OPPONENT_ARC_SIZE = Size(0f,0f)
// ================================================== |

// called from MainActivity.kt in onCreate()
fun initProperties(screenWidth : Int, screenHeight : Int){
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

    SCREEN_CENTER = Offset(SCREEN_WIDTH / 2f, SCREEN_WIDTH / 2f)
    MY_RADIUS_OUTER_EDGE = (SCREEN_WIDTH / 2).toFloat()
    MY_RADIUS_INNER_EDGE = (SCREEN_WIDTH / 2) * 0.2f
    MY_ARC_TOP_LEFT = Offset(SCREEN_CENTER.x - MY_RADIUS_OUTER_EDGE, SCREEN_CENTER.y - MY_RADIUS_OUTER_EDGE)
    MY_ARC_SIZE = Size(MY_RADIUS_OUTER_EDGE * 2, MY_RADIUS_OUTER_EDGE * 2)
    OPPONENT_RADIUS_OUTER_EDGE = (SCREEN_WIDTH / 2).toFloat() * 0.2f
    OPPONENT_ARC_TOP_LEFT = Offset(SCREEN_CENTER.x - OPPONENT_RADIUS_OUTER_EDGE, SCREEN_CENTER.y - OPPONENT_RADIUS_OUTER_EDGE)
    OPPONENT_ARC_SIZE = Size(OPPONENT_RADIUS_OUTER_EDGE * 2, OPPONENT_RADIUS_OUTER_EDGE * 2)
}


