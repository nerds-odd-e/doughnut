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

Pending.

## Findings

Pending.

## Proposals

Pending.

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
- Aggregates only: no note ids, note titles, tutor feedback, or exclusion reason text.
- Scripts: `analysis/` in `.planning/slice-plans/002-morning-probe-and-recall-analysis`, recoverable with `git log --all -- .planning/slice-plans/002-morning-probe-and-recall-analysis`. They run with system Python 3 (pandas, numpy, scipy) and are outside any product build or lint.
