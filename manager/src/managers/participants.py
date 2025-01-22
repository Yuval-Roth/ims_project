# GET THE ONLINE PARTICIPANTS

from . import *
from .logger import Logger


def get_participants():
    body = server_request(GAME_REQUEST_TYPE.get_online_player_ids.name).to_dict()
    try:
        response = requests.post(URL+"/manager", json=body, timeout=0.1)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                return ser_res.get_payload()

            Logger.log_error(f"Failed to get participants: {ser_res.get_message()}")
            return None

    except Exception as e:
        Logger.log_error(f"Failed to get participants, {e}")
        return None
    

def add_participant(participant):
    body = server_request(GAME_REQUEST_TYPE.add_participant.name, participant).to_dict()
    try:
        response = requests.post(URL+"/manager", json=body, timeout=0.1)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                return ser_res.get_payload()

            Logger.log_error(f"Failed to add participant: {ser_res.get_message()}")
            return None

    except Exception as e:
        Logger.log_error(f"Failed to add participant, {e}")
        return None
    
def remove_participant(participant_id):
    body = server_request(GAME_REQUEST_TYPE.remove_participant.name, {"id": participant_id}).to_dict()
    try:
        response = requests.post(URL+"/manager", json=body, timeout=0.1)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                return ser_res.get_payload()

            Logger.log_error(f"Failed to remove participant: {ser_res.get_message()}")
            return None

    except Exception as e:
        Logger.log_error(f"Failed to remove participant, {e}")
        return None
    

def edit_participant(participant):
    body = server_request(GAME_REQUEST_TYPE.edit_participant.name, participant).to_dict()
    try:
        response = requests.post(URL+"/manager", json=body, timeout=0.1)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                return ser_res.get_payload()

            Logger.log_error(f"Failed to edit participant: {ser_res.get_message()}")
            return None

    except Exception as e:
        Logger.log_error(f"Failed to edit participant, {e}")
        return None


