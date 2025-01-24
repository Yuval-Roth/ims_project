package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameType
import com.imsproject.gameserver.business.lobbies.Lobby
import com.imsproject.gameserver.business.lobbies.LobbyState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.kotlin.*
import kotlin.test.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class SessionControllerTest {

    companion object {
        private const val LOBBY1_ID = "0000"
        private const val SESSION_DURATION = 60
        private val GAME_TYPE1 = GameType.WATER_RIPPLES
        private val GAME_TYPE2 = GameType.WINE_GLASSES
        private const val SYNC_WINDOW_LENGTH = 100L
        private const val SYNC_TOLERANCE = 10L
    }

    @Mock
    private lateinit var mockLobbies: LobbyController
    @Mock
    private lateinit var mockLobby: Lobby

    // test subject
    private lateinit var sessionController: SessionController

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this) // init mocks

        sessionController = spy(SessionController(mockLobbies))

        // set up mock lobby controller
        whenever(mockLobbies.contains(LOBBY1_ID)).thenReturn(true)
        whenever(mockLobbies[LOBBY1_ID]).thenReturn(mockLobby)

        // set up mock lobby
        whenever(mockLobby.id).thenReturn(LOBBY1_ID)

    }

    // ==================================================================================== |
    // ============================ createSession() ======================================= |
    // ==================================================================================== |

    @Test
    fun `createSession() - GIVEN valid lobby ID and parameters WHEN createSession is called THEN session should be created successfully`() {
        // given valid lobby ID and parameters
            /* nothing to do */

        // when createSession is called
        val id = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // then the session should be created successfully
        val session = sessionController.getSessions(LOBBY1_ID)
        assertEquals(1, session.count())
        assertEquals(id, session.elementAt(0).sessionId)
        assertEquals(SESSION_DURATION, session.elementAt(0).duration)
        assertEquals(GAME_TYPE1, session.elementAt(0).gameType)
    }

    @Test
    fun `createSession() - GIVEN non-existing lobby ID WHEN createSession is called THEN an exception should be thrown`() {
        // given non-existing lobby ID
        val lobbyId = "non-existing-lobbyId"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when createSession is called
            sessionController.createSession(lobbyId, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        }
    }

    @Test
    fun `createSession() - GIVEN existing lobby with sessions WHEN createSession is called THEN new session should be added to lobby`() {
        // given existing lobby with sessions
        val id1 = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // when createSession is called
        val id2 = sessionController.createSession(LOBBY1_ID, GAME_TYPE2, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // then new session should be added to lobby
        val sessions = assertNotNull(sessionController.getSessions(LOBBY1_ID))
        assertEquals(2, sessions.count())
        assertEquals(id1, sessions.elementAt(0).sessionId)
        assertEquals(id2, sessions.elementAt(1).sessionId)
    }

    @Test
    fun `createSession() - GIVEN invalid duration WHEN createSession is called THEN an exception should be thrown`() {
        // given invalid duration
        val invalidDuration = 0

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when createSession is called
            sessionController.createSession(LOBBY1_ID, GAME_TYPE1, invalidDuration, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        }
    }

    @Test
    fun `createSession() - GIVEN invalid syncWindowLength WHEN createSession is called THEN an exception should be thrown`() {
        // given invalid syncWindowLength
        val invalidSyncWindowLength = 0L

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when createSession is called
            sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, invalidSyncWindowLength, SYNC_TOLERANCE)
        }
    }

    @Test
    fun `createSession() - GIVEN invalid syncTolerance WHEN createSession is called THEN an exception should be thrown`() {
        // given invalid syncTolerance
        val invalidSyncTolerance = 0L

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when createSession is called
            sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, invalidSyncTolerance)
        }
    }

    // ==================================================================================== |
    // ============================ removeSession() ======================================= |
    // ==================================================================================== |

    @Test
    fun `removeSession() - GIVEN lobby with sessions WHEN removeSession is called THEN session should be removed successfully`() {
        // given existing session in lobby
        val sessionId = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        val session2Id = sessionController.createSession(LOBBY1_ID, GAME_TYPE2, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // when removeSession is called
        sessionController.removeSession(LOBBY1_ID, sessionId)

        // then session should be removed successfully
        val sessions = assertNotNull(sessionController.getSessions(LOBBY1_ID))
        assertEquals(1, sessions.count())
        assertEquals(session2Id, sessions.elementAt(0).sessionId)
    }

    @Test
    fun `removeSession() - GIVEN non-existing lobby ID WHEN removeSession is called THEN an exception should be thrown`() {
        // given non-existing lobby ID
        val lobbyId = "non-existing-lobbyId"
        val sessionId = "0000"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when removeSession is called
            sessionController.removeSession(lobbyId, sessionId)
        }
    }

    @Test
    fun `removeSession() - GIVEN non-existing session ID WHEN removeSession is called THEN an exception should be thrown`() {
        val sessionId = "non-existing-sessionId"

        // given existing lobby with session
        sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when removeSession is called
            sessionController.removeSession(LOBBY1_ID, sessionId)
        }
    }

    // ==================================================================================== |
    // ============================ getSessions() ========================================= |
    // ==================================================================================== |

    @Test
    fun `getSessions() - GIVEN existing lobby with sessions WHEN getSessions is called THEN all sessions should be returned`() {
        // given existing lobby with sessions
        val id1 = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        val id2 = sessionController.createSession(LOBBY1_ID, GAME_TYPE2, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // when getSessions is called
        val sessions = sessionController.getSessions(LOBBY1_ID)

        // then all sessions should be returned
        assertEquals(2, sessions.count())
        assertEquals(id1, sessions.elementAt(0).sessionId)
        assertEquals(id2, sessions.elementAt(1).sessionId)
    }

    @Test
    fun `getSessions() - GIVEN lobby with no sessions WHEN getSessions is called THEN empty list should be returned`() {
        // given lobby with no sessions
        /* nothing to do */

        // when getSessions is called
        val sessions = sessionController.getSessions(LOBBY1_ID)

        // then empty list should be returned
        assertEquals(0, sessions.count())
    }

    @Test
    fun `getSessions() - GIVEN non-existing lobby ID WHEN getSessions is called THEN exception should be thrown`() {
        // given non-existing lobby ID
        val lobbyId = "non-existing-lobbyId"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when getSessions is called
            sessionController.getSessions(lobbyId)
        }
    }

    // ==================================================================================== |
    // ============================ changeSessionsOrder() ================================= |
    // ==================================================================================== |

    @Test
    fun `changeSessionsOrder() - GIVEN valid lobby and session order WHEN changeSessionsOrder is called THEN sessions should be reordered successfully`() {
        // given valid lobby and session order
        val id1 = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        val id2 = sessionController.createSession(LOBBY1_ID, GAME_TYPE2, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        val id3 = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // when changeSessionsOrder is called
        sessionController.changeSessionsOrder(LOBBY1_ID, listOf(id2, id1, id3))

        // then sessions should be reordered successfully
        val sessions = assertNotNull(sessionController.getSessions(LOBBY1_ID))
        assertEquals(3, sessions.count())
        assertEquals(id2, sessions.elementAt(0).sessionId)
        assertEquals(id1, sessions.elementAt(1).sessionId)
        assertEquals(id3, sessions.elementAt(2).sessionId)
    }

    @Test
    fun `changeSessionsOrder() - GIVEN non-existing lobby ID WHEN changeSessionsOrder is called THEN an exception should be thrown`() {
        // given non-existing lobby ID
        val lobbyId = "non-existing-lobbyId"

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when changeSessionsOrder is called
            sessionController.changeSessionsOrder(lobbyId, listOf("0000"))
        }
    }

    @Test
    fun `changeSessionsOrder() - GIVEN different number of session IDs WHEN changeSessionsOrder is called THEN an exception should be thrown`() {
        // given different number of session IDs
        val id1 = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        sessionController.createSession(LOBBY1_ID, GAME_TYPE2, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when changeSessionsOrder is called
            sessionController.changeSessionsOrder(LOBBY1_ID, listOf(id1))
        }
    }

    @Test
    fun `changeSessionsOrder() - GIVEN session IDs not in lobby WHEN changeSessionsOrder is called THEN an exception should be thrown`() {
        // given session IDs not in lobby
        val id1 = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        sessionController.createSession(LOBBY1_ID, GAME_TYPE2, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // then an exception should be thrown
        assertThrows<IllegalArgumentException> {

            // when changeSessionsOrder is called
            sessionController.changeSessionsOrder(LOBBY1_ID, listOf(id1, "non-existing-sessionId"))
        }
    }

    @Test
    fun `changeSessionsOrder() - GIVEN correct session order WHEN changeSessionsOrder is called THEN the order should be maintained`() {
        // given correct session order
        val id1 = sessionController.createSession(LOBBY1_ID, GAME_TYPE1, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)
        val id2 = sessionController.createSession(LOBBY1_ID, GAME_TYPE2, SESSION_DURATION, SYNC_WINDOW_LENGTH, SYNC_TOLERANCE)

        // when changeSessionsOrder is called
        sessionController.changeSessionsOrder(LOBBY1_ID, listOf(id1, id2))

        // then the order should be maintained
        val sessions = assertNotNull(sessionController.getSessions(LOBBY1_ID))
        assertEquals(2, sessions.count())
        assertEquals(id1, sessions.elementAt(0).sessionId)
        assertEquals(id2, sessions.elementAt(1).sessionId)
    }

}
