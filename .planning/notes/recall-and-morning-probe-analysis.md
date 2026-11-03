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

Pending.

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
- A day enters the relation only with at least 10 reviews after its probe.
- Aggregates only: no note ids, note titles, tutor feedback, or exclusion reason text.
- Scripts: `analysis/` in `.planning/slice-plans/002-morning-probe-and-recall-analysis`, recoverable with `git log --all -- .planning/slice-plans/002-morning-probe-and-recall-analysis`. They run with system Python 3 (pandas, numpy, scipy) and are outside any product build or lint.
