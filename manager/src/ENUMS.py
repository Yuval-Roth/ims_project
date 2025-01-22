from enum import Enum


class GAME_TYPE(Enum):
    water_ripples = 'Water Ripples'
    wine_glasses = 'Wine Glasses'
    flour_mill = 'Flour Mill'
    poc = 'Poc'
    chess = 'Chess'
    tic_tac_toe = 'Tic Tac Toe'
    racing = 'Racing'


class GAME_REQUEST_TYPE(Enum):
    get_online_player_ids = 1
    get_lobbies = 2
    get_lobby = 3
    create_lobby = 4
    set_lobby_type = 5
    join_lobby = 6
    leave_lobby = 7
    start_game = 8
    end_game = 9
    update_session_order = 10  # Added for changing the session order
    create_session = 11       # Added for creating a new session
    delete_session = 12       # Added for deleting a session
