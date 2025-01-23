# LOBBY MANAGER
# Creates and manages the lobby for the game

from . import *
from .logger import Logger


class lobbies_list_payload:
    def __init__(self, payload: list[str]):
        # Parse each JSON string in the payload into a dictionary
        self.lobbies: list[lobby_info_payload] = [
            lobby_info_payload(json.loads(lobby)) for lobby in payload
        ]


class lobby_info_payload:
    def __init__(self, payload: dict):
        self.lobbyId: str = payload.get("lobbyId")
        self.gameType: str = payload.get("gameType")
        self.state: str = payload.get("state","waiting")
        self.players: list[str] = payload.get("players", [])
        self.readyStatus: list[bool] = payload.get("readyStatus", [])

    def to_dict(self):
        return {
            "lobbyId": self.lobbyId,
            "gameType": self.gameType,
            "state": self.state,
            "players": self.players,
            "readyStatus": self.readyStatus
        }


def get_lobbies():
    body = server_request(GAME_REQUEST_TYPE.get_lobbies.name).to_dict()
    try:
        response = requests.post(URL + "/manager", json=body)

        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                # Pass the payload (list of JSON strings) to lobbies_list_payload
                return lobbies_list_payload(ser_res.get_payload())

            Logger.log_error(f"Failed to get lobbies: {ser_res.get_message()}")
            return None
    except Exception as e:
        Logger.log_error(f"Failed to get lobbies, {e}")
        return None
    

def get_lobby(lobby_id: str) -> lobby_info_payload:
    body = server_request(GAME_REQUEST_TYPE.get_lobby.name, lobbyId=lobby_id).to_dict()
    try:
        response = requests.post(URL + "/manager", json=body)

        if response.status_code in [200, 201]:
            print(response.json())  # Debugging print
            ser_res = server_response(response)
            if ser_res.get_success():
                # Parse the JSON string into a dictionary before using it
                payload_dict = json.loads(ser_res.get_payload()[0])
                return lobby_info_payload(payload_dict)

            Logger.log_error(f"Failed to get lobby: {ser_res.get_message()}")
            return None
    except Exception as e:
        Logger.log_error(f"Failed to get lobby, {e}")
        return None


def create_lobby(participants: list[str], gameType=GAME_TYPE.water_ripples):
    body = server_request(GAME_REQUEST_TYPE.create_lobby.name, gameType=gameType.name).to_dict()
    try:
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            if ser_res.get_success():
                lobby_id = ser_res.get_payload()[0]
                print(f"Created lobby {lobby_id} with participants {participants}")

                for participant in participants:
                    if not join_lobby(lobby_id, participant):
                        return f"Failed to join {participant} to lobby {lobby_id}"

                return lobby_id
            else:
                Logger.log_error(f"Failed to create lobby: {ser_res.get_message()}")
                return None

    except Exception as e:
        Logger.log_error(f"Failed to create lobby, {e}")
        return None

def remove_lobby(lobby_id: str):
    body = server_request(GAME_REQUEST_TYPE.remove_lobby.name, lobbyId=lobby_id).to_dict()
    try:
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Failed to remove lobby: {ser_res.get_message()}")
                return False

    except Exception as e:
        Logger.log_error(f"Failed to remove lobby, {e}")
        return False

def join_lobby(lobby_id: str, player_id: str):
    body = server_request(GAME_REQUEST_TYPE.join_lobby.name, player_id, lobby_id).to_dict()
    try:
        Logger.log_debug(f"TRYING TO JOIN LOBBY {lobby_id} WITH PLAYER {player_id}")
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Error joining lobby: {ser_res}")

    except Exception as e:
        return False


# leave_lobby
# Required fields:
#
# lobbyId
# playerId
# Description:
# This request type is used when a player leaves a lobby. Both lobbyId (to identify the lobby) and playerId (to identify the player leaving the lobby) are required.
#
# Return value
# The success field will be true if the game type was changed successfully, and false otherwise with an error message.

def leave_lobby(lobby_id: str, player_id: str):
    body = server_request(GAME_REQUEST_TYPE.leave_lobby.name, player_id, lobby_id).to_dict()
    try:
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Error leaving lobby: {ser_res}")

    except Exception as e:
        return False


def change_session_order(lobby_id: str, session_order: list[str]):
    """
    Updates the order of sessions in a lobby.
    """
    body = server_request(GAME_REQUEST_TYPE.update_session_order.name, lobbyId=lobby_id).to_dict()
    body["sessionOrder"] = session_order
    try:
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Failed to update session order: {ser_res.get_message()}")
                return False

    except Exception as e:
        Logger.log_error(f"Failed to update session order: {e}")
        return False


def create_session(lobby_id: str, game_type: str, duration: int, sync_tolerance: int, window: int):
    """
    Creates a new session in the specified lobby.
    """
    body = server_request(GAME_REQUEST_TYPE.create_session.name, lobbyId=lobby_id, gameType=game_type).to_dict()
    body["duration"] = duration
    body["syncTolerance"] = sync_tolerance
    body["window"] = window
    try:
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            if ser_res.get_success():
                return ser_res.get_payload()[0]  # Return the session ID
            else:
                Logger.log_error(f"Failed to create session: {ser_res.get_message()}")
                return None

    except Exception as e:
        Logger.log_error(f"Failed to create session: {e}")
        return None


def delete_session(lobby_id: str, session_id: str):
    """
    Deletes a session from a lobby.
    """
    body = server_request(GAME_REQUEST_TYPE.delete_session.name, lobbyId=lobby_id).to_dict()
    body["sessionId"] = session_id
    try:
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Failed to delete session: {ser_res.get_message()}")
                return False

    except Exception as e:
        Logger.log_error(f"Failed to delete session: {e}")
        return False
