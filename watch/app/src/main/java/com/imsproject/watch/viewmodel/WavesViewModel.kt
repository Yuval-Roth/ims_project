package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WavesViewModel: GameViewModel(GameType.WAVES) {


    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    var myDirection: Int = 1
        private set

    private val _animationDirection = MutableStateFlow(1)
    val animationDirection: StateFlow<Int> = _animationDirection

    private var _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)

        if(ACTIVITY_DEBUG_MODE){
            // TODO: add debug code if needed
            return
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
                val data = action.data?: run{
                    Log.e(TAG, "handleGameAction: missing data in user input action")
                    return
                }
                val sequenceNumber = action.sequenceNumber ?: run{
                    Log.e(TAG, "handleGameAction: missing sequence number in user input action")
                    return
                }

                val arrivedTimestamp = getCurrentGameTime()
                // TODO: handle data received

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

    companion object {
        private const val TAG = "WavesViewModel"
    }

}