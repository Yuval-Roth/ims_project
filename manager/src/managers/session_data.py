#session_data.py
from typing import Any
from . import *
from .logger import Logger
from collections import defaultdict
import math
import numpy as np
import requests
import json


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


def get_event_data(session_id: str, type_: str = None, subtype: str = None) -> list[dict]:
    try:
        js = {"sessionId": session_id}
        if subtype:
            js["subtype"] = subtype

        res = server_response(
            post_auth(f"{URL}/data/session/select/events", json=js, timeout=1.0)
        )

        if not res or not res.get_success():
            Logger.log_error(f"get_event_data – Failed to get data for session {session_id}")
            return []

        events = _decode_list(res.get_payload())
        if not events:
            Logger.log_error(f"Session {session_id}: empty event list")
            return []

        if type_:
            events = [ev for ev in events if ev.get("type") == type_]

        return events

    except Exception as e:
        Logger.log_error(f"get_event_data – {e}")
        return []


def get_heartrate(session_id: str):
    try:
        events = get_event_data(session_id, subtype="HEART_RATE")
        if not events:
            return {}

        events.sort(key=lambda x: x["timestamp"])
        t0 = events[0]["timestamp"]

        heart_rate_data = defaultdict(lambda: {"timestamps": [], "values": []})

        for ev in events:
            if ev["data"] == '0':
                continue
            timestamp = (ev["timestamp"] - t0) / 1000.0
            heart_rate_data[ev["actor"]]["timestamps"].append(f"{timestamp:.3f}")
            heart_rate_data[ev["actor"]]["values"].append(ev["data"])

        return dict(heart_rate_data)

    except Exception as e:
        Logger.log_error(f"get_heartrate – {e}")
        return {}


def get_latency(session_id: str):
    try:
        events = get_event_data(session_id, subtype="LATENCY")
        if not events:
            return {}

        events.sort(key=lambda x: x["timestamp"])
        t0 = events[0]["timestamp"]

        latency_data = defaultdict(lambda: {"timestamps": [], "values": []})

        for ev in events:
            timestamp = (ev["timestamp"] - t0) / 1000.0
            latency_data[ev["actor"]]["timestamps"].append(f"{timestamp:.3f}")
            latency_data[ev["actor"]]["values"].append(ev["data"])

        return dict(latency_data)

    except Exception as e:
        Logger.log_error(f"get_latency – {e}")
        return {}


def get_jitter(session_id: str):
    try:
        events = get_event_data(session_id, subtype="JITTER")
        if not events:
            return {}

        events.sort(key=lambda x: x["timestamp"])
        t0 = events[0]["timestamp"]

        jitter_data = defaultdict(lambda: {"timestamps": [], "values": []})

        for ev in events:
            if ev["data"] == '0':
                continue
            timestamp = (ev["timestamp"] - t0) / 1000.0
            jitter_data[ev["actor"]]["timestamps"].append(f"{timestamp:.3f}")
            jitter_data[ev["actor"]]["values"].append(ev["data"])

        return dict(jitter_data)

    except Exception as e:
        Logger.log_error(f"get_jitter – {e}")
        return {}


def get_and_calculate_HRV(session_id: str):
    try:
        ibi_events = get_event_data(session_id, subtype="INTER_BEAT_INTERVAL")
        if not ibi_events:
            Logger.log_error(f"Session {session_id}: No INTER_BEAT_INTERVAL events found")
            return {}

        ibi_events.sort(key=lambda x: x["timestamp"])

        actor_ibi = defaultdict(list)
        for ev in ibi_events:
            try:
                interval = float(ev["data"])
                if interval > 0:
                    actor_ibi[ev["actor"]].append((ev["timestamp"], interval))
            except Exception as e:
                Logger.log_error(f"HRV parse error: {e}  data={ev['data']}")

        hrv_data = {}
        window_size = 10
        for actor, intervals in actor_ibi.items():
            if len(intervals) < window_size:
                Logger.log_error(f"Actor {actor}: Not enough intervals for HRV calculation")
                continue

            timestamps = []
            values = []
            t0 = intervals[0][0]

            for i in range(window_size, len(intervals) + 1):
                window_intervals = [val for _, val in intervals[i - window_size:i]]
                hrv = np.std(window_intervals)
                timestamp = (intervals[i - 1][0] - t0) / 1000.0
                timestamps.append(f"{timestamp:.3f}")
                values.append(hrv)

            hrv_data[actor] = {"timestamps": timestamps, "values": values}

        return hrv_data

    except Exception as e:
        Logger.log_error(f"get_and_calculate_HRV – {e}")
        return {}


def get_click_game_sync(session_id: str):
    click_events = get_event_data(session_id, type_="USER_INPUT", subtype="CLICK")
    sync_events = get_event_data(session_id, subtype="SYNCED_AT_TIME")

    click_data = defaultdict(list)
    sync_data = []

    for ev in click_events:
        timestamp_sec = ev["timestamp"] / 1000.0
        click_data[ev["actor"]].append(f"{timestamp_sec:.3f}")

    for ev in sync_events:
        timestamp_sec = ev["timestamp"] / 1000.0
        sync_data.append(f"{timestamp_sec:.3f}")

    return dict(click_data), sorted(sync_data)


def get_swipe_game_frequency(session_id: str):
    frequency_events = get_event_data(session_id, type_="USER_INPUT", subtype="FREQUENCY")
    angle_events = get_event_data(session_id, type_="USER_INPUT", subtype="ANGLE")
    sync_start_events = get_event_data(session_id, subtype="SYNC_START_TIME")
    sync_end_events = get_event_data(session_id, subtype="SYNC_END_TIME")

    frequency_data = defaultdict(lambda: {"timestamps": [], "values": []})
    angle_data = defaultdict(lambda: {"timestamps": [], "values": []})

    for ev in frequency_events:
        try:
            timestamp_sec = ev["timestamp"] / 1000.0
            value = float(ev["data"])
            frequency_data[ev["actor"]]["timestamps"].append(f"{timestamp_sec:.3f}")
            frequency_data[ev["actor"]]["values"].append(value)
        except (ValueError, TypeError) as e:
            Logger.log_error(f"Invalid FREQUENCY data: {ev['data']}  error={e}")

    # angle data

    for ev in angle_events:
        try:
            timestamp_sec = ev["timestamp"] / 1000.0
            value = float(ev["data"])
            angle_data[ev["actor"]]["timestamps"].append(f"{timestamp_sec:.3f}")
            angle_data[ev["actor"]]["values"].append(value)
        except (ValueError, TypeError) as e:
            Logger.log_error(f"Invalid ANGLE data: {ev['data']}  error={e}")

    # Sort data for each actor by timestamp
    for actor, data in frequency_data.items():
        combined = list(zip(map(float, data["timestamps"]), data["values"]))
        combined.sort(key=lambda x: x[0])
        data["timestamps"] = [f"{t:.3f}" for t, _ in combined]
        data["values"] = [v for _, v in combined]

    # Sort angle data for each actor by timestamp
    for actor, data in angle_data.items():
        combined = list(zip(map(float, data["timestamps"]), data["values"]))
        combined.sort(key=lambda x: x[0])
        data["timestamps"] = [f"{t:.3f}" for t, _ in combined]
        data["values"] = [v for _, v in combined]

    sync_start_times = sorted(ev["timestamp"] / 1000.0 for ev in sync_start_events)
    sync_end_times = sorted(ev["timestamp"] / 1000.0 for ev in sync_end_events)

    if len(sync_start_times) != len(sync_end_times):
        Logger.log_error(f"Session {session_id}: Mismatched SYNC_START_TIME and SYNC_END_TIME events")

    sync_intervals = list(zip(sync_start_times, sync_end_times))

    return dict(frequency_data), dict(angle_data), sync_intervals


def get_lobbies_data() -> list[dict]:
    try:
        res = server_response(
            post_auth(URL + "/data/experiment/select/names", json={"sessionId": None}, timeout=1.0)
        )
        return _decode_list(res.get_payload()) if res.get_success() else []
    except Exception as e:
        Logger.log_error(f"get_lobbies – {e}")
        return []


def get_sessions_for_lobby(lobby_id: str) -> list[dict]:
    try:
        res = server_response(
            post_auth(URL + "/data/session/select", json={"expId": lobby_id}, timeout=1.0)
        )
        return _decode_list(res.get_payload()) if res.get_success() else []
    except Exception as e:
        Logger.log_error(f"get_sessions_for_lobby – {e}")
        return []
