import os
from flask import Flask, render_template, request, redirect, url_for, session, flash
import requests
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

    if request.method == 'POST':
        selected_participants = request.form.getlist('selected_participants')
        game_type = request.form.get('game_type')

        if not selected_participants:
            flash("Please select at least one participant.")
            return redirect(url_for('main_menu'))

        if not game_type:
            flash("Please select a game type.")
            return redirect(url_for('main_menu'))

        game_type_enum = GAME_TYPE[game_type]
        lobby_id = create_lobby(selected_participants, game_type_enum)

        if not lobby_id:
            flash("Failed to create lobby.")
            return redirect(url_for('main_menu'))

        return redirect(url_for('lobby', selected_participants=",".join(selected_participants), lobby_id=lobby_id))
    else:
        # Get participants and handle None case
        participants = get_participants()
        if not participants:  # Handle None or empty list
            participants = []
            flash("No participants found.")

        participants = [{"id": participant, "name": f"Player {participant}"} for participant in participants]

        return render_template('main_menu.html', participants=participants)


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
            action = 'stop' if suc else 'start'
            return render_template('lobby.html',
                                   selected_participants=selected_participants_list,
                                   lobby_id=lobby_id,
                                   action=action)
        elif action == 'stop':
            Logger.log_info(f"Stopping game in lobby {lobby_id}")
            suc = stop_game(lobby_id)
            if suc:
                for participant in selected_participants_list:
                    sec = leave_lobby(lobby_id, participant)
                    if not sec:
                        Logger.log_error(f"Failed to remove {participant} from lobby {lobby_id}")
            return redirect(url_for('main_menu'))

        return redirect(url_for('lobby',
                                selected_participants=selected_participants,
                                lobby_id=lobby_id,
                                action=action))

    selected_participants = request.args.get('selected_participants', '')
    selected_participants_list = selected_participants.split(",") if selected_participants else []
    lobby_id = request.args.get('lobby_id', '')

    return render_template('lobby.html', selected_participants=selected_participants_list, lobby_id=lobby_id, action='start')


if __name__ == '__main__':
    # run on port 80
    app.run(host='0.0.0.0', port=80)
