import os
from flask import Flask, render_template, request, redirect, url_for, session, flash
import requests


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
        selected_watches = request.form.getlist('selected_watches')
        # Redirect to the lobby route with selected watches as query parameters
        return redirect(url_for('lobby', selected_watches=",".join(selected_watches)))
    else:
        watches = [
            {"id": 1, "name": "Watch 1"},
            {"id": 2, "name": "Watch 2"},
            {"id": 3, "name": "Watch 3"},
            {"id": 4, "name": "Watch 4"},
            {"id": 5, "name": "Watch 5"},
            # Add more watches dynamically or fetch from a database
        ]
        return render_template('main_menu.html', watches=watches)

EXTERNAL_SERVER_URL = "http://external-server:5000"
@app.route('/lobby', methods=['GET', 'POST'])
def lobby():
    if 'username' not in session:
        return redirect(url_for('login'))

    if request.method == 'POST':
        action = request.form.get('action')
        selected_watches = request.form.get('selected_watches', '')

        # Validate if watches are selected
        selected_watches_list = selected_watches.split(",") if selected_watches else []
        if not selected_watches_list:
            flash("Please select at least one watch.")
            return redirect(url_for('lobby'))

        # Prepare the payload to send to the external server
        payload = {
            "action": action,
            "watches": selected_watches_list,
        }

        try:
            # Send data to the external server
            response = requests.post(EXTERNAL_SERVER_URL, json=payload)
            response.raise_for_status()  # Raise exception for HTTP errors

            flash(f"Session '{action}' successfully sent to the server.")
        except requests.RequestException as e:
            flash(f"Error communicating with the external server: {e}")

        return redirect(url_for('lobby'))

    # For GET requests, retrieve selected watches from the query parameters
    selected_watches = request.args.get('selected_watches', '')
    selected_watches_list = selected_watches.split(",") if selected_watches else []

    return render_template('lobby.html', selected_watches=selected_watches_list)


if __name__ == '__main__':
    # run on port 80
    app.run(host='0.0.0.0', port=80)
