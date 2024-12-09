import os
from flask import Flask, render_template, request, redirect, url_for, session, flash
import requests
from src.managers.participants import get_participants
from src.managers.lobby import *

app = Flask(__name__)
app.secret_key = os.urandom(24)


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

        selected_participants = request.form.getlist('selected_participants')  #list of str

        if not selected_participants:
            flash("Please select at least one participant.")
            return redirect(url_for('main_menu'))

        lobby_id = create_lobby(selected_participants)
        if not lobby_id:
            lobby_id = 1
        return redirect(url_for('lobby', selected_participants=",".join(selected_participants), lobby_id=lobby_id))

    else:
        participants = get_participants()
        if not participants:
            # TODO: Remove after implementing the participants manager
            participants = [
                {"id": 1, "name": "Player 1"},
                {"id": 2, "name": "Player 2"},
                {"id": 3, "name": "Player 3"},
                {"id": 4, "name": "Player 4"},
                {"id": 5, "name": "Player 5"},
            ]
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
            print(f"Starting game in lobby {lobby_id}")
            return render_template('lobby.html',
                                   selected_participants=selected_participants_list,
                                   lobby_id=lobby_id,
                                   action='stop')
        elif action == 'stop':
            print(f"Stopping game in lobby {lobby_id}")
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
