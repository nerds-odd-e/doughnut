# Recall and morning probe analysis

Date: 2026-11-03. Owner's production recall history and daily probe results; aggregates only.

## Inventory

Export of 133814 recall events, 2023-07-24 to 2026-11-03 (local days).

### Recall events by kind

| Kind | Events |
| --- | --- |
| ANSWERED_RECALL_PROMPT | 133764 |
| JUST_REVIEW | 35 |
| TUTOR_FEEDBACK | 15 |

Sample: all 133814 events. Only `ANSWERED_RECALL_PROMPT` events are answered prompts; the others do not enter correctness.

### Answered prompts that are not reviews

- `OVERLAP` answers: 0 of 133764 (0%) answered prompts.
- Excluded answers: 1 of 133764 (0%) answered prompts.
- Reviews (answered, not `OVERLAP`, not excluded): 133763 of 133764 (100%) answered prompts.

Excluded answers by reason (the reason is free text written at exclusion time, so it is labelled, not quoted):

| Reason | Excluded answers |
| --- | --- |
| reason 1 | 1 |

Sample: the 1 excluded answers.

### Probe days

- Probes: 66, on 66 local days (2026-08-29 to 2026-11-03). A day with several probes uses its first.
- Days with the probe and at least one review after it: 66 of 66 (100%) probe days.
- Days with at least 10 reviews after the probe: 65 of 66 (98%) probe days; the threshold removes 1 of the 66 days with any review after the probe.

### Probe completion time of day

| Local time | Probe days |
| --- | --- |
| 00:00–05:59 | 27 |
| 06:00–08:59 | 34 |
| 09:00–11:59 | 2 |
| 12:00–14:59 | 1 |
| 15:00–17:59 | 0 |
| 18:00–20:59 | 1 |
| 21:00–23:59 | 1 |

Not a morning probe (completed at 12:00 or later): 3 of 66 (5%) probe days. Every probe is that local day's probe whatever its time.

## Probe and same-day recall

Spearman rho per probe measure and recall measure, with a 95% percentile bootstrap interval (2000 resamples of days, seed 20261103). Verdict: "supported" when the interval excludes 0; "not supported" when it includes 0 with at least 30 days; "too few days to tell" below 30 days, naming the days needed to detect |rho| = 0.3 at 80% power (two-sided alpha 0.05, Fisher z): 85. Measures are defined under Method and limits.

### That day's recall after the probe

Sample: 66 probe days; 66 have at least one review after the probe on its local day; 1 of them have fewer than 10 and are excluded, leaving 65 days. Distraction share clipped at 1 on 0 of those days; 0 have no active time and so no pace. Days without a value, by measure: speed 1, variability 1. A row's n counts the days where both of its measures have a value.

| Probe measure | Recall measure | rho | 95% interval | n days | Verdict |
| --- | --- | --- | --- | --- | --- |
| speed | Correct share | 0.07 | [-0.20, 0.34] | 64 | not supported |
| speed | Surprise | 0.03 | [-0.22, 0.29] | 64 | not supported |
| speed | Median response time (s) | 0.02 | [-0.23, 0.28] | 64 | not supported |
| speed | Distraction share | -0.13 | [-0.39, 0.15] | 64 | not supported |
| speed | Session length (min) | -0.03 | [-0.28, 0.23] | 64 | not supported |
| speed | Pace (reviews per active min) | 0.09 | [-0.16, 0.32] | 64 | not supported |
| accuracy | Correct share | -0.22 | [-0.43, 0.01] | 65 | not supported |
| accuracy | Surprise | -0.23 | [-0.45, 0.02] | 65 | not supported |
| accuracy | Median response time (s) | -0.02 | [-0.26, 0.24] | 65 | not supported |
| accuracy | Distraction share | 0.06 | [-0.19, 0.30] | 65 | not supported |
| accuracy | Session length (min) | 0.11 | [-0.15, 0.34] | 65 | not supported |
| accuracy | Pace (reviews per active min) | 0.02 | [-0.23, 0.25] | 65 | not supported |
| lapseCount | Correct share | -0.01 | [-0.24, 0.24] | 65 | not supported |
| lapseCount | Surprise | -0.00 | [-0.23, 0.22] | 65 | not supported |
| lapseCount | Median response time (s) | 0.02 | [-0.22, 0.26] | 65 | not supported |
| lapseCount | Distraction share | -0.04 | [-0.30, 0.23] | 65 | not supported |
| lapseCount | Session length (min) | 0.04 | [-0.23, 0.29] | 65 | not supported |
| lapseCount | Pace (reviews per active min) | -0.23 | [-0.48, 0.03] | 65 | not supported |
| variability | Correct share | -0.12 | [-0.37, 0.15] | 64 | not supported |
| variability | Surprise | -0.11 | [-0.36, 0.17] | 64 | not supported |
| variability | Median response time (s) | 0.02 | [-0.21, 0.26] | 64 | not supported |
| variability | Distraction share | -0.21 | [-0.43, 0.04] | 64 | not supported |
| variability | Session length (min) | -0.07 | [-0.32, 0.18] | 64 | not supported |
| variability | Pace (reviews per active min) | -0.06 | [-0.31, 0.19] | 64 | not supported |

### Sensitivity: all of the probe day's reviews

Sample: 66 probe days; 66 have at least one review on its local day; 0 of them have fewer than 10 and are excluded, leaving 66 days. Distraction share clipped at 1 on 0 of those days; 0 have no active time and so no pace. Days without a value, by measure: speed 1, variability 1. A row's n counts the days where both of its measures have a value.

| Probe measure | Recall measure | rho | 95% interval | n days | Verdict |
| --- | --- | --- | --- | --- | --- |
| speed | Correct share | 0.09 | [-0.16, 0.35] | 65 | not supported |
| speed | Surprise | 0.07 | [-0.18, 0.33] | 65 | not supported |
| speed | Median response time (s) | -0.00 | [-0.26, 0.26] | 65 | not supported |
| speed | Distraction share | -0.12 | [-0.38, 0.15] | 65 | not supported |
| speed | Session length (min) | -0.06 | [-0.32, 0.19] | 65 | not supported |
| speed | Pace (reviews per active min) | 0.12 | [-0.13, 0.35] | 65 | not supported |
| accuracy | Correct share | -0.16 | [-0.38, 0.09] | 66 | not supported |
| accuracy | Surprise | -0.22 | [-0.44, 0.02] | 66 | not supported |
| accuracy | Median response time (s) | -0.05 | [-0.29, 0.20] | 66 | not supported |
| accuracy | Distraction share | 0.05 | [-0.20, 0.28] | 66 | not supported |
| accuracy | Session length (min) | 0.02 | [-0.22, 0.27] | 66 | not supported |
| accuracy | Pace (reviews per active min) | 0.05 | [-0.21, 0.28] | 66 | not supported |
| lapseCount | Correct share | -0.05 | [-0.28, 0.19] | 66 | not supported |
| lapseCount | Surprise | -0.01 | [-0.23, 0.21] | 66 | not supported |
| lapseCount | Median response time (s) | 0.04 | [-0.19, 0.28] | 66 | not supported |
| lapseCount | Distraction share | -0.02 | [-0.27, 0.22] | 66 | not supported |
| lapseCount | Session length (min) | 0.12 | [-0.11, 0.36] | 66 | not supported |
| lapseCount | Pace (reviews per active min) | -0.25 | [-0.50, 0.00] | 66 | not supported |
| variability | Correct share | -0.14 | [-0.38, 0.11] | 65 | not supported |
| variability | Surprise | -0.14 | [-0.40, 0.13] | 65 | not supported |
| variability | Median response time (s) | 0.04 | [-0.18, 0.29] | 65 | not supported |
| variability | Distraction share | -0.21 | [-0.42, 0.02] | 65 | not supported |
| variability | Session length (min) | -0.03 | [-0.27, 0.22] | 65 | not supported |
| variability | Pace (reviews per active min) | -0.10 | [-0.34, 0.15] | 65 | not supported |

### Gain over what the owner already knows

Each cell is the mean absolute error (in the recall measure's unit) of predicting a day's recall after the probe from all other days by least squares (leave one day out). Typical day: the overall mean, and the mean adjusted by weekday and by the probe's completion hour band (the bands of the Inventory); the typical-day baseline is whichever of the two errs less, and "Typical day + probe" adds the probe measure to that one. Yesterday: a linear prediction from the previous paired day's value of the same measure; "Yesterday + probe" adds the probe measure. The probe gains over a baseline when adding it lowers that baseline's error. Verdict: a supported relation reads "gain over both baselines" or names the baseline it does not beat; any other row keeps its relation verdict and adds the gain as information.

Sample: the 65 days of the after-probe relation table. A row's n counts the days where the recall measure, its previous paired day's value, and the probe measure all have a value, so the first paired day never counts; every error in a row is over the same days.

| Probe measure | Recall measure | n days | Overall mean | Weekday & time | Typical day + probe | Yesterday | Yesterday + probe | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| speed | Correct share | 64 | 0.0326 | 0.0408 | 0.0331 | 0.0330 | 0.0335 | not supported; no gain |
| speed | Surprise | 64 | 0.981 | 1.09 | 1.00 | 0.993 | 1.01 | not supported; no gain |
| speed | Median response time (s) | 64 | 2.07 | 1.94 | 1.94 | 1.69 | 1.72 | not supported; no gain |
| speed | Distraction share | 64 | 0.133 | 0.142 | 0.133 | 0.134 | 0.134 | not supported; no gain |
| speed | Session length (min) | 64 | 53.5 | 59.4 | 54.4 | 54.9 | 55.8 | not supported; no gain |
| speed | Pace (reviews per active min) | 64 | 0.327 | 0.357 | 0.333 | 0.334 | 0.341 | not supported; no gain |
| accuracy | Correct share | 64 | 0.0326 | 0.0408 | 0.0327 | 0.0330 | 0.0334 | not supported; no gain |
| accuracy | Surprise | 64 | 0.981 | 1.09 | 0.982 | 0.993 | 0.994 | not supported; no gain |
| accuracy | Median response time (s) | 64 | 2.07 | 1.94 | 1.98 | 1.69 | 1.70 | not supported; no gain |
| accuracy | Distraction share | 64 | 0.133 | 0.142 | 0.135 | 0.134 | 0.136 | not supported; no gain |
| accuracy | Session length (min) | 64 | 53.5 | 59.4 | 53.3 | 54.9 | 54.4 | not supported; gain over both baselines |
| accuracy | Pace (reviews per active min) | 64 | 0.327 | 0.357 | 0.336 | 0.334 | 0.342 | not supported; no gain |
| lapseCount | Correct share | 64 | 0.0326 | 0.0408 | 0.0330 | 0.0330 | 0.0335 | not supported; no gain |
| lapseCount | Surprise | 64 | 0.981 | 1.09 | 0.994 | 0.993 | 1.01 | not supported; no gain |
| lapseCount | Median response time (s) | 64 | 2.07 | 1.94 | 1.98 | 1.69 | 1.68 | not supported; gain over yesterday's recall only |
| lapseCount | Distraction share | 64 | 0.133 | 0.142 | 0.135 | 0.134 | 0.136 | not supported; no gain |
| lapseCount | Session length (min) | 64 | 53.5 | 59.4 | 54.2 | 54.9 | 56.0 | not supported; no gain |
| lapseCount | Pace (reviews per active min) | 64 | 0.327 | 0.357 | 0.321 | 0.334 | 0.331 | not supported; gain over both baselines |
| variability | Correct share | 64 | 0.0326 | 0.0408 | 0.0333 | 0.0330 | 0.0336 | not supported; no gain |
| variability | Surprise | 64 | 0.981 | 1.09 | 1.01 | 0.993 | 1.02 | not supported; no gain |
| variability | Median response time (s) | 64 | 2.07 | 1.94 | 1.96 | 1.69 | 1.71 | not supported; no gain |
| variability | Distraction share | 64 | 0.133 | 0.142 | 0.129 | 0.134 | 0.130 | not supported; gain over both baselines |
| variability | Session length (min) | 64 | 53.5 | 59.4 | 55.4 | 54.9 | 56.3 | not supported; no gain |
| variability | Pace (reviews per active min) | 64 | 0.327 | 0.357 | 0.326 | 0.334 | 0.335 | not supported; gain over the typical day only |

## Recall-session efficiency

Period: 2026-08-29 to 2026-11-03, from the first probe day on, where every answer carries its timing fields (`thinkingTimeMs` missing on 0 of 7318 (0%) answered prompts in the period); before it most answers lack them, so no all-time figures are given. Not related to the probe here. Each cell is median (first quartile–third quartile); measures, sessions and positions are defined under Method and limits.

Sample: 67 days with at least one review; 349 sessions on them, 0 of which have no review (only `JUST_REVIEW` or excluded answers) and are left out of the per-session rows; 7317 reviews. Distraction share clipped at 1 on 0 days and 1 session.

### Overall

| Unit | n | Median response time (s) | Distraction share | Session length (min) | Pace (reviews per active min) |
| --- | --- | --- | --- | --- | --- |
| per day (n days) | 67 | 17.9 (16.2–19.9) | 0.141 (0.0694–0.249) | 140 (94.8–179) | 1.03 (0.812–1.34) |
| per session (n sessions) | 349 | 17.9 (15.3–21.4) | 0.0349 (0–0.167) | 12.2 (3.52–37.6) | 1.64 (0.848–2.42) |

Sessions per day: 5 (4–6).

### By weekday (per day, n days)

| Weekday | n days | Median response time (s) | Distraction share | Session length (min) | Pace (reviews per active min) |
| --- | --- | --- | --- | --- | --- |
| Monday | 10 | 18.3 (17.9–18.6) | 0.0641 (0.039–0.149) | 116 (77.9–147) | 1.24 (1.03–1.48) |
| Tuesday | 10 | 17.9 (16.5–20) | 0.201 (0.0957–0.259) | 175 (124–192) | 0.956 (0.845–1.19) |
| Wednesday | 9 | 17.1 (15.8–19.6) | 0.127 (0.0747–0.222) | 115 (95.8–174) | 0.858 (0.829–1.18) |
| Thursday | 9 | 16.9 (16–17.3) | 0.17 (0.0476–0.373) | 138 (113–143) | 1.11 (0.871–1.18) |
| Friday | 9 | 19.9 (16.4–22) | 0.137 (0.0926–0.162) | 119 (84.3–151) | 1.03 (0.961–1.48) |
| Saturday | 10 | 19.7 (16.6–20.1) | 0.173 (0.093–0.267) | 155 (118–190) | 0.796 (0.721–1.24) |
| Sunday | 10 | 17.6 (17.3–18.4) | 0.167 (0.133–0.295) | 157 (101–211) | 0.949 (0.678–1.29) |

### By session start time (per session, n sessions)

| Session start | n sessions | Median response time (s) | Distraction share | Session length (min) | Pace (reviews per active min) |
| --- | --- | --- | --- | --- | --- |
| 00:00–05:59 | 34 | 20.4 (17.8–25.1) | 0.0911 (0.0419–0.177) | 11 (6.71–67.3) | 1.61 (1.04–2.07) |
| 06:00–08:59 | 91 | 18.2 (16–20.7) | 0.0569 (0.00157–0.122) | 24 (6.35–53.2) | 1.52 (0.897–2.26) |
| 09:00–11:59 | 44 | 18.8 (15.4–24.1) | 0.00816 (0–0.233) | 21 (3.76–31.6) | 1.64 (0.811–2.35) |
| 12:00–14:59 | 30 | 16.7 (13.5–21.7) | 0.0292 (0–0.377) | 7.2 (2.86–35.9) | 1.69 (0.91–2.64) |
| 15:00–17:59 | 38 | 17.5 (13.3–22.6) | 0 (0–0.195) | 3.63 (2.47–21.7) | 2.08 (1.07–2.73) |
| 18:00–20:59 | 75 | 17 (14.4–19.7) | 0.00177 (0–0.0983) | 8.57 (2.12–31.2) | 1.69 (0.649–2.84) |
| 21:00–23:59 | 37 | 16.3 (13.8–18.9) | 0.0333 (0–0.161) | 19.5 (3.12–34.1) | 1.39 (0.794–2.67) |

### By position in the session (per review, n reviews)

Position applies to the per-review measures only: response time and the review's own distraction (`awayMs + detourMs + idleMs` over its `thinkingTimeMs + awayMs + detourMs`). Session length and pace belong to a whole session and have no position.

| Position | n reviews | Response time (s) | Distraction share of review time | Same, without the 10 most distracted reviews | Reviews with any distraction |
| --- | --- | --- | --- | --- | --- |
| First 10 reviews of a session | 2927 | 18.4 (11.8–29.4) | 0.550 | 0.346 | 272 of 2927 (9%) |
| Later reviews | 4390 | 17.9 (11.7–28.1) | 0.322 | 0.267 | 418 of 4390 (10%) |

The distraction share of review time is a ratio of sums, driven by a few long absences: the 10 most distracted reviews of a position hold 58% for first 10 reviews of a session (longest 186 min) and 24% for later reviews (longest 31 min) of its distracted time.

Paired by day: later reviews have a higher median response time than the first 10 on 30 of 66 (45%) days with both.

## Repetition efficiency

Not related to the probe here. Calibration compares the scheduler's raw `retrievability` with the share correct, without the recall stats tiles' per-type recalibration; with thousands of reviews per band even a small gap leaves the interval, so the size of observed − predicted matters more than the judgment. Bands, too-early and too-late reviews are defined under Method and limits.

### From the first probe day on

Period: 2026-08-29 to 2026-11-03. Sample: 7317 reviews; `retrievability` missing on 38 of 7317 (1%); of the 7279 with it, 144 are neither correct nor wrong and stay out of the calibration, leaving 7135. Grades: `GOOD` 6202, `AGAIN` 971, `CONFUSION` 144.

Calibration by retrievability band:

| Retrievability | n | Predicted (mean retrievability) | Observed correct share | 95% interval | Observed − predicted | Judgment |
| --- | --- | --- | --- | --- | --- | --- |
| 0.00–<0.50 | 486 | 0.443 | 0.798 | 0.760–0.833 | +0.355 | under-predicts |
| 0.50–<0.70 | 327 | 0.597 | 0.801 | 0.754–0.843 | +0.204 | under-predicts |
| 0.70–<0.80 | 601 | 0.752 | 0.859 | 0.828–0.885 | +0.107 | under-predicts |
| 0.80–<0.90 | 5139 | 0.883 | 0.876 | 0.866–0.885 | -0.008 | matched |
| 0.90–<0.95 | 217 | 0.904 | 0.876 | 0.824–0.916 | -0.028 | matched |
| 0.95–1.00 | 365 | 0.983 | 0.855 | 0.814–0.889 | -0.128 | over-predicts |
| all | 7135 | 0.835 | 0.864 | 0.856–0.872 | +0.030 | under-predicts |

Calibration by question type (all bands):

| Question type | n | Predicted (mean retrievability) | Observed correct share | 95% interval | Observed − predicted | Judgment |
| --- | --- | --- | --- | --- | --- | --- |
| (none) | 337 | 0.738 | 0.751 | 0.701–0.796 | +0.012 | matched |
| MCQ | 5170 | 0.837 | 0.883 | 0.874–0.892 | +0.047 | under-predicts |
| SPELLING | 1628 | 0.849 | 0.828 | 0.809–0.846 | -0.021 | over-predicts |

- Too early (`GOOD` at `retrievability` ≥ 0.95): 312 of 7279 (4%) reviews with a `retrievability`. Graded `EASY` at that level: 0.
- Too late (`AGAIN` at `retrievability` < 0.7): 163 of 7279 (2%) reviews with a `retrievability`; that is 163 of 967 (17%) `AGAIN` reviews with a `retrievability`.

`elapsedHours` by grade, median (first quartile–third quartile):

| Grade | n reviews | elapsedHours |
| --- | --- | --- |
| AGAIN | 971 | 650.0 (142.0–2946.0) |
| CONFUSION | 144 | 261.0 (50.8–1247.5) |
| GOOD | 6202 | 568.0 (144.0–1956.0) |

### All time

Period: 2023-07-24 to 2026-11-03. Sample: 133763 reviews; `retrievability` missing on 14635 of 133763 (11%); of the 119128 with it, 152 are neither correct nor wrong and stay out of the calibration, leaving 118976. Grades: `GOOD` 122043, `AGAIN` 11565, `CONFUSION` 155.

Calibration by retrievability band:

| Retrievability | n | Predicted (mean retrievability) | Observed correct share | 95% interval | Observed − predicted | Judgment |
| --- | --- | --- | --- | --- | --- | --- |
| 0.00–<0.50 | 4880 | 0.451 | 0.856 | 0.846–0.866 | +0.405 | under-predicts |
| 0.50–<0.70 | 4708 | 0.603 | 0.862 | 0.852–0.872 | +0.260 | under-predicts |
| 0.70–<0.80 | 7361 | 0.755 | 0.847 | 0.839–0.855 | +0.092 | under-predicts |
| 0.80–<0.90 | 25198 | 0.867 | 0.865 | 0.861–0.870 | -0.001 | matched |
| 0.90–<0.95 | 21258 | 0.927 | 0.905 | 0.901–0.908 | -0.023 | over-predicts |
| 0.95–1.00 | 55571 | 0.979 | 0.953 | 0.951–0.955 | -0.026 | over-predicts |
| all | 118976 | 0.895 | 0.912 | 0.910–0.913 | +0.016 | under-predicts |

Calibration by question type (all bands):

| Question type | n | Predicted (mean retrievability) | Observed correct share | 95% interval | Observed − predicted | Judgment |
| --- | --- | --- | --- | --- | --- | --- |
| (none) | 1361 | 0.774 | 0.822 | 0.801–0.842 | +0.048 | under-predicts |
| MCQ | 109442 | 0.898 | 0.916 | 0.914–0.917 | +0.017 | under-predicts |
| SPELLING | 8173 | 0.877 | 0.873 | 0.866–0.880 | -0.004 | matched |

- Too early (`GOOD` at `retrievability` ≥ 0.95): 52966 of 119128 (44%) reviews with a `retrievability`. Graded `EASY` at that level: 0.
- Too late (`AGAIN` at `retrievability` < 0.7): 1352 of 119128 (1%) reviews with a `retrievability`; that is 1352 of 10502 (13%) `AGAIN` reviews with a `retrievability`.

`elapsedHours` by grade, median (first quartile–third quartile):

| Grade | n reviews | elapsedHours |
| --- | --- | --- |
| AGAIN | 11565 | 195.0 (24.0–1821.0) |
| CONFUSION | 155 | 254.0 (49.5–1265.0) |
| GOOD | 122043 | 119.0 (23.0–924.0) |

## Findings

Short answers:

- **Does the daily probe result relate to that day's recall?** Not that this data can show. Over 65 usable days, none of the 24 pairs of probe measure and recall measure has a relation that clearly differs from none, and adding the probe barely improves a prediction of the day's recall. The data cannot yet rule out a moderate relation either: 65 days are too few to detect one of size 0.3 reliably (Findings 1–3).
- **How efficient are the owner's recall sessions and spaced repetition?** A typical day has about 5 sessions and 140 minutes of recall at about 18 seconds per answer and about 1 review per active minute; about a seventh of a day's session time is distraction. Answer speed hardly changes with weekday, time of day, or position in a session. The scheduler's raw retrievability is right where most reviews sit (0.80–0.95) but too pessimistic below 0.80 and too optimistic at 0.95 and above; few reviews look too early or too late in the probe period (Findings 4–8).
- **Which probe changes would make it more useful?** None of the changes to what the probe shows or when it is offered rests on evidence yet. The first change is to keep collecting until 85 usable days and then rerun this analysis, with the owner's usual timezone; the other proposals either check the probe's own reliability first or need the owner's ADR 0003 decision (Proposals).

Numbered findings:

1. **No probe measure relates to that day's recall after the probe.** All 24 rows read "not supported"; |rho| is at most 0.23 and every 95% interval includes 0, with intervals about ±0.25 wide. Section "That day's recall after the probe"; n 64 (speed, variability rows) or 65 (accuracy, lapseCount rows) days.
2. **The all-day sensitivity check agrees.** All 24 rows read "not supported"; the largest is lapseCount × pace −0.25 [−0.50, 0.00], whose interval reaches 0. Section "Sensitivity: all of the probe day's reviews"; n 65 or 66 days.
3. **The probe adds no useful prediction over what the owner already knows.** The probe lowers a baseline's leave-one-day-out error on 5 of 24 rows, by at most about 3% (variability × distraction share 0.133 → 0.129; the others 0.3%–2%). The comparison has no uncertainty and no gain threshold, so these small drops are treated as noise, not as gains. The weekday-and-time baseline errs more than the overall mean on 5 of 6 measures, and yesterday's value beats the mean only for median response time (1.69 s against 2.07 s). Section "Gain over what the owner already knows"; n 64 days per row.
   - Watch items only (no proposal rests on them alone): the after-probe rows whose intervals end within 0.05 of 0 — accuracy × correct share −0.22 [−0.43, 0.01], accuracy × surprise −0.23 [−0.45, 0.02], lapseCount × pace −0.23 [−0.48, 0.03], variability × distraction share −0.21 [−0.43, 0.04] (n 64–65 days) — and the 5 rows with small error drops above. With 24 rows tested, about 1 row could leave its interval by chance alone at a 5% error rate per row, so a single row near the boundary is weak evidence. Two of the watch rows run against expectation (higher probe accuracy with lower recall correct share and surprise), which also argues for caution.
4. **Recall days are long and split into several sessions.** Per day: 5 (4–6) sessions, session length 140 (94.8–179) min, median response time 17.9 (16.2–19.9) s, distraction share 0.141 (0.0694–0.249), pace 1.03 (0.812–1.34) reviews per active minute. Per session, distraction is lower, 0.0349 (0–0.167), because a day's share is dominated by its long sessions. Section "Recall-session efficiency", table "Overall"; n 67 days, 349 sessions, 7317 reviews.
5. **Answer speed hardly moves with weekday or time of day; session length and distraction move more.** Weekday median response time ranges 16.9–19.9 s, while session length ranges 115–175 min and distraction share 0.0641 (Monday) to 0.201 (Tuesday); each weekday has 9–10 days and no interval, so the weekday differences are descriptive only. Sessions started 06:00–08:59 are the most common (91) and the longest (median 24 min); afternoon sessions are short with the highest pace (15:00–17:59: 3.63 min, 2.08 reviews per active minute, n 38). Sections "By weekday" (n 9–10 days each) and "By session start time" (n 30–91 sessions each).
6. **There is no warm-up or tiring effect on answer speed within a session.** First 10 reviews 18.4 s against later reviews 17.9 s, and later reviews are slower than the first 10 on only 30 of 66 (45%) days with both. The first 10 reviews show a larger distraction share of review time (0.550 against 0.322), but a few long absences drive it: the 10 most distracted first-10 reviews hold 58% of that position's distracted time (longest 186 min); without them the shares are 0.346 and 0.267, and the share of reviews with any distraction is about the same (9% against 10%). Section "By position in the session"; n 2927 first-10 and 4390 later reviews.
7. **Raw retrievability is well calibrated where most reviews sit, and off at both ends.** In the probe period the 0.80–<0.90 band (5139 of 7135 reviews) matches (observed − predicted −0.008) and 0.90–<0.95 matches (−0.028, n 217); below 0.80 the scheduler under-predicts (+0.355, +0.204, +0.107; n 486, 327, 601), and at 0.95 and above it over-predicts (−0.128, n 365). All time shows the same shape, except that 0.90–<0.95 also over-predicts (−0.023, n 21258; all bands n 118976). By question type, MCQ is under-predicted (+0.047, n 5170) and SPELLING over-predicted (−0.021, n 1628). Section "Repetition efficiency", calibration tables; n as given.
8. **Few reviews look too early or too late in the probe period, and misses do not cluster at low retrievability.** Too early (`GOOD` at retrievability ≥ 0.95): 312 of 7279 (4%), against 52966 of 119128 (44%) all time. Too late (`AGAIN` below 0.7): 163 of 7279 (2%); only 17% of `AGAIN` reviews sit there (13% all time). Median `elapsedHours` is similar for `AGAIN` (650) and `GOOD` (568), with wide quartiles. No review is graded `EASY`, so `GOOD` stands in for the easy grade. Section "Repetition efficiency", too-early, too-late and `elapsedHours` lines; n 7279 reviews with a retrievability (probe period), 119128 (all time).
9. **Not every probe is an early-morning probe, and some times may be shifted.** 61 of 66 probes were completed before 09:00 and 3 of 66 (5%) at 12:00 or later; 27 of 66 were completed before 06:00 local time. The export used Asia/Jakarta, this machine's zone; if the owner's usual zone is one hour ahead of it, those early probes are an hour later in the owner's own day. That is a possibility, not an established fact. Section "Probe completion time of day"; n 66 probe days.

Limits that bear on these findings: every timestamp is read in one timezone (Asia/Jakarta), so days spent elsewhere are cut at the wrong midnight (Findings 1–3, 5, 9); surprise and calibration use raw retrievability, not the recall stats tiles' per-type recalibration, so the surprise measure in Findings 1–3 rests on a prediction that Finding 7 shows is off at the ends; the calibration interval treats every review as independent and ignores that a note is reviewed many times, so it is somewhat too narrow and the "matched / over / under" judgments are somewhat too eager (Finding 7).

## Proposals

Each proposal names its findings, the probe change it implies, and its status. Product changes, an in-product analytics tile, and a prospective experiment with changed probe settings are deferred from this story; the proposals below name them only as candidate stories.

1. **Keep collecting until 85 usable days, then rerun this analysis.** Rests on Findings 1–3. Probe change: none; keep taking the probe daily with the current settings. 85 usable days detect |rho| = 0.3 at 80% power (two-sided alpha 0.05; section "Probe and same-day recall"). There are 65 usable days, so about 20 more are needed; at the observed rate of about one usable day per calendar day (65 of the 67 days from 2026-08-29 to 2026-11-03), that is around 2026-11-23, later if days are missed. Why 0.3 is the smallest effect worth acting on: at 65 days the intervals are already about ±0.25 wide, so 0.3 is about the smallest relation this data could tell from none; a smaller relation such as 0.2 would need 194 days by the same formula (about four more months) and 0.25 would need 124, and a relation that weak would rank days too loosely to change what the owner does on a given morning — the probe currently improves predictions by at most about 3% (Finding 3). To keep chance results from deciding, the rerun should look first at the watch rows of Finding 3 and treat any other single row that leaves its interval with the same caution. Status: ready to queue as a story (an analysis rerun, no product change).
2. **Rerun with the owner's usual timezone and recheck the probe's time of day.** Rests on Finding 9 (and Findings 1–3, whose day cuts depend on the zone). Probe change: none until checked; if probes before 06:00 are real, the offer timing stays as it is. This fits in the same rerun as Proposal 1. Status: ready to queue as a story (can be part of Proposal 1's story).
3. **Measure the probe's own day-to-day reliability before changing its length.** Rests on Findings 1–3: a 20-trial probe may vary too much from day to day to relate to anything, but this report did not measure that. Probe change: none yet; the result would decide whether a longer probe is worth trying (Proposal 4). The export carries the per-trial results needed to split each day's 20 trials in two: every probe has 20 trials, and 63 of 66 probes have a response time on all 20 (1 has none, 2 have 14 and 18). Status: ready to queue as a story (an analysis, no product change).
4. **A longer probe, tried as a prospective experiment.** Rests on Findings 1–3 and on Proposal 3 finding the probe too noisy. Probe change: more scored trials. Conflicts with ADR 0003 — owner's ADR decision: ADR 0003 defines the Daily probe as a task of about 60 seconds, so a clearly longer probe changes that definition. This is a candidate story from the deferred prospective experiment, not ready to queue.
5. **Show the probe result beside that morning's Pace, Accuracy and Consistency, as a check on them.** Rests on Findings 1–3, which do not support it: no probe measure relates to that day's recall. Probe change: pairing with recall feedback, on the recall stats page or as an in-product tile (both deferred). Conflicts with ADR 0003 — owner's ADR decision: ADR 0003 says the probe does not validate Pace, Accuracy, or Consistency. Not ready to queue.
6. **Use the probe to adjust the day's recall load or schedule.** Rests on Findings 1–3, which do not support it. Probe change: the probe becomes a scheduling input. Conflicts with ADR 0003 — owner's ADR decision: ADR 0003 says the probe is not a scheduling input. Not ready to queue.

No proposal rests on the watch rows or on the small error drops of Finding 3 alone. Findings 4–8 describe recall-session and repetition efficiency and imply no probe change; open question for the owner: whether the calibration gaps of Finding 7 (pessimistic below 0.80, optimistic at 0.95 and above, SPELLING over-predicted) should become a separate story outside the probe, which would sit in ADR 0003's FSRS profile.

Open question for the owner: which timezone is your usual one, so the rerun in Proposals 1–2 exports with it?

The owner decides which of these proposals to queue as stories.

## Method and limits

- Input: the owner's own export from `GET /api/user/recall-history-export?timezone=Asia/Jakarta`, fetched 2026-11-03 with the owner's token. Only the owner's data is analysed.
- Export: `exportedAt` 2026-11-03T21:27:06.183+07:00, `timezone` Asia/Jakarta.
- Every timestamp is read in that one timezone; Donut stores no per-event zone, so days spent in another zone are cut at the wrong midnight.
- Local day: the calendar date of the timestamp in the export's timezone. An answered prompt's day is that of `answer.createdAt`; a probe's day is that of `completedAt`.
- Review: an event of kind `ANSWERED_RECALL_PROMPT` whose answer's outcome is not `OVERLAP` and whose `excludedAt` is empty (as the product's `RecallAnswerRow`). `OVERLAP` answers and excluded answers are not reviews.
- Correct: grade `GOOD` or outcome `OVERLAP`; wrong: grade `AGAIN`; `HARD`, `EASY`, `CONFUSION` are neither and stay out of correctness measures (as `Answer.correctFrom`).
- After the probe: `answer.createdAt` later than the probe's `completedAt` on the same local day.
- Surprise for a set of reviews with a `retrievability`: `sum(y - p) / sqrt(sum(p (1 - p)))`, `y` 1 for correct and 0 for wrong, `p` the raw `retrievability`; the recall stats tiles additionally recalibrate `p` per question type, this report does not.
- Response time: `answer.thinkingTimeMs`, else `answer.createdAt - answer.promptCreatedAt` when positive; capped at 120 s (thinking time) or 300 s (timestamp difference); dropped below 1 s.
- Activity: answered prompts (reviews or not) and `JUST_REVIEW` events. An answered prompt ends at `answer.createdAt` and starts `thinkingTimeMs + awayMs + detourMs` before it, because away and detour pause the thinking-time clock while idle time stays inside it; a `JUST_REVIEW` is a moment at `recordedAt`. `answer.promptCreatedAt` does not mark a start: prompts are generated ahead, often days before they are answered.
- Sessions: a local day's activity is split where an event starts more than 30 minutes after the previous one ended; a session runs from its first event's start to its last event's end.
- Session length: the sum of the day's session spans; in the relation after the probe, only the part of each span after the probe's `completedAt`.
- Distraction share: the reviews' `awayMs + detourMs + idleMs` over the session length, clipped at 1 (the relation section states on how many days).
- Pace: reviews per active minute, active being session length minus the reviews' away, detour and idle time; a day without active time has no pace.
- Median response time: the median of the day's reviews' response times.
- Correct share: correct over correct plus wrong among the day's reviews.
- A day enters the relation only with at least 10 reviews after its probe (in the all-day sensitivity variant, 10 reviews that day).
- Strength: Spearman rho over days with a 95% percentile bootstrap interval (2000 resamples of days, fixed seed 20261103, so the report regenerates identically). Days needed: `(z(0.975) + z(0.8))^2 / atanh(0.3)^2 + 3`, the Fisher-z approximation.
- Baselines: each after-probe recall measure is predicted for every day from all other days by least squares (leave one day out) and scored by mean absolute error. Typical day: the overall mean, or the mean adjusted by weekday and by the probe's completion hour band, whichever errs less. Yesterday: a linear prediction from the previous paired day's value of the same measure. The probe gains over a baseline when adding the probe measure to it lowers that error; a row's days are those with the measure, its previous paired day's value, and the probe measure.
- Recall-session efficiency: from the first probe day on, the same measures per day (days with at least one review) and per session (sessions with at least one review), grouped by weekday (per day) and by the hour band of the session's first event start (per session). Position: a review's rank by `answer.createdAt` among its session's reviews; the first 10 are "first", the rest "later". A review's own distraction share is `awayMs + detourMs + idleMs` over `thinkingTimeMs + awayMs + detourMs`, summed over the reviews of a position; the companion column sums without the position's 10 most distracted reviews.
- Repetition efficiency: reviews only, in two periods (from the first probe day on, and all time). Calibration: reviews with a `retrievability` and a correctness (correct or wrong) are grouped by `retrievability` band (lower bound included, the top band includes 1); predicted is the band's mean `retrievability`, observed its correct share with an exact (Clopper-Pearson) binomial 95% interval. Judgment: "matched" when the interval contains the predicted mean, "over-predicts" when the interval lies below it, "under-predicts" when above. Too early: grade `GOOD` at `retrievability` 0.95 or more (the story's "easy grade"; no review in the export is graded `EASY`, so `GOOD` stands in for it; the report also states the `EASY` count). Too late: grade `AGAIN` at `retrievability` below 0.7. Both shares are over the reviews with a `retrievability`. Elapsed time: `elapsedHours` by grade, over all reviews of the period.
- Aggregates only: no note ids, note titles, tutor feedback, or exclusion reason text.
- Evidence: commit `e82f6044efbf3043de419ee2c3ad59467ffe9e5d` holds the scripts at `.planning/slice-plans/002-morning-probe-and-recall-analysis/analysis/`; read one with `git show e82f6044:<path>`. They run with system Python 3 (pandas, numpy, scipy) and are outside any product build or lint.
