# CONTROL GAME
from . import *


# {
#   "type": "string",
#   "playerId": "string",
#   "lobbyId": "string",
#   "gameType": "string",
# }

# start_game
# Required fields:
#
# lobbyId
# Description:
# This request type starts the game in a specific lobby. The lobbyId must be provided to identify the lobby where the game should start.
#
#
# end_game
# Required fields:
#
# lobbyId
# Description:
# This request type ends the game in a specific lobby. The lobbyId must be provided to identify the lobby where the game should end.

def start_game(lobby_id):
    body = {
        "type": "start_game",
        "lobbyId": lobby_id
    }

    try:
        res = requests.post(URL, json=body)
        if res.status_code == 200:
            return True

    except Exception as e:
        print(f"Error starting game: {e}")
        return False


def stop_game(lobby_id):
    body = {
        "type": "end_game",
        "lobbyId": lobby_id
    }

    try:
        res = requests.post(URL, json=body)
        if res.status_code == 200:
            return True

    except Exception as e:
        print(f"Error stopping game: {e}")
        return False
