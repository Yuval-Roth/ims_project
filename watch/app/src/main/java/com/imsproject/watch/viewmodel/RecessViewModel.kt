package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class RecessViewModel: GameViewModel(GameType.RECESS) {

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    private val _recessLength = MutableStateFlow(-1)
    val recessLength : StateFlow<Int> = _recessLength

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)

        if(ACTIVITY_DEBUG_MODE){
            _recessLength.value = 30
            return
        }

        val gameDuration = intent.getIntExtra("$PACKAGE_PREFIX.gameDuration", -1).also {
            if (it <= 0) {
                exitWithError("Missing or invalid game duration", Result.Code.BAD_REQUEST)
                return
            }
        }
        _recessLength.value = gameDuration

        viewModelScope.launch(Dispatchers.IO) {
            model.sessionSetupComplete()
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
                // no user input actions in recess
            }
            else -> super.handleGameAction(action)
        }
    }

    companion object {
        private const val TAG = "RecessViewModel"
    }
}