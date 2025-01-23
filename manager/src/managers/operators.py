from . import *
from .logger import Logger
import re
import requests
from flask import jsonify




# Utility function to validate password requirements
# def validate_password(password):
#     # Ensure the password is not empty and contains only English letters and digits
#     return bool(password) and bool(re.match(r"^[A-Za-z\d]+$", password))


def add_operator(user_id, password):
    try:
        # if not validate_password(password):
        #     Logger.log_error("Password does not meet the requirements")
        #     return jsonify({"message": "Password does not meet the requirements", "success": False})
        body = operators_request(user_id, password).to_dict()
        response = requests.post(URL + "/operators/add", json=body)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                Logger.log_info("User added successfully")
                return jsonify({"message": "User added successfully", "success": True})
            else:
                Logger.log_error(f"Failed to add user: {ser_res.get_message()}")
                return jsonify({"message": ser_res.get_message(), "success": False})
        else:
            Logger.log_error("Failed to add user" + str(response.json()))
            return jsonify({"message": "Failed to add user", "success": False})
    except Exception as e:
        Logger.log_error(f"Failed to add user: {e}")
        return jsonify({"message": f"Failed to add user, {e}", "success": False})


def remove_operator(user_id):
    try:
        body = operators_request(user_id, "").to_dict()
        response = requests.post(URL + "/operators/remove", json=body)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                Logger.log_info("User removed successfully")
                return jsonify({"message": "User removed successfully", "success": True})
            else:
                Logger.log_error(f"Failed to remove user: {ser_res.get_message()}")
                return jsonify({"message": ser_res.get_message(), "success": False})
        else:
            Logger.log_error("Failed to remove user" + str(response.json()))
            return jsonify({"message": "Failed to remove user", "success": False})
    except Exception as e:
        Logger.log_error(f"Failed to remove user: {e}")
        return jsonify({"message": f"Failed to remove user, {e}", "success": False})

def get_operators():
    try:
        response = requests.post(URL + "/operators/get", json={'test': 'test'})
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                print(ser_res.get_payload())
                return jsonify({"message": "Operators retrieved successfully", "success": True, "payload": ser_res.payload})
            else:
                Logger.log_error(f"Failed to get operators: {ser_res.get_message()}")
                return jsonify({"message": ser_res.get_message(), "success": False})
        else:
            Logger.log_error("Failed to get operators" + str(response.json()))
            return jsonify({"message": "Failed to get operators", "success": False})
    except Exception as e:
        Logger.log_error(f"Failed to get operators: {e}")
        return jsonify({"message": f"Failed to get operators, {e}", "success": False})
