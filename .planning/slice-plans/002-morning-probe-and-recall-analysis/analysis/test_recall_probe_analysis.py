import json
import os
import re
import sys

import pytest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import recall_probe_analysis as analysis  # noqa: E402


def answer(created_at, outcome=None, excluded_at=None, reason=None, thinking_time_ms=5000, away_ms=0):
    return {
        "id": 1,
        "questionType": "SPELLING",
        "promptCreatedAt": created_at,
        "createdAt": created_at,
        "thinkingTimeMs": thinking_time_ms,
        "awayMs": away_ms,
        "awayCount": 0,
        "detourMs": 0,
        "detourCount": 0,
        "idleMs": 0,
        "outcome": outcome,
        "excludedAt": excluded_at,
        "exclusionReason": reason,
    }


def event(at, kind="ANSWERED_RECALL_PROMPT", grade="GOOD", retrievability=0.9, **answer_fields):
    return {
        "recordedAt": at,
        "grade": grade,
        "kind": kind,
        "tutorFeedback": None,
        "elapsedHours": 24.0,
        "stabilityBefore": 3.0,
        "difficultyBefore": 5.0,
        "retrievability": retrievability,
        "memoryTrackerId": 1,
        "memoryTrackerType": "NORMAL",
        "noteId": 1,
        "answer": answer(at, **answer_fields) if kind == "ANSWERED_RECALL_PROMPT" else None,
    }


def probe(completed_at, speed=1.5, accuracy=80, lapse_count=3, variability=0.5):
    return {
        "completedAt": completed_at,
        "speed": speed,
        "accuracy": accuracy,
        "lapseCount": lapse_count,
        "variability": variability,
        "trials": [],
    }


def reviews_at(day, hour, count):
    return [event(f"{day}T{hour:02d}:{minute:02d}:00+07:00") for minute in range(count)]


def make_export():
    return {
        "timezone": "Asia/Jakarta",
        "exportedAt": "2026-11-03T21:27:06+07:00",
        "recallEvents": [
            # 2026-10-01: morning probe at 07:00, 10 reviews after it, one before it
            event("2026-10-01T06:00:00+07:00"),
            *reviews_at("2026-10-01", 8, 10),
            event("2026-10-01T09:00:00+07:00", outcome="OVERLAP"),
            event("2026-10-01T09:01:00+07:00", excluded_at="2026-10-02T00:00:00+07:00", reason="secret text"),
            event("2026-10-01T09:02:00+07:00", kind="JUST_REVIEW", grade=None, retrievability=None),
            event("2026-10-01T09:03:00+07:00", kind="TUTOR_FEEDBACK", grade=None, retrievability=None),
            # 2026-10-02: afternoon probe at 15:10, 2 reviews after it (UTC date still 10-02)
            *reviews_at("2026-10-02", 16, 2),
            # 2026-10-03: probe at 06:30 with reviews only on the next day
            *reviews_at("2026-10-04", 8, 3),
        ],
        "dailyProbes": [
            probe("2026-10-01T07:00:00+07:00"),
            probe("2026-10-02T15:10:00+07:00"),
            probe("2026-10-03T06:30:00+07:00"),
        ],
    }


def run(tmp_path, export, section):
    path = tmp_path / "export.json"
    path.write_text(json.dumps(export))
    return analysis.run(str(path), section)


def test_inventory_counts_events_by_kind(tmp_path):
    report = run(tmp_path, make_export(), "inventory")
    assert "| ANSWERED_RECALL_PROMPT | 18 |" in report
    assert "| JUST_REVIEW | 1 |" in report
    assert "| TUTOR_FEEDBACK | 1 |" in report


def test_inventory_counts_overlap_and_excluded_answers_by_unquoted_reason(tmp_path):
    report = run(tmp_path, make_export(), "inventory")
    assert "`OVERLAP` answers: 1 of 18" in report
    assert "Excluded answers: 1 of 18" in report
    assert "| reason 1 | 1 |" in report
    assert "Reviews (answered, not `OVERLAP`, not excluded): 16 of 18" in report
    assert "secret text" not in report


def test_inventory_counts_probe_days_with_reviews_after_the_probe(tmp_path):
    report = run(tmp_path, make_export(), "inventory")
    assert "Probes: 3, on 3 local days" in report
    assert "Days with the probe and at least one review after it: 2 of 3" in report
    assert "Days with at least 10 reviews after the probe: 1 of 3" in report
    assert "the threshold removes 1 of the 2 days" in report


def test_inventory_shows_the_afternoon_probe_as_that_days_probe(tmp_path):
    report = run(tmp_path, make_export(), "inventory")
    assert "| 15:00–17:59 | 1 |" in report
    assert "| 06:00–08:59 | 2 |" in report
    assert "Not a morning probe (completed at 12:00 or later): 1 of 3" in report


def test_inventory_method_records_export_identity(tmp_path):
    report = run(tmp_path, make_export(), "inventory")
    assert "`exportedAt` 2026-11-03T21:27:06+07:00, `timezone` Asia/Jakarta" in report
    assert "no per-event zone" in report
    assert "recall-history-export?timezone=Asia/Jakarta" in report
    assert "`OVERLAP` answers and excluded answers are not reviews" in report


@pytest.mark.parametrize(
    "remove, named",
    [
        (lambda e: e["recallEvents"][0].pop("kind"), "'kind' in recallEvents[0]"),
        (lambda e: e["recallEvents"][1]["answer"].pop("outcome"), "'outcome' in recallEvents[1].answer"),
        (lambda e: e["dailyProbes"][2].pop("speed"), "'speed' in dailyProbes[2]"),
        (lambda e: e.pop("timezone"), "'timezone' in the export"),
    ],
)
def test_inventory_stops_naming_a_missing_field(tmp_path, remove, named):
    export = make_export()
    remove(export)
    with pytest.raises(SystemExit, match=re.escape(named)):
        run(tmp_path, export, "inventory")


def test_h_check_reports_paired_days_and_probe_order(tmp_path):
    report = run(tmp_path, make_export(), "h-check")
    assert "H1 paired days: 2 with >=1 review after the probe, 1 with >=10, of 3 probe days" in report
    assert "H2 probe precedes the day's first review: 1 of 2" in report
