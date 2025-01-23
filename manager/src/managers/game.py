# CONTROL GAME
from . import *
from .logger import Logger


def start_game(lobby_id):
    body = server_request(GAME_REQUEST_TYPE.start_experiment.name, lobbyId=lobby_id).to_dict()

    try:
        res = requests.post(URL+"/manager", json=body)
        Logger.log_info(f"Starting game: {res}")
        if res.status_code == 200:
            ser_res = server_response(res)
            Logger.log_info(f"Starting game: {ser_res}")
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
        res = requests.post(URL+"/manager", json=body)
        if res.status_code == 200:
            ser_res = server_response(res)
            if ser_res.get_success():
                return True
            else:
                Logger.log_error(f"Error stopping game: {ser_res}")

    except Exception as e:
        Logger.log_error(f"Error stopping game: {e}")
        return False
