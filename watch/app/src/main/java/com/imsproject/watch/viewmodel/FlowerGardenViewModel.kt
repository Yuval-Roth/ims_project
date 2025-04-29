package com.imsproject.watch.viewmodel

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.FLOWER_GARDEN_SYNC_TIME_THRESHOLD
import com.imsproject.watch.GRAY_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.VIVID_ORANGE_COLOR
import com.imsproject.watch.WATER_RIPPLES_ANIMATION_DURATION
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.WATER_RIPPLES_SYNC_TIME_THRESHOLD
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.FlourMillViewModel.AxleSide
import com.imsproject.watch.viewmodel.WaterRipplesViewModel.Ripple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.absoluteValue


class FlowerGardenViewModel() : GameViewModel(GameType.FLOWER_GARDEN) {

    enum class ItemType(){
        WATER,
        PLANT,
        FLOWER;

        companion object {
            fun fromString(string: String) = when(string.lowercase()){
                "plant" -> PLANT
                "water" -> WATER
                else -> throw IllegalArgumentException("invalid string")
            }
        }
    }

    class WaterDroplet(
        var timestamp: Long = 0,
    ) {
        var visible = false
        var color by mutableStateOf(Color.Cyan)
        fun visibleNow(timestamp: Long) {
            visible = true
            this.timestamp = timestamp
            color = Color.Cyan
        }
    }

    class Plant(
        var timestamp: Long = 0,
    ) {
        var visible = false
        var color by mutableStateOf(Color.Green)
        fun visibleNow(timestamp: Long) {
            visible = true
            this.timestamp = timestamp
            color = Color.Green
        }
    }

    class Flower() {
        var visible = false
        var color by mutableStateOf(Color.Magenta)
        fun visibleNow() {
            visible = true
            color = Color.Magenta
        }
    }

    val waterDroplets = ConcurrentLinkedDeque<WaterDroplet>()
    var waterDroplet : WaterDroplet = WaterDroplet()
    val plants = ConcurrentLinkedDeque<Plant>()
    var plant : Plant = Plant()
    var flower : Flower = Flower()

    lateinit var myItemType: ItemType
        private set

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
//            showRipple(playerId, System.currentTimeMillis())
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

        if(ACTIVITY_DEBUG_MODE){
            myItemType = ItemType.WATER

            viewModelScope.launch(Dispatchers.Default) {
                while(true){
                    val otherPlayerId = "other player"
                    delay(1000)
                    showItem(otherPlayerId, System.currentTimeMillis())
                }
            }
            return
        }

        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
//        val additionalData = intent.getStringExtra("$PACKAGE_PREFIX.additionalData", "") //todo: for 2 distinguished players...
        myItemType = intent.getStringExtra("$PACKAGE_PREFIX.additionalData")?.let { ItemType.fromString(it) }!!


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
        Log.d("", "$actor has pressed")

        // check the delta between the last taps of opponent and me
        var opponentsLatestTimestamp =
            if((actor == playerId) == (myItemType == ItemType.WATER))
                plant.timestamp
            else
               waterDroplet.timestamp


        if((opponentsLatestTimestamp - timestamp)  // synced click
                .absoluteValue <= FLOWER_GARDEN_SYNC_TIME_THRESHOLD) {
            Log.d("", "Synced!")
            waterDroplet.visibleNow(timestamp)
            plant.visibleNow(timestamp)
            flower.visibleNow()

        } else {
            Log.d("", "Not synced :(")

            if((actor == playerId) == (myItemType == ItemType.WATER)) // if thats me and water, or not me and im not water
                waterDroplet.visibleNow(timestamp)
            else
                plant.visibleNow(timestamp)

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

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

