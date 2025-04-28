# managers/session_data.py
from . import *
from .logger import Logger
import requests
from datetime import datetime

# ───────────────────────────────────────── get all lobbies
def get_lobbies_data() -> list[dict]:
    """
    Ask the API for the list of lobbies (= distinct session groups).
    Each item the API returns must contain lobbyId, players, created_at.
    """
    body = {"sessionId": None}                    # <- empty = “give me everything”
    try:
        r = requests.post(URL + "data/experiment/select/names", json=body, timeout=1.0)
        res = server_response(r)
        entries = []
        for item in res.get_payload():
            # API gives 'item' as a JSON string → turn it into dict
            try:
                entries.append(json.loads(item) if isinstance(item, str) else item)
            except Exception as e:
                Logger.log_error(f"lobby JSON decode failed: {e}  raw={item[:120]}")
        return entries
    except Exception as e:
        Logger.log_error(f"get_lobbies – {e}")
        return []

# ───────────────────────────────────────── get sessions for one lobby
def get_sessions_for_lobby(lobby_id: str) -> list[dict]:
    """
    Returns every session that belongs to the lobby.
    """
    body = {"expId": lobby_id}
    try:
        r = requests.post(URL + "data/session/select", json=body, timeout=1.0)
        res = server_response(r)
        return res.get_payload() if res.get_success() else []
    except Exception as e:
        Logger.log_error(f"get_sessions_for_lobby – {e}")
        return []

# ───────────────────────────────────────── get one session with charts
def get_single_session(session_id: str) -> tuple[dict, dict, dict]:
    """
    • metadata  – flat dict  
    • sync_data – {labels:[], <p1>:[], <p2>:[]}  
    • intensity – {labels:[], <p1>:[], <p2>:[]}  
    Everything is derived from the raw list of SessionEvent rows the API returns.
    """
    body = {"sessionId": session_id}
    try:
        r = requests.post(URL + "data/sessionEvent/select", json=body, timeout=1.0)
        res = server_response(r)
        if not res.get_success():
            return {}, {}, {}

        events = res.get_payload()          # → list[dict] straight from DB

        # ---------- build charts (adapt field names if yours differ) ----------
        participants = sorted({ev["pid"] for ev in events})
        start_ts     = min(ev["timestamp"] for ev in events)

        # sync timeline – 1-second buckets of sync-value:
        sync_values: dict[str, list[float]] = {p: [] for p in participants}
        labels_sync: list[str]              = []

        # intensity chart – count taps per participant:
        intensity: dict[str, list[int]]     = {p: [] for p in participants}
        labels_int: list[str]               = []

        ## --- example implementation, adjust to your schema ---
        bucket  = {}
        coarse  = lambda t: int((t - start_ts) / 1000)   # ms → sec offset
        for ev in events:
            sec = coarse(ev["timestamp"])
            bucket.setdefault(sec, {}).setdefault(ev["pid"], []).append(ev)

        for sec in sorted(bucket):
            labels_sync.append(f"t+{sec}s")
            for p in participants:
                vals = [b["syncScore"] for b in bucket[sec].get(p, [])]
                sync_values[p].append(sum(vals) / len(vals) if vals else 0.0)

        # intensity:  simply take N taps – replace with your own rule
        tap_idx = {p: 0 for p in participants}
        for ev in events:
            if ev["type"] == "TAP":
                p          = ev["pid"]
                tap_idx[p] += 1
                label      = f"Tap {tap_idx[p]}"
                if len(intensity[p]) < tap_idx[p]:
                    labels_int.append(label)
                    for q in participants:                 # keep arrays aligned
                        intensity[q].append(0)
                intensity[p][tap_idx[p]-1] += 1

        metadata = {
            "sessionId":    session_id,
            "gameType":     events[0]["gameType"],
            "duration":     int((max(ev["timestamp"] for ev in events) - start_ts)/1000),
            "participants": participants
        }
        sync_data      = {"labels": labels_sync, **sync_values}
        intensity_data = {"labels": labels_int,  **intensity}

        return metadata, sync_data, intensity_data

    except Exception as e:
        Logger.log_error(f"get_single_session – {e}")
        return {}, {}, {}
