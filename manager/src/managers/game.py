# CONTROL GAME
from . import *
from .logger import Logger


def start_game(lobby_id):
    body = server_request(GAME_REQUEST_TYPE.start_game.name, lobbyId=lobby_id).to_dict()

    try:
        res = requests.post(URL, json=body)
        if res.status_code == 200:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Error starting game: {ser_res}")

        else:
            Logger.log_error(f"Error starting game: {res}")

    except Exception as e:
        Logger.log_error(f"Error starting game: {e}")
        return False


def stop_game(lobby_id):
    body = server_request(GAME_REQUEST_TYPE.end_game.name, lobbyId=lobby_id).to_dict()

    try:
        res = requests.post(URL, json=body)
        if res.status_code == 200:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Error stopping game: {ser_res}")

    except Exception as e:
        Logger.log_error(f"Error stopping game: {e}")
        return False
