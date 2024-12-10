# GET THE ONLINE PARTICIPANTS

from . import *


# body = {
#   "type": "string",
#   "playerId": "string",
#   "lobbyId": "string",
#   "gameType": "string",
# }
# get_online_player_ids


def get_participants():
    body = {
        "type": "get_online_player_ids"
    }
    try:
        response = requests.post(URL+"/manager", json=body)
        return response.json()

    except Exception as e:
        return None


