# LOBBY MANAGER
# Creates and manages the lobby for the game

from . import *
from .logger import Logger


class lobbies_list_payload:
    def __init__(self, payload: list[dict]):
        self.lobbies: list[lobby_info_payload] = [lobby_info_payload(lobby) for lobby in payload]


class lobby_info_payload:
    def __init__(self, payload: dict):
        self.lobbyId: str = payload.get("lobbyId")
        self.gameType: str = payload.get("gameType")
        self.state: str = payload.get("state")
        self.players: list[str] = payload.get("players")


def create_lobby(participants: list[str], gameType=GAME_TYPE.water_ripples):
    body = server_request(GAME_REQUEST_TYPE.create_lobby.name, gameType=gameType.name).to_dict()
    try:
        res = requests.post(URL + "/manager", json=body)
        if res.status_code in [200, 201]:
            ser_res = server_response(res)
            lobby_id = ser_res.get_payload()[0]

            for participant in participants:
                if not join_lobby(lobby_id, participant):
                    return f"Failed to join {participant} to lobby {lobby_id}"

            return lobby_id

    except Exception as e:
        Logger.log_error(f"Failed to create lobby, {e}")
        return None


def join_lobby(lobby_id: str, player_id: str):
    body = server_request(GAME_REQUEST_TYPE.join_lobby.name, player_id, lobby_id).to_dict()
    try:
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
