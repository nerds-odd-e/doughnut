"""Reading the owner's recall history export and the definitions every section shares."""

import numpy as np
import pandas as pd

REQUIRED_TOP = ["timezone", "exportedAt", "recallEvents", "dailyProbes"]
EVENT_FIELDS = ["recordedAt", "kind", "grade", "retrievability", "elapsedHours"]
REQUIRED_EVENT = EVENT_FIELDS + ["answer"]
REQUIRED_ANSWER = [
    "createdAt",
    "promptCreatedAt",
    "thinkingTimeMs",
    "awayMs",
    "detourMs",
    "idleMs",
    "outcome",
    "excludedAt",
    "exclusionReason",
]
REQUIRED_PROBE = ["completedAt", "speed", "accuracy", "lapseCount", "variability"]
MIN_REVIEWS_AFTER_PROBE = 10


def require(record, fields, where):
    for field in fields:
        if field not in record:
            raise SystemExit(f"Missing required field '{field}' in {where}")


def load(export):
    require(export, REQUIRED_TOP, "the export")
    tz = export["timezone"]
    rows = []
    for i, event in enumerate(export["recallEvents"]):
        require(event, REQUIRED_EVENT, f"recallEvents[{i}]")
        answer = event["answer"]
        row = {k: event[k] for k in EVENT_FIELDS}
        if answer is not None:
            require(answer, REQUIRED_ANSWER, f"recallEvents[{i}].answer")
            row.update(answer)
        rows.append(row)
    events = pd.DataFrame(rows, columns=EVENT_FIELDS + REQUIRED_ANSWER)
    for i, probe in enumerate(export["dailyProbes"]):
        require(probe, REQUIRED_PROBE, f"dailyProbes[{i}]")
    probes = pd.DataFrame(export["dailyProbes"], columns=REQUIRED_PROBE)
    for frame, columns in ((events, ["recordedAt", "createdAt", "promptCreatedAt"]), (probes, ["completedAt"])):
        for column in columns:
            frame[column] = pd.to_datetime(frame[column], utc=True, format="ISO8601").dt.tz_convert(tz)
    events["day"] = events["recordedAt"].dt.date
    answered = events["kind"] == "ANSWERED_RECALL_PROMPT"
    events.loc[answered, "day"] = events.loc[answered, "createdAt"].dt.date
    events["isReview"] = answered & (events["outcome"] != "OVERLAP") & events["excludedAt"].isna()
    events["correct"] = np.where(
        (events["grade"] == "GOOD") | (events["outcome"] == "OVERLAP"),
        1.0,
        np.where(events["grade"] == "AGAIN", 0.0, np.nan),
    )
    probes["day"] = probes["completedAt"].dt.date
    probes = probes.sort_values("completedAt")
    return {"timezone": tz, "exportedAt": export["exportedAt"], "events": events, "probes": probes}


def day_probes(data):
    """The day's probe is its first completed probe."""
    return data["probes"].drop_duplicates("day", keep="first").set_index("day")


def reviews_after_probe(data):
    probes = day_probes(data)
    reviews = data["events"][data["events"]["isReview"]]
    reviews = reviews[reviews["day"].isin(probes.index)]
    completed = reviews["day"].map(probes["completedAt"])
    return reviews[reviews["createdAt"] > completed]


def reviews_after_probe_per_day(data):
    """Number of reviews after the probe on each probe day, zero included."""
    return reviews_after_probe(data).groupby("day").size().reindex(day_probes(data).index, fill_value=0)


def surprise(reviews):
    scored = reviews.dropna(subset=["retrievability", "correct"])
    p = scored["retrievability"]
    variance = (p * (1 - p)).sum()
    return (scored["correct"] - p).sum() / np.sqrt(variance) if variance > 0 else np.nan


THINKING_CAP_MS = 120_000
DIFF_CAP_MS = 300_000
MIN_VALID_MS = 1_000
SESSION_GAP_MINUTES = 30
ACTIVITY_KINDS = ["ANSWERED_RECALL_PROMPT", "JUST_REVIEW"]


def response_time_ms(reviews):
    """Per review, as the product's RecallStatsAggregator.responseTimeMs; invalid ones dropped."""
    thinking = reviews["thinkingTimeMs"]
    diff = (reviews["createdAt"] - reviews["promptCreatedAt"]).dt.total_seconds() * 1000
    raw = thinking.where(thinking.notna(), diff.where(diff > 0))
    capped = np.minimum(raw, np.where(thinking.notna(), THINKING_CAP_MS, DIFF_CAP_MS))
    return capped[capped >= MIN_VALID_MS]


def activity(events):
    """Answered prompts and JUST_REVIEW events with the time each started and ended.

    An event ends at `answer.createdAt` (`recordedAt` for JUST_REVIEW) and starts
    `thinkingTimeMs + awayMs + detourMs` before that, since away and detour pause the
    thinking clock while idle stays inside it (a JUST_REVIEW is a moment).
    """
    act = events[events["kind"].isin(ACTIVITY_KINDS)]
    end = act["createdAt"].fillna(act["recordedAt"])
    duration = act["thinkingTimeMs"].fillna(0) + act["awayMs"].fillna(0) + act["detourMs"].fillna(0)
    start = end - pd.to_timedelta(duration, unit="ms")
    return act.assign(start=start, end=end)


def session_spans(day_activity):
    """A new session starts when an event starts more than SESSION_GAP_MINUTES after the previous one ended."""
    ordered = day_activity.sort_values("end")
    new_session = (ordered["start"] - ordered["end"].shift()) > pd.Timedelta(minutes=SESSION_GAP_MINUTES)
    return ordered.groupby(new_session.cumsum()).agg(start=("start", "min"), end=("end", "max"))


def session_ms(day_activity, after=None):
    """Sum of the day's session spans; with `after`, only the part of each span after that time."""
    spans = session_spans(day_activity)
    start = spans["start"] if after is None else spans["start"].clip(lower=after)
    return float((spans["end"] - start).dt.total_seconds().clip(lower=0).sum() * 1000)


def distraction_ms(reviews):
    return float((reviews["awayMs"].fillna(0) + reviews["detourMs"].fillna(0) + reviews["idleMs"].fillna(0)).sum())
