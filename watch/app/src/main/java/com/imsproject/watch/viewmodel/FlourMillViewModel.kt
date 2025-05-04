package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.common.utils.Angle
import com.imsproject.common.utils.UNDEFINED_ANGLE
import com.imsproject.watch.FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.FREQUENCY_HISTORY_MILLISECONDS
import com.imsproject.watch.WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.utils.PacketTracker
import com.imsproject.watch.utils.polarDistance
import com.imsproject.watch.utils.toAngle
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FlourMillViewModel : GameViewModel(GameType.FLOUR_MILL) {


    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |


    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent,context)

        if(ACTIVITY_DEBUG_MODE) {
            viewModelScope.launch(Dispatchers.Default) {
                while (true) {
                    delay(100)
                    //TODO: implement debug mode opponent
                }
            }
            return
        }

        // set up sync params
        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
        if (syncTolerance <= 0L) {
            exitWithError("Missing sync tolerance", Result.Code.BAD_REQUEST)
            return
        }
        val syncWindowLength = intent.getLongExtra("$PACKAGE_PREFIX.syncWindowLength", -1)
        if (syncWindowLength <= 0L) {
            exitWithError("Missing sync window length", Result.Code.BAD_REQUEST)
            return
        }
        FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD = syncTolerance.toFloat() * 0.01f
        FREQUENCY_HISTORY_MILLISECONDS = syncWindowLength
        Log.d(TAG, "syncTolerance: $syncTolerance")
        Log.d(TAG, "syncWindowLength: $syncWindowLength")
    }

    fun setTouchPoint(angle: Angle) {

        if(ACTIVITY_DEBUG_MODE){
            //TODO: implement debug mode
        }

        viewModelScope.launch(Dispatchers.IO) {
            //TODO:implement network call and event log
        }

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
                val data = action.data?.split(",") ?: run{
                    Log.e(TAG, "handleGameAction: missing data in user input action")
                    return
                }
                val sequenceNumber = action.sequenceNumber ?: run{
                    Log.e(TAG, "handleGameAction: missing sequence number in user input action")
                    return
                }

                val arrivedTimestamp = getCurrentGameTime()

                val outOfOrder = packetTracker.receivedOtherPacket(sequenceNumber)
                if(outOfOrder) return

                // TODO: implement networking

                // TODO: log action event

            }
            else -> super.handleGameAction(action)
        }
    }

    companion object {
        private const val TAG = "FlourMillViewModel"
    }
}