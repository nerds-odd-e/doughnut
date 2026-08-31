# Daily probe tap affordance

**Status:** in progress (slice 1 done).
**Type:** ad-hoc plan (`.planning/quick/`)
**Measurement spec:** [daily-probe-protocol.md](../../notes/daily-probe-protocol.md)

## Goal

A learner on a phone can **see where to tap**, **tap a large side panel**,
and **know the tap registered**, without the instruction jumping when the
arrow appears or blanks. Keyboard F / J / arrows still work. Same scoring,
same trial clock. Zones stay unlabeled (no ←/→, no F/J, no “Left”/“Right”).

## Why the current UI fails

`DailyProbe.vue` centers a stack with `justify-center` and shows the arrow
with `v-if="stimulus"`. When the arrow mounts, the stack grows and recenters,
so the instruction jumps up; during the blank ISI it jumps back down.

The tap targets are two empty `flex-1` divs in a `min-h-24` bottom strip.
The instruction+stimulus column takes `flex-1`, so the strip stays ~96px of
unpainted space. Nothing looks tappable. `pointerdown` blanks the stimulus
immediately, so there is no press feedback.

## Design decisions

- **Keep unlabeled spatial mapping.** Visible **panels**, not glyphs. No
  ←/→, F/J, or “Left”/“Right” on the zones (that would turn the trial into
  identity match). Stimulus stays ←/→. Instruction string and protocol copy
  stay as they are unless a later discovery requires it.
- **Stable board, large panels.** Instruction sits in a **fixed upper
  block** (not a growing `justify-center` stack). Stimulus uses a **reserved
  slot** of the arrow’s height: arrow text when a trial is live, empty during
  ISI. The two tap panels **fill the remaining height** (`flex-1`, keep a
  `min-h-24` floor) with theme-neutral fill (`bg-base-200`) and a split.
  One path for phone, tablet, and desktop. No `pointer: coarse` / UA split.
- **Press flash is chrome, not scoring.** On a mapped response (tap **or**
  key), the recorded side gets a short highlight (~200ms, CSS class). It
  must **not** delay ISI, change RT, or synthesize keys. Timeout (no
  response) does not flash. `pointerdown` stays the tap clock.
- **No backend/API change.** Existing keyboard and tap-complete E2E stay.
- **Capability names** in product/tests: Daily probe. Slice numbers stay in
  this PLAN only.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. See unlabeled tap panels — Behavior `[x]`

Shipped: zones use `bg-base-200` with `divide-x divide-base-300`; still
unlabeled; still `min-h-24`. `DailyProbe.spec.ts` covers fill, split, and
no ←/→ / Left/Right / F/J on the zones.

---

### 2. Keep the board still; tap panels fill the rest — Behavior `[ ]`

**Pre:** instruction is on screen. **Trigger:** the arrow appears, then
blanks for the ISI. **Post:** instruction does not move; the arrow occupies
a reserved slot (empty, same height, during ISI); the two tap panels fill
the remaining height below that board (large enough that each is more than
the old ~96px sliver in a full-height probe). Scoring, ISI, and unlabeled
rule unchanged.

**Verify:** `DailyProbe.spec.ts` mounted in a known-height wrapper — instruction
vertical position is unchanged across stimulus-on and ISI; stimulus slot
stays in the DOM during ISI without arrow text; each zone height is a large
share of the remaining space. Update the existing ISI assertion that the
stimulus **element** is gone — the slot stays, the arrow does not.
Run `e2e_test/features/recall/daily_probe.feature` if selectors or
`pointerdown` wiring change; otherwise unit tests are enough for this
layout. No protocol change.

---

### 3. Flash the recorded side — Behavior `[ ]`

**Pre:** a stimulus is showing. **Trigger:** `pointerdown` on a zone, or a
mapped key. **Post:** that side panel shows a brief press highlight; the
other side does not; the trial still ends and ISI still runs on the same
clock; a timeout does not flash.

**Verify:** `DailyProbe.spec.ts` — after a left tap (and after `f`), the
left zone has the press class and the right does not; after ~200ms of fake
timers the class is gone. Existing complete-by-tap and keyboard tests still
pass. No E2E scenario for the flash. No protocol change.

## Learnings

Slice 1 did not need a parent height tweak. Slice 2 still owns filling
height and a reserved stimulus slot.

## Jidoka

- Do not put ←/→ (or F/J / “Left” / “Right”) on the tap panels.
- Do not score `click` or delay ISI for the flash.
- Do not change backend DTOs, offer, or abandon.
- If always-visible large panels feel noisy on a desktop keyboard, stop —
  do not add user-agent or `pointer: coarse` splitting in this plan.
- If making the probe `h-full` actually fill RecallPage needs a parent
  height tweak (`DailyProbeGate` / recall page), that is in slice 2, not a
  new plan.

## Out of scope

Swipe, on-screen keyboard, separate touch vs keyboard formulas, changing
trial count or ISI, instruction/protocol wording, hiding panels on
“desktop.”
