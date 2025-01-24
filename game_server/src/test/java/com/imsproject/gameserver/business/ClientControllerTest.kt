package com.imsproject.gameserver.business

import com.imsproject.gameserver.toHostPortString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.springframework.web.socket.WebSocketSession
import java.net.SocketAddress

class ClientControllerTest {

    private class MockSocketAddress(
        address: String
    ) : SocketAddress() {
        private val address = "/$address"
        override fun toString(): String {
            return address
        }
    }

    companion object {
        private const val CLIENT1_ID = "000"
        private const val CLIENT2_ID = "001"
        private const val CLIENT3_ID = "002"
        private const val WS_SESSION1_ID = "wsSession1"
        private const val WS_SESSION2_ID = "wsSession2"
        private const val WS_SESSION3_ID = "wsSession3"
        private val UDP_ADDRESS1 = MockSocketAddress("127.0.0.1:00001")
        private val UDP_ADDRESS2 = MockSocketAddress("127.0.0.1:00002")
        private val UDP_ADDRESS3 = MockSocketAddress("127.0.0.1:00003")
    }

    @Mock
    lateinit var mockClientHandler1: ClientHandler
    @Mock
    lateinit var mockClientHandler2: ClientHandler
    @Mock
    lateinit var mockClientHandler3: ClientHandler
    @Mock
    lateinit var mockWsSession1: WebSocketSession
    @Mock
    lateinit var mockWsSession2: WebSocketSession
    @Mock
    lateinit var mockWsSession3: WebSocketSession

    // test subject
    private lateinit var clientController: ClientController

    /**
     * When exiting the setup, the following facts are true:
     *
     * 1. The mock [ClientController] instance is a spy, meaning its methods can be verified and stubbed.
     *
     * 2. The mock [ClientHandler] objects are associated with specific client IDs:
     *    - The mock [ClientHandler] for client1 has the ID `"000"`.
     *    - The mock [ClientHandler] for client2 has the ID `"001"`.
     *    - The mock [ClientHandler] for client3 has the ID `"002"`.
     *
     * 3. The mock [ClientHandler] instances are correctly linked to their respective WebSocket sessions:
     *    - `mockClientHandler1.wsSession` returns `mockWsSession1` with ID `"wsSession1"`.
     *    - `mockClientHandler2.wsSession` returns `mockWsSession2` with ID `"wsSession2"`.
     *    - `mockClientHandler3.wsSession` returns `mockWsSession3` with ID `"wsSession3"`.
     *
     * 4. The mock [ClientHandler] instances have their respective UDP addresses set up using [MockSocketAddress]:
     *    - `mockClientHandler1.udpAddress` returns `MockSocketAddress("127.0.0.1:00001")`.
     *    - `mockClientHandler2.udpAddress` returns `MockSocketAddress("127.0.0.1:00002")`.
     *    - `mockClientHandler3.udpAddress` returns `MockSocketAddress("127.0.0.1:00003")`.
     *
     * 5. The mock [WebSocketSession] instances are correctly initialized with IDs:
     *    - `mockWsSession1.id == "wsSession1"`.
     *    - `mockWsSession2.id == "wsSession2"`.
     *    - `mockWsSession3.id == "wsSession3"`.
     *
     */
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this) // initializes the mocks

        clientController = spy(ClientController())

        // set up mock client handlers
        whenever(mockClientHandler1.id).thenReturn(CLIENT1_ID)
        whenever(mockClientHandler2.id).thenReturn(CLIENT2_ID)
        whenever(mockClientHandler3.id).thenReturn(CLIENT3_ID)
        whenever(mockClientHandler1.wsSession).thenReturn(mockWsSession1)
        whenever(mockClientHandler2.wsSession).thenReturn(mockWsSession2)
        whenever(mockClientHandler3.wsSession).thenReturn(mockWsSession3)
        whenever(mockClientHandler1.udpAddress).thenReturn(UDP_ADDRESS1)
        whenever(mockClientHandler2.udpAddress).thenReturn(UDP_ADDRESS2)
        whenever(mockClientHandler3.udpAddress).thenReturn(UDP_ADDRESS3)

        // set up mock ws sessions
        whenever(mockWsSession1.id).thenReturn(WS_SESSION1_ID)
        whenever(mockWsSession2.id).thenReturn(WS_SESSION2_ID)
        whenever(mockWsSession3.id).thenReturn(WS_SESSION3_ID)
    }


    // ======================================================================================= |
    // ================================= getByClientId()  ==================================== |
    // ======================================================================================= |
    
    @Test
    fun `getByClientId() - GIVEN existing client WHEN getByClientId is called THEN correct client handler should be returned`() {
        // given
        clientController.addClientHandler(mockClientHandler1)
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // when
        val result = clientController.getByClientId(CLIENT1_ID)

        // then
        assertEquals(mockClientHandler1, result)
    }

    @Test
    fun `getByClientId() - GIVEN non-existing client WHEN getByClientId is called THEN null should be returned`() {
        // given non-existing client
        val clientId = "non-existing-client"

        // when getByClientId is called
        val result = clientController.getByClientId(clientId)

        // then null should be returned
        assertNull(result)
    }

    // ======================================================================================= |
    // =============================== getByWsSessionId()  =================================== |
    // ======================================================================================= |


    @Test
    fun `getByWsSessionId() - GIVEN existing session ID WHEN getByWsSessionId is called THEN correct client handler should be returned`() {
        // given
        clientController.addClientHandler(mockClientHandler1)
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // when
        val result = clientController.getByWsSessionId(WS_SESSION1_ID)

        // then
        assertEquals(mockClientHandler1, result)
    }

    @Test
    fun `getByWsSessionId() - GIVEN non-existing session ID WHEN getByWsSessionId is called THEN null should be returned`() {
        // given non-existing session ID
        val sessionId = "non-existing-session"

        // when getByWsSessionId is called
        val result = clientController.getByWsSessionId(sessionId)

        // then null should be returned
        assertNull(result)
    }

    // ======================================================================================= |
    // =============================== getByHostPort()  ====================================== |
    // ======================================================================================= |

    @Test
    fun `getByHostPort() - GIVEN existing host port WHEN getByHostPort is called THEN correct client handler should be returned`() {
        // given
        clientController.addClientHandler(mockClientHandler1)
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // when
        val result = clientController.getByHostPort(UDP_ADDRESS1.toHostPortString())

        // then
        assertEquals(mockClientHandler1, result)
    }

    @Test
    fun `getByHostPort() - GIVEN non-existing host port WHEN getByHostPort is called THEN null should be returned`() {
        // given non-existing host port
        val hostPort = "non-existing-host-port"

        // when getByHostPort is called
        val result = clientController.getByHostPort(hostPort)

        // then null should be returned
        assertNull(result)
    }

    // ======================================================================================== |
    // =============================== addClientHandler()  ==================================== |
    // ======================================================================================== |

    @Test
    fun `addClientHandler() - GIVEN new client handler WHEN addClientHandler is called THEN client should be added successfully`() {
        // given new client handler
        val newClientHandler = mockClientHandler1

        // when addClientHandler is called
        clientController.addClientHandler(newClientHandler)

        // then client should be added successfully
        assertTrue(clientController.containsByWsSessionId(WS_SESSION1_ID))
        assertEquals(newClientHandler,clientController.getByClientId(CLIENT1_ID))
    }

    @Test
    fun `addClientHandler() - GIVEN duplicate client handler WHEN addClientHandler is called THEN existing handler should be replaced`() {
        // set up
        val newHandler = mock(ClientHandler::class.java)
        whenever(newHandler.id).thenReturn(CLIENT1_ID)
        whenever(newHandler.wsSession).thenReturn(mockWsSession1)
        whenever(newHandler.udpAddress).thenReturn(UDP_ADDRESS1)

        // given existing client handler
        clientController.addClientHandler(mockClientHandler1)
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // when addClientHandler is called with new client handler
        clientController.addClientHandler(newHandler)
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // then existing handler should be replaced
        assertEquals(newHandler,clientController.getByClientId(CLIENT1_ID))
        assertEquals(newHandler, clientController.getByHostPort(UDP_ADDRESS1.toHostPortString()))
        assertEquals(newHandler, clientController.getByWsSessionId(WS_SESSION1_ID))
    }

    // ======================================================================================== |
    // =============================== removeClientHandler()  ================================= |
    // ======================================================================================== |

    @Test
    fun `removeClientHandler() - GIVEN existing client WHEN removeClientHandler is called THEN client should be removed successfully`() {
        // given existing client
        clientController.addClientHandler(mockClientHandler1)
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // when removeClientHandler is called
        clientController.removeClientHandler(CLIENT1_ID)

        // then client should be removed successfully
        assertNull(clientController.getByClientId(CLIENT1_ID))
        assertNull(clientController.getByHostPort(UDP_ADDRESS1.toHostPortString()))
        assertNull(clientController.getByWsSessionId(WS_SESSION1_ID))
    }

    @Test
    fun `removeClientHandler() - GIVEN non-existing client WHEN removeClientHandler is called THEN no action should be taken`() {
        // given non-existing client
        val clientId = "non-existing-client"

        // when removeClientHandler is called
        clientController.removeClientHandler(clientId)

        // then no action should be taken
        assertNull(clientController.getByClientId(clientId))
    }

    // ======================================================================================== |
    // =============================== setHostPort()  ========================================= |
    // ======================================================================================== |

    @Test
    fun `setHostPort() - GIVEN valid client and host port WHEN setHostPort is called THEN host port should be associated with client`() {
        // given valid client and host port
        clientController.addClientHandler(mockClientHandler1)

        // when setHostPort is called
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // then host port should be associated with client
        assertEquals(mockClientHandler1, clientController.getByHostPort(UDP_ADDRESS1.toHostPortString()))
    }

    @Test
    fun `setHostPort() - GIVEN non-existing client WHEN setHostPort is called THEN exception should be thrown`() {
        // given non-existing client
        val clientId = "non-existing-client"

        // then an exception should be thrown
        assertThrows<IllegalStateException> {

            // when setHostPort is called
            clientController.setHostPort(clientId, UDP_ADDRESS1.toHostPortString())
        }
    }

    @Test
    fun `setHostPort() - GIVEN existing host port WHEN setHostPort is called THEN it should be updated successfully`() {
        // given existing host port
        clientController.addClientHandler(mockClientHandler1)
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS1.toHostPortString())

        // when setHostPort is called
        clientController.setHostPort(CLIENT1_ID, UDP_ADDRESS2.toHostPortString())

        // then it should be updated successfully
        assertEquals(mockClientHandler1, clientController.getByHostPort(UDP_ADDRESS2.toHostPortString()))
    }

    // ======================================================================================== |
    // =============================== containsByWsSessionId()  =============================== |
    // ======================================================================================== |

    @Test
    fun `containsByWsSessionId() - GIVEN existing session ID WHEN containsByWsSessionId is called THEN it should return true`() {
        // given existing session ID
        clientController.addClientHandler(mockClientHandler1)

        // when containsByWsSessionId is called
        val result = clientController.containsByWsSessionId(WS_SESSION1_ID)

        // then it should return true
        assertTrue(result)
    }

    @Test
    fun `containsByWsSessionId() - GIVEN non-existing session ID WHEN containsByWsSessionId is called THEN it should return false`() {
        // given non-existing session ID
        val sessionId = "non-existing-session"

        // when containsByWsSessionId is called
        val result = clientController.containsByWsSessionId(sessionId)

        // then it should return false
        assertFalse(result)
    }

    // ======================================================================================== |
    // =============================== getAllClientIds()  ===================================== |
    // ======================================================================================== |

    @Test
    fun `getAllClientIds() - GIVEN multiple clients WHEN getAllClientIds is called THEN list of all client IDs should be returned`() {
        // given multiple clients
        clientController.addClientHandler(mockClientHandler1)
        clientController.addClientHandler(mockClientHandler2)
        clientController.addClientHandler(mockClientHandler3)

        // when getAllClientIds is called
        val result = clientController.getAllClientIds()

        // then list of all client IDs should be returned
        assertEquals(3,result.size)
        assertTrue(result.contains(CLIENT1_ID))
        assertTrue(result.contains(CLIENT2_ID))
        assertTrue(result.contains(CLIENT3_ID))
    }

    @Test
    fun `getAllClientIds() - GIVEN no clients WHEN getAllClientIds is called THEN empty list should be returned`() {
        // given no clients
            /* nothing to do */

        // when getAllClientIds is called
        val result = clientController.getAllClientIds()

        // then empty list should be returned
        assertTrue(result.isEmpty())
    }

}
