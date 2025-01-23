from enum import Enum


class GAME_TYPE(Enum):
    water_ripples = 'Water Ripples'
    wine_glasses = 'Wine Glasses'
    # flour_mill = 'Flour Mill'
    chess = 'Chess'
    tic_tac_toe = 'Tic Tac Toe'
    racing = 'Racing'


class GAME_REQUEST_TYPE(Enum):
    get_online_player_ids = "get_online_player_ids"
    get_lobbies = "get_lobbies"
    get_lobby = "get_lobby"
    create_lobby = "create_lobby"
    remove_lobby = "remove_lobby"
    set_lobby_type = "set_lobby_type"
    join_lobby = "join_lobby"
    leave_lobby = "leave_lobby"
    start_game = "start_game"
    end_game = "end_game"
    update_session_order = "update_session_order"
    create_session = "create_session"
    delete_session = "delete_session"
    add_participant = "add_participant"
    remove_participant = "remove_participant"
    edit_participant = "edit_participant"

    ## Operators
    add_operator = "add_operator"
    remove_operator = "remove_operator"
    edit_operator = "edit_operator"
