# Game Server REST API Documentation

---

## Server URL

http://game_server:8640

---

## Endpoints

### 1. **GET** `/auth`

#### Description
This endpoint is used for user authentication. <br/>
It validates the credentials provided in the `Authorization` header and returns a `Bearer` token that is required for subsequent requests.

#### Request

- **Headers**: 
    - `Authorization : Basic <base64>` <br/> where `<base64>` - base64 encoded string of `username:password`

<br/>

---

### 2. **POST** `/manager`

#### Description
Handles all game server requests

#### Request

- **Headers**:

  - `Authorization: Bearer <token>` - Required for all requests.
  
- **Body**:
  ```json
  {
    "type": "string",
    "playerId": "string",
    "lobbyId": "string",
    "gameType": "string",
  }
### Fields:

#### `type` (required):
Specifies the type of operation.  
**Possible values:**
- `get_online_player_ids`
- `get_lobbies`
- `get_lobby`
- `create_lobby`
- `set_lobby_type`
- `join_lobby`
- `leave_lobby`
- `start_game`
- `end_game`

#### `playerId` (situational):
ID of a player involved in the operation.

#### `lobbyId` (situational):
ID of the target lobby.

#### `gameType` (situational):
The game type (e.g., `poc`, `water_ripples`).

<br/>

### Request types and their required fields: <br/><br/>

### `get_online_player_ids`
**Required fields:**
- None

**Description:**
Retrieves the list with ids of online players. No additional fields are required for this request type.

<br/>

### `get_lobbies`
**Required fields:**
- None

**Description:**  
Retrieves the list of available lobbies. No additional fields are required for this request type.

<br/>

### `get_lobby`
**Required fields:**
- `lobbyId`

**Description:**  
This request type is used to get the details of a specific lobby. The `lobbyId` must be provided to identify the lobby.

<br/>

### `create_lobby`
**Required fields:**
- `gameType`

**Description:**  
This request type is used to create a new lobby. The `gameType` field must be provided to specify the type of game to be played in the new lobby.

<br/>

### `set_lobby_type`
**Required fields:**
- `lobbyId`
- `gameType`

**Description:**  
This request type is used to change the game type for a specific lobby. Both the `lobbyId` (to identify the lobby) and the `gameType` (to specify the new type of game) are required.

<br/>

### `join_lobby`
**Required fields:**
- `lobbyId`
- `playerId`

**Description:**  
This request type is used to join an existing lobby. Both the `lobbyId` (to identify the target lobby) and the `playerId` (to identify the player joining the lobby) are required.

<br/>

### `leave_lobby`
**Required fields:**
- `lobbyId`
- `playerId`

**Description:**  
This request type is used when a player leaves a lobby. Both `lobbyId` (to identify the lobby) and `playerId` (to identify the player leaving the lobby) are required.

<br/>

### `start_game`
**Required fields:**
- `lobbyId`

**Description:**  
This request type starts the game in a specific lobby. The `lobbyId` must be provided to identify the lobby where the game should start.

<br/>

### `end_game`
**Required fields:**
- `lobbyId`

**Description:**  
This request type ends the game in a specific lobby. The `lobbyId` must be provided to identify the lobby where the game should end.

---

### 3. POST /data
**Description**  
Currently a placeholder endpoint, not implemented.

**Response**  
"Not Implemented" - Always returned since this endpoint is not yet functional.

---

## Response Format

All responses from the API adhere to the following structure:

```json
{
  "message": "string",
  "success": "boolean",
  "payload": ["string"]
}
```
Not all fields are present in every response.
- `message` - provides a message to show to the client if there is any.
Also used as an error message if the success field is `false`.
- `success` indicates whether the operation was successful,
- `payload` if applicable, holds a list of data that is returned from the server.
If the expected data is otherwise not a simple string,
it will be a serialized json object that needs to be parsed.
