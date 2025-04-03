from enum import Enum

# def get_game_type_name_from_value(value):
#     """
#     Maps the enum name or human-readable enum value to its human-readable name.
#     """
#     for game in GAME_TYPE:
#         if game.name == value:  # Match enum name (e.g., 'water_ripples')
#             return game.value  # Return human-readable value (e.g., 'Water Ripples')
#         if game.value == value:  # Match human-readable value (e.g., 'Water Ripples')
#             return game.value  # Return human-readable value
#     raise KeyError(f"Invalid game type: {value}")

def get_game_type_name_from_value(value):
    """
    Maps the enum name or human-readable enum value to its human-readable name.
    """
    for game in GAME_TYPE:
        if game.name == value:  # Match enum name (e.g., 'water_ripples')
            return game.name  # Return human-readable value (e.g., 'Water Ripples')
        if game.value == value:  # Match human-readable value (e.g., 'Water Ripples')
            return game.name  # Return human-readable value
    raise KeyError(f"Invalid game type: {value}")

class GAME_TYPE(Enum):
    water_ripples = 'Water Ripples'
    wine_glasses = 'Wine Glasses'
    flour_mill = 'Flour Mill'


class GAME_REQUEST_TYPE(Enum):
    get_online_player_ids = "get_online_player_ids"
    get_lobbies = "get_lobbies"
    get_lobby = "get_lobby"
    create_lobby = "create_lobby"
    remove_lobby = "remove_lobby"
    set_lobby_type = "set_lobby_type"
    join_lobby = "join_lobby"
    leave_lobby = "leave_lobby"
    start_experiment = "start_experiment"
    end_experiment = "end_experiment"
    change_sessions_order = "change_sessions_order"
    get_sessions = "get_sessions"
    create_session = "create_session"
    remove_session = "remove_session"
    add_participant = "add_participant"
    remove_participant = "remove_participant"
    edit_participant = "edit_participant"

    ## Operators
    add_operator = "add_operator"
    remove_operator = "remove_operator"
    edit_operator = "edit_operator"

    ## Session Data
    get_all_sessions = "get_all_sessions"
