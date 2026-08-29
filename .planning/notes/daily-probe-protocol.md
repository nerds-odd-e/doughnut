---
title: Daily probe trial and scoring protocol
date: 2026-08-29
context: Product name stays in ADR 0001 / 0003; this note owns measurement.
---

# Daily probe trial and scoring protocol

A two-choice left/right reaction task. Same trials every day. About a minute.
Accuracy is a real readout (simple RT would not give one). Speed and lapses
follow the usual PVT-style numbers without copying PVT’s length or random ISI.

Do not put this spec in an ADR.

## The six holes

### 1. Stimulus set and order

Each trial shows **left** or **right**.

**Practice** (unscored, not stored), always:

`left, right, right, left`

**Scored** (always this order, 10 left / 10 right, no run longer than 2):

`left, right, left, left, right, right, left, right, left, right, right, left, left, right, left, right, right, left, right, left`

Identifiers in stored trials are the strings `left` and `right`.

### 2. Response mapping

| Stimulus | Keys |
|----------|------|
| left | `f`, `F`, `ArrowLeft` |
| right | `j`, `J`, `ArrowRight` |

Any other key is ignored. Keys during the blank ISI are ignored. Only the
first mapped key after stimulus onset counts.

### 3. Practice and instruction

One line before the first practice trial:

> Each trial shows ← or →. Press F for left, J for right (arrow keys also work). Go as fast as you can without mistakes.

Then the four practice trials, same pacing as scored. No extra “ready”
screen. After the last practice ISI, scored trials start. The UI may label
practice vs scored; scoring does not use practice trials.

### 4. Trial pacing

- Stimulus stays until a mapped key or **2000 ms** timeout.
- Then a **2000 ms** blank ISI, then the next stimulus.
- Clock for a trial starts at stimulus onset and stops at the first mapped
  key (or timeout). Tests pass timestamps in; they do not wait.

Twenty scored trials plus four practice at this pacing land near one minute.

### 5. Exact trial count

**4 practice + 20 scored.** A run is complete only when scored trial 20 has
an outcome (key or timeout).

### 6. Formulas and units

**RT (ms)** = response time − stimulus onset. Timeout: no RT.

**Valid RT:** 100 ≤ RT < 2000. Faster than 100 ms is a false start (incorrect,
no RT). Timeout is incorrect, no RT.

**Correct:** mapped key matches the stimulus, and the RT is valid.

**Speed (mean reciprocal RT):** mean of `1 / (RT / 1000)` over **correct
valid** scored trials. Unit **s⁻¹**. Display **2 decimal places**. If none,
omit speed (do not show 0).

Worked example: correct 250 ms and 500 ms → `1/0.25` and `1/0.50` → mean
**3.00** s⁻¹. A wrong 250 ms trial does not enter the mean.

**Accuracy:** `round(100 × correctCount / 20)` percent. Timeouts and false
starts are incorrect. Integer percent.

**Lapse:** scored trial with **RT ≥ 500 ms** or **timeout**. False starts are
not lapses. A slow error is both an error and a lapse. Unit: count 0–20.

**Variability:** sample standard deviation (`n − 1`) of the same reciprocal
RTs that enter speed. Unit **s⁻¹**. Display **2 decimal places**. If fewer
than 2 correct valid RTs, omit variability.

Worked example: reciprocal RTs 4.00 and 2.00 → variability **1.41** s⁻¹.

Each stored scored trial keeps enough to recompute every summary: stimulus,
response (`left` / `right` / omitted on timeout), RT in ms (omitted on
timeout or false start), whether correct.

## Daily offer

Consumed **only when the 20 scored trials are complete**. Abandoning
(navigate away, close, stop mid-run) **writes nothing**. The same local day
offers the probe again until one run completes. Persistence is completed rows
only — no incomplete status.
