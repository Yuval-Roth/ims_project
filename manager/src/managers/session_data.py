from . import *
from .logger import Logger
import requests

DUMMY_SESSIONS = [
    {
        "sessionId": "001",
        "participants": ["Alice Cohen", "Ben Levi"],
        "gameType": "Water Ripples",
        "duration": 120,
        "syncTolerance": 0.3,
        "syncWindowLength": 2.0,
        "timestamp": "2025-04-02T10:30:00"
    },
    {
        "sessionId": "002",
        "participants": ["Dana Shapiro", "Eli Golan"],
        "gameType": "Flour Mill",
        "duration": 90,
        "syncTolerance": 0.25,
        "syncWindowLength": 2.5,
        "timestamp": "2025-04-01T14:45:00"
    },
    {
        "sessionId": "003",
        "participants": ["Liron K.", "Maya T."],
        "gameType": "Wine Glasses",
        "duration": 75,
        "syncTolerance": 0.15,
        "syncWindowLength": 1.8,
        "timestamp": "2025-03-31T17:10:00"
    }
]

def get_all_sessions():
    body = server_request(GAME_REQUEST_TYPE.get_all_sessions.name).to_dict()

    try:
        response = requests.post(URL + "/manager", json=body, timeout=0.5)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                return ser_res.get_payload()

            Logger.log_error(f"Failed to get sessions: {ser_res.get_message()}")
            return DUMMY_SESSIONS

    except Exception as e:
        Logger.log_error(f"Failed to get sessions, {e}")
        return DUMMY_SESSIONS
