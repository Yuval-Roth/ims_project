# WebSocket API Documentation for Manager Events

#### WebSocket Endpoint
`/manager/events`

This WebSocket endpoint allows clients to subscribe to and handle manager-related events in real time. The events include player connection/disconnection, readiness toggles, and game state changes.

---

### Client Messages
The client can send the following message types to the server. Messages must be in JSON format:

#### `register_for_events`
- **Description**: Registers the client to receive manager events.
- **Message Format**:
  ```json
  {
    "type": "register_for_events"
  }
  ```

#### `ping`
- **Description**: Sends a ping message to check server connectivity.
- **Message Format**:
  ```json
  {
    "type": "ping"
  }
  ```

#### `heartbeat`
- **Description**: Updates the server with the client’s heartbeat to indicate the connection is alive.
- **Message Format**:
  ```json
  {
    "type": "heartbeat"
  }
  ```

---

### Server Messages
The server can send the following message types to the client. Messages will be in JSON format:

#### `pong`
- **Description**: Sent in response to a client’s `ping` message.
- **Message Format**:
  ```json
  {
    "type": "pong"
  }
  ```

#### `heartbeat`
- **Description**: Sent in response to a client’s `heartbeat` message.
- **Message Format**:
  ```json
  {
    "type": "heartbeat"
  }
  ```

#### `player_connected`
- **Description**: Indicates that a player has connected.
- **Message Format**:
  ```json
  {
    "type": "player_connected",
    "playerId": "<player_id>"
  }
  ```

#### `player_disconnected`
- **Description**: Indicates that a player has disconnected.
- **Message Format**:
  ```json
  {
    "type": "player_disconnected",
    "playerId": "<player_id>"
  }
  ```

#### `player_ready_toggle`
- **Description**: Indicates that a player has toggled their readiness state.
- **Message Format**:
  ```json
  {
    "type": "player_ready_toggle",
    "playerId": "<player_id>",
    "lobbyId": "<lobby_id>"
  }
  ```

#### `game_ended`
- **Description**: Indicates that a game has ended.
- **Message Format**:
  ```json
  {
    "type": "game_ended",
    "lobbyId": "<lobby_id>"
  }
  ```

---

### Heartbeat Mechanism
The server maintains a heartbeat mechanism to track active connections. The following rules apply:

1. **Client Responsibility**: The client must send a `heartbeat` message periodically.
2. **Timeout Threshold**: If no heartbeat is received from a client within the threshold defined by `HEARTBEAT_TIMEOUT_THRESHOLD = 30`, the server will:
    - Close the WebSocket connection.
    - Remove the client from the list of observers.

---

### Error Handling

#### Invalid Message Format
- **Description**: If the server receives a message that cannot be parsed or contains an invalid type.
- **Server Response**: No specific response; an error will be logged internally.

#### Unexpected Message Type
- **Description**: If the server receives a valid JSON message with an unexpected `type`.
- **Server Response**: The server logs the error internally without closing the connection.


