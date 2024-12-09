# LOBBY MANAGER
# Creates and manages the lobby for the game

from . import *


def create_lobby(participants: list[str], gameType=GAME_TYPE.water_ripples):
    body = {
        "type": "create_lobby",
        "gameType": gameType.name,
    }
    try:
        response = requests.post(URL + "/manager", json=body)
        if response.status_code in [200, 201]:
            jsoned = response.json()
            lobby_id = jsoned.get("lobbyId")

            for participant in participants:
                if not join_lobby(lobby_id, participant):
                    return f"Failed to join {participant} to lobby {lobby_id}"

            return lobby_id

    except Exception as e:
        print(f"Failed to create lobby, {e}")
        return None


def join_lobby(lobby_id: str, player_id: str):
    body = {
        "type": "join_lobby",
        "lobbyId": lobby_id,
        "playerId": player_id,
    }

    try:
        res = requests.post(URL + "/manager", json=body)
        return res.status_code in [200, 201]

    except Exception as e:
        return False