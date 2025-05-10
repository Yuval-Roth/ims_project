# services/sessions.py  (or wherever you keep your helpers)  ────────────────
from typing import Any

from . import *
from .logger import Logger
from collections import defaultdict
import math
import numpy as np
import requests

TAP_STD_TOL  = 0.15                           # ε_taps  (within-sequence)
TAP_FREQ_TOL = 0.15                           # ε_freq  (between actors)
PEAR_WIN_MS  = 10_000                         # 10-s moving window
BIN_MS       = 100                            # resolution inside window


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

def _stable_sequences(taps):
    """Return list of dict(start, end, freq) for one actor’s tap timestamps (ms)."""
    seqs, i = [], 0
    taps = sorted(taps)
    while i + 3 < len(taps):
        four = taps[i:i + 4]
        if max(np.diff(four)) > 5_000:          # too slow
            i += 1
            continue
        mu = np.mean(np.diff(four))
        if np.std(np.diff(four)) > TAP_STD_TOL * mu:
            i += 1
            continue
        j = i + 4
        while j < len(taps) and abs(taps[j] - taps[j - 1] - mu) <= TAP_STD_TOL * mu:
            j += 1
        seqs.append({"start": taps[i], "end": taps[j - 1], "freq": 1_000 / mu})
        i = j
    return seqs


def _shared_tempo_time(ts_a, ts_b, duration_ms):
    seq_a, seq_b = _stable_sequences(ts_a), _stable_sequences(ts_b)
    shared = 0
    for sa in seq_a:
        for sb in seq_b:
            if abs(sa["freq"] - sb["freq"]) > TAP_FREQ_TOL * sa["freq"]:
                continue
            shared += max(0, min(sa["end"], sb["end"]) - max(sa["start"], sb["start"]))
    return shared / duration_ms if duration_ms else 0.0


def _pearson_windowed(ts_a, ts_b, t0, t_end):
    centers, r_vals = [], []
    idx_a = idx_b = 0
    while t0 + PEAR_WIN_MS <= t_end:
        win_end = t0 + PEAR_WIN_MS
        seg_a, seg_b = [], []
        while idx_a < len(ts_a) and ts_a[idx_a] < win_end:
            seg_a.append(ts_a[idx_a] - t0)
            idx_a += 1
        while idx_b < len(ts_b) and ts_b[idx_b] < win_end:
            seg_b.append(ts_b[idx_b] - t0)
            idx_b += 1
        bins = int(PEAR_WIN_MS / BIN_MS)
        v_a, v_b = np.zeros(bins), np.zeros(bins)
        for t in seg_a:
            v_a[int(t // BIN_MS)] += 1
        for t in seg_b:
            v_b[int(t // BIN_MS)] += 1
        if v_a.std() == 0 or v_b.std() == 0:
            r = math.nan
        else:
            r = float(np.corrcoef(v_a, v_b)[0, 1])
        centers.append((t0 + win_end) // 2)
        r_vals.append(r)
        t0 += PEAR_WIN_MS
    return centers, r_vals



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
def get_single_session(session_id: str) -> tuple[dict[Any, Any], dict[Any, Any], dict[Any, Any], None] | tuple[
    dict[str, list[Any]], dict[str, list[Any]], dict[str | Any, list[str] | list[Any]] | dict[
        Any, Any], float | None | Any]:
    """
    Returns:
        sync_data       – avg NET latency per actor, 1-sec bins (for ‘latency’ chart)
        intensity_data  – USER_INPUT counts per actor, 2-sec bins
        pearson_data    – Pearson-r time-series for taps, 10-s windows
        stt             – Shared-Tempo-Time (0-1) or None if <2 actors
    """
    try:
        res = server_response(
            requests.post(f"{URL}data/sessionEvent/select",
                          json={"sessionId": session_id},
                          timeout=1.0)
        )
        if not res or not res.get_success():
            return {}, {}, {}, None

        events = _decode_list(res.get_payload())
        if not events:
            Logger.log_error(f"Session {session_id}: empty event list")
            return {}, {}, {}, None

        t0        = min(ev["timestamp"] for ev in events)
        t_end     = max(ev["timestamp"] for ev in events)
        duration  = t_end - t0

        sec_bin = lambda t: (t - t0) // 1000      # 1-s
        two_bin = lambda t: (t - t0) // 2000      # 2-s

        actors = sorted({ev.get("actor", "UNK") for ev in events})

        # ─────────── latency (NETWORK_DATA)
        lat_bins = defaultdict(lambda: defaultdict(list))
        for ev in events:
            if ev["type"] == "NETWORK_DATA" and ev["subtype"] in ("AVERAGE_LATENCY", "LATENCY"):
                lat_bins[sec_bin(ev["timestamp"])][ev["actor"]].append(float(ev["data"]))
        sync_labels, sync_vals = [], {a: [] for a in actors}
        for sec in sorted(lat_bins):
            sync_labels.append(str(sec))
            for a in actors:
                vals = lat_bins[sec].get(a, [])
                sync_vals[a].append(sum(vals) / len(vals) if vals else math.nan)
        sync_data = {"labels": sync_labels, **sync_vals}

        # ─────────── intensity (USER_INPUT count)
        int_bins = defaultdict(lambda: defaultdict(int))
        for ev in events:
            if ev["type"] == "USER_INPUT":
                int_bins[two_bin(ev["timestamp"])][ev["actor"]] += 1
        int_labels, int_vals = [], {a: [] for a in actors}
        for b in sorted(int_bins):
            int_labels.append(str(b * 2))           # seconds
            for a in actors:
                int_vals[a].append(int_bins[b].get(a, 0))
        intensity_data = {"labels": int_labels, **int_vals}

        # ─────────── tap-based metrics
        tap_ts = defaultdict(list)
        for ev in events:
            if ev["type"] == "USER_INPUT" and ev["subtype"] in ("TAP", "CLICK"):
                tap_ts[ev["actor"]].append(ev["timestamp"])

        for lst in tap_ts.values():  # 🔑 ensure ascending order
            lst.sort()

        if len(tap_ts) == 2:
            act_a, act_b = list(tap_ts.keys())
            stt = _shared_tempo_time(tap_ts[act_a], tap_ts[act_b], duration)
            centers, r_vals = _pearson_windowed(tap_ts[act_a], tap_ts[act_b], t0, t_end)
            pearson_data = {
                "labels": [str((c - t0) // 1000) for c in centers],
                act_a: r_vals,
                act_b: r_vals       # same series – easier legend
            }
        else:
            stt, pearson_data = None, {}

        return sync_data, intensity_data, pearson_data, stt

    except Exception as e:
        Logger.log_error(f"get_single_session – {e}")
        return {}, {}, {}, None
