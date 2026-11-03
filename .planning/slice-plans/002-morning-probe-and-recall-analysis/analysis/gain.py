"""Probe gain over what the owner already knows: leave-one-day-out prediction error per relation row."""

import numpy as np
import pandas as pd

from markdown import table
from recall_export import HOUR_BANDS, day_probes
from relation import PROBE_MEASURES, RECALL_MEASURES, day_table, relation_table

TYPICAL = ["overall", "weekdayTime"]
GAINED = {
    (True, True): "gain over both baselines",
    (True, False): "gain over the typical day only",
    (False, True): "gain over yesterday's recall only",
    (False, False): "no gain",
}


def baseline_days(data):
    """The relation's after-probe days in date order, with weekday and the probe's completion hour band."""
    days = day_table(data, after_probe=True)[0].sort_index()
    hours = day_probes(data).loc[days.index, "completedAt"].dt.hour.to_numpy()
    days["weekday"] = [day.weekday() for day in days.index]
    days["band"] = np.searchsorted([start for start, _ in HOUR_BANDS], hours, side="right") - 1
    return days


def one_hot(values):
    return (values[:, None] == np.unique(values)[None]).astype(float)


def loo_mae(x, y):
    """Mean absolute error of predicting each day by least squares fitted on all other days."""
    errors = []
    for i in range(len(y)):
        others = np.arange(len(y)) != i
        coef = np.linalg.lstsq(x[others], y[others], rcond=None)[0]
        errors.append(abs(x[i] @ coef - y[i]))
    return float(np.mean(errors))


def error_cell(error):
    return f"{error:#.3g}"


def gain_row(days, probe, measure):
    """MAE per baseline and per baseline plus the probe, over days with the measure, its previous value and the probe."""
    frame = pd.DataFrame(
        {
            "y": days[measure],
            "previous": days[measure].shift(1),
            "probe": days[probe],
            "weekday": days["weekday"],
            "band": days["band"],
        }
    ).astype(float).dropna()
    y = frame["y"].to_numpy()
    ones = np.ones((len(y), 1))
    models = {
        "overall": ones,
        "weekdayTime": np.hstack([ones, one_hot(frame["weekday"].to_numpy()), one_hot(frame["band"].to_numpy())]),
        "yesterday": np.hstack([ones, frame[["previous"]].to_numpy()]),
    }
    noise = 1e-9 * max(float(np.abs(y).max()), 1.0)

    def error(x):
        """Leave-one-day-out error, zero when within floating-point noise of the measure's size."""
        e = loo_mae(x, y)
        return e if e > noise else 0.0

    mae = {name: error(x) for name, x in models.items()}
    typical = min(TYPICAL, key=mae.get)
    probe_col = frame[["probe"]].to_numpy()
    with_probe = {name: error(np.hstack([models[name], probe_col])) for name in (typical, "yesterday")}
    return {
        "n": len(y),
        "mae": mae,
        "typical": typical,
        "typicalProbe": with_probe[typical],
        "yesterdayProbe": with_probe["yesterday"],
        "overTypical": with_probe[typical] < mae[typical] - noise,
        "overYesterday": with_probe["yesterday"] < mae["yesterday"] - noise,
    }


def gain_verdict(relation_verdict, over_typical, over_yesterday):
    if relation_verdict != "supported":
        return f"{relation_verdict}; {GAINED[(over_typical, over_yesterday)]}"
    if over_typical and over_yesterday:
        return "supported, gain over both baselines"
    if over_typical:
        return "supported, but no gain over yesterday's recall"
    if over_yesterday:
        return "supported, but no gain over the typical day"
    return "supported, but no gain over either baseline"


def gain_table(days):
    rows = []
    relation_verdicts = {(row[0], row[1]): row[5] for row in relation_table(days)}
    for probe in PROBE_MEASURES:
        for measure, label in RECALL_MEASURES.items():
            g = gain_row(days, probe, measure)
            rows.append(
                (
                    probe,
                    label,
                    g["n"],
                    error_cell(g["mae"]["overall"]),
                    error_cell(g["mae"]["weekdayTime"]),
                    error_cell(g["typicalProbe"]),
                    error_cell(g["mae"]["yesterday"]),
                    error_cell(g["yesterdayProbe"]),
                    gain_verdict(relation_verdicts[(probe, label)], g["overTypical"], g["overYesterday"]),
                )
            )
    return rows


def gain(data):
    days = baseline_days(data)
    header = [
        "Probe measure",
        "Recall measure",
        "n days",
        "Overall mean",
        "Weekday & time",
        "Typical day + probe",
        "Yesterday",
        "Yesterday + probe",
        "Verdict",
    ]
    return "\n".join(
        [
            "### Gain over what the owner already knows",
            "",
            "Each cell is the mean absolute error (in the recall measure's unit) of predicting a day's recall after "
            "the probe from all other days by least squares (leave one day out). Typical day: the overall mean, and "
            "the mean adjusted by weekday and by the probe's completion hour band (the bands of the Inventory); "
            "the typical-day baseline is whichever of the two errs less, and \"Typical day + probe\" adds the probe "
            "measure to that one. Yesterday: a linear prediction from the previous paired day's value of the same "
            "measure; \"Yesterday + probe\" adds the probe measure. The probe gains over a baseline when adding it "
            "lowers that baseline's error. Verdict: a supported relation reads \"gain over both baselines\" or names "
            "the baseline it does not beat; any other row keeps its relation verdict and adds the gain as information.",
            "",
            f"Sample: the {len(days)} days of the after-probe relation table. A row's n counts the days where the "
            "recall measure, its previous paired day's value, and the probe measure all have a value, so the first "
            "paired day never counts; every error in a row is over the same days.",
            "",
            table(header, gain_table(days)),
            "",
        ]
    )
