package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.utils.Angle
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.business.ClientHandler
import com.imsproject.gameserver.business.TimeServerHandler
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs


class FlourMillGame(
    lobbyId: String,
    player1: ClientHandler,
    player2: ClientHandler
) : Game(lobbyId, player1, player2) {

    private var packetNumber = 0L

    // player 1 data
    @Volatile
    private var player1TouchPoint = -1f to Angle.undefined
    @Volatile
    private var player1InBounds = false
    @Volatile
    private var player1LastUpdate = 0L
    @Volatile
    private var player1LastSequenceNumber = 0L

    // player 2 data
    @Volatile
    private var player2TouchPoint = -1f to Angle.undefined
    @Volatile
    private var player2InBounds = false
    @Volatile
    private var player2LastUpdate = 0L
    @Volatile
    private var player2LastSequenceNumber = 0L

    // axle data
    private var axleAngle = Angle.undefined

    private val scope = CoroutineScope(Dispatchers.Default)

    private suspend fun manageAxle(){
        while(true){
            delay(16)
            val timestamp = System.currentTimeMillis() - localStartTime
            if(axleAngle == Angle.undefined) {
                if(player1TouchPoint.first > 0f && player2TouchPoint.first > 0f){
                    if(player1LastUpdate > player2LastUpdate) {
                        axleAngle = player1TouchPoint.second + Angle(90f)
                    } else {
                        axleAngle = player2TouchPoint.second + Angle(-90f)
                    }
                } else if (player1TouchPoint.first > 0f) {
                    axleAngle = player1TouchPoint.second + Angle(90f)
                } else if (player2TouchPoint.first > 0f) {
                    axleAngle = player2TouchPoint.second + Angle(-90f)
                } else {
                    axleAngle = Angle.undefined
                }
            } else {
                if(player1TouchPoint.first > 0f && player2TouchPoint.first > 0f) {
                    if(player1InBounds && player2InBounds){
                        val player1Angle = player1TouchPoint.second
                        val player2Angle = player2TouchPoint.second
                        val player1SideAngle = axleAngle + -90f
                        val player2SideAngle = axleAngle + 90f
                        val player1AngleDiff = player1Angle - player1SideAngle
                        val player2AngleDiff = player2Angle - player2SideAngle
                        val player1Direction = if(Angle.isClockwise(player1SideAngle,player1Angle)) 1 else -1
                        val player2Direction = if(Angle.isClockwise(player2SideAngle, player2Angle)) 1 else -1
                        if(player1AngleDiff > 0 && player2AngleDiff > 0 && player1Direction == player2Direction){
                            val amountToRotate = abs(player1AngleDiff - player2AngleDiff)
                            axleAngle += amountToRotate * player1Direction
                        }
                    }
                } else if (player1TouchPoint.first < 0f && player2TouchPoint.first < 0f) {
                    axleAngle = Angle.undefined
                }
            }
            sendGameAction(GameAction.builder(GameAction.Type.USER_INPUT)
                .actor("system")
                .timestamp(timestamp.toString())
                .sequenceNumber(packetNumber++)
                .data("$axleAngle")
                .build())
        }

    }

    override fun handleGameAction(actor: ClientHandler, action: GameAction) {
        when(action.type) {
            GameAction.Type.USER_INPUT -> {
//                val dataSplit = action.data?.split(",") ?: run {
//                    log.error("Missing data in USER_INPUT GameAction")
//                    return
//                }
//                if (dataSplit.size != 3) {
//                    log.error("Invalid data in USER_INPUT GameAction: $dataSplit")
//                    return
//                }
//                val timestamp = action.timestamp?.toLong() ?: run {
//                    log.error("Missing timestamp in USER_INPUT GameAction")
//                    return
//                }
//                val sequenceNumber = action.sequenceNumber ?: run {
//                    log.error("Missing sequence number in USER_INPUT GameAction")
//                    return
//                }
//                val relativeRadius = dataSplit[0].toFloat()
//                val angle = Angle(dataSplit[1].toFloat())
//                val inBounds = dataSplit[2].toBoolean()
//
//                if (actor == player1) {
//                    if(sequenceNumber <= player1LastSequenceNumber){
//                        // Ignore old messages that are not in order
//                        return
//                    }
//                    player1TouchPoint = relativeRadius to angle
//                    player1InBounds = inBounds
//                    player1LastUpdate = timestamp
//                    player1LastSequenceNumber = sequenceNumber
//                } else if (actor == player2) {
//                    if(sequenceNumber <= player2LastSequenceNumber){
//                        // Ignore old messages that are not in order
//                        return
//                    }
//                    player2TouchPoint = relativeRadius to angle
//                    player2InBounds = inBounds
//                    player2LastUpdate = timestamp
//                    player2LastSequenceNumber = sequenceNumber
//                } else {
//                    log.error("Unknown actor: ${actor.id}")
//                    return
//                }

                val otherPlayer = if (actor == player1) player2 else player1
                otherPlayer.sendUdp(action.toString())
            }
            else -> {
                log.debug("Unexpected action type: {}", action.type)
            }
        }
    }

    override fun startGame(sessionId: Int) {
        val timeHandler = TimeServerHandler.instance
        val timeServerCurr = timeHandler.timeServerCurrentTimeMillis().toString()
        localStartTime =  System.currentTimeMillis() + timeHandler.timeServerDelta
        val toSend = GameRequest.builder(GameRequest.Type.START_GAME)
            .sessionId(sessionId.toString())
            .timestamp(timeServerCurr)
        player1.sendTcp(toSend.data(listOf("left")).build().toJson())
        player2.sendTcp(toSend.data(listOf("right")).build().toJson())
//        scope.launch {
//            manageAxle()
//        }
    }

    override fun endGame(errorMessage: String?) {
        scope.cancel()
        super.endGame(errorMessage)
    }

    companion object {
        private val log = LoggerFactory.getLogger(FlourMillGame::class.java)
    }
}