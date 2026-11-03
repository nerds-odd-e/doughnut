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

## Recall-session efficiency

Pending.

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
- Aggregates only: no note ids, note titles, tutor feedback, or exclusion reason text.
- Scripts: `analysis/` in `.planning/slice-plans/002-morning-probe-and-recall-analysis`, recoverable with `git log --all -- .planning/slice-plans/002-morning-probe-and-recall-analysis`. They run with system Python 3 (pandas, numpy, scipy) and are outside any product build or lint.
