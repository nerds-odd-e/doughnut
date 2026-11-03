import os
import sys
from datetime import date, timedelta

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from relation import days_needed  # noqa: E402
from test_recall_probe_analysis import event, probe, run  # noqa: E402

TOO_FEW = "too few days to tell (85 days needed)"


def day_of(i):
    return (date(2026, 9, 1) + timedelta(days=i)).isoformat()


def reviews(day, grades, hour=8, retrievability=0.7, thinking=5000, away=0, extra=()):
    """One review a minute from `hour`, graded in order; `extra` adds rows at given minutes."""
    rows = [
        event(f"{day}T{hour:02d}:{m:02d}:00+07:00", grade=g, retrievability=retrievability, thinking_time_ms=thinking, away_ms=away)
        for m, g in enumerate(grades)
    ]
    return rows + [event(f"{day}T{hour:02d}:{m:02d}:00+07:00", **fields) for m, fields in extra]


def export(events, probes):
    return {
        "timezone": "Asia/Jakarta",
        "exportedAt": "2026-11-03T21:27:06+07:00",
        "recallEvents": events,
        "dailyProbes": probes,
    }


def planted(days, extra=()):
    """Probe speed rises with the day index, and so does the day's share of GOOD answers."""
    rng = np.random.default_rng(7)
    events, probes = [], []
    for i in range(days):
        good = 2 + i // 4
        grades = ["GOOD"] * good + ["AGAIN"] * (12 - good)
        events += reviews(
            day_of(i), grades, thinking=int(rng.integers(2000, 9000)), away=int(rng.integers(0, 3000)), extra=extra
        )
        probes.append(
            probe(
                f"{day_of(i)}T07:00:00+07:00",
                speed=1.0 + i / 10,
                accuracy=int(rng.integers(60, 100)),
                lapse_count=int(rng.integers(0, 6)),
                variability=float(rng.uniform(0.1, 0.9)),
            )
        )
    return export(events, probes)


def independent(days):
    rng = np.random.default_rng(1)
    events, probes = [], []
    for i in range(days):
        grades = rng.choice(["GOOD", "AGAIN"], size=12, p=[0.8, 0.2]).tolist()
        events += reviews(day_of(i), grades, thinking=int(rng.integers(2000, 9000)), away=int(rng.integers(0, 3000)))
        probes.append(
            probe(
                f"{day_of(i)}T07:00:00+07:00",
                speed=float(rng.uniform(1, 3)),
                accuracy=int(rng.integers(60, 100)),
                lapse_count=int(rng.integers(0, 6)),
                variability=float(rng.uniform(0.1, 0.9)),
            )
        )
    return export(events, probes)


def after_probe_table(report):
    return report.split("### Sensitivity")[0]


def row(report, probe_measure, recall_measure):
    line = next(x for x in after_probe_table(report).splitlines() if x.startswith(f"| {probe_measure} | {recall_measure} |"))
    cells = [c.strip() for c in line.strip("|").split("|")]
    low, high = (float(v) for v in cells[3].strip("[]").split(","))
    return {"rho": float(cells[2]), "low": low, "high": high, "n": int(cells[4]), "verdict": cells[5]}


def test_days_needed():
    assert days_needed() == 85
    assert days_needed(rho=0.5) < days_needed(rho=0.3) < days_needed(rho=0.2)


def test_relation_planted_speed_and_surprise_is_supported(tmp_path):
    result = row(run(tmp_path, planted(40), "relation"), "speed", "Surprise")
    assert result["n"] == 40
    assert result["rho"] > 0.9
    assert result["low"] > 0
    assert result["verdict"] == "supported"


def test_relation_independent_days_are_not_supported(tmp_path):
    result = row(run(tmp_path, independent(40), "relation"), "speed", "Surprise")
    assert result["n"] == 40
    assert result["low"] < 0 < result["high"]
    assert result["verdict"] == "not supported"


def test_relation_with_12_days_says_too_few_days_and_states_excluded_days(tmp_path):
    data = planted(12)
    data["recallEvents"] += reviews(day_of(12), ["GOOD"] * 5)
    data["dailyProbes"].append(probe(f"{day_of(12)}T07:00:00+07:00"))
    report = run(tmp_path, data, "relation")
    assert report.count(f"| 12 | {TOO_FEW} |") == 2 * 24
    assert "13 probe days; 13 have at least one review after the probe" in report
    assert "1 of them have fewer than 10 and are excluded, leaving 12 days" in report


def test_relation_counts_only_days_with_reviews_after_the_probe(tmp_path):
    data = planted(48)
    for i in range(48, 60):
        data["recallEvents"] += reviews(day_of(i), ["GOOD", "AGAIN"] * 6)
        data["dailyProbes"].append(probe(f"{day_of(i)}T20:00:00+07:00"))
    report = run(tmp_path, data, "relation")
    assert "60 probe days; 48 have at least one review after the probe" in report
    assert after_probe_table(report).count("| 48 | ") == 24
    assert "Days with the probe and at least one review after it: 48 of 60" in run(tmp_path, data, "inventory")
    sensitivity = report.split("### Sensitivity")[1]
    assert sensitivity.count("| 60 | ") == 24


def test_relation_measures_are_unchanged_by_an_excluded_answer(tmp_path):
    excluded = {"grade": "AGAIN", "retrievability": 0.99, "thinking_time_ms": 100000,
                "excluded_at": "2026-11-01T00:00:00+07:00", "reason": "secret text"}
    with_excluded = planted(40, extra=[(3, excluded)])
    assert run(tmp_path, with_excluded, "relation") == run(tmp_path, planted(40), "relation")
    assert "Excluded answers: 40 of 520" in run(tmp_path, with_excluded, "inventory")


def test_relation_long_away_time_stays_inside_one_session(tmp_path):
    data = planted(40)
    day0 = [e for e in data["recallEvents"] if e["recordedAt"].startswith(day_of(0))]
    day0[5]["answer"]["awayMs"] = 45 * 60 * 1000
    report = run(tmp_path, data, "relation")
    assert "Distraction share clipped at 1 on 0 of those days" in report
