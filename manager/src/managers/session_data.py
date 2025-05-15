from typing import Any
from . import *
from .logger import Logger
from collections import defaultdict
import math
import numpy as np
import requests
import json

TAP_STD_TOL = 0.15
TAP_FREQ_TOL = 0.15
PEAR_WIN_MS = 10_000
BIN_MS = 100


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
    """
    General function to retrieve event data filtered by type and/or subtype.

    Args:
        session_id: The session ID.
        type_: The event type to filter (optional).
        subtype: The event subtype to filter (optional).

    Returns:
        List of event dicts.
    """
    try:
        js = {"sessionId": session_id}
        if subtype:
            js["subtype"] = subtype

        res = server_response(
            requests.post(f"{URL}data/sessionEvent/select", json=js, timeout=1.0)
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
    """
    Returns:
        heart_rate_data – heart rate data for each actor
        heart_rate_labels – labels for the heart rate data
    """
    try:
        events = get_event_data(session_id, subtype="HEART_RATE")
        if not events:
            return {}, []

        events.sort(key=lambda x: x["timestamp"])
        heart_rate_data = defaultdict(list)
        heart_rate_labels = []

        for ev in events:
            if ev["data"] == '0':
                continue
            heart_rate_data[ev["actor"]].append(ev["data"])
            heart_rate_labels.append(str((ev["timestamp"] - events[0]["timestamp"]) // 1000))

        return heart_rate_data, heart_rate_labels

    except Exception as e:
        Logger.log_error(f"get_heartrate – {e}")
        return {}, []


def get_click_game_sync(session_id: str):
    """
    Retrieves click game sync data:
    - click_data: dict of actor -> sorted list of timestamps in seconds (float)
    - sync_data: sorted list of timestamps in seconds (float)
    """
    click_events = get_event_data(session_id, type_="USER_INPUT", subtype="CLICK")
    sync_events = get_event_data(session_id, subtype="SYNCED_AT_TIME")

    click_data = defaultdict(list)
    sync_data = []

    # Collect timestamps
    for ev in click_events:
        timestamp_sec = ev["timestamp"] / 1000.0
        click_data[ev["actor"]].append(timestamp_sec)

    for ev in sync_events:
        timestamp_sec = ev["timestamp"] / 1000.0
        sync_data.append(timestamp_sec)

    # Sort lists
    for actor in click_data:
        click_data[actor].sort()

    sync_data.sort()

    return click_data, sync_data



def get_swipe_game_frequency(session_id: str):
    """
    Retrieves swipe game frequency data:
    - frequency_data: dict of actor -> sorted list of values
    - frequency_labels: sorted list of timestamps in seconds (float)
    - sync_intervals: list of (start_time_sec, end_time_sec) tuples
    """
    frequency_events = get_event_data(session_id, type_="USER_INPUT", subtype="FREQUENCY")
    sync_start_events = get_event_data(session_id, subtype="SYNC_START_TIME")
    sync_end_events = get_event_data(session_id, subtype="SYNC_END_TIME")

    # --- Frequency Data ---
    frequency_tmp = []

    for ev in frequency_events:
        try:
            timestamp_sec = ev["timestamp"] / 1000.0
            value = float(ev["data"])
            frequency_tmp.append((timestamp_sec, ev["actor"], value))
        except (ValueError, TypeError) as e:
            Logger.log_error(f"Invalid FREQUENCY data: {ev['data']}  error={e}")

    # Sort globally by timestamp
    frequency_tmp.sort(key=lambda x: x[0])

    frequency_data = defaultdict(list)
    frequency_labels = []

    for timestamp_sec, actor, value in frequency_tmp:
        frequency_data[actor].append(value)
        frequency_labels.append(f"{timestamp_sec:.3f}")

    # --- Sync Intervals ---
    sync_start_times = sorted(ev["timestamp"] / 1000.0 for ev in sync_start_events)
    sync_end_times = sorted(ev["timestamp"] / 1000.0 for ev in sync_end_events)

    if len(sync_start_times) != len(sync_end_times):
        Logger.log_error(f"Session {session_id}: Mismatched SYNC_START_TIME ({len(sync_start_times)}) and SYNC_END_TIME ({len(sync_end_times)}) events")

    # Combine into intervals (zip trims to shortest list)
    sync_intervals = list(zip(sync_start_times, sync_end_times))

    return frequency_data, frequency_labels, sync_intervals




def get_latency(session_id: str):
    """
    Returns:
        latency_data – latency data for each actor
        latency_labels – labels for the latency data (float seconds with ms precision)
    """
    try:
        events = get_event_data(session_id, subtype="LATENCY")
        if not events:
            return {}, []

        events.sort(key=lambda x: x["timestamp"])
        latency_data = defaultdict(list)
        latency_labels = []

        t0 = events[0]["timestamp"]

        for ev in events:
            latency_data[ev["actor"]].append(ev["data"])
            elapsed_sec = (ev["timestamp"] - t0) / 1000.0  # Keep precision
            latency_labels.append(f"{elapsed_sec:.3f}")     # format to 3 decimal places

        return latency_data, latency_labels

    except Exception as e:
        Logger.log_error(f"get_latency – {e}")
        return {}, []


    except Exception as e:
        Logger.log_error(f"get_latency – {e}")
        return {}, []


def get_jitter(session_id: str):
    """
    Returns:
        jitter_data – jitter data for each actor
        jitter_labels – labels for the jitter data (float seconds with ms precision)
    """
    try:
        events = get_event_data(session_id, subtype="JITTER")
        if not events:
            return {}, []

        events.sort(key=lambda x: x["timestamp"])
        jitter_data = defaultdict(list)
        jitter_labels = []

        t0 = events[0]["timestamp"]

        for ev in events:
            if ev["data"] == '0':
                continue
            jitter_data[ev["actor"]].append(ev["data"])
            elapsed_sec = (ev["timestamp"] - t0) / 1000.0  # Keep ms precision
            jitter_labels.append(f"{elapsed_sec:.3f}")     # Format as 3 decimal places

        return jitter_data, jitter_labels

    except Exception as e:
        Logger.log_error(f"get_jitter – {e}")
        return {}, []


def get_and_calculate_HRV(session_id: str):
    """
    Calculates Heart Rate Variability (HRV) for each actor based on INTER_BEAT_INTERVAL events.

    Returns:
        hrv_data: dict of actor -> HRV values list (ms)
        hrv_labels: list of timestamps (seconds)
    """
    try:
        # Get INTER_BEAT_INTERVAL events
        ibi_events = get_event_data(session_id, subtype="INTER_BEAT_INTERVAL")

        if not ibi_events:
            Logger.log_error(f"Session {session_id}: No INTER_BEAT_INTERVAL events found")
            return {}, []

        # Sort by timestamp
        ibi_events.sort(key=lambda x: x["timestamp"])

        # Group intervals by actor
        actor_ibi = defaultdict(list)
        for ev in ibi_events:
            try:
                interval = float(ev["data"])
                if interval > 0:  # ignore invalid intervals
                    actor_ibi[ev["actor"]].append((ev["timestamp"], interval))
            except Exception as e:
                Logger.log_error(f"HRV parse error: {e}  data={ev['data']}")

        # Calculate HRV (Standard deviation of inter-beat intervals) per actor
        hrv_data = defaultdict(list)
        hrv_labels = []

        window_size = 10  # number of intervals per HRV calculation (can be tuned)
        for actor, intervals in actor_ibi.items():
            if len(intervals) < window_size:
                Logger.log_error(f"Actor {actor}: Not enough intervals for HRV calculation")
                continue

            for i in range(window_size, len(intervals) + 1):
                window_intervals = [val for _, val in intervals[i - window_size:i]]
                hrv = np.std(window_intervals)
                hrv_data[actor].append(hrv)

                # Use the timestamp of the latest interval in the window
                label_time = (intervals[i - 1][0] - intervals[0][0]) // 1000  # seconds
                if actor == list(actor_ibi.keys())[0]:  # avoid duplicates
                    hrv_labels.append(str(label_time))

        return hrv_data, hrv_labels

    except Exception as e:
        Logger.log_error(f"get_and_calculate_HRV – {e}")
        return {}, []

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
