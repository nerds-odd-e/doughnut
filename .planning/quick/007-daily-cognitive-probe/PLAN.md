# Daily cognitive probe

**Status:** planned, not started. Extracted unbuilt from
`.planning/quick/001-morning-cognitive-index/PLAN.md` (slices 26–31 there),
which is otherwise fully closed: its timer/pace/accuracy channels shipped,
and its composite morning-index effort was dropped after the split-half
reliability gate failed decisively on real production data
(`pairCount: 91`, `rawCorrelation: 0.076`, `spearmanBrownCorrelation: 0.141`,
all far below the ~0.6 threshold). None of this plan's slices were touched
before extraction.
**Type:** ad-hoc plan (`.planning/quick/`)
**Related:** `.planning/quick/001-morning-cognitive-index/PLAN.md` (shipped
component readouts — pace, accuracy, consistency, lapse count — this plan
does not depend on them); `.planning/quick/008-probe-convergent-analyses/PLAN.md`
(analyses that consume this plan's probe once shipped — extracted
separately because they need re-scoping before they're executable).

## Goal

Give the learner an opt-in, ~60-second daily cognitive probe — identical
stimuli every day — as an independent, item-structure-free measurement of
speed, accuracy, and lapses. This was originally motivated as a
convergent-validity criterion for the morning cognitive index (an
independent speeded task with no shared item structure is what makes a
usable criterion). **That composite index no longer exists** (see Status
above), so this plan stands on its own: the probe's own trend (slice 6) is
its deliverable, not a validation instrument for something else. Whether the
probe is later worth correlating against anything is `quick/008`'s question,
not this plan's.

## Key design decisions

- **Identical stimuli every day.** Removes every item confound by
  construction — the whole point of a probe distinct from recall history,
  where item identity, prior exposure, and personal difficulty are all
  confounded with performance.
- **Opt-in, default off.** A daily ~60-second tax beyond recall itself needs
  explicit consent, not an ambient default. Help text must be honest about
  what turning it off forecloses — originally "the convergent-validity
  route" to the index; since that index is gone, describe the tradeoff as
  losing the probe's own trend readout (slice 6) plus whatever `quick/008`
  builds on top of it, not a specific named feature that no longer exists.
- **Raw trials stored alongside summaries** (`trials_json` on
  `cognitive_probe`). Summary statistics cannot be un-summarized: a revised
  lapse definition or an EZ-diffusion fit (see `quick/008` slice 2) needs the
  per-trial array, not just aggregates computed once at probe time.
- **Offered once per local day.** A second recall session the same morning
  does not re-prompt — the probe measures a daily baseline state, not
  something to average across repeated same-day attempts.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Probe flag on `user` — Structure `[ ]`

`V<next>__add_daily_probe_enabled_to_user.sql`: `TINYINT(1) NOT NULL DEFAULT
'0'`, following `health_remove_empty_folders_default`. Plus the DTO field
and a regenerated TypeScript client.

- The one deliberate exception to the nullable-pause-column rule elsewhere in
  this codebase's recall instrumentation: this is a setting, not an
  observation, and default-off is the intended behaviour for existing users.
- **Compute the migration version fresh from the migration directory at
  implementation time** — plan text guessing a specific `V` number has
  repeatedly collided with migrations landed by other work in this
  codebase's history; don't trust a hardcoded number here.
- **Enables slice 2 only.**

### 2. The daily probe can be switched on in General settings — Behavior `[ ]`

A checkbox in `GeneralSettingsTab.vue` that persists, default off. Nothing
runs yet — but the learner opts in before anything is measured.

- E2E: extend `users/user_profile.feature`
- Help text must state what turning the probe off forecloses (see Key
  design decisions above — do not reference "the index" or
  convergent-validity by name, since neither currently exists).

### 3. The probe runs and shows this morning's result — Behavior `[ ]`

~20 trials, about 60 seconds, offered before the first recall of the day
when enabled. Identical stimuli every day — that is what removes every item
confound by construction.

- **Interim:** result is not stored — removed by slice 5.
- E2E: new `recall/daily_cognitive_probe.feature`

### 4. `cognitive_probe` table — Structure `[ ]`

`V<next>__create_cognitive_probe.sql`: user FK with CASCADE, `started_at
timestamp(3)`, summary columns, and `trials_json` for the raw per-trial
array.

- Raw trials as well as summaries, deliberately: summary statistics cannot
  be un-summarized, and a revised lapse definition or an EZ fit (see
  `quick/008`) needs them.
- Compute the migration version fresh from the migration directory, per
  slice 1's note.
- **Enables slice 5 only.** Regenerate `docs/database-erd.md`.

### 5. The probe result persists and is offered once a day — Behavior `[ ]`

A second recall session the same morning does not re-prompt.

### 6. Recall Stats shows the probe trend — Behavior `[ ]`

Mean reciprocal RT, lapses and variability over the same window toggle
already used elsewhere on the Recall Stats page (30 / 90 / All).

---

## Permanent artifacts (capability-named)

| Artifact | Slices |
|----------|--------|
| `e2e_test/features/recall/daily_cognitive_probe.feature` | 3, 5 |
| `e2e_test/features/users/user_profile.feature` | 2 (extend) |

## Per-slice wrap-up

Per `.cursor/rules/planning.mdc`: test first and confirm it fails for the
right reason → smallest change to green → `post-change-refactor` on the
uncommitted change → update this plan → commit and push before the next
slice. Targeted `cypress run --spec` only, never the full suite. Unfinished
E2E stays `@wip`; never commit on red.

Migration slices additionally regenerate `docs/database-erd.md`
(`database-erd` skill). Slices changing a controller signature regenerate
the TypeScript client (`generate-api-client` skill).
