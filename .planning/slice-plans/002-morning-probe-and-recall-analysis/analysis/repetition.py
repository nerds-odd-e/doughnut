"""Repetition efficiency on its own: retrievability calibration, too-early and too-late reviews, elapsed time by grade."""

import pandas as pd
from scipy.stats import binomtest

from markdown import share, table
from recall_export import day_probes

BANDS = [(0.0, 0.5), (0.5, 0.7), (0.7, 0.8), (0.8, 0.9), (0.9, 0.95), (0.95, 1.0)]
TOO_EARLY_AT = 0.95
TOO_LATE_BELOW = 0.7
METHOD = (
    "- Repetition efficiency: reviews only, in two periods (from the first probe day on, and all time). "
    "Calibration: reviews with a `retrievability` and a correctness (correct or wrong) are grouped by "
    "`retrievability` band (lower bound included, the top band includes 1); predicted is the band's mean "
    "`retrievability`, observed its correct share with an exact (Clopper-Pearson) binomial 95% interval. "
    "Judgment: \"matched\" when the interval contains the predicted mean, \"over-predicts\" when the interval "
    "lies below it, \"under-predicts\" when above. "
    f"Too early: grade `GOOD` at `retrievability` {TOO_EARLY_AT} or more (the story's \"easy grade\"; no review in "
    "the export is graded `EASY`, so `GOOD` stands in for it; the report also states the `EASY` count). "
    f"Too late: grade `AGAIN` at `retrievability` below {TOO_LATE_BELOW}. Both shares are over the reviews with "
    "a `retrievability`. Elapsed time: `elapsedHours` by grade, over all reviews of the period."
)


def retrievability_band_label(lo, hi):
    return f"{lo:.2f}–{hi:.2f}" if hi == 1.0 else f"{lo:.2f}–<{hi:.2f}"


def in_band(p, lo, hi):
    return (p >= lo) & ((p <= hi) if hi == 1.0 else (p < hi))


def judge(scored):
    """Predicted mean, observed share, its interval and the judgment for a set of scored reviews."""
    n, k = len(scored), int(scored["correct"].sum())
    predicted, observed = scored["retrievability"].mean(), k / n
    ci = binomtest(k, n).proportion_ci()
    verdict = "matched" if ci.low <= predicted <= ci.high else "over-predicts" if ci.high < predicted else "under-predicts"
    return [n, f"{predicted:.3f}", f"{observed:.3f}", f"{ci.low:.3f}–{ci.high:.3f}", f"{observed - predicted:+.3f}", verdict]


def calibration_rows(groups):
    return [[label, *judge(g)] if len(g) else [label, 0, "–", "–", "–", "–", "–"] for label, g in groups]


CALIBRATION_HEADER = ["n", "Predicted (mean retrievability)", "Observed correct share", "95% interval", "Observed − predicted", "Judgment"]


def hours(values):
    q1, median, q3 = values.quantile([0.25, 0.5, 0.75])
    return f"{median:.1f} ({q1:.1f}–{q3:.1f})"


def period_section(title, reviews):
    with_p = reviews[reviews["retrievability"].notna()]
    scored = with_p[with_p["correct"].notna()]
    p = scored["retrievability"]
    grades = reviews["grade"].fillna("(none)").value_counts()
    out = [f"### {title}", ""]
    out.append(
        f"Period: {reviews['day'].min()} to {reviews['day'].max()}. Sample: {len(reviews)} reviews; "
        f"`retrievability` missing on {share(len(reviews) - len(with_p), len(reviews))}; of the {len(with_p)} with it, "
        f"{len(with_p) - len(scored)} are neither correct nor wrong and stay out of the calibration, leaving "
        f"{len(scored)}. Grades: " + ", ".join(f"`{g}` {n}" for g, n in grades.items()) + "."
    )
    out += ["", "Calibration by retrievability band:", ""]
    groups = [(retrievability_band_label(lo, hi), scored[in_band(p, lo, hi)]) for lo, hi in BANDS]
    out.append(table(["Retrievability", *CALIBRATION_HEADER], calibration_rows(groups + [("all", scored)])))
    types = scored["questionType"].fillna("(none)")
    out += ["", "Calibration by question type (all bands):", ""]
    out.append(table(["Question type", *CALIBRATION_HEADER], calibration_rows([(t, scored[types == t]) for t in sorted(types.unique())])))
    q = with_p["retrievability"]
    early = int(((q >= TOO_EARLY_AT) & (with_p["grade"] == "GOOD")).sum())
    easy = int(((q >= TOO_EARLY_AT) & (with_p["grade"] == "EASY")).sum())
    late = with_p[(q < TOO_LATE_BELOW) & (with_p["grade"] == "AGAIN")]
    again = with_p[with_p["grade"] == "AGAIN"]
    out += [
        "",
        f"- Too early (`GOOD` at `retrievability` ≥ {TOO_EARLY_AT}): {share(early, len(with_p))} reviews with a "
        f"`retrievability`. Graded `EASY` at that level: {easy}.",
        f"- Too late (`AGAIN` at `retrievability` < {TOO_LATE_BELOW}): {share(len(late), len(with_p))} reviews with a "
        f"`retrievability`; that is {share(len(late), len(again))} `AGAIN` reviews with a `retrievability`.",
        "",
        "`elapsedHours` by grade, median (first quartile–third quartile):",
        "",
    ]
    by_grade = reviews.dropna(subset=["elapsedHours"]).groupby(reviews["grade"].fillna("(none)"))["elapsedHours"]
    out.append(table(["Grade", "n reviews", "elapsedHours"], [(g, len(v), hours(v)) for g, v in by_grade]))
    out.append("")
    return out


def repetition(data):
    reviews = data["events"][data["events"]["isReview"]]
    start = day_probes(data).index.min()
    out = ["## Repetition efficiency", ""]
    out.append(
        "Not related to the probe here. Calibration compares the scheduler's raw `retrievability` with the share "
        "correct, without the recall stats tiles' per-type recalibration; with thousands of reviews per band even a "
        "small gap leaves the interval, so the size of observed − predicted matters more than the judgment. "
        "Bands, too-early and too-late reviews are defined under Method and limits."
    )
    out.append("")
    out += period_section("From the first probe day on", reviews[reviews["day"] >= start])
    out += period_section("All time", reviews)
    return "\n".join(out)
