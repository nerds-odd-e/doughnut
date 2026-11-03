"""Probe-to-recall relation: Spearman rho per probe measure and recall measure, with a verdict."""

import math

import numpy as np
import pandas as pd
from scipy.stats import norm, rankdata

from recall_export import (
    MIN_REVIEWS_AFTER_PROBE,
    activity,
    day_probes,
    distraction_ms,
    response_time_ms,
    session_ms,
    surprise,
)

PROBE_MEASURES = ["speed", "accuracy", "lapseCount", "variability"]
RECALL_MEASURES = {
    "correctShare": "Correct share",
    "surprise": "Surprise",
    "medianResponseS": "Median response time (s)",
    "distractionShare": "Distraction share",
    "sessionMin": "Session length (min)",
    "pace": "Pace (reviews per active min)",
}
MIN_DAYS = 30
DETECT_RHO = 0.3
BOOTSTRAP_RESAMPLES = 2000
BOOTSTRAP_SEED = 20261103


def days_needed(rho=DETECT_RHO, alpha=0.05, power=0.8):
    """Days to detect |rho| at the given power, two-sided alpha, by the Fisher-z approximation."""
    z = norm.ppf(1 - alpha / 2) + norm.ppf(power)
    return math.ceil((z / math.atanh(rho)) ** 2 + 3)


def day_recall(reviews, day_activity, after):
    length_ms = session_ms(day_activity, after)
    distracted_ms = distraction_ms(reviews)
    active_min = (length_ms - distracted_ms) / 60000
    times = response_time_ms(reviews)
    return {
        "correctShare": reviews["correct"].mean(),
        "surprise": surprise(reviews),
        "medianResponseS": times.median() / 1000,
        "distractionShare": min(distracted_ms / length_ms, 1.0) if length_ms > 0 else np.nan,
        "sessionMin": length_ms / 60000,
        "pace": len(reviews) / active_min if active_min > 0 else np.nan,
        "clipped": distracted_ms > length_ms,
    }


def day_table(data, after_probe):
    """One row per probe day with enough reviews: the probe measures and that day's recall measures.

    Returns the table and the number of days with at least one review that the threshold removed.
    """
    probes = day_probes(data)
    events = data["events"][data["events"]["day"].isin(probes.index)]
    reviews = events[events["isReview"]]
    act = activity(events)
    rows, below = {}, 0
    for day, probe in probes.iterrows():
        after = probe["completedAt"] if after_probe else None
        day_reviews = reviews[reviews["day"] == day]
        if after_probe:
            day_reviews = day_reviews[day_reviews["createdAt"] > after]
        if len(day_reviews) < MIN_REVIEWS_AFTER_PROBE:
            below += len(day_reviews) > 0
            continue
        recall = day_recall(day_reviews, act[act["day"] == day], after)
        rows[day] = {**probe[PROBE_MEASURES].to_dict(), **recall}
    return pd.DataFrame.from_dict(rows, orient="index"), below


def rank_corr(x, y):
    """Row-wise Spearman rho of two 2-D arrays (rows are samples)."""
    rx = rankdata(x, axis=1)
    ry = rankdata(y, axis=1)
    rx -= rx.mean(axis=1, keepdims=True)
    ry -= ry.mean(axis=1, keepdims=True)
    with np.errstate(invalid="ignore", divide="ignore"):
        return (rx * ry).sum(axis=1) / np.sqrt((rx**2).sum(axis=1) * (ry**2).sum(axis=1))


def strength(x, y, rng):
    paired = pd.DataFrame({"x": x, "y": y}).astype(float).dropna()
    xs, ys = paired["x"].to_numpy(), paired["y"].to_numpy()
    n = len(xs)
    rho = rank_corr(xs[None], ys[None])[0]
    if np.isnan(rho):
        return rho, np.nan, np.nan, n
    idx = rng.integers(0, n, (BOOTSTRAP_RESAMPLES, n))
    low, high = np.nanpercentile(rank_corr(xs[idx], ys[idx]), [2.5, 97.5])
    return rho, low, high, n


def verdict(n, low, high):
    if np.isnan(low):
        return "a measure does not vary"
    if n < MIN_DAYS:
        return f"too few days to tell ({days_needed()} days needed)"
    return "supported" if low > 0 or high < 0 else "not supported"


def relation_table(days):
    rng = np.random.default_rng(BOOTSTRAP_SEED)
    rows = []
    for probe in PROBE_MEASURES:
        for measure, label in RECALL_MEASURES.items():
            rho, low, high, n = strength(days[probe], days[measure], rng)
            rows.append((probe, label, f"{rho:.2f}", f"[{low:.2f}, {high:.2f}]", n, verdict(n, low, high)))
    return rows
