from . import *
from .logger import Logger
import re
import requests
from flask import jsonify




# Utility function to validate password requirements
def validate_password(password):
    if len(password) < 8:
        return False
    if not re.search(r"[A-Z]", password):
        return False
    if not re.search(r"[a-z]", password):
        return False
    if not re.search(r"\d", password):
        return False
    return True

# 4. POST /operators/{action}
# Description
# This endpoint manages user operations such as adding or removing operators. The specific action is determined by the action path variable, which can be either add or remove.
#
# Request
# Path Variables:
#
# action (required): The action to perform.
# Possible values:
# add: Add a new operator.
# remove: Remove an existing operator.
# Body:
#
# {
#   "userId": "string",
#   "password": "string"
# }
# Required Fields:
#
# userId: The unique identifier of the user. Must be in lowercase.
# password: The password of the user. (Required only for the add action.)
# Password Requirements (for add action):
#
# At least 8 characters.
# At least one uppercase letter.
# At least one lowercase letter.
# At least one digit.
# May contain special characters (!@#$%^&*()-=_+[]{};:<>?/\~|).
# Response
# Success:
#
# {
#   "message": "User added successfully",
#   "success": true
# }
# or
#
# {
#   "message": "User removed successfully",
#   "success": true
# }
# Failure:
#
# {
#   "message": "string",
#   "success": false
# }
# Examples of failure messages:
#
# "Invalid action"
# "User already exists"
# "Password does not meet the requirements"
# "User not found"


def add_operator(user_id, password):
    try:
        if not validate_password(password):
            return jsonify({"message": "Password does not meet the requirements", "success": False})
        body = operators_request(user_id, password).to_dict()
        response = requests.post(URL + "/operators/add", json=body)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                return jsonify({"message": "User added successfully", "success": True})
            else:
                return jsonify({"message": ser_res.get_message(), "success": False})
        else:
            return jsonify({"message": "Failed to add user", "success": False})
    except Exception as e:
        return jsonify({"message": f"Failed to add user, {e}", "success": False})


def remove_operator(user_id):
    try:
        body = operators_request(user_id, "").to_dict()
        response = requests.post(URL + "/operators/remove", json=body)
        if response.status_code in [200, 201]:
            ser_res = server_response(response)
            if ser_res.get_success():
                return jsonify({"message": "User removed successfully", "success": True})
            else:
                return jsonify({"message": ser_res.get_message(), "success": False})
        else:
            return jsonify({"message": "Failed to remove user", "success": False})
    except Exception as e:
        return jsonify({"message": f"Failed to remove user, {e}", "success": False})