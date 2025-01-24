package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.gameserver.business.games.Game
import com.imsproject.gameserver.business.lobbies.Lobby
import com.imsproject.gameserver.business.lobbies.LobbyState
import getField
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertTrue
import org.mockito.kotlin.any as anyParam

class GameControllerTest {

    // test data
    companion object {
        private const val CLIENT1_ID = "000"
        private const val CLIENT2_ID = "001"
        private const val LOBBY_ID = "000"
        private val GAME_TYPE = GameType.WATER_RIPPLES
        private val USER_INPUT_ACTION = GameAction.builder(GameAction.Type.USER_INPUT).build()
    }

    @Mock
    lateinit var mockClients: ClientController
    @Mock
    lateinit var mockTimeServer: TimeServerHandler
    @Mock
    lateinit var mockLobbies: LobbyController
    @Mock
    lateinit var mockClientHandler1: ClientHandler
    @Mock
    lateinit var mockClientHandler2: ClientHandler
    @Mock
    lateinit var mockLobby: Lobby
    @Mock
    lateinit var mockGame: Game

    // test subject
    private lateinit var gameController: GameController

    /**
     * When exiting the setup, the following facts are true:
     *
     * 1. The mock [GameController] instance is a spy, meaning its methods can be verified and stubbed.
     *
     * 2. The mock [ClientHandler] objects are associated with specific client IDs:
     *    - The mock [ClientHandler] for client1 has the ID `"000"`.
     *    - The mock [ClientHandler] for client2 has the ID `"001"`.
     *
     * 3. The mock [ClientController] returns the correct mock [ClientHandler] based on client ID:
     *    - `mockClients.getByClientId("000")` returns the mock [ClientHandler] for client1.
     *    - `mockClients.getByClientId("001")` returns the mock [ClientHandler] for client2.
     *
     * 4. The mock [Lobby] is correctly set up with:
     *    - `mockLobby.player1Id == "000"` (client1).
     *    - `mockLobby.player2Id == "001"` (client2).
     *    - `mockLobby.gameType == GameType.WATER_RIPPLES`.
     *    - `mockLobby.id == "000"`.
     *
     * 5. The mock [LobbyController] provides correct lobby information:
     *    - `mockLobbies["000"]` returns the mock [Lobby].
     *    - `mockLobbies.getByClientId("000")` returns the mock [Lobby].
     *    - `mockLobbies.getByClientId("001")` returns the mock [Lobby].
     *
     * 6. The mock [Game] instance represents a game between the two mock clients:
     *    - `mockGame.player1 == mockClientHandler1` (client1).
     *    - `mockGame.player2 == mockClientHandler2` (client2).
     */
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this) // initializes the mocks

        // test subject setup
        gameController = spy(GameController(mockClients, mockTimeServer, mockLobbies))

        // client handlers setup
        whenever(mockClientHandler1.id).thenReturn(CLIENT1_ID)
        whenever(mockClientHandler2.id).thenReturn(CLIENT2_ID)

        // clients setup
        whenever(mockClients.getByClientId(CLIENT1_ID)).thenReturn(mockClientHandler1)
        whenever(mockClients.getByClientId(CLIENT2_ID)).thenReturn(mockClientHandler2)

        // lobby setup
        whenever(mockLobby.player1Id).thenReturn(CLIENT1_ID)
        whenever(mockLobby.player2Id).thenReturn(CLIENT2_ID)
        whenever(mockLobby.gameType).thenReturn(GAME_TYPE)
        whenever(mockLobby.id).thenReturn(LOBBY_ID)

        // lobbies setup
        whenever(mockLobbies[LOBBY_ID]).thenReturn(mockLobby)
        whenever(mockLobbies.getByClientId(CLIENT1_ID)).thenReturn(mockLobby)
        whenever(mockLobbies.getByClientId(CLIENT2_ID)).thenReturn(mockLobby)

        // game setup
        whenever(mockGame.player1).thenReturn(mockClientHandler1)
        whenever(mockGame.player2).thenReturn(mockClientHandler2)
        whenever(mockGame.lobbyId).thenReturn(LOBBY_ID)
    }

    @AfterEach
    fun tearDown() {}

    // ============================================================================== |
    // =========================== handleGameAction() =============================== |
    // ============================================================================== |

    @Test
    fun `handleGameAction() - GIVEN game running WHEN action received THEN handle ok`() {
        //setup
        val games : MutableMap<String, Game> = getField(gameController, "games")
        val clientIdToGame : MutableMap<String, Game> = getField(gameController, "clientIdToGame")

        // given a game is running
        games[LOBBY_ID] = mockGame
        clientIdToGame[CLIENT1_ID] = mockGame

        // when game action is receive
        gameController.handleGameAction(mockClientHandler1, USER_INPUT_ACTION)

        // then verify that the game action was handled
        verify(mockGame, times(1)).handleGameAction(mockClientHandler1, USER_INPUT_ACTION)
    }

    @Test
    fun `handleGameAction() - GIVEN game not running WHEN action received THEN throw exception`() {
        // given a game is not running
            /* nothing to do */

        // then expect an exception
        assertThrows<IllegalArgumentException> {

            // when game action is received
            gameController.handleGameAction(mockClientHandler1, USER_INPUT_ACTION)
        }
    }

    // ============================================================================== |
    // =========================== onClientConnect() ================================ |
    // ============================================================================== |

    @Test
    fun `onClientDisconnect() - GIVEN client not in game WHEN client disconnect THEN do nothing`() {
        // given a client is not in a game
        whenever(mockLobbies.getByClientId(CLIENT1_ID)).thenReturn(null)

        // when client disconnects
        gameController.onClientDisconnect(mockClientHandler1)

        // then nothing happens
        verify(gameController, never()).endGame(anyParam(), anyParam())
    }

    @ParameterizedTest
    @ValueSource(strings = [CLIENT1_ID, CLIENT2_ID])
    fun `onClientDisconnect() - GIVEN client in game WHEN client disconnect THEN remove from lobby and end game`() {
        // setup
        val games : MutableMap<String, Game> = getField(gameController, "games")
        val clientIdToGame : MutableMap<String, Game> = getField(gameController, "clientIdToGame")
        // disable endGame method call, we are only interested that it was called
        // we don't want to test the endGame method here
        doNothing().whenever(gameController).endGame(anyParam(),anyParam())

        // given a client is in a game
        games[LOBBY_ID] = mockGame
        clientIdToGame[CLIENT1_ID] = mockGame
        clientIdToGame[CLIENT2_ID] = mockGame

        // when client disconnects
        gameController.onClientDisconnect(mockClientHandler1)

        // then he is removed from lobby and game is ended
        verify(gameController, times(1)).endGame(anyParam(), notNull())
    }

    // ============================================================================== |
    // =============================== endGame() ==================================== |
    // ============================================================================== |

    @ParameterizedTest
    @ValueSource(strings = ["error message"]) // game end with error
    @NullSource // game end without error
    fun `endGame() - GIVEN game running WHEN end game THEN success`(errorMessage: String?) {
        // setup
        val games : MutableMap<String, Game> = getField(gameController, "games")
        val clientIdToGame : MutableMap<String, Game> = getField(gameController, "clientIdToGame")

        // given a game is running
        games[LOBBY_ID] = mockGame
        clientIdToGame[CLIENT1_ID] = mockGame
        clientIdToGame[CLIENT2_ID] = mockGame

        // when endGame is called
        gameController.endGame(LOBBY_ID,errorMessage)

        // then the game is ended
        verify(mockGame, times(1)).endGame(errorMessage)
        verify(mockLobby, times(1)).state = LobbyState.WAITING
        assertTrue(gameController.getLobbiesWithRunningGames().isEmpty(), "Lobbies should be empty after ending the game")
        assertThrows<IllegalArgumentException> {
            gameController.handleGameAction(mockClientHandler1, USER_INPUT_ACTION)
        }
        assertThrows<IllegalArgumentException> {
            gameController.handleGameAction(mockClientHandler2, USER_INPUT_ACTION)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["error message"]) // game end with error
    @NullSource // game end without error
    fun `endGame() - GIVEN game not running WHEN end game THEN throw exception`(errorMessage: String?) {
        // given a game is not running
            /* nothing to do */

        // then expect an exception
        assertThrows<IllegalArgumentException> {

            // when endGame is called
            gameController.endGame(LOBBY_ID, errorMessage)
        }

        verify(mockLobby, never()).state = LobbyState.WAITING
    }

    @ParameterizedTest
    @ValueSource(strings = ["error message"]) // game end with error
    @NullSource // game end without error
    fun `endGame() - GIVEN lobby does not exist WHEN end game THEN throw exception`(errorMessage: String?) {
        // given a lobby does not exist
        whenever(mockLobbies[LOBBY_ID]).thenReturn(null)

        // then expect an exception
        assertThrows<IllegalArgumentException> {

            // when endGame is called
            gameController.endGame(LOBBY_ID, errorMessage)
        }

        verify(mockLobby, never()).state = LobbyState.WAITING
    }

    // ============================================================================== |
    // =============================== startGame() ================================== |
    // ============================================================================== |

    @Test
    fun `startGame() - GIVEN lobby ready WHEN start game THEN success`() {
        // given a lobby is ready
        whenever(mockLobby.isReady()).thenReturn(true)

        // when startGame is called
        gameController.startGame(LOBBY_ID)

        // then the game is started
        verify(mockLobby, times(1)).state = LobbyState.PLAYING
        assertTrue(gameController.getLobbiesWithRunningGames().contains(LOBBY_ID))
        assertDoesNotThrow { gameController.handleGameAction(mockClientHandler1, USER_INPUT_ACTION) }
        assertDoesNotThrow { gameController.handleGameAction(mockClientHandler2, USER_INPUT_ACTION) }
    }

    @Test
    fun `startGame() - GIVEN lobby not ready WHEN start game THEN throw exception`() {
        // given a lobby is not ready
        whenever(mockLobby.isReady()).thenReturn(false)

        // then expect an exception
        assertThrows<IllegalStateException> {

            // when startGame is called
            gameController.startGame(LOBBY_ID)
        }

        verify(mockLobby, never()).state = LobbyState.PLAYING
        assertTrue(gameController.getLobbiesWithRunningGames().isEmpty())
        assertThrows<IllegalArgumentException> {
            gameController.handleGameAction(mockClientHandler1, USER_INPUT_ACTION)
        }
        assertThrows<IllegalArgumentException> {
            gameController.handleGameAction(mockClientHandler2, USER_INPUT_ACTION)
        }
    }

    @Test
    fun `startGame() - GIVEN lobby does not exist WHEN start game THEN throw exception`() {
        // given a lobby does not exist
        whenever(mockLobbies[LOBBY_ID]).thenReturn(null)

        // then expect an exception
        assertThrows<IllegalArgumentException> {

            // when startGame is called
            gameController.startGame(LOBBY_ID)
        }

        verify(mockLobby, never()).state = LobbyState.PLAYING
        assertTrue(gameController.getLobbiesWithRunningGames().isEmpty())
        assertThrows<IllegalArgumentException> {
            gameController.handleGameAction(mockClientHandler1, USER_INPUT_ACTION)
        }
        assertThrows<IllegalArgumentException> {
            gameController.handleGameAction(mockClientHandler2, USER_INPUT_ACTION)
        }
    }
}