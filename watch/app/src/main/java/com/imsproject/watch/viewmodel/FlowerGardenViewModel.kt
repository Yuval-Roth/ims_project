package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.ALMOST_WHITE_COLOR
import com.imsproject.watch.BANANA_YELLOW_COLOR
import com.imsproject.watch.AMOUNT_OF_FLOWERS
import com.imsproject.watch.BROWN_COLOR
import com.imsproject.watch.BUBBLE_PINK_COLOR
import com.imsproject.watch.DEEP_BLUE_COLOR
import com.imsproject.watch.FLOWER_GARDEN_SYNC_TIME_THRESHOLD
import com.imsproject.watch.FLOWER_RING_OFFSET_ANGLE
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.GRASS_WATER_ANGLE
import com.imsproject.watch.GRASS_WATER_RADIUS
import com.imsproject.watch.ORANGE_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.PURPLE_WISTERIA_COLOR
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WATER_BLUE_COLOR
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.absoluteValue


class FlowerGardenViewModel : GameViewModel(GameType.FLOWER_GARDEN) {

    enum class ItemType{
        WATER,
        PLANT;

        companion object {
            fun fromString(string: String) = when(string.lowercase()){
                "plant" -> PLANT
                "water" -> WATER
                else -> throw IllegalArgumentException("invalid string")
            }
        }
    }

    private lateinit var myItemType: ItemType


    // ======================================
    // =========== water droplet ============
    // ======================================

    class WaterDroplet(
        var timestamp: Long = 0,
    ) {
        var color by mutableStateOf(WATER_BLUE_COLOR)
        val centers : List<Pair<Float, Float>> =
            listOf(polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 0 * GRASS_WATER_ANGLE),
                    polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 2 * -GRASS_WATER_ANGLE),
                    polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 2 * GRASS_WATER_ANGLE),
                    polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 4 * -GRASS_WATER_ANGLE),
                    polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 4 * GRASS_WATER_ANGLE)
            )
        var time = 0
        var drop = 0f
    }
    val waterDropletSets = ConcurrentLinkedDeque<WaterDroplet>()

    // ======================================
    // ============ grass plant =============
    // ======================================

    class Plant(
        var timestamp: Long = 0,
    ) {
        val centers : List<Pair<Float, Float>> =
            listOf(polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 5 * -GRASS_WATER_ANGLE),
                polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 1 * GRASS_WATER_ANGLE),
                polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 1 * -GRASS_WATER_ANGLE),
                polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 3 * GRASS_WATER_ANGLE),
                polarToCartesian(GRASS_WATER_RADIUS, -90.0 + 3 * -GRASS_WATER_ANGLE)
            )
        var color by mutableStateOf(GRASS_GREEN_COLOR)
        var time = 0
        var sway : Float = 0f
    }

    val grassPlantSets = ConcurrentLinkedDeque<Plant>()

    // ======================================
    // ============== flowers ===============
    // ======================================

    class Flower(
        var centerX : Float,
        var centerY : Float,
        var numOfPetals : Int,
        var petalWidthCoef : Float,
        var petalHeightCoef : Float,
        var centerColor : Color,
        var petalColor : Color,
    )

    private val amountOfFlowers = AMOUNT_OF_FLOWERS
    private lateinit var flowerPoints : List<Flower>
    private var _currFlowerIndex = MutableStateFlow(-1)
    val currFlowerIndex: StateFlow<Int> = _currFlowerIndex
    var activeFlowerPoints : ConcurrentLinkedDeque<Flower> = ConcurrentLinkedDeque()

    private lateinit var clickVibration : VibrationEffect
    private lateinit var soundPool: SoundPool
    private var bellSoundId : Int = -1

    private var colorsList : List<Pair<Color, Color>> =
        listOf(
            Pair(ORANGE_COLOR, BROWN_COLOR),
            Pair(ALMOST_WHITE_COLOR, BANANA_YELLOW_COLOR),
            Pair(BUBBLE_PINK_COLOR, ALMOST_WHITE_COLOR),
            Pair(PURPLE_WISTERIA_COLOR, DEEP_BLUE_COLOR)
        )
    private var colorsListIndex = 0

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    //tracks the new taps
    private var _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter


    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun click() {
        if(ACTIVITY_DEBUG_MODE){
            Log.d("", "pressing")
            showItem(playerId, System.currentTimeMillis())
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = super.getCurrentGameTime()
            model.sendUserInput(timestamp,packetTracker.newPacket())
            addEvent(SessionEvent.click(playerId,timestamp))
        }
    }

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)
        clickVibration = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        soundPool = SoundPool.Builder().setAudioAttributes(
            AudioAttributes.Builder().setUsage(
                AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        bellSoundId = soundPool.load(context, R.raw.flower_bell, 1)


        if(ACTIVITY_DEBUG_MODE){
//            myItemType = ItemType.WATER
            myItemType = ItemType.PLANT

            viewModelScope.launch(Dispatchers.Default) {
                while(true){
                    val otherPlayerId = "other player"
                    delay(1000)
                    showItem(otherPlayerId, System.currentTimeMillis())
                }
            }
            return
        }

        //decode the sent configuration data
        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
        val additionalData = intent.getStringExtra("$PACKAGE_PREFIX.additionalData")!!.split(";")
        myItemType = ItemType.fromString(additionalData[0]) //todo: change after order removed from server

        if (syncTolerance <= 0L) {
            exitWithError("Missing sync tolerance", Result.Code.BAD_REQUEST)
            return
        }
        FLOWER_GARDEN_SYNC_TIME_THRESHOLD = syncTolerance.toInt()
        Log.d(TAG, "syncTolerance: $syncTolerance")
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    /**
     * handles game actions
     */
    override suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.USER_INPUT -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in user input action")
                    return
                }
                val timestamp = action.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameAction: missing timestamp in user input action")
                    return
                }
                val sequenceNumber = action.sequenceNumber ?: run{
                    Log.e(TAG, "handleGameAction: missing sequence number in user input action")
                    return
                }

                val arrivedTimestamp = getCurrentGameTime()
                showItem(actor, timestamp)
                
                if(actor == playerId){
                    packetTracker.receivedMyPacket(sequenceNumber)
                } else {
                    packetTracker.receivedOtherPacket(sequenceNumber)
                    addEvent(SessionEvent.opponentClick(playerId, arrivedTimestamp))
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    private fun showItem(actor: String, timestamp : Long) {
        // check the delta between taps and show new tap
        val opponentsLatestTimestamp =
            if((actor == playerId) == (myItemType == ItemType.WATER)) {
                waterDropletSets.addLast(WaterDroplet(timestamp))
                if(grassPlantSets.isEmpty()) 0 else grassPlantSets.last().timestamp
            } else {
                grassPlantSets.addLast(Plant(timestamp))
                if(waterDropletSets.isEmpty()) 0 else waterDropletSets.last().timestamp
            }

        // add new flower if synced click
        if((opponentsLatestTimestamp - timestamp)
                .absoluteValue <= FLOWER_GARDEN_SYNC_TIME_THRESHOLD) {
            _currFlowerIndex.value = (_currFlowerIndex.value + 1) % amountOfFlowers

            //generate next dozen of flowers
            if(activeFlowerPoints.size % amountOfFlowers == 0) {
                val iter = (activeFlowerPoints.size / amountOfFlowers)
                flowerPoints = buildFlowers(
                    petalColor = colorsList[colorsListIndex].first,
                    centerColor = colorsList[colorsListIndex].second,
                    angleOffset = FLOWER_RING_OFFSET_ANGLE * iter)
                colorsListIndex = (colorsListIndex + 1) %  colorsList.size
            }
            //add new active flower
            activeFlowerPoints.add(flowerPoints[_currFlowerIndex.value])

            //sound and vibration in sync
            viewModelScope.launch(Dispatchers.IO) {
                soundPool.play(bellSoundId, 1f, 1f, 0, 0, 1f)
                delay(100)
                vibrator.vibrate(clickVibration)
            }
        }
        _counter.value++ // used to trigger recomposition
    }

    private fun buildFlowers(petalColor: Color, centerColor: Color, angleOffset: Double): List<Flower> {
        return List(amountOfFlowers) { i ->
            val distanceFromCenter = (SCREEN_RADIUS * 2f)  / 2.5f
            val petalCount: Int = listOf(5, 6, 7).random()
            val petalLength: Float = listOf(0.7f, 0.9f, 1.1f).random()
            val petalWidth: Float = listOf(0.4f, 0.5f, 0.6f, 0.7f).random()

            val angle = -90.0 + i * (360.0 / amountOfFlowers) + angleOffset // Start at 12 o'clock (−90°) and go clockwise
            val coor = polarToCartesian(distanceFromCenter, angle)
            Flower(centerX =  coor.first, centerY = coor.second, petalCount, petalWidth, petalLength, centerColor, petalColor)
        }
    }

    companion object {
        private const val TAG = "FlowerGardenViewModel"
    }
}


