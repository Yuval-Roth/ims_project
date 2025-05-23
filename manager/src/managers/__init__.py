import requests
import json
from ..ENUMS import *
import base64
from .logger import Logger

RUNNING_LOCAL = False

if RUNNING_LOCAL:
    URL = "http://localhost:8080"
else:
    URL = "http://ims-game-server:8080/"

GAL = False

if GAL:
    URL = "https://ims-project.cs.bgu.ac.il:8640/"


from flask import session


def auth_headers():
    token = session.get('token')
    return {"Authorization": f"Bearer {token}"} if token else {}

def post_auth(url, json, headers=None, timeout=2.0):
    final_headers = auth_headers()
    if headers:
        final_headers.update(headers)
    return requests.post(url, json=json, headers=final_headers, timeout=timeout)

def get_auth(url, headers=None, timeout=2.0):
    final_headers = auth_headers()
    if headers:
        final_headers.update(headers)
    return requests.get(url, headers=final_headers, timeout=timeout)

def authenticate_basic(username: str, password: str):
    try:
        token = base64.b64encode(f"{username}:{password}".encode()).decode()
        headers = {"Authorization": f"Basic {token}"}
        Logger.log_debug(f"Sending auth request to {URL + 'auth'} with headers: {headers}")
        res = requests.get(URL + "login", headers=headers)

        Logger.log_debug(f"Auth status: {res.status_code}, response: {res.text}")
        return server_response(res)
    except Exception as e:
        Logger.log_error(f"Authentication error: {e}")
        return None




class server_request:
    # {
    #   "type": "string",
    #   "playerId": "string?",
    #   "lobbyId": "string?",
    #   "gameType": "string?",
    # }
    # can add more fields if needed, automaticly
    def __init__(self, type: str, playerId: str = "", lobbyId: str = "", gameType: str = ""):
        self.type: str = type
        self.playerId: str = playerId
        self.lobbyId: str = lobbyId
        self.gameType: str = gameType

    def to_dict(self):
        return {k: v for k, v in self.__dict__.items() if v != ""}


class operators_request:
    # {
    #   "userId": "string",
    #   "password": "string"
    # }
    def __init__(self, userId: str, password: str):
        self.userId: str = userId
        self.password: str = password

    def to_dict(self):
        return {k: v for k, v in self.__dict__.items() if v != ""}

class server_response:
    def __init__(self, res: requests.Response):
        try:
            data = res.json()
        except Exception as e:
            Logger.log_error(f"Invalid JSON response: {e} — Raw response: {res.text}")
            data = {}

        self.message: str = data.get("message", "")
        self.success: bool = bool(data.get("success"))
        self.payload: list[str] = data.get("payload", [])

    def __str__(self):
        return f"Message: {self.message}, Success: {self.success}, Payload: {self.payload}"

    def get_success(self):
        return self.success

    def get_message(self):
        return self.message

    def get_payload(self):
        return self.payload
