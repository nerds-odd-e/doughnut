"""Analysis of the owner's recall history export against the daily probe.

Usage: python3 -E -B recall_probe_analysis.py <export.json> <section>
Each section prints Markdown for the report
.planning/notes/recall-and-morning-probe-analysis.md.
"""

import json
import sys

import pandas as pd

from recall_export import (
    MIN_REVIEWS_AFTER_PROBE,
    day_probes,
    load,
    reviews_after_probe,
    reviews_after_probe_per_day,
    surprise,
)

KINDS = ["ANSWERED_RECALL_PROMPT", "JUST_REVIEW", "TUTOR_FEEDBACK"]
HOUR_BANDS = [(0, 6), (6, 9), (9, 12), (12, 15), (15, 18), (18, 21), (21, 24)]


def band_label(start, end):
    return f"{start:02d}:00–{end - 1:02d}:59"


def table(header, rows):
    lines = ["| " + " | ".join(header) + " |", "| " + " | ".join("---" for _ in header) + " |"]
    lines += ["| " + " | ".join(str(c) for c in row) + " |" for row in rows]
    return "\n".join(lines)


def share(part, whole):
    return f"{part} of {whole} ({100 * part / whole:.0f}%)" if whole else f"{part} of 0"


def inventory(data):
    events, probes = data["events"], data["probes"]
    answered = events[events["kind"] == "ANSWERED_RECALL_PROMPT"]
    excluded = answered[answered["excludedAt"].notna()]
    overlap = answered[answered["outcome"] == "OVERLAP"]
    reviews = events[events["isReview"]]
    first = day_probes(data)
    after_counts = reviews_after_probe_per_day(data)
    paired = int((after_counts >= 1).sum())
    usable = int((after_counts >= MIN_REVIEWS_AFTER_PROBE).sum())
    hours = first["completedAt"].dt.hour
    out = ["## Inventory", ""]
    out.append(
        f"Export of {len(events)} recall events, "
        f"{events['recordedAt'].min():%Y-%m-%d} to {events['recordedAt'].max():%Y-%m-%d} (local days)."
    )
    out += ["", "### Recall events by kind", ""]
    counts = events["kind"].value_counts()
    out.append(table(["Kind", "Events"], [(k, int(counts.get(k, 0))) for k in KINDS]))
    out += [
        "",
        f"Sample: all {len(events)} events. Only `ANSWERED_RECALL_PROMPT` events are answered prompts; "
        "the others do not enter correctness.",
        "",
        "### Answered prompts that are not reviews",
        "",
        f"- `OVERLAP` answers: {share(len(overlap), len(answered))} answered prompts.",
        f"- Excluded answers: {share(len(excluded), len(answered))} answered prompts.",
        f"- Reviews (answered, not `OVERLAP`, not excluded): {share(len(reviews), len(answered))} answered prompts.",
        "",
        "Excluded answers by reason (the reason is free text written at exclusion time, "
        "so it is labelled, not quoted):",
        "",
    ]
    by_reason = excluded["exclusionReason"].fillna("(no reason)").value_counts()
    out.append(
        table(["Reason", "Excluded answers"], [(f"reason {i + 1}", int(n)) for i, n in enumerate(by_reason)])
    )
    out += [
        "",
        f"Sample: the {len(excluded)} excluded answers.",
        "",
        "### Probe days",
        "",
        f"- Probes: {len(probes)}, on {len(first)} local days "
        f"({first.index.min()} to {first.index.max()}). A day with several probes uses its first.",
        f"- Days with the probe and at least one review after it: {share(paired, len(first))} probe days.",
        f"- Days with at least {MIN_REVIEWS_AFTER_PROBE} reviews after the probe: {share(usable, len(first))} "
        f"probe days; the threshold removes {paired - usable} of the {paired} days with any review after the probe.",
        "",
        "### Probe completion time of day",
        "",
    ]
    rows = [(band_label(s, e), int(((hours >= s) & (hours < e)).sum())) for s, e in HOUR_BANDS]
    out.append(table(["Local time", "Probe days"], rows))
    out += [
        "",
        f"Not a morning probe (completed at 12:00 or later): {share(int((hours >= 12).sum()), len(first))} probe days. "
        "Every probe is that local day's probe whatever its time.",
        "",
    ]
    return "\n".join(out)


def method(data):
    return "\n".join(
        [
            "## Method and limits",
            "",
            "- Input: the owner's own export from "
            f"`GET /api/user/recall-history-export?timezone={data['timezone']}`, fetched 2026-11-03 with the "
            "owner's token. Only the owner's data is analysed.",
            f"- Export: `exportedAt` {data['exportedAt']}, `timezone` {data['timezone']}.",
            "- Every timestamp is read in that one timezone; Donut stores no per-event zone, "
            "so days spent in another zone are cut at the wrong midnight.",
            "- Local day: the calendar date of the timestamp in the export's timezone. "
            "An answered prompt's day is that of `answer.createdAt`; a probe's day is that of `completedAt`.",
            "- Review: an event of kind `ANSWERED_RECALL_PROMPT` whose answer's outcome is not `OVERLAP` "
            "and whose `excludedAt` is empty (as the product's `RecallAnswerRow`). `OVERLAP` answers and "
            "excluded answers are not reviews.",
            "- Correct: grade `GOOD` or outcome `OVERLAP`; wrong: grade `AGAIN`; `HARD`, `EASY`, "
            "`CONFUSION` are neither and stay out of correctness measures (as `Answer.correctFrom`).",
            "- After the probe: `answer.createdAt` later than the probe's `completedAt` on the same local day.",
            "- Surprise for a set of reviews with a `retrievability`: `sum(y - p) / sqrt(sum(p (1 - p)))`, "
            "`y` 1 for correct and 0 for wrong, `p` the raw `retrievability`; the recall stats tiles "
            "additionally recalibrate `p` per question type, this report does not.",
            "- Response time: `answer.thinkingTimeMs`, else `answer.createdAt - answer.promptCreatedAt` when "
            "positive; capped at 120 s (thinking time) or 300 s (timestamp difference); dropped below 1 s.",
            f"- A day enters the relation only with at least {MIN_REVIEWS_AFTER_PROBE} reviews after its probe.",
            "- Aggregates only: no note ids, note titles, tutor feedback, or exclusion reason text.",
            "- Scripts: `analysis/` in `.planning/slice-plans/002-morning-probe-and-recall-analysis`, "
            "recoverable with `git log --all -- .planning/slice-plans/002-morning-probe-and-recall-analysis`. "
            "They run with system Python 3 (pandas, numpy, scipy) and are outside any product build or lint.",
            "",
        ]
    )


def spread(values):
    v = pd.Series(values).dropna()
    q = v.quantile([0, 0.25, 0.5, 0.75, 1]).round(3).tolist()
    return f"n={len(v)} min={q[0]} q1={q[1]} median={q[2]} q3={q[3]} max={q[4]} sd={v.std():.3f}"


def h_check(data):
    first = day_probes(data)
    after = reviews_after_probe(data)
    counts = reviews_after_probe_per_day(data)
    reviews = data["events"][data["events"]["isReview"]]
    first_review = reviews.groupby("day")["createdAt"].min()
    with_reviews = first.index[first.index.isin(first_review.index)]
    precedes = int((first.loc[with_reviews, "completedAt"] < first_review[with_reviews]).sum())
    usable = counts[counts >= MIN_REVIEWS_AFTER_PROBE].index
    grouped = after[after["day"].isin(usable)].groupby("day")
    return "\n".join(
        [
            "## H-check (plan Learnings, not part of the report)",
            "",
            f"- H1 paired days: {int((counts >= 1).sum())} with >=1 review after the probe, "
            f"{len(usable)} with >={MIN_REVIEWS_AFTER_PROBE}, of {len(first)} probe days.",
            f"- H2 probe precedes the day's first review: {share(precedes, len(with_reviews))} probe days with a review.",
            f"- H3 correct share over usable days: {spread(grouped['correct'].mean())}",
            f"- H3 surprise over usable days: {spread(grouped.apply(surprise, include_groups=False))} "
            "(sd near 1 means no day-level variation beyond item mix)",
            "",
        ]
    )


SECTIONS = {
    "inventory": lambda data: inventory(data) + "\n" + method(data),
    "h-check": h_check,
}


def run(path, section):
    with open(path) as f:
        data = load(json.load(f))
    return SECTIONS[section](data)


if __name__ == "__main__":
    print(run(sys.argv[1], sys.argv[2]))
