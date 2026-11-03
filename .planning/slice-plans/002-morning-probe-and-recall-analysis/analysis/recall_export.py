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
