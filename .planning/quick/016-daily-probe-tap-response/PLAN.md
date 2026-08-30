# Daily probe side tap

**Status:** in progress (slice 1 done; next: slice 2).
**Type:** ad-hoc plan (`.planning/quick/`)
**Measurement spec:** [daily-probe-protocol.md](../../notes/daily-probe-protocol.md)**

## Goal

A learner can complete the Daily probe on a phone (no physical keyboard) by
tapping **unlabeled left/right zones**, while desktop **F / J / arrow keys
still work**. Same scoring, same trial clock.

## Why this shape

The probe is a two-choice left/right task. Accuracy is a real readout.
Keyboard mapping is spatially compatible (left stimulus → left hand) but
does **not** look like the stimulus.

Tap targets that show ← / → would add **identity match** on top of spatial
match: the trial becomes “tap the matching picture.” That is easier than F/J
and would make phone runs a different task from keyboard runs.

## Design decisions

- **Keep keyboard.** `f`/`F`/`ArrowLeft` → left; `j`/`J`/`ArrowRight` →
  right. Existing keyboard E2E stays. Do not open the OS keyboard.
- **Unlabeled left/right zones**, always visible during a trial (one path for
  phone, tablet, and desktop). Bottom thumb-reach, safe-area padding,
  `touch-action: none`. No ←/→, no on-screen F/J glyphs, no “Left”/“Right”
  labels on the zones.
- **Score a side, not a key.** Keyboard maps to a side at the event layer;
  a tap already is a side. Do not synthesize fake `f`/`j` key events.
- **`pointerdown` is the tap response**, not `click`. Same first-response and
  ISI ignore rules as keys. Window `keydown` stays the keyboard channel; do
  not let Space/Enter on a focused zone fire a side.
- **Same scoring** for tap and key. No device-specific formulas. No
  user-agent “mobile” split.
- **Instruction** names both inputs. Stimulus stays ←/→. Protocol response
  mapping table includes tap as a first-class mapped response.
- **No backend/API change.** Stored trials remain `left` / `right`. Offer,
  abandon, save, Continue/Retry unchanged.
- **Capability names** in product/tests: Daily probe. Slice numbers stay in
  this PLAN only.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Score a mapped side, not a key — Structure `[x]`

`recordDailyProbeTrial` takes optional `response: DailyProbeSide`. Keyboard
still maps via `mapDailyProbeKey` in `DailyProbe`, then records the side.
`finishTrial` already takes a side, so slice 2 can pass tap sides without
synthesizing keys. Scoring and stored trial shape unchanged.

---

### 2. Tap a side zone to answer the current trial — Behavior `[ ]`

**Pre:** Daily probe is showing a stimulus; unlabeled left and right response
zones are on screen (not arrow-labeled). **Trigger:** `pointerdown` on the
left or right zone. **Post:** that trial ends with that side as the response
(same scoring as F/J); stimulus blanks for the ISI. Keyboard still answers
the trial. Taps during the blank ISI do nothing. A second tap on the same
trial is ignored.

**Verify:** `frontend/tests/components/recall/DailyProbe.spec.ts` — pointerdown
on a zone records that side; keyboard still works; zones must not contain
←/→ (stimulus still does).

---

### 3. Complete the Daily probe by tapping — Behavior `[ ]`

**Pre:** opted-in learner on the Daily probe (existing setup). **Trigger:**
answer every practice and scored trial by tapping the matching side zone.
**Post:** same summaries as the keyboard complete path (speed, accuracy,
lapses, variability, Saved); Continue into ordinary recall. Instruction names
tap as well as F/J (arrow keys still mentioned). Protocol hole 2 lists tap
left/right alongside the keys.

**Verify:** new scenario on `e2e_test/features/recall/daily_probe.feature`
(capability-named; not a slice number). Existing “I complete the Daily probe”
keyboard scenario stays. Page object taps zones with `pointerdown`, not
`click`. Amend `.planning/notes/daily-probe-protocol.md` hole 2 and the
instruction line in the same slice.

## Jidoka

- Do not put ←/→ (or F/J) on the tap zones.
- Do not replace the keyboard complete E2E with tap.
- Do not score `click` as the response clock.
- Do not change backend DTOs, offer, or abandon.
- If hybrid devices make always-visible zones feel noisy, stop — do not add
  user-agent or `pointer: coarse` splitting in this plan.

## Out of scope

Swipe, on-screen keyboard, separate touch vs keyboard formulas, forcing
landscape, hiding zones on “desktop,” changing trial count or ISI.
