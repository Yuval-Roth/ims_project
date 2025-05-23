import os
from flask import Flask, render_template, request, redirect, url_for, session, flash, jsonify
import requests
from . import *
from .managers.participants import *
from .managers.lobby import *
from .managers.game import *
from .managers.operators import *
from .managers.session_data import *
from .managers.logger import Logger
from .ENUMS import *
import json

app = Flask(__name__)
app.secret_key = os.urandom(24)

Logger()


@app.route('/')
def home():
    # take username from sessionStorage
    username = session.get('username')
    if username:
        return redirect(url_for('main_menu'))
    return redirect(url_for('login'))


@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        auth_res = authenticate_basic(username, password)
        if auth_res and auth_res.get_success():
            # session.permanent = False  # <- this line ensures session ends on browser close
            session['username'] = username
            session['token'] = auth_res.get_payload()[0]
            return redirect(url_for('main_menu'))
        else:
            flash("Invalid credentials", "error")

    return render_template('login.html')



@app.route('/main_menu', methods=['GET', 'POST'])
def main_menu():
    if 'username' not in session:
        return redirect(url_for('login'))

    return render_template('main_menu.html')


@app.route('/get_participants', methods=['GET'])
def get_parts():
    online_participants_ids = get_participants()  # Replace with your function to fetch participants
    all_participants = get_participants_for_view()

    if len(online_participants_ids) > 0 and all_participants:
        # make every id in the list, from a '003' to '3'
        online_participants_ids = [int(x) for x in online_participants_ids]

        online_participants = []
        for part in all_participants:
            part = json.loads(part)
            if part['pid'] in online_participants_ids:
                part['id'] = str(part['pid']).zfill(3)
                online_participants.append(part)
        print(f"online_participants: {online_participants}")
        return jsonify(online_participants)
    return jsonify(all_participants)


@app.route('/lobbies', methods=['GET'])
def lobbies_menu():
    if 'username' not in session:
        return redirect(url_for('login'))
    lobbies = get_lobbies()  # list of dict : {lobbyId, players}
    # lobbies = None
    if lobbies:
        lobbies = lobbies.lobbies
    else:
        lobbies = LOBBIES
    return render_template('lobbies.html', lobbies=lobbies)


@app.route('/create_lobby', methods=['POST'])
def create_lobby_action():
    try:
        selected_participants = request.form.getlist('selected_participants')  # Get all selected participants
        if len(selected_participants) != 2:  # Ensure exactly two participants are selected
            return "Exactly two participants must be selected to create a lobby.", 400

        lobby_id = create_lobby(selected_participants)
        if not lobby_id:
            return "Failed to create lobby.", 500

        return redirect(url_for('lobby', lobby_id=lobby_id, selected_participants=",".join(selected_participants)))

    except Exception as e:
        Logger.log_error(f"Error creating lobby: {e}")
        return f"Error: {e}", 500


@app.route('/delete_lobby', methods=['GET'])
def delete_lobby_action():
    lobby_id = request.args.get('lobby_id')
    if lobby_id and remove_lobby(lobby_id):
        flash("Lobby removed successfully!")
    else:
        flash("Failed to remove lobby.")
    return redirect(url_for('lobbies_menu'))


@app.route('/lobby', methods=['GET', 'POST'])
def lobby():
    if 'username' not in session:
        return redirect(url_for('login'))

    # if request.method == 'POST':
    #     lobby_id = request.form.get('lobby_id')
    #     action = request.form.get('action')
    #     selected_participants = request.form.get('selected_participants')
    #     selected_participants_list = selected_participants.split(",") if selected_participants else []
    #
    #     if action == 'start':
    #         Logger.log_info(f"Starting game in lobby {lobby_id}")
    #         suc = start_game(lobby_id)
    #         # action = 'stop' if suc else 'start'
    #         # return render_template('lobby.html',
    #         #                        selected_participants=selected_participants_list,
    #         #                        lobby_id=lobby_id,
    #         #                        action=action)
    #     elif action == 'stop':
    #         Logger.log_info(f"Stopping game in lobby {lobby_id}")
    #         suc = stop_game(lobby_id)
    #         if suc:
    #             for participant in selected_participants_list:
    #                 sec = leave_lobby(lobby_id, participant)
    #                 if not sec:
    #                     Logger.log_error(f"Failed to remove {participant} from lobby {lobby_id}")
    #         return redirect(url_for('main_menu'))
    #
    #     # return redirect(url_for('lobby',
    #     #                         selected_participants=selected_participants,
    #     #                         lobby_id=lobby_id,
    #     #                         action=action))

    lobby_id = request.args.get('lobby_id', '')
    selected_participants = request.args.get('selected_participants', '')
    selected_participants_list = selected_participants.split(",") if selected_participants else []
    selected_participants_list = [x.strip() for x in selected_participants_list]
    state = request.args.get('state', 'waiting')
    if state == 'waiting':
        action = 'start'
    elif state == 'playing':
        action = 'stop'
    else:
        action = 'start'

    print(selected_participants_list, lobby_id, action, state)

    return render_template('lobby.html', selected_participants=selected_participants_list, lobby_id=lobby_id,
                           action='start', GAME_TYPE=GAME_TYPE)


@app.route('/start_game', methods=['POST'])
def start_game_route():
    try:
        # Extract the lobby_id from the JSON body
        lobby_id = request.json.get('lobby_id')
        if not lobby_id:
            return jsonify({"status": "error", "message": "Invalid data: Missing lobby_id"}), 400

        # Attempt to start the game
        success = start_game(lobby_id)
        if success:
            Logger.log_info(f"Game started successfully for lobby {lobby_id}")
            return jsonify({"status": "success", "message": "Game started successfully"}), 200
        else:
            Logger.log_error(f"Failed to start game for lobby {lobby_id}")
            return jsonify({"status": "error", "message": "Failed to start game"}), 500

    except Exception as e:
        Logger.log_error(f"Unexpected error in /start_game: {e}")
        return jsonify({"status": "error", "message": "Internal server error"}), 500


@app.route('/stop_game', methods=['POST'])
def stop_game_route():
    try:
        # Extract the lobby_id from the JSON body
        lobby_id = request.json.get('lobby_id')
        if not lobby_id:
            return jsonify({"status": "error", "message": "Invalid data: Missing lobby_id"}), 400

        # Attempt to stop the game
        success = stop_game(lobby_id)
        if success:
            Logger.log_info(f"Experiment stopped successfully for lobby {lobby_id}")
            return jsonify({"status": "success", "message": "Experiment stopped successfully"}), 200
        else:
            Logger.log_error(f"Failed to stop experiment for lobby {lobby_id}")
            return jsonify({"status": "error", "message": "Failed to stop experiment"}), 500

    except Exception as e:
        Logger.log_error(f"Unexpected error in /stop_game: {e}")
        return jsonify({"status": "error", "message": "Internal server error"}), 500


@app.route('/get_lobby', methods=['POST'])
def get_lobby_route():
    lobby_id = request.json.get('lobby_id')
    if not lobby_id:
        return jsonify({"status": "error", "message": "Invalid data"}), 400

    lobby = get_lobby(lobby_id)
    if lobby:
        return jsonify({
            "status": "success",
            "lobby": lobby.to_dict()
        })
    return jsonify({"status": "error", "message": "Lobby not found"}), 404


@app.route('/update_session_order', methods=['POST'])
def update_session_order():
    data = request.json
    lobby_id = data.get('lobby_id')
    session_order = data.get('session_order')

    if not lobby_id or not session_order:
        return jsonify({"status": "error", "message": "Invalid data"}), 400

    success = change_session_order(lobby_id, session_order)
    return jsonify({"status": "success" if success else "error"})


@app.route('/get_sessions', methods=['POST'])
def get_sessions_route():
    try:
        lobby_id = request.json.get('lobby_id')
        if not lobby_id:
            return jsonify({"status": "error", "message": "Missing lobby_id"}), 400

        sessions = get_sessions(lobby_id)
        if sessions:
            return jsonify({
                "status": "success",
                "sessions": sessions
            })
        return jsonify({"status": "error", "message": "Failed to get sessions"}), 500
    except Exception as e:
        Logger.log_error(f"Error getting sessions: {e}")
        return jsonify({"status": "error", "message": "Internal server error"}), 500


@app.route('/add_session', methods=['POST'])
def add_session():
    data = request.json
    lobby_id = data.get('lobby_id')
    game_type_name = data.get('gameType')  # Get the enum name
    duration = data.get('duration')
    sync_tolerance = data.get('syncTolerance')
    sync_window_length = data.get('syncWindowLength')
    print(data)

    if not (lobby_id and game_type_name and duration and sync_tolerance and sync_window_length):
        Logger.log_error(f"Invalid data: {data}")
        return jsonify({"status": "error", "message": "Invalid data"}), 400

    try:
        # Map the enum name to the corresponding value
        game_type = get_game_type_name_from_value(game_type_name)

        # Process and create the session (example function call)
        session_id = create_session(lobby_id, game_type, duration, sync_tolerance, sync_window_length)

        if session_id:
            return jsonify({
                "status": "success",
                "session": {
                    "gameType": game_type_name,  # Return the human-readable game type
                    "duration": duration,
                    "sessionId": session_id,
                    "syncTolerance": sync_tolerance,
                    "syncWindowLength": sync_window_length,
                }
            })
        Logger.log_error(f"Failed to create session for lobby {lobby_id}")
        return jsonify({"status": "error", "message": "Failed to create session"}), 500
    except KeyError:
        Logger.log_error(f"Invalid game type: {game_type_name}")
        return jsonify({"status": "error", "message": "Invalid game type"}), 400
    except Exception as e:
        Logger.log_error(f"Error adding session: {e}")
        return jsonify({"status": "error", "message": "Internal server error"}), 500


@app.route('/game_type_from_name', methods=['POST'])
def game_type_from_name():
    game_type_name = request.json.get('gameType')
    try:
        game_type = GAME_TYPE[game_type_name].value
    except KeyError:
        game_type = game_type_name
    return jsonify({'status': 'success', 'gameType': game_type})


@app.route('/delete_session', methods=['POST'])
def delete_session_route():
    # return jsonify({"status": "success"})

    lobby_id = request.json.get('lobby_id')
    session_id = request.json.get('session_id')

    if not lobby_id or not session_id:
        return jsonify({"status": "error", "message": "Invalid data"}), 400

    success = delete_session(lobby_id, session_id)
    return jsonify({"status": "success" if success else "error"})


###################### PARTCIPANTS ######################

@app.route('/participants', methods=['GET'])
def participants_menu():
    if 'username' not in session:
        return redirect(url_for('login'))
    participants = get_participants_for_view()
    # print(participants)

    if participants:
        participants = [json.loads(part) for part in participants]
    else:
        participants = []

    return render_template('participants.html', participants=participants)


@app.route('/add_participant', methods=['POST'])
def add_part():
    try:
        participant = {
            "firstName": request.json['firstName'],
            "lastName": request.json['lastName'],
            "age": request.json['age'],
            "gender": request.json['gender'],
            "phone": request.json['phone'],
            "email": request.json['email']
        }
        return add_participant(participant)
    except Exception as e:
        Logger.log_error(f"Error adding participant: {e}")
        return jsonify({"status": "error", "message": "Internal server error"}), 500


@app.route('/remove_participant', methods=['DELETE'])
def remove_part():
    try:
        if remove_participant(request.json.get('id')):
            return jsonify({"success": True})
        return jsonify({"status": "error", "message": "Failed to remove participant"}), 500
    except Exception as e:
        Logger.log_error(f"Error removing participant: {e}")
        return jsonify({"status": "error", "message": "Internal server error"}), 500


###################### OPERATORS ######################
@app.route('/operators', methods=['GET'])
def operators_menu():
    if 'username' not in session:
        return redirect(url_for('login'))
    return render_template('operators.html')


@app.route('/add_operator', methods=['POST'])
def add_oper():
    # fetch(url, {
    #     method: method,
    #     headers: {'Content-Type': 'application/json'},
    #     body: JSON.stringify({username, password})
    # })
    username = request.json.get('username')
    password = request.json.get('password')

    return add_operator(username, password)
    # return jsonify({"success":True})


@app.route('/get_operators', methods=['GET'])
def get_opers():
    return get_operators()


@app.route('/remove_operator', methods=['DELETE'])
def remove_oper():
    return remove_operator(request.json.get('username'))


###################### SESSION DATA ######################
@app.route('/session_data', methods=['GET'])
def session_data_menu():
    if 'username' not in session:
        return redirect(url_for('login'))

    lobby_id = request.args.get('lobby_id')
    if lobby_id:
        participants = request.args.get('participants')
        sessions = get_sessions_for_lobby(lobby_id)
        return render_template('session_data.html',
                               sessions=sessions,
                                 participants=participants,
                               lobby_id=lobby_id)
    else:
        lobbies = get_lobbies_data()
        return render_template('lobbies_data.html', lobbies=lobbies)

# ───────────────────────────────────────── single-session page
@app.route('/session_data/single', methods=['GET'])
def single_session_data():
    if 'username' not in session:
        return redirect(url_for('login'))

    sid          = request.args.get('session_id')
    game_type    = request.args.get('game_type', '')
    participants = request.args.get('participants', '')
    duration     = request.args.get('duration', '')

    # ── metrics ──
    heart    = get_heartrate(sid)
    hrv      = get_and_calculate_HRV(sid)
    latency  = get_latency(sid)
    jitter   = get_jitter(sid)

    click_events = sync_events = None
    frequency_data = sync_intervals = None
    angle_data = None

    if game_type in ("WATER_RIPPLES", "FLOWER_GARDEN"):
        click_events, sync_events = get_click_game_sync(sid)
    else:
        frequency_data, angle_data, sync_intervals = get_swipe_game_frequency(sid)

    metadata = {
        "gameType"    : game_type,
        "participants": [p.strip() for p in participants.split(',') if p],
        "sessionId"   : sid,
        "duration"    : duration
    }
    data = {
        "heart"          : heart,
        "hrv"            : hrv,
        "latency"        : latency,
        "jitter"         : jitter,
        "click_events"   : click_events,
        "sync_events"    : sync_events,
        "frequency_data" : frequency_data,
        "sync_intervals" : sync_intervals,
        "angle_data"     : angle_data
    }

    return render_template("single_session_data.html",
                           metadata=metadata,
                           data=data)


# ───────────────────────────────────────── (optional) JSON API
@app.route('/get_all_sessions', methods=['GET'])
def get_all_sessions_route():
    try:
        sessions = get_sessions_for_lobby("")      # empty ⇒ all
        return jsonify({"status": "success", "sessions": sessions})
    except Exception as e:
        Logger.log_error(f"Error getting all sessions: {e}")
        return jsonify({"status": "error",
                        "message": "Internal server error"}), 500

@app.route('/experiment_questions', methods=['GET'])
def get_experiment_questions_route():
    return render_template('experiment_questions.html')


if __name__ == '__main__':
    # run on port 80
    app.run(host='0.0.0.0', port=80)
