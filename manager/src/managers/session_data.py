# managers/session_data.py
from . import *
from .logger import Logger
import requests
import json, math
from collections import defaultdict

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
    Returns (metadata, sync_chart, intensity_chart).

    sync_chart   – latency values per actor, binned into 1-sec intervals
    intensity    – sensor-event counts per actor, binned into 2-sec intervals
    """
    body = {"sessionId": session_id}
    try:
        r   = requests.post(URL + "data/sessionEvent/select", json=body, timeout=1.0)
        res = server_response(r)
        if not res or not res.get_success():
            return {}, {}

        # ------------------------------------------------------------------ #
        # 1)  Parse the payload:                                              #
        # ------------------------------------------------------------------ #
        raw_events = res.get_payload()
        events: list[dict] = []
        for e in raw_events:
            if isinstance(e, str):
                try:
                    events.append(json.loads(e))
                except Exception as ex:
                    Logger.log_error(f"JSON decode error in event string: {ex}")
            else:
                events.append(e)

        if not events:
            Logger.log_error(f"Session {session_id}: no events returned")
            return {}, {}, {}

        # --------------- utility ------------------------------------------ #
        start_ts = min(ev["timestamp"] for ev in events)            # first ms
        sec      = lambda t: int((t - start_ts) / 1000)             # ms ▸ sec
        bin2     = lambda t: int((t - start_ts) / 2000)             # 2-sec bin

        actors = sorted({ev.get("actor", "UNK") for ev in events})

        # ------------------------------------------------------------------ #
        # 2)  Build the sync (latency) chart                                 #
        # ------------------------------------------------------------------ #
        latency_bins: dict[int, dict[str, list[float]]] = defaultdict(
            lambda: defaultdict(list)
        )
        for ev in events:
            if ev["type"] == "NETWORK_DATA" and ev["subtype"] == "LATENCY":
                latency_bins[sec(ev["timestamp"])][ev["actor"]].append(float(ev["data"]))

        sync_labels: list[str]                    = []
        sync_values: dict[str, list[float]]       = {a: [] for a in actors}

        for s in sorted(latency_bins.keys()):
            sync_labels.append(f"t+{s}s")
            for a in actors:
                vals = latency_bins[s].get(a, [])
                sync_values[a].append(sum(vals) / len(vals) if vals else math.nan)

        # ------------------------------------------------------------------ #
        # 3)  Build the intensity chart (sensor-event counts)                #
        # ------------------------------------------------------------------ #
        sensor_bins: dict[int, dict[str, int]] = defaultdict(lambda: defaultdict(int))
        for ev in events:
            if ev["type"] == "SENSOR_DATA":
                sensor_bins[bin2(ev["timestamp"])][ev["actor"]] += 1

        intensity_labels: list[str]          = []
        intensity_values: dict[str, list[int]] = {a: [] for a in actors}

        for b in sorted(sensor_bins.keys()):
            intensity_labels.append(f"Bin {b}")
            for a in actors:
                intensity_values[a].append(sensor_bins[b].get(a, 0))

        # ------------------------------------------------------------------ #
        # 4)  Metadata                                                       #
        # ------------------------------------------------------------------ #

        sync_data      = {"labels": sync_labels, **sync_values}
        intensity_data = {"labels": intensity_labels, **intensity_values}

        return sync_data, intensity_data

    except Exception as e:
        Logger.log_error(f"get_single_session – {e}")
        return {}, {}