package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.business.lobbies.LobbyState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertNotNull
import org.mockito.kotlin.any as anyParam

class LobbyControllerTest {

    companion object {
        private const val CLIENT1_ID = "000"
        private const val CLIENT2_ID = "001"
        private const val CLIENT3_ID = "002"
        private val GAME_TYPE_1 = GameType.WATER_RIPPLES
        private val GAME_TYPE_2 = GameType.WINE_GLASSES
        private val LEAVE_LOBBY_MESSAGE = GameRequest.builder(GameRequest.Type.LEAVE_LOBBY).build().toJson()
        private const val SESSION_ID = "00001"
        private const val DURATION = 60
        private const val SYNC_WINDOW_LENGTH = 1000L
        private const val SYNC_TOLERANCE = 100L
    }

    @Mock
    private lateinit var mockClients: ClientController
    @Mock
    lateinit var mockClientHandler1: ClientHandler
    @Mock
    lateinit var mockClientHandler2: ClientHandler
    @Mock
    lateinit var mockClientHandler3: ClientHandler
    @Mock
    lateinit var mockSession: Session

    // test subject
    private lateinit var lobbyController: LobbyController

    /**
     * When exiting the setup, the following facts are true:
     *
     * 1. The mock [LobbyController] instance is a spy, meaning its methods can be verified and stubbed.
     *
     * 2. The mock [ClientHandler] objects are associated with specific client IDs:
     *    - The mock [ClientHandler] for client1 has the ID `CLIENT1_ID`.
     *    - The mock [ClientHandler] for client2 has the ID `CLIENT2_ID`.
     *    - The mock [ClientHandler] for client3 has the ID `CLIENT3_ID`.
     *
     * 3. The mock [ClientsController] returns the correct mock [ClientHandler] based on client ID:
     *    - `mockClients.getByClientId(CLIENT1_ID)` returns the mock [ClientHandler] for client1.
     *    - `mockClients.getByClientId(CLIENT2_ID)` returns the mock [ClientHandler] for client2.
     *    - `mockClients.getByClientId(CLIENT3_ID)` returns the mock [ClientHandler] for client3.
     *
     * 4. The mock [ClientHandler] instances are properly set up with their respective IDs:
     *    - `mockClientHandler1.id == CLIENT1_ID`.
     *    - `mockClientHandler2.id == CLIENT2_ID`.
     *    - `mockClientHandler3.id == CLIENT3_ID`.
     */
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this) // initializes the mocks

        lobbyController = spy(LobbyController(mockClients))

        // set up mock ClientsController
        whenever(mockClients.getByClientId(CLIENT1_ID)).thenReturn(mockClientHandler1)
        whenever(mockClients.getByClientId(CLIENT2_ID)).thenReturn(mockClientHandler2)
        whenever(mockClients.getByClientId(CLIENT3_ID)).thenReturn(mockClientHandler3)

        // set up mock client handlers
        whenever(mockClientHandler1.id).thenReturn(CLIENT1_ID)
        whenever(mockClientHandler2.id).thenReturn(CLIENT2_ID)
        whenever(mockClientHandler3.id).thenReturn(CLIENT3_ID)

        // set up mock session
        whenever(mockSession.sessionId).thenReturn(SESSION_ID)
        whenever(mockSession.gameType).thenReturn(GAME_TYPE_1)
        whenever(mockSession.duration).thenReturn(DURATION)
        whenever(mockSession.syncWindowLength).thenReturn(SYNC_WINDOW_LENGTH)
        whenever(mockSession.syncTolerance).thenReturn(SYNC_TOLERANCE)
    }

    // ================================================================================= |
    // ============================= onClientDisconnect() ============================== |
    // ================================================================================= |

    @Test
    fun `onClientDisconnect() - GIVEN client in lobby WHEN client disconnects THEN client should be removed from the lobby`() {
        // given a client in a lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        doNothing().whenever(lobbyController).leaveLobby(anyParam(), anyParam(),anyParam())

        // when client disconnects
        lobbyController.onClientDisconnect(mockClientHandler1)

        // then the client should be removed from the lobby
        verify(lobbyController,times(1)).leaveLobby(anyParam(),anyParam(),anyParam())
    }

    @Test
    fun `onClientDisconnect() - GIVEN client not in any lobby WHEN client disconnects THEN do nothing`() {
        // given a client not in any lobby
            /* nothing to do */

        // when client disconnects
        lobbyController.onClientDisconnect(mockClientHandler1)

        // then no further action should be taken
        verify(lobbyController,never()).leaveLobby(anyParam(), anyParam(), anyParam())
    }

    // ================================================================================= |
    // ================================= createLobby() ================================= |
    // ================================================================================= |

    @Test
    fun `createLobby() - GIVEN valid game type WHEN createLobby is called THEN a new lobby ID should be returned`() {
        // given a valid game type
        val gameType = GAME_TYPE_1

        // when createLobby is called
        val lobbyId = lobbyController.createLobby(gameType)

        // then a new lobby should be created
        assertNotNull(lobbyController[lobbyId])
    }

    @Test
    fun `createLobby() - GIVEN multiple createLobby calls WHEN called repeatedly THEN unique lobby IDs should be generated`() {
        // given a valid game type
        val gameType = GAME_TYPE_1

        // when createLobby is called multiple times
        val lobbyId1 = lobbyController.createLobby(gameType)
        val lobbyId2 = lobbyController.createLobby(gameType)
        val lobbyId3 = lobbyController.createLobby(gameType)

        // then unique lobby IDs should be generated
        assertNotNull(lobbyController[lobbyId1])
        assertNotNull(lobbyController[lobbyId2])
        assertEquals(3,setOf(lobbyId1, lobbyId2, lobbyId3).size)
    }

    // ================================================================================= |
    // ================================= removeLobby() ================================= |
    // ================================================================================= |

    @Test
    fun `removeLobby() - GIVEN empty lobby WHEN removeLobby is called THEN the lobby is removed successfully`() {
        // given an existing lobby
        val gameType = GAME_TYPE_1
        val lobbyId = lobbyController.createLobby(gameType)

        // when removeLobby is called
        lobbyController.removeLobby(lobbyId)

        // then the lobby should be removed
        assertNull(lobbyController[lobbyId])
    }

    @Test
    fun `removeLobby() - GIVEN lobby with players WHEN removeLobby is called THEN lobby is removed and clients should be notified`() {
        // given an existing lobby with players
        val gameType = GAME_TYPE_1
        val lobbyId = lobbyController.createLobby(gameType)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        lobbyController.joinLobby(lobbyId,CLIENT2_ID)

        // when removeLobby is called
        lobbyController.removeLobby(lobbyId)

        // then the clients should be notified and the lobby should be removed
        assertNull(lobbyController[lobbyId])
        verify(mockClientHandler1,times(1)).sendTcp(LEAVE_LOBBY_MESSAGE)
        verify(mockClientHandler2,times(1)).sendTcp(LEAVE_LOBBY_MESSAGE)
    }

    @Test
    fun `removeLobby() - GIVEN non-existing lobby WHEN removeLobby is called THEN an exception should be thrown`() {
        // given a non-existing lobby
        val lobbyId = "non-existing-lobby"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when removeLobby is called
            lobbyController.removeLobby(lobbyId)
        }

        verify(mockClients,never()).getByClientId(anyParam())
    }

    @Test
    fun `removeLobby() - GIVEN existing lobby WHEN removeLobby is called twice THEN an exception should be thrown on second attempt`() {
        // given an existing lobby
        val gameType = GAME_TYPE_1
        val lobbyId = lobbyController.createLobby(gameType)

        lobbyController.removeLobby(lobbyId) // first call
        assertNull(lobbyController[lobbyId])

        // then an exception should be thrown on second attempt
        assertThrows<IllegalArgumentException> {

            // when removeLobby is called twice
            lobbyController.removeLobby(lobbyId)
        }
    }

    // ================================================================================= |
    // ================================= joinLobby() =================================== |
    // ================================================================================= |

    @Test
    fun `joinLobby() - GIVEN valid client and lobby WHEN joinLobby is called THEN client should be added successfully`() {
        // given an existing lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)

        // when joinLobby is called
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)

        // then the client should be added to the lobby
        val lobby = assertNotNull(lobbyController[lobbyId])
        assertTrue(lobby.getPlayers().contains(CLIENT1_ID))
        val joinLobbyMessage = GameRequest.builder(GameRequest.Type.JOIN_LOBBY)
            .lobbyId(lobbyId)
            .gameType(GAME_TYPE_1)
            .build().toJson()
        verify(mockClientHandler1,times(1)).sendTcp(joinLobbyMessage)
    }

    @Test
    fun `joinLobby() - GIVEN full lobby WHEN joinLobby is called THEN an exception should be thrown`() {
        // given a full lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        lobbyController.joinLobby(lobbyId,CLIENT2_ID)

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when joinLobby is called
            lobbyController.joinLobby(lobbyId,CLIENT3_ID)
        }

        val lobby = assertNotNull(lobbyController[lobbyId])
        assertTrue(lobby.getPlayers().contains(CLIENT1_ID))
        assertTrue(lobby.getPlayers().contains(CLIENT2_ID))
        assertFalse(lobby.getPlayers().contains(CLIENT3_ID))
    }

    @Test
    fun `joinLobby() - GIVEN client already in another lobby WHEN joinLobby is called THEN an exception should be thrown`() {
        // given a client already in another lobby
        val lobbyId1 = lobbyController.createLobby(GAME_TYPE_1)
        val lobbyId2 = lobbyController.createLobby(GAME_TYPE_1)
        lobbyController.joinLobby(lobbyId1,CLIENT1_ID)

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when joinLobby is called
            lobbyController.joinLobby(lobbyId2,CLIENT1_ID)
        }

        val lobby1 = assertNotNull(lobbyController[lobbyId1])
        val lobby2 = assertNotNull(lobbyController[lobbyId2])
        assertTrue(lobby1.getPlayers().contains(CLIENT1_ID))
        assertFalse(lobby2.getPlayers().contains(CLIENT1_ID))
    }

    @Test
    fun `joinLobby() - GIVEN non-existing lobby WHEN joinLobby is called THEN an exception should be thrown`() {
        // given a non-existing lobby
        val lobbyId = "non-existing-lobby"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when joinLobby is called
            lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        }

        assertNull(lobbyController.getByClientId(CLIENT1_ID))
    }

    // ================================================================================= |
    // ================================= leaveLobby() ================================== |
    // ================================================================================= |

    @Test
    fun `leaveLobby() - GIVEN client in full lobby WHEN leaveLobby is called THEN client should be removed from lobby`() {
        // given a client in a lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        lobbyController.joinLobby(lobbyId,CLIENT2_ID)

        // when leaveLobby is called
        lobbyController.leaveLobby(lobbyId,CLIENT1_ID)

        // then the client should be removed from the lobby
        val lobby = assertNotNull(lobbyController[lobbyId])
        assertFalse(lobby.getPlayers().contains(CLIENT1_ID))
        assertTrue(lobby.getPlayers().contains(CLIENT2_ID))
        assertNull(lobbyController.getByClientId(CLIENT1_ID))
        assertNotNull(lobbyController.getByClientId(CLIENT2_ID))
        verify(mockClientHandler1,times(1)).sendTcp(LEAVE_LOBBY_MESSAGE)
    }

    @Test
    fun `leaveLobby() - GIVEN client doesn't exist WHEN leaveLobby is called THEN an exception should be thrown`() {
        // given a non-existing client
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        val clientId = "non-existing-client"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when leaveLobby is called
            lobbyController.leaveLobby(lobbyId,clientId)
        }

        assertNotNull(lobbyController[lobbyId])
    }

    @Test
    fun `leaveLobby() - GIVEN last client in lobby WHEN leaveLobby is called THEN client is removed form the lobby and lobby should be removed automatically`() {
        // given a client in a lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        val lobby = assertNotNull(lobbyController[lobbyId])

        // when leaveLobby is called
        lobbyController.leaveLobby(lobbyId,CLIENT1_ID)

        // then the lobby should be removed
        assertFalse(lobby.getPlayers().contains(CLIENT1_ID))
        assertNull(lobbyController[lobbyId])
        assertNull(lobbyController.getByClientId(CLIENT1_ID))
    }

    @Test
    fun `leaveLobby() - GIVEN client not in lobby WHEN leaveLobby is called THEN an exception should be thrown`() {
        // given a client not in a lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when leaveLobby is called
            lobbyController.leaveLobby(lobbyId,CLIENT1_ID)
        }

        assertNotNull(lobbyController[lobbyId])
    }

    // ================================================================================= |
    // ================================= toggleReady() ================================= |
    // ================================================================================= |

    @Test
    fun `toggleReady() - GIVEN client in lobby WHEN toggleReady is called THEN the ready status should be toggled`() {
        // given a client in a lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        lobbyController.joinLobby(lobbyId,CLIENT2_ID)

        // when toggleReady is called
        lobbyController.toggleReady(mockClientHandler1)
        lobbyController.toggleReady(mockClientHandler2)

        // then the ready status should be toggled
        val lobby = assertNotNull(lobbyController[lobbyId])
        assertTrue(lobby.isReady())
    }

    @Test
    fun `toggleReady() - GIVEN client not in lobby WHEN toggleReady is called THEN an exception should be thrown`() {
        // given a client not in a lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when toggleReady is called
            lobbyController.toggleReady(mockClientHandler1)
        }

        assertNotNull(lobbyController[lobbyId])
    }

    @Test
    fun `toggleReady() - GIVEN client in lobby WHEN toggleReady is called multiple times THEN ready status should toggle each time`() {
        // given a client in a lobby
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        lobbyController.joinLobby(lobbyId,CLIENT2_ID)

        val lobby = assertNotNull(lobbyController[lobbyId])

        // when toggleReady is called multiple times
        // then the ready status should toggle each time
        lobbyController.toggleReady(mockClientHandler1)
        lobbyController.toggleReady(mockClientHandler2)
        assertTrue(lobby.isReady())

        lobbyController.toggleReady(mockClientHandler1)
        assertFalse(lobby.isReady())

        lobbyController.toggleReady(mockClientHandler1)
        assertTrue(lobby.isReady())
    }

    // ================================================================================= |
    // ================================= getLobby() ==================================== |
    // ================================================================================= |

    @Test
    fun `getLobby() - GIVEN valid lobby ID WHEN getLobby is called THEN correct lobby info should be returned`() {
        // given an existing lobby
        val gameType = GAME_TYPE_1
        val lobbyId = lobbyController.createLobby(gameType)

        // when getLobby is called
        val lobbyInfo = lobbyController.getLobby(lobbyId)

        // then correct lobby info should be returned
        assertNotNull(lobbyInfo)
        assertEquals(lobbyId,lobbyInfo.lobbyId)
        assertEquals(gameType,lobbyInfo.gameType)
        assertEquals(0,lobbyInfo.players.size)
    }

    @Test
    fun `getLobby() - GIVEN invalid lobby ID WHEN getLobby is called THEN an exception should be thrown`() {
        // given a non-existing lobby
        val lobbyId = "non-existing-lobby"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when getLobby is called
            lobbyController.getLobby(lobbyId)
        }
    }

    @Test
    fun `getLobby() - GIVEN full lobby WHEN getLobby is called THEN correct number of players should be returned`() {
        // given a full lobby
        val gameType = GAME_TYPE_1
        val lobbyId = lobbyController.createLobby(gameType)
        lobbyController.joinLobby(lobbyId,CLIENT1_ID)
        lobbyController.joinLobby(lobbyId,CLIENT2_ID)

        // when getLobby is called
        val lobbyInfo = lobbyController.getLobby(lobbyId)

        // then correct number of players should be returned
        assertNotNull(lobbyInfo)
        assertEquals(lobbyId,lobbyInfo.lobbyId)
        assertEquals(gameType,lobbyInfo.gameType)
        assertEquals(2,lobbyInfo.players.size)

    }

    // ================================================================================= |
    // ================================= getLobbiesInfo() ============================== |
    // ================================================================================= |

    @Test
    fun `getLobbiesInfo() - GIVEN existing lobbies WHEN getLobbiesInfo is called THEN all lobby info should be returned`() {
        // given existing lobbies
        val gameType1 = GAME_TYPE_1
        val gameType2 = GAME_TYPE_2
        val lobbyId1 = lobbyController.createLobby(gameType1)
        val lobbyId2 = lobbyController.createLobby(gameType2)

        // when getLobbiesInfo is called
        val lobbiesInfo = lobbyController.getLobbiesInfo()

        // then all lobby info should be returned
        assertEquals(2,lobbiesInfo.size)
        assertNotNull(lobbiesInfo.find { it.lobbyId == lobbyId1 })
        assertNotNull(lobbiesInfo.find { it.lobbyId == lobbyId2 })
    }

    @Test
    fun `getLobbiesInfo() - GIVEN no lobbies WHEN getLobbiesInfo is called THEN empty list should be returned`() {
        // given no lobbies
            /* nothing to do */

        // when getLobbiesInfo is called
        val lobbiesInfo = lobbyController.getLobbiesInfo()

        // then empty list should be returned
        assertEquals(0,lobbiesInfo.size)
    }

    // ================================================================================= |
    // ================================= configureLobby() ============================== |
    // ================================================================================= |

    @Test
    fun `configureLobby() - GIVEN valid lobby ID and game type WHEN configureLobby is called THEN lobby should be updated`() {
        // given an existing lobby
        val gameType = GAME_TYPE_1
        val lobbyId = lobbyController.createLobby(gameType)
        whenever(mockSession.gameType).thenReturn(GAME_TYPE_2)

        // when configureLobby is called
        lobbyController.configureLobby(lobbyId,mockSession)

        // then the lobby should be updated
        val lobby = assertNotNull(lobbyController[lobbyId])
        assertEquals(GAME_TYPE_2,lobby.gameType)
        kotlin.test.assertEquals(DURATION,lobby.gameDuration)
        assertEquals(SYNC_WINDOW_LENGTH,lobby.syncWindowLength)
        assertEquals(SYNC_TOLERANCE,lobby.syncTolerance)
    }

    @Test
    fun `configureLobby() - GIVEN non-existing lobby WHEN configureLobby is called THEN an exception should be thrown`() {
        // given a non-existing lobby
        val lobbyId = "non-existing-lobby"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when configureLobby is called
            lobbyController.configureLobby(lobbyId,mockSession)
        }
    }

    @Test
    fun `configureLobby() - GIVEN lobby is playing WHEN configureLobby THEN throw exception`(){
        // given a lobby is playing
        val lobbyId = lobbyController.createLobby(GAME_TYPE_1)
        val lobby = assertNotNull(lobbyController[lobbyId])
        lobby.state = LobbyState.PLAYING

        // then an exception should be thrown
        assertThrows<IllegalStateException> {

            // when configureLobby is called
            lobbyController.configureLobby(lobbyId, mockSession)
        }
    }
}