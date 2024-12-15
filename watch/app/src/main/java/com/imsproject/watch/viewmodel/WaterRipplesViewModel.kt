package com.imsproject.watch.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.GRAY_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.VIVID_ORANGE_COLOR
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WaterRipplesViewModel() : GameViewModel(GameType.WATER_RIPPLES) {

    class Ripple(
        color: Color,
        val startingAlpha: Float = 1f,
    ) {
        var color = mutableStateOf(color)
        var size = mutableFloatStateOf(WATER_RIPPLES_BUTTON_SIZE.toFloat())
        var currentAlpha = mutableFloatStateOf(startingAlpha)
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val ripples = mutableStateListOf<Ripple>()

    private var _counter = MutableStateFlow(0)
    val counter : StateFlow<Int> = _counter

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun click() {
        viewModelScope.launch(Dispatchers.IO) {
            model.sendClick()
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
            GameAction.Type.CLICK -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in click action")
                    return
                }
                val inSync = action.inSync ?: run{
                    Log.e(TAG, "handleGameAction: missing inSync in click action")
                    return
                }
                // switch to main thread to update UI
                withContext(Dispatchers.Main) {
                    showRipple(actor, inSync)
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    /**
     * handles game requests
     */
    override suspend fun handleGameRequest(request: GameRequest) {
        when (request.type) {
            GameRequest.Type.END_GAME -> exitOk()
            else -> super.handleGameRequest(request)
        }
    }

    private fun showRipple(actor: String, inSync : Boolean) {

        val ripple = if (inSync){
            // Synced click
            Ripple(VIVID_ORANGE_COLOR)
        } else if(actor == playerId){
            // My click
            Ripple(LIGHT_BLUE_COLOR)
        } else  {
            // Other player's click
            Ripple(GRAY_COLOR,0.5f)
        }
        ripples.add(0,ripple)
        _counter.value++
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

