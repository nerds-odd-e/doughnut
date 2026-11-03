import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from test_recall_probe_analysis import probe, run  # noqa: E402
from test_relation import day_of, export, independent, reviews  # noqa: E402


def days_with_good(good_counts, speeds):
    """Day i has good_counts[i] GOOD of 12 reviews at one retrievability, so its surprise is linear in that count."""
    events, probes = [], []
    for i, (good, speed) in enumerate(zip(good_counts, speeds)):
        events += reviews(day_of(i), ["GOOD"] * good + ["AGAIN"] * (12 - good))
        probes.append(probe(f"{day_of(i)}T07:00:00+07:00", speed=float(speed)))
    return export(events, probes)


def gain_row(report, probe_measure, recall_measure):
    line = next(x for x in report.splitlines() if x.startswith(f"| {probe_measure} | {recall_measure} |"))
    cells = [c.strip() for c in line.strip("|").split("|")]
    errors = dict(zip(["overall", "weekdayTime", "typicalProbe", "yesterday", "yesterdayProbe"], map(float, cells[3:8])))
    return {"n": int(cells[2]), **errors, "verdict": cells[8]}


def test_gain_probe_that_repeats_yesterdays_recall_has_no_gain_over_yesterday(tmp_path):
    good = [round(6 + 4 * math.sin(i / 6)) for i in range(41)]
    data = days_with_good(good[1:], speeds=good[:-1])
    result = gain_row(run(tmp_path, data, "gain"), "speed", "Surprise")
    assert result["n"] == 39
    assert result["verdict"] == "supported, but no gain over yesterday's recall"
    assert result["yesterday"] < min(result["overall"], result["weekdayTime"])


def test_gain_probe_that_alone_predicts_the_day_gains_over_both_baselines(tmp_path):
    rng = np.random.default_rng(3)
    good = rng.integers(2, 11, 40)
    data = days_with_good(good, speeds=good + rng.uniform(0, 0.5, 40))
    result = gain_row(run(tmp_path, data, "gain"), "speed", "Surprise")
    assert result["verdict"] == "supported, gain over both baselines"
    assert result["typicalProbe"] < min(result["overall"], result["weekdayTime"])
    assert result["yesterdayProbe"] < result["yesterday"]


def test_gain_unsupported_row_still_gets_its_baseline_errors(tmp_path):
    result = gain_row(run(tmp_path, independent(40), "gain"), "speed", "Surprise")
    assert result["n"] == 39
    assert all(result[k] > 0 for k in ("overall", "weekdayTime", "typicalProbe", "yesterday", "yesterdayProbe"))
    assert result["verdict"].startswith("not supported; ")
