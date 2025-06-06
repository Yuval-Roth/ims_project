# managers/feedback.py

from typing import List
from flask import session
from . import post_auth, server_response, URL
from .logger import Logger
import json


def get_feedback(sid: str) -> List[dict]:
    """
    Fetches feedback for a given session ID by calling the internal
    /data/session/select/feedback endpoint, then returns a list of
    { "question": ..., "answer": ... } dictionaries.
    """
    feedback_list: List[dict] = []
    try:
        # Build request body
        body = {"sessionId": sid}

        # Perform authenticated POST to the internal endpoint
        token = session.get('token', '')
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        response = post_auth(f"{URL}/data/session/select/feedback", json=body, headers=headers)

        # Only proceed if we got a 200 or 201
        if response.status_code in (200, 201):
            ser_res = server_response(response)
            if ser_res.get_success():
                raw_payload = ser_res.get_payload() or []
                for item in raw_payload:
                    try:
                        # If payload items are JSON‐strings, parse them
                        feedback_list.append(json.loads(item))
                    except Exception:
                        # If already a dict, just append
                        if isinstance(item, dict):
                            feedback_list.append(item)
                return feedback_list

            Logger.log_error(f"get_feedback – API returned error: {ser_res.get_message()}")
            return []

        Logger.log_error(f"get_feedback – HTTP {response.status_code}")
        return []

    except Exception as e:
        Logger.log_error(f"get_feedback – Exception: {e}")
        return []


def get_experiment_feedback(exp_id: str) -> List[dict]:
    """
    Fetch all feedback for experiment (= lobby) with ID = exp_id.
    Calls internal /data/experiment/select/feedback endpoint.
    Returns a list of dictionaries like { "pid": ..., "question": ..., "answer": ... }.
    """
    feedback_list: List[dict] = []
    try:
        # Build request body
        body = {"expId": exp_id}

        # Perform authenticated POST to the internal endpoint
        token = session.get('token', '')
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        response = post_auth(f"{URL}/data/experiment/select/feedback", json=body, headers=headers)

        # Check HTTP status
        if response.status_code in (200, 201):
            ser_res = server_response(response)
            if ser_res.get_success():
                raw_payload = ser_res.get_payload() or []
                for item in raw_payload:
                    try:
                        # If payload items are JSON‐strings, parse them
                        feedback_list.append(json.loads(item))
                    except Exception:
                        # If already a dict, just append
                        if isinstance(item, dict):
                            feedback_list.append(item)
                return feedback_list

            Logger.log_error(f"get_experiment_feedback – API error: {ser_res.get_message()}")
            return []
        else:
            Logger.log_error(f"get_experiment_feedback – HTTP {response.status_code}")
            return []

    except Exception as e:
        Logger.log_error(f"get_experiment_feedback – Exception: {e}")
        return []

