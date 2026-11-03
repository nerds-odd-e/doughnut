import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from test_recall_probe_analysis import event, probe, run  # noqa: E402
from test_relation import export  # noqa: E402


def review(minute, grade, retrievability, elapsed_hours=24.0, day="2026-09-01"):
    e = event(f"{day}T08:{minute:02d}:00+07:00", grade=grade, retrievability=retrievability)
    e["elapsedHours"] = elapsed_hours
    return e


def report(tmp_path, events):
    return run(tmp_path, export(events, [probe("2026-09-01T06:00:00+07:00")]), "repetition")


def probe_period(text):
    return text.split("### All time")[0]


def cells(text, label):
    line = next(x for x in text.splitlines() if x.startswith(f"| {label} |"))
    return [c.strip() for c in line.strip("|").split("|")][1:]


def test_repetition_again_below_0_7_shows_the_scheduler_over_predicts_in_that_band(tmp_path):
    planted = [review(i, "AGAIN", 0.6) for i in range(8)] + [review(8 + i, "GOOD", 0.6) for i in range(2)]
    all_correct = [review(20 + i, "GOOD", 0.85) for i in range(30)]
    text = probe_period(report(tmp_path, planted + all_correct))
    n, predicted, observed, _, gap, judgment = cells(text, "0.50–<0.70")
    assert (n, predicted, observed, gap, judgment) == ("10", "0.600", "0.200", "-0.400", "over-predicts")
    assert cells(text, "0.80–<0.90")[-1] == "under-predicts"


def test_repetition_too_early_and_too_late_shares_from_known_reviews(tmp_path):
    events = [
        review(0, "GOOD", 0.97),
        review(1, "GOOD", 0.95),
        review(2, "AGAIN", 0.96),  # high retrievability but wrong: not too early
        review(3, "AGAIN", 0.5),
        review(4, "AGAIN", 0.8),  # wrong but not below 0.7: not too late
        review(5, "GOOD", 0.5),
        review(6, "GOOD", None),  # no retrievability: outside both shares
        review(7, "CONFUSION", 0.97),
    ]
    text = probe_period(report(tmp_path, events))
    assert "Too early (`GOOD` at `retrievability` ≥ 0.95): 2 of 7 (29%) reviews" in text
    assert "Too late (`AGAIN` at `retrievability` < 0.7): 1 of 7 (14%) reviews" in text
    assert "that is 1 of 3 (33%) `AGAIN` reviews" in text
    assert "`retrievability` missing on 1 of 8 (12%)" in text
    assert "1 are neither correct nor wrong" in text


def test_repetition_elapsed_hours_by_grade_and_periods(tmp_path):
    events = [review(i, "GOOD", 0.9, elapsed_hours=h) for i, h in enumerate([10, 20, 30, 40, 50])]
    events += [review(10 + i, "AGAIN", 0.9, elapsed_hours=h) for i, h in enumerate([100, 200, 300])]
    events.append(review(0, "AGAIN", 0.9, elapsed_hours=5, day="2025-01-01"))  # before the probe: all time only
    text = report(tmp_path, events)
    assert cells(probe_period(text), "GOOD") == ["5", "30.0 (20.0–40.0)"]
    assert cells(probe_period(text), "AGAIN") == ["3", "200.0 (150.0–250.0)"]
    all_time = text.split("### All time")[1]
    assert cells(all_time, "AGAIN") == ["4", "150.0 (76.2–225.0)"]
