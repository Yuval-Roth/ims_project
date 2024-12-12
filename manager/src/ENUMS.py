from enum import Enum


class GAME_TYPE(Enum):
    water_ripples = 1
    poc = 2


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