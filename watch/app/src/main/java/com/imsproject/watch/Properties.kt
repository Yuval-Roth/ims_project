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

const val ACTIVITY_DEBUG_MODE = false // set true to be able run the activity directly from the IDE

// ============== Screen size related =============== |

var SCREEN_WIDTH : Int = 0
var SCREEN_HEIGHT : Int = 0
var SCREEN_RADIUS : Float = 0f
var SCREEN_CENTER = Offset(0f,0f)
var TEXT_SIZE = 0.sp
var COLUMN_PADDING = 0.dp
var textStyle : TextStyle = TextStyle()

// ===================== Colors ===================== |

val DARK_BACKGROUND_COLOR = Color(0xFF333842)
val LIGHT_BLUE_COLOR = Color(0xFFACC7F6)
val BLUE_COLOR = Color(0xFF87B0F3)
val VIVID_ORANGE_COLOR = Color(0xFFFF5722)
val GRAY_COLOR = Color(0xFFDEDBDB)
val GREEN_COLOR = Color(0xFF4EFF00)
val RED_COLOR = Color(0xFFFF0000)
val GLOWING_YELLOW_COLOR = Color(0xFFFFA500)
val LIGHT_GRAY_COLOR = Color(0xFFD5D5D5)
val BRIGHT_CYAN_COLOR = Color(0xFF20BECE)
val CYAN_COLOR = Color(0xFF00BCD4)
val BROWN_COLOR = Color(0xFF4E342E)
val LIGHT_BROWN_COLOR = Color(0xFFAF746C)
val ALMOST_WHITE_COLOR = Color(0xFFECECEC)
val SILVER_COLOR = Color(0xFFC0C0C0)

// ================= Water Ripples ================== |
var WATER_RIPPLES_SYNC_TIME_THRESHOLD = 50
const val WATER_RIPPLES_ANIMATION_DURATION = 2000
var WATER_RIPPLES_BUTTON_SIZE = 0
var RIPPLE_MAX_SIZE = 0

// ================= Wine Glasses =================== |

// general
const val MARKER_FADE_DURATION = 500
const val UNDEFINED_ANGLE = 600f
const val ARC_DEFAULT_ALPHA = 0.8f
const val MAX_ANGLE_SKEW = 60f
const val MIN_ANGLE_SKEW = 15f
var WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD = 0.5f
var FREQUENCY_HISTORY_MILLISECONDS = 1000L
var OUTER_TOUCH_POINT = 0f
var INNER_TOUCH_POINT = 0f

// my arc
const val MY_STROKE_WIDTH = 15
const val MY_SWEEP_ANGLE = 45f
var MY_RADIUS_OUTER_EDGE = 0f
var MY_ARC_TOP_LEFT = Offset(0f,0f)
var MY_ARC_SIZE = Size(0f,0f)

// opponent arc
const val OPPONENT_STROKE_WIDTH = 15
const val OPPONENT_SWEEP_ANGLE = 45f
var OPPONENT_RADIUS_OUTER_EDGE = 0f
var OPPONENT_ARC_TOP_LEFT = Offset(0f,0f)
var OPPONENT_ARC_SIZE = Size(0f,0f)

// tracks
const val LOW_BUILD_IN_TRACK = 0
const val LOW_LOOP_TRACK = 1
const val LOW_BUILD_OUT_TRACK = 2
const val HIGH_LOOP_TRACK = 4

// ================= Flour Mill ===================== |

const val AXLE_STARTING_ANGLE = -90f
var FLOUR_MILL_SYNC_TIME_THRESHOLD = 100L
const val RESET_COOLDOWN_WAIT_TIME = 16
const val STRETCH_PEAK = 12.0f
// ============================================ |
// these values must be a normal fraction of STRETCH_PEAK to ensure that we reach
// the target angle exactly and not overshoot it
const val STRETCH_PEAK_DECAY = STRETCH_PEAK / 4
const val STRETCH_STEP = STRETCH_PEAK / 4
// ============================================ |
var BEZIER_START_DISTANCE = 0f
var CONTROL_POINT_DISTANCE = 0f
var STRETCH_POINT_DISTANCE = 0f
var AXLE_WIDTH = 0f
var AXLE_HANDLE_LENGTH = 0f

// =================== initProperties =================== |

// called from MainActivity.kt in onCreate()
fun initProperties(screenWidth : Int, screenHeight : Int){
    SCREEN_WIDTH = screenWidth
    SCREEN_HEIGHT = screenHeight
    SCREEN_RADIUS = SCREEN_WIDTH / 2f
    SCREEN_CENTER = Offset(SCREEN_WIDTH / 2f, SCREEN_WIDTH / 2f)

    TEXT_SIZE = (SCREEN_WIDTH * 0.03f).sp
    COLUMN_PADDING = (SCREEN_HEIGHT * 0.06f).dp
    textStyle = TextStyle(
        color = Color.White,
        fontSize = TEXT_SIZE,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Ltr
    )

    // Water Ripples
    WATER_RIPPLES_BUTTON_SIZE = SCREEN_WIDTH / 6
    RIPPLE_MAX_SIZE = SCREEN_RADIUS.toInt()

    // Wine Glasses
    OUTER_TOUCH_POINT = SCREEN_RADIUS
    INNER_TOUCH_POINT = SCREEN_RADIUS * 0.2f
    MY_RADIUS_OUTER_EDGE = SCREEN_RADIUS * 0.6f
    MY_ARC_TOP_LEFT = Offset(SCREEN_CENTER.x - MY_RADIUS_OUTER_EDGE , SCREEN_CENTER.y - MY_RADIUS_OUTER_EDGE)
    MY_ARC_SIZE = Size(MY_RADIUS_OUTER_EDGE * 2, MY_RADIUS_OUTER_EDGE * 2)
    OPPONENT_RADIUS_OUTER_EDGE = SCREEN_RADIUS * 0.6f
    OPPONENT_ARC_TOP_LEFT = Offset(SCREEN_CENTER.x - OPPONENT_RADIUS_OUTER_EDGE, SCREEN_CENTER.y - OPPONENT_RADIUS_OUTER_EDGE)
    OPPONENT_ARC_SIZE = Size(OPPONENT_RADIUS_OUTER_EDGE * 2, OPPONENT_RADIUS_OUTER_EDGE * 2)

    // Flour Mill
    CONTROL_POINT_DISTANCE = SCREEN_RADIUS * 0.5f
    BEZIER_START_DISTANCE = SCREEN_RADIUS * 0.2f
    STRETCH_POINT_DISTANCE = SCREEN_RADIUS * 0.9f
    AXLE_WIDTH = SCREEN_WIDTH * 0.04f
    AXLE_HANDLE_LENGTH = AXLE_WIDTH * 4
}


