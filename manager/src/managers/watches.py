# GET THE ONLINE WATCHES

import requests

# body = {
#   "type": "string",
#   "playerId": "string",
#   "lobbyId": "string",
#   "gameType": "string",
# }
# get_online_player_ids


def get_watches():
    url = "https://api.sandbox.watch.game"
    body = {
        "type": "get_online_player_ids"
    }
    response = requests.post(url, json=body)
    return response.json()

