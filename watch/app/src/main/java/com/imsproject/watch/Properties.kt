package com.imsproject.watch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imsproject.common.utils.Angle

const val PACKAGE_PREFIX = "com.imsproject.watch"

const val ACTIVITY_DEBUG_MODE = true // set true to be able run the activity directly from the IDE

// ============== Screen size related =============== |

var SCREEN_RADIUS : Float = 0f
var SCREEN_CENTER = Offset(0f,0f)
var TEXT_SIZE = 0.sp
var COLUMN_PADDING = 0.dp
var textStyle : TextStyle = TextStyle()

// ===================== Colors ===================== |

val DARK_BACKGROUND_COLOR = Color(0xFF333842)
//val LIGHT_BACKGROUND_COLOR = Color(0xFFE2DFD1)
val DARK_GREEN_BACKGROUND_COLOR = Color(0xff65784C)
val WHITE_ANTIQUE_COLOR = Color(0xFFF0E5D3)//ivory
val LIGHT_BLUE_COLOR = Color(0xFFACC7F6)
val BLUE_COLOR = Color(0xFF87B0F3)
val WATER_BLUE_COLOR = Color(0xFF7CC1D6)
val VIVID_ORANGE_COLOR = Color(0xFFFF5722)
val GRAY_COLOR = Color(0xFFDEDBDB)
val GREEN_COLOR = Color(0xFF4EFF00)
val GRASS_GREEN_COLOR = Color(0xFF7DC482)
val RED_COLOR = Color(0xFFFF0000)
val INDIAN_RED_COLOR = Color(0xffDE6C68)
val GLOWING_YELLOW_COLOR = Color(0xFFFFA500)
val BANANA_YELLOW_COLOR = Color(0xFFEFE080)
val ORANGE_COLOR = Color(0xFFFFAE45)
val LIGHT_GRAY_COLOR = Color(0xFFD5D5D5)
val BRIGHT_CYAN_COLOR = Color(0xFF20BECE)
val CYAN_COLOR = Color(0xFF00BCD4)
val BROWN_COLOR = Color(0xFF4E342E)
val LIGHTER_BROWN_COLOR = Color(0xFF725934)
val DARKER_BROWN_COLOR = Color(0xff4F3B1C)
val DARKER_DARKER_BROWN_COLOR = Color(0xff3F2F17)
val LIGHT_BROWN_COLOR = Color(0xFFAF746C)
val DARK_BEIGE_COLOR = Color(0xFFA59E7E)
val BUBBLE_PINK_COLOR = Color(0xFFF1A5BB)
val PURPLE_WISTERIA_COLOR = Color(0xFFC495DA)
val ALMOST_WHITE_COLOR = Color(0xFFECECEC)
val SILVER_COLOR = Color(0xFFC0C0C0)
val DEEP_BLUE_COLOR = Color(0xFF294168)
val LIGHT_ORANGE_COLOR = Color(0xFFD9A978)
val DARK_ORANGE_COLOR = Color(0xFFC0674F)

// ================= Water Ripples ================== |

var WATER_RIPPLES_SYNC_TIME_THRESHOLD = 100
const val WATER_RIPPLES_ANIMATION_DURATION = 2000
var WATER_RIPPLES_BUTTON_SIZE = 0
var RIPPLE_MAX_SIZE = 0

// ================= Wine Glasses =================== |

var WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD = 0.2f

// tracks
const val LOW_BUILD_IN_TRACK = 0
const val LOW_LOOP_TRACK = 1
const val LOW_BUILD_OUT_TRACK = 2
const val HIGH_LOOP_TRACK = 4
const val RUB_LOOP_TRACK = 5

// ================= Flour Mill ===================== |

var FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD = 0.2f

// tracks
const val MILL_SOUND_TRACK = 0

// =================== Arc Related =================== |

// general
const val MARKER_FADE_DURATION = 500
const val ARC_DEFAULT_ALPHA = 0.8f
const val MAX_ANGLE_SKEW = 60f
const val MIN_ANGLE_SKEW = 15f
var OUTER_TOUCH_POINT = 0f
var INNER_TOUCH_POINT = 0f

// my arc
const val MY_STROKE_WIDTH = 15
const val MY_SWEEP_ANGLE = 100f
var MY_RADIUS_OUTER_EDGE = 0f
var MY_ARC_TOP_LEFT = Offset(0f,0f)
var MY_ARC_SIZE = Size(0f,0f)

// opponent arc
const val OPPONENT_STROKE_WIDTH = 15
const val OPPONENT_SWEEP_ANGLE = 45f
var OPPONENT_RADIUS_OUTER_EDGE = 0f
var OPPONENT_ARC_TOP_LEFT = Offset(0f,0f)
var OPPONENT_ARC_SIZE = Size(0f,0f)

// =================== General ====================== |
var FREQUENCY_HISTORY_MILLISECONDS = 1000L

// ================= Flower Garden ===================== |
var FLOWER_GARDEN_SYNC_TIME_THRESHOLD = 200
// flowers
var FLOWER_RING_OFFSET_ANGLE = 11.25
var AMOUNT_OF_FLOWERS = 12
// water droplets
var WATER_DROPLET_FADE_COEFFICIENT = -0.05f
var WATER_DROPLET_FADE_THRESHOLD = 0.05f
// grass plant
var GRASS_PLANT_FADE_COEFFICIENT = -0.05f
var GRASS_PLANT_FADE_THRESHOLD = 0.05f
var GRASS_PLANT_BASE_HEIGHT = 30f
var GRASS_PLANT_BASE_WIDTH = 15f
var GRASS_PLANT_STROKE_WIDTH = 4.5f
//shared
var GRASS_WATER_RADIUS = 0f //initialized later
var GRASS_WATER_ANGLE = 36
var GRASS_WATER_VISIBILITY_THRESHOLD = 250
var CURRENT_PLAYER_ITEM_ALPHA = 0.4f
var OPPONENT_PLAYER_ITEM_ALPHA = 0.9f

// ===================== Pacman ===================== |

const val PACMAN_ROTATION_DURATION = 2800f
const val PACMAN_MOUTH_OPENING_ANGLE = 66f
const val PACMAN_ANGLE_STEP = 360f / (PACMAN_ROTATION_DURATION / 16f)
const val PACMAN_START_ANGLE = -90f + PACMAN_MOUTH_OPENING_ANGLE / 2f
const val PACMAN_SWEEP_ANGLE = 360f - PACMAN_MOUTH_OPENING_ANGLE
const val PARTICLE_CAGE_STROKE_WIDTH = 4f
const val PACMAN_SHRINK_ANIMATION_DURATION = (((180f - PACMAN_MOUTH_OPENING_ANGLE*2f) / 360f) * PACMAN_ROTATION_DURATION).toInt()
const val REWARD_SIZE_BONUS = 0.008f
val PARTICLE_CAGE_COLOR = Color(0xFF0000FF)
val PARTICLE_COLOR = Color(0xFFF3D3C3)
const val PARTICLE_ANIMATION_MAX_DURATION = 750
const val PARTICLE_ANIMATION_MIN_DURATION = 150
var PARTICLE_RADIUS = 0f
var PARTICLE_DISTANCE_FROM_CENTER = 0f
var PACMAN_MAX_SIZE = 0f

// =================== Waves ===================== |

const val WAVE_MAX_ANIMATION_DURATION = 5000
const val WAVE_MIN_ANIMATION_DURATION = 1500

// ================== After game questions =============== |

const val FIRST_QUESTION = "עד כמה חשת תחושת \"ביחד\" עם השותפ/ה במשחקון הזה?"
const val SECOND_QUESTION = "עד כמה הצלחתם לפעול ביחד?"


// =================== initProperties =================== |

// called from MainActivity.kt in onCreate()
fun initProperties(screenWidth: Int){
    SCREEN_RADIUS = screenWidth / 2f
    SCREEN_CENTER = Offset(SCREEN_RADIUS,SCREEN_RADIUS)

    TEXT_SIZE = (SCREEN_RADIUS * 0.06f).sp
    COLUMN_PADDING = (SCREEN_RADIUS * 0.12f).dp
    textStyle = TextStyle(
        color = Color.White,
        fontSize = TEXT_SIZE,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Ltr
    )

    // Water Ripples
    WATER_RIPPLES_BUTTON_SIZE = (SCREEN_RADIUS / 3).toInt()
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
    // TODO: add properties

    //flower garden
    GRASS_WATER_RADIUS = (SCREEN_RADIUS * 2f) / 4f

    // Pacman
    PARTICLE_RADIUS = SCREEN_RADIUS * 0.02f
    PARTICLE_DISTANCE_FROM_CENTER = SCREEN_RADIUS * 0.88f
    PACMAN_MAX_SIZE = SCREEN_RADIUS * 0.7f
}


