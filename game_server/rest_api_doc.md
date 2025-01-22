# Game Server REST API Documentation

---

## Server URL

http://ims-game-server:8640

---

## Response Format

All responses from the API adhere to the following structure:

```json
{
  "message": "string?",
  "success": "boolean",
  "payload": "[string]?"
}
```
Note: Not all fields are present in every response. fields marked with `?` can be missing
- `message` - provides a message to show to the client if there is any.
  Also can be used as an error message if the success field is `false`.
- `success` - indicates whether the operation was successful `READ: this field always exists`
- `payload` - if applicable, holds a list of data that is returned from the server.
  If the expected data is otherwise not a simple string,
  it will be a serialized json object that needs to be parsed.

---

## Endpoints

### 1. **GET** `/auth`

#### Description
This endpoint is used for user authentication. <br/>
It validates the credentials provided in the `Authorization` header and returns a `Bearer` token that is required for subsequent requests.

#### Request

- **Headers**: 
    - `Authorization : Basic <base64>` <br/> where `<base64>` - base64 encoded string of `username:password`

#### Return value
the success field will be true if the credentials are valid, and false otherwise. <br/>
If the credentials are valid, the `payload` field in the response json will hold a
list with a single string, which is the token. 

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
  "playerId": "string?",
  "lobbyId": "string?",
  "gameType": "string?",
  "duration": "integer?",
  "sessionId": "string?",
  "sessionIds": ["string?"]
}
```
  Note: Not all fields are present in every request. fields marked with `?` can be missing
### Fields:

#### `type` (required):
Specifies the type of operation.  
**Possible values:**
- `get_online_player_ids`
- `get_lobbies`
- `get_lobby`
- `create_lobby`
- `remove_lobby`
- `set_lobby_type`
- `set_game_duration`
- `join_lobby`
- `leave_lobby`
- `start_game`
- `end_game`
- `create_session`
- `remove_session`
- `get_sessions`
- `change_sessions_order`

#### `playerId` (situational):
ID of a player involved in the operation.

#### `lobbyId` (situational):
ID of the target lobby.

#### `gameType` (situational):
The game type (e.g. `water_ripples`).

#### `duration` (situational):
The duration of the game in seconds.

#### `sessionId` (situational):
The ID of the session.

#### `sessionIds` (situational):
A list of session IDs.

<br/>

### Request types and their required fields: <br/><br/>

### `get_online_player_ids`
**Required fields:**
- None

**Description:**
Retrieves the list with ids of online players. No additional fields are required for this request type.

**Return value:**
```json
{
  "success": true,
  "payload": [
    "player1",
    "player2",
    "player3"
  ]
}
```

<br/>

### `get_lobbies`
**Required fields:**
- None

**Description:**  
Retrieves the list of available lobbies. No additional fields are required for this request type.

**Return value:**
```json
{
  "success": true,
  "payload": [
    {
      "lobbyId": "lobby1",
      "gameType": "gameTypeA",
      "state": "waiting",
      "players": ["player1", "player2"],
      "readyStatus": ["false", "true"]
    },
    {
      "lobbyId": "lobby2",
      "gameType": "gameTypeB",
      "state": "playing",
      "players": ["player3"],
      "readyStatus": ["true"]
    }
  ]
}
```

<br/>

### `get_lobby`
**Required fields:**
- `lobbyId`

**Description:**  
This request type is used to get the details of a specific lobby. The `lobbyId` must be provided to identify the lobby.

**Return value:**
```json
{
  "success": true,
  "payload": [
    {
      "lobbyId": "lobby1",
      "gameType": "gameTypeA",
      "state": "waiting",
      "players": ["player1", "player2"],
      "readyStatus": ["false", "true"]
    }
  ]
}
```

<br/>

### `create_lobby`
**Required fields:**
- `gameType`

**Description:**  
This request type is used to create a new lobby. The `gameType` field must be provided to specify the type of game to be played in the new lobby.

**Return value:**
```json
{
  "success": true,
  "payload": ["newLobbyId"]
}
```

<br/>

### `remove_lobby`
**Required fields:**
- `lobbyId`

**Description:**
This request type is used to remove a specific lobby. The `lobbyId` must be provided to identify the lobby to be removed.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `set_lobby_type`
**Required fields:**
- `lobbyId`
- `gameType`

**Description:**  
This request type is used to change the game type for a specific lobby. Both the `lobbyId` (to identify the lobby) and the `gameType` (to specify the new type of game) are required.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `set_game_duration`
**Required fields:**
- `lobbyId`
- `duration`

**Description:**
This request type is used to set the duration of the game in a specific lobby. Both the `lobbyId` (to identify the lobby) and the `duration` (to set the duration of the game) are required.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `join_lobby`
**Required fields:**
- `lobbyId`
- `playerId`

**Description:**  
This request type is used to join an existing lobby. Both the `lobbyId` (to identify the target lobby) and the `playerId` (to identify the player joining the lobby) are required.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `leave_lobby`
**Required fields:**
- `lobbyId`
- `playerId`

**Description:**  
This request type is used when a player leaves a lobby. Both `lobbyId` (to identify the lobby) and `playerId` (to identify the player leaving the lobby) are required.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `start_game`
**Required fields:**
- `lobbyId`

**Description:**  
This request type starts the game in a specific lobby. The `lobbyId` must be provided to identify the lobby where the game should start.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `end_game`
**Required fields:**
- `lobbyId`

**Description:**  
This request type ends the game in a specific lobby. The `lobbyId` must be provided to identify the lobby where the game should end.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `create_session`
**Required fields:**
- `lobbyId`
- `gameType`
- `duration`

**Description:**
This request type is used to create a new session.
The `lobbyId` (to identify the lobby), `gameType` (to specify the type of game),
and `duration` (to set the duration of the game) are required.

**Return value:**
```json
{
  "success": true,
  "payload": ["newSessionId"]
}
```

<br/>

### `remove_session`
**Required fields:**
- `lobbyId`
- `sessionId`

**Description:**
This request type is used to remove a specific session from a lobby.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `get_sessions`
**Required fields:**
- `lobbyId`

**Description:**
This request type is used to get the list of sessions in a specific lobby.

**Return value:**
```json
{
  "success": true,
  "payload": [
    {
      "sessionId": "session1",
      "gameType": "gameTypeA",
      "duration": 60
    },
    {
      "sessionId": "session2",
      "gameType": "gameTypeB",
      "duration": 120
    }
  ]
}
```

<br/>

### `change_sessions_order`
**Required fields:**
- `lobbyId`
- `sessionIds`

**Description:**
This request type is used to change the order of sessions in a lobby.

**Return value:**
```json
{
  "success": true
}
```

<br/>

### `start_experiment`
**Required fields:**
- `lobbyId`

**Description:**
This request type is used to start an experiment in a specific lobby.

**Return value:**
```json
{
  "success": true
}
```

---

### 3. **POST** `/data/{section}`

#### Description
This endpoint is used to add a new entity to the database.

#### Request
- **Path Variables**:
  - `section` (required): The action to perform.
    - **Possible values**:
      - `participant`
      - `lobby`
      - `session`
      - `sessionEvent`


- **Body**:
  - participant: 
    ```json
    {
      "firstName": "string",
      "lastName": "string",
      "age": "integer",
      "gender": "string",
      "phone": "string",
      "email": "string"
    }
    ```
    - lobby:
    ```json
    {
      "pid1": "integer",
      "pid2": "integer"
    }
    ```

#### **Response**

- **Success**:
  ```json
  {
    "success": true,
    "payload": ["id"]
  }
  ```

- **Failure**:
  ```json
  {
    "message": "Failed to insert to table Participants",
    "success": false,
    "payload": ["error details"]
  }
  ```

---

### 3. **DELETE** `/data/{section}`

#### Description
This endpoint is used to delete an existing entity from the database.

#### Request
- **Path Variables**:
  - `section` (required): The action to perform.
    - **Possible values**:
      - `participant`
      - `lobby`
      - `session`
      - `sessionEvent`


- **Body**:
  - participant:
    ```json
    {
      "pid": "id"
    }
    ```
    - lobby:
    ```json
    {
      "lobbyId": "integer"
    }
    ```

### 3. **PUT** `/data/{section}`

#### Description
This endpoint is used to update an existing entity from the database.

#### Request
- **Path Variables**:
  - `section` (required): The action to perform.
    - **Possible values**:
      - `participant`
      - `lobby`
      - `session`
      - `sessionEvent`


- **Body**:
  - participant:
  pid must be provided. every other field is optional.
    ```json
    {
      "pid": "id"
    }
    ```
    - lobby:
    ```json
    {
      "placeholder": "bluh bluh"
    }
    ```


### 3. **GET** `/data/{section}`

#### Description
This endpoint is used to get an existing entity from the database, or all of the data in a table.

#### Request
- **Path Variables**:
  - `section` (required): The action to perform.
    - **Possible values**:
      - `participant`
      - `lobby`
      - `session`
      - `sessionEvent`


- **Body**:
  - participant:
      - In order to get a specific participant, provide pid.
      - To get all of the table, provide no additional fields
        ```json
        {
          "pid": "id"
        }
        ```
        - lobby:
        ```json
        {
          "placeholder": "bluh bluh"
        }
        ```
#### **Response**

- **Success**:
  ```json
  {
    "success": true,
    "payload": ["json data of asked entity"]
  }
  ```
  or
```json
  {
    "success": true,
    "payload": ["json data of entity1 in table",
                "json data of entity2 in table",
                "json data of entity3 in table"]
  }
  ```

- **Failure**:
  ```json
  {
    "message": "Failed to select table Participants",
    "success": false,
    "payload": ["error details"]
  }
  ```

### 4. POST `/operators/{action}`

#### **Description**
This endpoint manages user operations such as adding or removing operators. The specific action is determined by the `action` path variable, which can be either `add` or `remove`.

---

#### **Request**

- **Path Variables**:
  - `action` (required): The action to perform.
    - **Possible values**:
      - `add`: Add a new operator.
      - `remove`: Remove an existing operator.

- **Body**:
  ```json
  {
    "userId": "string",
    "password": "string"
  }
  ```
  **Required Fields**:
  - `userId`: The unique identifier of the user. Must be in lowercase.
  - `password`: The password of the user. (Required only for the `add` action.)

  **Password Requirements** (for `add` action):
  - At least 8 characters.
  - At least one uppercase letter.
  - At least one lowercase letter.
  - At least one digit.
  - May contain special characters (`!@#$%^&*()-=_+[]{};:<>?/\~|`).

---

#### **Response**

- **Success**:
  ```json
  {
    "message": "User added successfully",
    "success": true
  }
  ```
  or
  ```json
  {
    "message": "User removed successfully",
    "success": true
  }
  ```

- **Failure**:
  ```json
  {
    "message": "string",
    "success": false
  }
  ```
  **Examples of failure messages**:
  - `"Invalid action"`
  - `"User already exists"`
  - `"Password does not meet the requirements"`
  - `"User not found"`

---



