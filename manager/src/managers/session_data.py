# managers/session_data.py
from . import *
from .logger import Logger
import requests, json, math
from collections import defaultdict

# ───────────────────────────────────────── helpers
def _decode_list(raw):
    out = []
    for item in raw:
        if isinstance(item, str):
            try:
                out.append(json.loads(item))
            except Exception as e:
                Logger.log_error(f"JSON decode fail: {e}  sample={item[:120]}")
        else:
            out.append(item)
    return out

# ───────────────────────────────────────── lobbies
def get_lobbies_data() -> list[dict]:
    try:
        res = server_response(
            requests.post(URL + "data/experiment/select/names",
                          json={"sessionId": None}, timeout=1.0)
        )
        return _decode_list(res.get_payload()) if res.get_success() else []
    except Exception as e:
        Logger.log_error(f"get_lobbies – {e}")
        return []

# ───────────────────────────────────────── sessions list
def get_sessions_for_lobby(lobby_id: str) -> list[dict]:
    try:
        res = server_response(
            requests.post(URL + "data/session/select",
                          json={"expId": lobby_id}, timeout=1.0)
        )
        return _decode_list(res.get_payload()) if res.get_success() else []
    except Exception as e:
        Logger.log_error(f"get_sessions_for_lobby – {e}")
        return []

# ───────────────────────────────────────── one session (charts)
def get_single_session(session_id: str) -> tuple[dict, dict]:
    """
    Returns:
        sync_data      – latency per actor, 1-sec bins
        intensity_data – USER_INPUT counts per actor, 2-sec bins
    """
    try:
        res = server_response(
            requests.post(URL + "data/sessionEvent/select",
                          json={"sessionId": session_id}, timeout=1.0)
        )
        if not res or not res.get_success():
            return {}, {}

        events = _decode_list(res.get_payload())
        if not events:
            Logger.log_error(f"Session {session_id}: empty event list")
            return {}, {}

        # ──────────────── bin helpers
        t0   = min(ev["timestamp"] for ev in events)          # earliest ms
        sbin = lambda t: (t - t0) // 1000                    # 1-sec
        b2   = lambda t: (t - t0) // 2000                    # 2-sec

        actors = sorted({ev.get("actor", "UNK") for ev in events})

        # ───────────────────────── sync chart  (latency)
        lat_bins = defaultdict(lambda: defaultdict(list))
        for ev in events:
            if ev["type"] == "NETWORK_DATA":
                if ev["subtype"] in ("AVERAGE_LATENCY", "LATENCY"):
                    lat_bins[sbin(ev["timestamp"])][ev["actor"]].append(
                        float(ev["data"])
                    )
        sync_labels = []
        sync_vals   = {a: [] for a in actors}
        for sec in sorted(lat_bins):
            sync_labels.append(str(sec))

            for a in actors:
                vals = lat_bins[sec].get(a, [])
                sync_vals[a].append(sum(vals)/len(vals) if vals else math.nan)
        sync_data = {"labels": sync_labels, **sync_vals}

        # ───────────────────────── intensity chart  (USER_INPUT count)
        int_bins = defaultdict(lambda: defaultdict(int))
        for ev in events:
            if ev["type"] == "USER_INPUT":
                int_bins[b2(ev["timestamp"])][ev["actor"]] += 1
        int_labels = []
        int_vals   = {a: [] for a in actors}
        for b in sorted(int_bins):
            int_labels.append(str(b * 2))  # because 2-second bins

            for a in actors:
                int_vals[a].append(int_bins[b].get(a, 0))
        intensity_data = {"labels": int_labels, **int_vals}

        return sync_data, intensity_data

    except Exception as e:
        Logger.log_error(f"get_single_session – {e}")
        return {}, {}
