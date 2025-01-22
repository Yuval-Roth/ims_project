import os
from flask import Flask, render_template, request, redirect, url_for, session, flash, jsonify
import requests
from . import *
from .managers.participants import *
from .managers.lobby import *
from .managers.game import *
from .managers.logger import Logger

app = Flask(__name__)
app.secret_key = os.urandom(24)

Logger()


@app.route('/')
def home():
    if 'username' in session:
        return redirect(url_for('main_menu'))
    return redirect(url_for('login'))


@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        if username == 'admin' and password == 'pass':  # Replace with secure credentials
            session['username'] = username
            return redirect(url_for('main_menu'))
    return render_template('login.html')


@app.route('/main_menu', methods=['GET', 'POST'])
def main_menu():
    if 'username' not in session:
        return redirect(url_for('login'))

    return render_template('main_menu.html')


@app.route('/get_participants', methods=['GET'])
def get_parts():
    # participants = get_participants()  # Replace with your function to fetch participants
    participants = None
    if not participants:
        participants = PARTICIPANTS
    return jsonify(participants)


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
    participant1 = request.form['participant1']
    participant2 = request.form['participant2']
    lobby_id = create_lobby([participant1, participant2])
    if lobby_id:
        flash("Lobby created successfully!")
    else:
        flash("Failed to create lobby.")
    return redirect(url_for('lobbies_menu'))


@app.route('/delete_lobby', methods=['GET'])
def delete_lobby_action():
    lobby_id = request.args.get('lobby_id')
    if lobby_id and leave_lobby(lobby_id, None):  # Implement logic to remove lobby
        flash("Lobby removed successfully!")
    else:
        flash("Failed to remove lobby.")
    return redirect(url_for('lobbies_menu'))


@app.route('/lobby', methods=['GET', 'POST'])
def lobby():
    if 'username' not in session:
        return redirect(url_for('login'))

    if request.method == 'POST':
        lobby_id = request.form.get('lobby_id')
        action = request.form.get('action')
        selected_participants = request.form.get('selected_participants')
        selected_participants_list = selected_participants.split(",") if selected_participants else []

        if action == 'start':
            Logger.log_info(f"Starting game in lobby {lobby_id}")
            suc = start_game(lobby_id)
            # action = 'stop' if suc else 'start'
            # return render_template('lobby.html',
            #                        selected_participants=selected_participants_list,
            #                        lobby_id=lobby_id,
            #                        action=action)
        elif action == 'stop':
            Logger.log_info(f"Stopping game in lobby {lobby_id}")
            suc = stop_game(lobby_id)
            if suc:
                for participant in selected_participants_list:
                    sec = leave_lobby(lobby_id, participant)
                    if not sec:
                        Logger.log_error(f"Failed to remove {participant} from lobby {lobby_id}")
            return redirect(url_for('main_menu'))

        # return redirect(url_for('lobby',
        #                         selected_participants=selected_participants,
        #                         lobby_id=lobby_id,
        #                         action=action))

    # Handle GET request
    lobby_id = request.args.get('lobby_id', '')
    selected_participants = request.args.get('selected_participants', '')
    selected_participants_list = selected_participants.split(",") if selected_participants else []
    state = request.args.get('state', 'waiting')
    if state == 'waiting':
        action = 'start'
    elif state == 'playing':
        action = 'stop'
    else:
        action = 'start'

    print(f"action: {action}")

    return render_template('lobby.html', selected_participants=selected_participants_list, lobby_id=lobby_id,
                           action='start', GAME_TYPE=GAME_TYPE)


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
    return jsonify({"status": "success"})
    data = request.json
    lobby_id = data.get('lobby_id')
    session_order = data.get('session_order')

    if not lobby_id or not session_order:
        return jsonify({"status": "error", "message": "Invalid data"}), 400

    success = change_session_order(lobby_id, session_order)
    return jsonify({"status": "success" if success else "error"})


@app.route('/add_session', methods=['POST'])
def add_session():
    return jsonify({"status": "success", "session": {"gameType": "chess", "duration": 60, "sessionId": "123"}})

    lobby_id = request.form.get('lobby_id')
    game_type = request.form.get('gameType')
    duration = int(request.form.get('duration', 0))
    sync_tolerance = int(request.form.get('syncTolerance', 100))  # Default: 100
    window = int(request.form.get('window', 2000))  # Default: 2000

    if not lobby_id or not game_type or duration <= 0 or sync_tolerance <= 0 or window <= 0:
        return jsonify({"status": "error", "message": "Invalid data"}), 400

    session_id = create_session(lobby_id, game_type, duration, sync_tolerance, window)
    if session_id:
        return jsonify(
            {"status": "success", "session": {"gameType": game_type, "duration": duration, "syncTolerance": sync_tolerance, "window": window, "sessionId": session_id}})
    return jsonify({"status": "error"})


@app.route('/delete_session', methods=['POST'])
def delete_session_route():
    return jsonify({"status": "success"})

    lobby_id = request.json.get('lobby_id')
    session_id = request.json.get('session_id')

    if not lobby_id or not session_id:
        return jsonify({"status": "error", "message": "Invalid data"}), 400

    success = delete_session(lobby_id, session_id)
    return jsonify({"status": "success" if success else "error"})


if __name__ == '__main__':
    # run on port 80
    app.run(host='0.0.0.0', port=80)
