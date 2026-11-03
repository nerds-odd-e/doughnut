import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from test_recall_probe_analysis import event, probe, run  # noqa: E402
from test_relation import export  # noqa: E402

MIN = 60_000


def at(day, hour, minute):
    return f"{day}T{hour:02d}:{minute:02d}:00+07:00"


def just_review(time):
    return event(time, kind="JUST_REVIEW", grade=None, retrievability=None)


def session_export(events, probe_days=("2026-09-01",)):
    return export(events, [probe(at(day, 6, 0)) for day in probe_days])


def cells(report, label):
    line = next(x for x in report.splitlines() if x.startswith(f"| {label} |"))
    return [c.strip() for c in line.strip("|").split("|")][1:]


def per_day(report):
    n, response, distraction, length, pace = cells(report, "per day (n days)")
    return {"n": int(n), "response": response, "distraction": distraction, "length": length, "pace": pace}


TWO_REVIEWS = [
    event(at("2026-09-01", 8, 1), thinking_time_ms=MIN),
    event(at("2026-09-01", 8, 2), thinking_time_ms=MIN),
]


def test_session_trailing_just_review_extends_length_and_lowers_pace(tmp_path):
    without = per_day(run(tmp_path, session_export(TWO_REVIEWS), "session"))
    trailing = per_day(run(tmp_path, session_export(TWO_REVIEWS + [just_review(at("2026-09-01", 8, 4))]), "session"))
    assert (without["length"], without["pace"]) == ("2 (2–2)", "1 (1–1)")
    assert (trailing["length"], trailing["pace"]) == ("4 (4–4)", "0.5 (0.5–0.5)")
    assert trailing["response"] == without["response"] == "60 (60–60)"


def test_session_tutor_feedback_changes_nothing(tmp_path):
    feedback = event(at("2026-09-01", 8, 4), kind="TUTOR_FEEDBACK", grade=None, retrievability=None)
    assert run(tmp_path, session_export(TWO_REVIEWS + [feedback]), "session") == run(
        tmp_path, session_export(TWO_REVIEWS), "session"
    )


def test_session_distraction_share_and_pace_from_away_detour_and_idle(tmp_path):
    events = [
        # starts 08:03 (4 min thinking + 2 min away + 1 min detour before 08:10); idle 1 min stays inside thinking
        event(at("2026-09-01", 8, 10), thinking_time_ms=4 * MIN, away_ms=2 * MIN, detour_ms=MIN, idle_ms=MIN),
        event(at("2026-09-01", 8, 12), thinking_time_ms=2 * MIN),
    ]
    day = per_day(run(tmp_path, session_export(events), "session"))
    assert day["length"] == "9 (9–9)"
    assert day["distraction"] == "0.444 (0.444–0.444)"
    assert day["pace"] == "0.4 (0.4–0.4)"


def test_session_position_puts_each_sessions_first_ten_reviews_first(tmp_path):
    first_session = [event(at("2026-09-01", 8, m), thinking_time_ms=2000) for m in range(10)]
    first_session += [event(at("2026-09-01", 8, 10), thinking_time_ms=9000, away_ms=2 * MIN)]  # one long absence
    first_session += [event(at("2026-09-01", 8, m), thinking_time_ms=9000) for m in range(11, 21)]
    second_session = [event(at("2026-09-01", 10, m), thinking_time_ms=2000) for m in range(3)]
    report = run(tmp_path, session_export(first_session + second_session), "session")
    assert cells(report, "First 10 reviews of a session") == ["13", "2 (2–2)", "0.000", "0.000", "0 of 13 (0%)"]
    assert cells(report, "Later reviews") == ["11", "9 (9–9)", "0.548", "0.000", "1 of 11 (9%)"]
    assert "100% for later reviews (longest 2 min)" in report
    assert "than the first 10 on 1 of 1 (100%) days with both" in report


def test_session_groups_days_by_weekday_and_sessions_by_start_hour(tmp_path):
    events = [
        event(at("2026-08-20", 8, 0)),  # before the first probe day: outside the period
        event(at("2026-09-01", 7, 0)),  # Tuesday, two sessions
        event(at("2026-09-01", 19, 0)),
        just_review(at("2026-09-01", 21, 0)),  # a session without a review
        event(at("2026-09-02", 10, 0)),  # Wednesday
        event(at("2026-09-08", 8, 0)),  # Tuesday
    ]
    report = run(tmp_path, session_export(events, ("2026-09-01", "2026-09-08")), "session")
    assert "Period: 2026-09-01 to 2026-09-08" in report
    assert "Sample: 3 days with at least one review; 5 sessions on them, 1 of which have no review" in report
    assert [cells(report, w)[0] for w in ("Monday", "Tuesday", "Wednesday")] == ["0", "2", "1"]
    assert [cells(report, b)[0] for b in ("06:00–08:59", "09:00–11:59", "18:00–20:59", "21:00–23:59")] == [
        "2", "1", "1", "0"
    ]
    assert cells(report, "per session (n sessions)")[0] == "4"
