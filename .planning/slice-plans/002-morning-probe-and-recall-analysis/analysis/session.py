"""Recall-session efficiency on its own: response time, distraction share, session length and pace."""

import pandas as pd

from markdown import band_label, share, table
from recall_export import HOUR_BANDS, activity, day_probes, response_time_ms, session_ids
from relation import RECALL_MEASURES, day_recall

FIRST_REVIEWS = 10
MOST_DISTRACTED = 10
MIN_MS = 60_000
MEASURES = ["medianResponseS", "distractionShare", "sessionMin", "pace"]
METHOD = (
    "- Recall-session efficiency: from the first probe day on, the same measures per day (days with at least "
    "one review) and per session (sessions with at least one review), grouped by weekday (per day) and by "
    "the hour band of the session's first event start (per session). Position: a review's rank by "
    f"`answer.createdAt` among its session's reviews; the first {FIRST_REVIEWS} are \"first\", the rest "
    "\"later\". A review's own distraction share is `awayMs + detourMs + idleMs` over "
    "`thinkingTimeMs + awayMs + detourMs`, summed over the reviews of a position; the companion column sums "
    f"without the position's {MOST_DISTRACTED} most distracted reviews."
)
WEEKDAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]


def period_activity(data):
    """Activity from the first probe day on, each event labelled with its session within its day."""
    events = data["events"]
    act = activity(events[events["day"] >= day_probes(data).index.min()])
    ids = pd.concat([session_ids(day) for _, day in act.groupby("day")])
    return act.assign(session=ids)


def measures(act, keys):
    """The day-level recall measures (as in the relation) per group of activity that has a review."""
    rows = {}
    for key, group in act.groupby(keys):
        reviews = group[group["isReview"]]
        if len(reviews):
            rows[key] = {**day_recall(reviews, group, None), "startHour": group["start"].min().hour}
    return pd.DataFrame.from_dict(rows, orient="index")


def cell(values):
    v = pd.Series(values).dropna()
    if v.empty:
        return "–"
    q1, median, q3 = v.quantile([0.25, 0.5, 0.75])
    return f"{median:.3g} ({q1:.3g}–{q3:.3g})"


def summary_rows(groups):
    return [(label, len(frame), *(cell(frame[m]) for m in MEASURES)) for label, frame in groups]


def per_review(act):
    """Each review with its position in its session, response time, own span and distraction."""
    reviews = act[act["isReview"]].sort_values("end")
    ms = reviews[["thinkingTimeMs", "awayMs", "detourMs", "idleMs"]].fillna(0)
    return pd.DataFrame(
        {
            "day": reviews["day"],
            "first": reviews.groupby(["day", "session"]).cumcount() < FIRST_REVIEWS,
            "responseS": response_time_ms(reviews) / 1000,
            "spanMs": ms["thinkingTimeMs"] + ms["awayMs"] + ms["detourMs"],
            "distractedMs": ms["awayMs"] + ms["detourMs"] + ms["idleMs"],
        }
    )


def count(n, noun):
    return f"{n} {noun}" + ("" if n == 1 else "s")


def ratio(part, whole):
    return f"{part / whole:.3f}" if whole > 0 else "–"


def positions(reviews):
    return [
        (label, reviews[reviews["first"] == first])
        for label, first in ((f"First {FIRST_REVIEWS} reviews of a session", True), ("Later reviews", False))
    ]


def position_rows(reviews):
    rows = []
    for label, group in positions(reviews):
        rest = group.drop(group["distractedMs"].nlargest(MOST_DISTRACTED).index)
        rows.append(
            (
                label,
                len(group),
                cell(group["responseS"]),
                ratio(group["distractedMs"].sum(), group["spanMs"].sum()),
                ratio(rest["distractedMs"].sum(), rest["spanMs"].sum()),
                share(int((group["distractedMs"] > 0).sum()), len(group)),
            )
        )
    return rows


def long_absences(reviews):
    """How much of each position's distracted time its MOST_DISTRACTED most distracted reviews hold."""
    parts = []
    for label, group in positions(reviews):
        top = group["distractedMs"].nlargest(MOST_DISTRACTED)
        total = group["distractedMs"].sum()
        held = f"{100 * top.sum() / total:.0f}%" if total > 0 else "–"
        parts.append(f"{held} for {label.lower()} (longest {top.max() / MIN_MS:.0f} min)")
    return (
        f"The distraction share of review time is a ratio of sums, driven by a few long absences: the {MOST_DISTRACTED} most "
        f"distracted reviews of a position hold {' and '.join(parts)} of its distracted time."
    )


def session(data):
    act = period_activity(data)
    days = measures(act, "day")
    sessions = measures(act, ["day", "session"])
    reviews = per_review(act)
    answered = act[act["kind"] == "ANSWERED_RECALL_PROMPT"]
    session_count = act[act["day"].isin(days.index)].groupby(["day", "session"]).ngroups
    header = ["Unit", "n", *(RECALL_MEASURES[m] for m in MEASURES)]
    out = ["## Recall-session efficiency", ""]
    out.append(
        f"Period: {act['day'].min()} to {act['day'].max()}, from the first probe day on, where every answer carries "
        f"its timing fields (`thinkingTimeMs` missing on {share(int(answered['thinkingTimeMs'].isna().sum()), len(answered))} "
        "answered prompts in the period); before it most answers lack them, so no all-time figures are given. "
        "Not related to the probe here. Each cell is median (first quartile–third quartile); measures, sessions "
        "and positions are defined under Method and limits."
    )
    out += [
        "",
        f"Sample: {count(len(days), 'day')} with at least one review; {count(session_count, 'session')} on them, "
        f"{session_count - len(sessions)} of which have no review (only `JUST_REVIEW` or excluded answers) and are "
        f"left out of the per-session rows; {count(len(reviews), 'review')}. Distraction share clipped at 1 on "
        f"{count(int(days['clipped'].sum()), 'day')} and {count(int(sessions['clipped'].sum()), 'session')}.",
        "",
        "### Overall",
        "",
        table(header, summary_rows([("per day (n days)", days), ("per session (n sessions)", sessions)])),
        "",
        f"Sessions per day: {cell(sessions.groupby(level=0).size().reindex(days.index, fill_value=0))}.",
        "",
        "### By weekday (per day, n days)",
        "",
    ]
    weekday = pd.Series([d.weekday() for d in days.index], index=days.index)
    out.append(table(["Weekday", "n days", *header[2:]], summary_rows((w, days[weekday == i]) for i, w in enumerate(WEEKDAYS))))
    out += ["", "### By session start time (per session, n sessions)", ""]
    hour = sessions["startHour"]
    out.append(
        table(
            ["Session start", "n sessions", *header[2:]],
            summary_rows((band_label(s, e), sessions[(hour >= s) & (hour < e)]) for s, e in HOUR_BANDS),
        )
    )
    first, later = (reviews[reviews["first"] == f].groupby("day")["responseS"].median() for f in (True, False))
    both = first.index.intersection(later.dropna().index)
    out += [
        "",
        "### By position in the session (per review, n reviews)",
        "",
        "Position applies to the per-review measures only: response time and the review's own distraction "
        "(`awayMs + detourMs + idleMs` over its `thinkingTimeMs + awayMs + detourMs`). Session length and pace "
        "belong to a whole session and have no position.",
        "",
        table(
            [
                "Position",
                "n reviews",
                "Response time (s)",
                "Distraction share of review time",
                f"Same, without the {MOST_DISTRACTED} most distracted reviews",
                "Reviews with any distraction",
            ],
            position_rows(reviews),
        ),
        "",
        long_absences(reviews),
        "",
        f"Paired by day: later reviews have a higher median response time than the first {FIRST_REVIEWS} on "
        f"{share(int((later[both] > first[both]).sum()), len(both))} days with both.",
        "",
    ]
    return "\n".join(out)
