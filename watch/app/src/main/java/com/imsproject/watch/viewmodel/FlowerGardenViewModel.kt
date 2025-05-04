package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
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
import com.imsproject.watch.INDIAN_RED_COLOR
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.BROWN_COLOR
import com.imsproject.watch.BUBBLE_PINK_COLOR
import com.imsproject.watch.FLOWER_GARDEN_SYNC_TIME_THRESHOLD
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.ORANGE_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.PURPLE_WISTERIA_COLOR
import com.imsproject.watch.SCREEN_HEIGHT
import com.imsproject.watch.WATER_BLUE_COLOR
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.WaterRipplesViewModel.Ripple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.absoluteValue


class FlowerGardenViewModel() : GameViewModel(GameType.FLOWER_GARDEN) {

    enum class ItemType(){
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

    lateinit var myItemType: ItemType
        private set


    // ======================================
    // =========== water droplet ============
    // ======================================

    class WaterDroplet(
        var timestamp: Long = 0,
    ) {
        var visible = false
        var color by mutableStateOf(WATER_BLUE_COLOR)
        val centers : List<Pair<Float, Float>> =
            listOf(polarToCartesian(SCREEN_HEIGHT/3.5f, -90 + 30.0),
                    polarToCartesian(SCREEN_HEIGHT/3.5f, -90 -30.0),
                    polarToCartesian(SCREEN_HEIGHT/3.5f,-90 -  60.0),
                    polarToCartesian(SCREEN_HEIGHT/3.5f, -90 + 60.0),
                    polarToCartesian(SCREEN_HEIGHT/3.5f, -90.0))
        var drop : Float = 0f
        fun visibleNow(timestamp: Long) {
            visible = true
            this.timestamp = timestamp
            color = color.copy(alpha = 1f)
        }
    }

    val waterDropletSets = ConcurrentLinkedDeque<WaterDroplet>()

    var freshDropletClick : Boolean = false

    // ======================================
    // ============ grass plant =============
    // ======================================

    class Plant(
        var timestamp: Long = 0,
    ) {
        var visible = false
        var color by mutableStateOf(GRASS_GREEN_COLOR)
        fun visibleNow(timestamp: Long) {
            visible = true
            this.timestamp = timestamp
            color = color.copy(alpha = 1f)
        }
    }

    var plant : Plant = Plant()
    var freshPlantClick : Boolean = false

    // ======================================
    // ============== flowers ===============
    // ======================================

    class Flower(
        var distanceFromCenter : Float,
        var angle : Double,
        var centerX : Float,
        var centerY : Float,
        var numOfPetals : Int,
        var petalWidthCoef : Float,
        var petalHeightCoef : Float,
        var centerColor : Color,
        var petalColor : Color,
    ) {}

//    var activeFlowerPoints : MutableList<Pair<Float, Double>> = mutableListOf()
//    lateinit var flowerPoints : List<Pair<Float, Double>>
//    lateinit var flowerOrder : Queue<Int>

    var activeFlowerPoints : MutableList<Flower> = mutableListOf()
    lateinit var flowerPoints : List<Flower>
    lateinit var flowerOrder : Queue<Int>


    private lateinit var clickVibration : VibrationEffect

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |


    private var _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter


    // ================================================================================ |
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
//        Log.d("FlowerViewModel", "OnCreate() called")
        super.onCreate(intent, context)
        flowerPoints = buildFlowers()
        val amountOfFlowers = flowerPoints.size
        flowerOrder = LinkedList((0 until amountOfFlowers).toList())

        clickVibration = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)

        if(ACTIVITY_DEBUG_MODE){
            myItemType = ItemType.WATER

            viewModelScope.launch(Dispatchers.Default) {
                while(true){
                    val otherPlayerId = "other player"
                    delay(2000)
                    showItem(otherPlayerId, System.currentTimeMillis())
                }
            }
            return
        }

        //decode the sent configuration data
        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
        val additionalData = intent.getStringExtra("$PACKAGE_PREFIX.additionalData")!!.split(";")
        myItemType = ItemType.fromString(additionalData[0]) //todo: change after order removed from server
//        flowerOrder = LinkedList(additionalData[1].split(",").map { it.toInt() })

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
        var opponentsLatestTimestamp =
            if((actor == playerId) == (myItemType == ItemType.WATER)) {
                waterDropletSets.addLast(WaterDroplet(timestamp))
//                waterDroplet.visibleNow(timestamp)
                freshDropletClick = true
                plant.timestamp
            } else {
                freshPlantClick = true
                plant.visibleNow(timestamp)
//                waterDroplet.timestamp
                if(waterDropletSets.isEmpty()) 0 else waterDropletSets.first().timestamp
            }

        // add new flower if synced click
        if((opponentsLatestTimestamp - timestamp)
                .absoluteValue <= FLOWER_GARDEN_SYNC_TIME_THRESHOLD) {
            if(!flowerOrder.isEmpty()) { //todo: delete later, after removing
                activeFlowerPoints.add(flowerPoints[flowerOrder.poll()!!])
            } else {
                Log.d("FlowerGardenViewModel", "ShowItem(): all the flowers had been shown.")
            }
        }

        // add a vibration effect to clicks that are not mine
        if (actor != playerId) {
            viewModelScope.launch(Dispatchers.IO) {
                delay(100)
                vibrator.vibrate(clickVibration)
            }
        }
        _counter.value++ // used to trigger recomposition
    }

    private fun buildFlowers(clockPoints: Int = 12): List<Flower> {

        return List(clockPoints) { i ->
            val distanceFromCenter = SCREEN_HEIGHT / 2.5f
            val petalCount: Int = listOf(5, 6, 7).random()
            val petalLength: Float = listOf(0.7f, 0.9f, 1.1f).random()
            val petalWidth: Float = listOf(0.4f, 0.5f, 0.6f, 0.7f).random()
            val petalColor: Color = listOf(BUBBLE_PINK_COLOR, ORANGE_COLOR, BLUE_COLOR, INDIAN_RED_COLOR).random()
            val centerColor: Color = listOf(BANANA_YELLOW_COLOR, PURPLE_WISTERIA_COLOR, ALMOST_WHITE_COLOR,
                BROWN_COLOR
            ).random()

            val angle = -90.0 + i * (360.0 / clockPoints)  // Start at 12 o'clock (−90°) and go clockwise
            val coor = polarToCartesian(distanceFromCenter, angle)
            Flower(distanceFromCenter, angle, centerX =  coor.first, centerY = coor.second, petalCount, petalWidth, petalLength, centerColor, petalColor)
        }
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}


