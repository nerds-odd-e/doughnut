# CLI `/push --dry-run` — preview local edits and conflicts before pushing

**Status:** Phases 1-3 done and committed (Phase 3 together with its code-review fixes and the
`---`/`+++` diff side headers, as a single commit — the two were too intertwined by the time
review was done to split safely). Phase 4 planned, not started.

**Goal:** Inside the notebook context that `/use` establishes, `/push --dry-run <workspace path>`
reports, per note, whether a push would update Doughnut, whether a pull would update the
workspace instead, or whether the two have diverged into a conflict — without ever writing to
Doughnut, the workspace's Markdown files, or (except its own bookkeeping) anything else.

## Context

`cli/src/sync/previewPull.ts` (`/sync --dry-run`) already previews the *pull* direction: it
re-exports the notebook every run and compares it against the workspace as it stands, reporting
a difference "whichever side it came from." It is deliberately stateless — nothing is
remembered between runs — so it cannot say *which* side actually changed, only that the two
currently differ. That is enough for previewing a pull (either way, a pull would overwrite local
with remote), but not enough for previewing a *push*: pushing an accidentally-stale local file
over a legitimately newer remote note is exactly the mistake this feature exists to prevent.

This is epic backlog story #5 in
`.planning/notes/2026-07-24-portable-notebook-workspace.md` ("Preview local edits and conflicts
before pushing"), currently unpromoted (`promoted: false`). This plan promotes and implements
story #5 only — not story #6 (the actual, mutating push).

## Decisions

1. **New independent command**, `/push --dry-run <workspace path>`, rather than extending
   `/sync --dry-run` — keeps `previewPull.ts` and its tests untouched. A call without
   `--dry-run` returns a usage error (the real push is story #6, not built here).
2. **A baseline is required** to tell local-changed apart from remote-changed apart from
   conflict; without one, two-way comparison can only ever say "they differ." The baseline is a
   **merge base**: per note, the content Doughnut and the workspace were last *observed to agree
   on*. It is therefore recorded only for a note whose two sides agree in the run doing the
   recording, and is otherwise carried forward untouched — never refreshed to whatever the
   remote side happened to look like this run. (Corrected during Phase 3; originally "a snapshot
   of the remote content last observed, refreshed on every run", which let the baseline absorb
   the remote side of a difference it had just reported — see "Classification algorithm" below.)
3. **The baseline is established lazily, and never by touching local files.** A run finding no
   baseline entry for a note does the same plain two-way diff `previewPull` already does (no
   pull/push/conflict label). No separate bootstrap step, no forced overwrite of local `.md`
   files — a local edit that predates the first-ever preview is exactly the kind of difference
   this tool exists to surface, not silently resolve. Following from decision 2, a first run
   seeds an entry only for the notes whose two sides already agree; a note that differs on its
   very first run stays unlabeled on every later run too, until the two sides do agree once,
   because no merge base exists for it yet and no direction can be honestly claimed.
4. **Baseline storage never touches `.md` files.** It lives in a hidden
   `<workspacePath>/.doughnut-sync/baseline.json` (`{ notebookId, notes: { [path]: agreedContent
   } }`). `readWorkspace()` only ever collects `.md` files, so this file is naturally invisible
   to the diff engine on both read and write. It is the *only* thing this command ever writes.
   A stored `notebookId` that doesn't match the current notebook is treated as no baseline at
   all (falls back to decision 3's bootstrap), rather than comparing against an unrelated
   notebook's history.
5. **Note matching stays path-based**, exactly like `previewPull`/`applyPull` (the path is
   already title-derived and sanitized by the export). No `doughnut_id`-based identity matching,
   no rename handling, in this plan.
6. **Local-only and remote-only notes are out of scope.** Only notes present on both sides are
   compared, matching `previewPull`/`applyPull`'s existing scope. Reporting "this would create a
   new note" is future work.

## Classification algorithm

Per note present in both the workspace and the fresh export:

```
baseline = load baseline.json → notes[path]   // undefined until the two sides agree once
remote    = fresh export content
local     = current workspace content

if baseline is undefined:
  // no merge base yet for this note: show the plain two-way diff,
  // exactly like previewPull, with no local/remote/conflict label
  if local !== remote: report as DIFF (unlabeled)
else:
  remoteChanged = remote !== baseline
  localChanged  = local !== baseline
  if remoteChanged and not localChanged: report as DIFF (pull-suggested)
  if not remoteChanged and localChanged: report as DIFF (push-suggested)
  if remoteChanged and localChanged and local !== remote: report as CONFLICT
  // both changed but converged to the same content → nothing to report

then, for every path in this run's export — the only mutation this command ever performs:
  if local === remote:                    notes[path] = remote   // they agree: a real merge base
  else if the path has a baseline:         leave notes[path] as it is
  else:                                   no entry for this path at all
  // paths the export no longer holds are dropped from notes
```

**Corrected during Phase 3** (originally: "write notes[path] = remote for every path seen this
run", regardless of branch; then, after the first review round, still exempting a path with no
entry yet). An entry is written only when the two sides *actually agree* — bootstrap included.
Writing one while a difference is being reported lets the baseline absorb the remote side of that
difference, so the next run, with nothing edited in between, sees only the workspace as changed
and reports the same untouched divergence as a `(push)`: exactly the stale-local-over-newer-remote
push this feature exists to prevent, and a direction nothing had established. Exempting the
bootstrap case reintroduced that one run earlier in a note's life, so it is gone too.

Enumerated over one note — {entry exists?} × {local vs entry} × {remote vs entry} × {local vs
remote} — every reachable combination now either names a direction the recorded agreement
supports, or names none at all:

| Entry | local vs entry | remote vs entry | local vs remote | Report | Next baseline |
| --- | --- | --- | --- | --- | --- |
| absent | — | — | same | nothing | `remote` (they agree) |
| absent | — | — | differ | unlabeled | no entry |
| present | same | same | same | nothing | unchanged |
| present | same | differ | differ | `(pull)` | unchanged |
| present | differ | same | differ | `(push)` | unchanged |
| present | differ | differ | same | nothing | `remote` (converged) |
| present | differ | differ | differ | `(CONFLICT)` | unchanged |

A note the export holds but the workspace does not is out of scope (decision 6): it is left out
of the report, and out of any new entry — `workspace.get(path)` is `undefined`, which equals no
remote string, so it can never be mistaken for agreement. An entry already there is carried
forward, so restoring the file resumes from the merge base rather than losing it.

Line-ending normalization reuses `readWorkspace.ts`'s existing CRLF→LF handling, so a workspace
saved with CRLF line endings doesn't spuriously report every line as changed.

## Output format

Reuses `unifiedDiff.ts`'s `diffLines` and the rendering shape `diffReport.ts`'s `renderNoteDiff`
already established (path header, indented hunk lines, blank-line separator, trailing count
line), adding a status label per note and a distinct summary phrase for conflicts:

```
less.md (push)
  --- Doughnut
  +++ workspace
  - Hello
  + Hello world!

scrum.md (CONFLICT)
  --- workspace
  +++ Doughnut
  - Sprint plan A
  + Sprint plan B

1 note would change. 1 conflict.
```

The `---`/`+++` side headers were **added after Phase 3**, outside this plan's phase structure —
see "Naming each diff's sides" at the end of the phase list below.

Empty case: `"No changes to push."`, following `previewPull`'s `"No changes to pull."`
convention. Exact wording for the pull-suggested and bootstrap-unlabeled cases is finalized
while writing tests, following `previewPull`'s existing phrasing rather than invented fresh.

## Reusable parts — do not reimplement

| Purpose | Location |
| --- | --- |
| Read every workspace note, keyed by path, CRLF-normalized | `readWorkspace()` — `cli/src/sync/readWorkspace.ts` |
| zip → `Map<path, content>` | `unzipToEntries()` — `cli/src/sync/unzip.ts` |
| Line-level diff rendering | `diffLines()` — `cli/src/sync/unifiedDiff.ts`; rendering shape — `cli/src/sync/diffReport.ts` (`renderNoteDiff`, `renderDiffReport`, shared with `previewPull.ts`) |
| Download the export zip (401/403/404/5xx user-readable errors) | `downloadNotebookExportZip()` — `cli/src/backendApi/doughnutBackendClient.ts` |
| Argument-parsing return shape (`{ value } \| { error }`), flag parsing | `parseSyncArgument()` — `cli/src/sync/syncArgument.ts` |
| Notebook stage slash command registry | `notebookStageSlashCommandsFor()` — `cli/src/commands/notebook/notebookStageSlashCommands.ts` |
| "parse argument → run async → assistant message" stage template | `syncSlashCommandFor()` — `cli/src/commands/notebook/syncSlashCommand.tsx` + `AsyncAssistantFetchStage` |
| Usage-error stage (spinner never flashes before a parse error) | `UsageErrorStage` — `cli/src/commands/UsageErrorStage.tsx` |
| Quote-stripping for a path argument | `stripSurroundingQuotes()` — `cli/src/sync/stripSurroundingQuotes.ts` |
| Real zip bytes for tests | `zipOfNotes()` — `cli/tests/zipFixture.ts` |

## Execution protocol — Definition of Done and working agreement

Per `docs/teams/definition_of_done.md` and `docs/teams/initial_working_agreement.md`, which
apply to all work in this repo, not just this plan:

| DoD item | How this plan satisfies it |
|---|---|
| Changes committed to trunk | Directly on `main`, no branch |
| Code, tests, docs free of lint errors | `CURSOR_DEV=true nix develop -c pnpm lint:all` |
| English for all code, tests, documentation | Code, tests, `CommandDoc`, and this plan are English |
| Warnings treated as errors | lint and tsc clean, no warnings left |
| All code is fully small tested | Red test before each new piece (`pushArgument`, `pushBaseline`, `previewPush`, slash-command wiring), per phase below |
| Eliminate duplicate code | Reuse table above; no new shared abstraction beyond `pushBaseline.ts` (genuinely new: nothing today persists sync state) |
| Automated end-to-end tested | Each phase's Cucumber scenario is green in that phase's commit, never `@wip` past its own phase |
| No known defects | Run the CLI test suite and the targeted e2e spec before closing each phase |
| CI green in 10 minutes or less | Confirm CI after each push |
| Reviewed by at least one other developer | Invoke the `code-reviewer` agent after each phase's changes, per this repo's standing agent-orchestration agreement |

Commit messages follow `<type>(<scope>): <summary>` — imperative, lowercase, no period. Scope is
`cli` for every phase here.

## Implementation — phases

Each phase is stop-safe: if work stops after any phase, the command already on disk is safe and
gives real value proportional to what shipped.

**Rhythm for every phase (outside-in, test-first — no exceptions):**

1. Write the phase's Cucumber scenario(s) in `cli_push_dry_run.feature` first. Run the targeted
   spec and confirm it fails — and confirm it fails because the behavior is missing (e.g. `/push`
   not recognized, or the wrong output), not because of a typo, missing step definition, or env
   issue.
2. Work inward one layer at a time — slash command → `previewPush`/`pushArgument`/`pushBaseline` →
   any shared helper. Before writing each layer's code, write that layer's failing unit test in
   the relevant `cli/tests/*.test.ts` file and confirm it fails for the right reason first.
3. Write only the minimal code to make the current failing test pass, at each layer.
4. The phase ends with its e2e scenario(s) green and `cli/` green. Never commit a red test, and
   never write production code before its test exists and has been watched to fail.

### Phase 1 — `/push --dry-run` previews with a bootstrap diff — DONE

Pinned at the outermost layer: `cli_push_dry_run.feature` (4 scenarios), then inward
`cli/tests/pushArgument.test.ts`, `cli/tests/pushBaseline.test.ts`, `cli/tests/previewPush.test.ts`,
and two new cases in `cli/tests/notebookStageSlashCommands.test.ts` — each written and confirmed
red before its implementation. Post-green refactor: extracted the diff-rendering logic
`previewPush.ts` would otherwise have duplicated from `previewPull.ts` into shared
`cli/src/sync/diffReport.ts` (`renderNoteDiff`, `renderDiffReport`); `previewPull.ts` now uses it
too, with `previewPull.test.ts` re-confirmed green as a Structure-only change.

Known pre-existing flake, unrelated to this phase: `InteractiveCliApp.useNotebook.test.tsx`'s
"notebook stage: slash guidance..." test occasionally times out (its fixed 5s budget) only when
the full `cli` suite runs with default file parallelism under CPU contention — passes reliably
alone or with `vitest run --no-file-parallelism`. Not touched here; out of scope for this plan.

Added after first landing (closing a gap the user flagged): a fifth e2e scenario, "The remote
note is not modified by the preview" — asserts Doughnut's own note content, not just the
workspace file, is unchanged after `/push --dry-run`. Needed a new read-only testability helper,
`getInjectedNoteContent()` (`e2e_test/start/testability.ts`, mirrors the existing `showNote`-based
pattern), and its step, `the note {string} in Doughnut should still hold {string}`
(`e2e_test/step_definitions/cli_sync.ts`). Written and confirmed red (undefined step) before
wiring the step to the already-added helper.

Original phase description follows, kept for reference:

First use of the command in a workspace with no stored baseline: shows the plain two-way diff
(workspace vs. fresh export, same shape as `previewPull`'s output — no pull/push/conflict label
yet) and silently writes `.doughnut-sync/baseline.json` with the freshly-exported remote
content. Nothing under `.md` is ever written.

Stop-safe: on its own this already gives a working, safe `/push --dry-run` — equivalent value to
today's `/sync --dry-run` but framed for push — and quietly seeds the baseline every later phase
depends on.

Write first, in this order — outermost e2e scenario, then each layer's unit test, red before its
implementation:

1. `e2e_test/features/cli/cli_push_dry_run.feature` (new, mirrors `cli_sync_dry_run.feature`'s
   tags/background): first `/push --dry-run` shows the diff and leaves the workspace's `.md`
   files untouched; a no-difference run reports "No changes to push."; `.doughnut-sync/baseline.json`
   is the only thing added to the workspace. Reuse existing step definitions (`the workspace
   {string} holds the same content as {string}`, `the note {string} is changed in Doughnut to
   {string}`, `I should see the preview in past CLI assistant messages:`, `the workspace {string}
   should hold only:`, and the generic `I enter the slash command {string} in the interactive
   CLI`, which already resolves workspace names — confirmed reusable, no new step definitions
   needed for this phase). Run it and confirm it fails because `/push` is not a recognized
   command, not for any other reason.
2. `cli/tests/pushArgument.test.ts` (mirrors `syncArgument.test.ts`): flag parsing, missing
   `--dry-run` usage error, quoted paths, other usage errors.
3. `cli/tests/previewPush.test.ts` (mirrors `previewPull.test.ts`): bootstrap diff reported for a
   changed note, "No changes to push." when equal, does not write any `.md` file, writes
   `.doughnut-sync/baseline.json` with the exported content, missing workspace directory error,
   failed export surfaced, CRLF-normalized comparison (confirm reuse of `readWorkspace`, don't
   reimplement), stale baseline with a different `notebookId` treated as absent.

Then implement, inside-in from the innermost failing unit test outward, until the e2e scenario
from step 1 is green:

- New `cli/src/sync/pushBaseline.ts` — `loadPushBaseline(workspacePath, notebookId)` /
  `savePushBaseline(workspacePath, notebookId, notes)`. Reads/writes
  `<workspacePath>/.doughnut-sync/baseline.json`; returns/treats as absent when the file is
  missing or its `notebookId` doesn't match (Phase 4's behavior falls out for free here — its
  unit test was already written above, in this phase, rather than deferred).
- New `cli/src/sync/previewPush.ts` — `previewPush({ notebookId, workspacePath,
  exportNotebookAsZip, signal })`. Mirrors `previewPull.ts`'s shape (read workspace, export,
  unzip, filter to `.md`, sort). This phase's branch: no baseline entry for a path → diff exactly
  like `previewPull`'s `renderNote` (unlabeled). Always ends by saving the fresh export as the
  new baseline. (Superseded in Phase 3: an entry is recorded only for a note whose two sides
  agree — see decision 2 and "Classification algorithm".)
- New `cli/src/sync/pushArgument.ts` — `parsePushArgument`, same parsing shape as
  `syncArgument.ts`'s `parseSyncArgument`. Only `--dry-run` is implemented; a call without the
  flag returns a usage error — `Usage: /push --dry-run <workspace path>`.
- New `cli/src/commands/notebook/pushSlashCommand.tsx` — mirrors `syncSlashCommand.tsx`, wired
  to `previewPush` only.
- Modify `cli/src/commands/notebook/notebookStageSlashCommands.ts` — register
  `pushSlashCommandFor(notebook)`.

Test commands:
```bash
cd cli && CURSOR_DEV=true nix develop -c pnpm vitest run tests/pushArgument.test.ts tests/previewPush.test.ts
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature
```

### Phase 2 — Distinguish which side changed, using the baseline — DONE

Pinned at the outermost layer first: two scenarios under a new
`cli_push_dry_run.feature` rule, each running `/push --dry-run` twice (the first invocation moved
into the rule's Background, since establishing the baseline is the precondition rather than the
behavior under test), confirmed red only on the missing `(pull)`/`(push)` label — the surrounding
steps all reused what Phase 1 already had, so no new step definition was needed. Then
`previewPush.test.ts`'s two label cases, likewise watched red on the label alone.

Learned during implementation:

- **The both-sides-changed case had to keep reporting, unlabeled.** Phase 1 reported it as a plain
  difference; classifying it as "nothing to report" until Phase 3 adds the conflict label would
  have silently dropped the most dangerous case from the report. So `classify` falls through to
  the unlabeled difference whenever it cannot name a direction — a regression guard test
  (`leaves a note unlabeled while both sides differ from the baseline`) pins that, and Phase 3
  only has to turn that fall-through into `(CONFLICT)`.
- **Phase 1's `reports the same difference when run twice` test was made obsolete by this phase**
  and became the `(push)` case: with a baseline on disk, a second run over an unchanged remote and
  a changed local *should* read differently from the first. Replaced, not kept.
- **The diff direction now flips for `(push)`.** Flagged after first landing as counter-intuitive
  — a `(push)` note read `- <local>` / `+ <remote>`, backwards from "what a push would do to
  Doughnut." Decided explicitly and fixed: `renderNoteDiff` (`diffReport.ts`) shows `push` notes
  notebook-to-workspace (removed = Doughnut as it stands, added = workspace as it stands, i.e.
  what pushing would write) and leaves every other case — unlabeled, `(pull)`, and the
  not-yet-implemented `(CONFLICT)` — as workspace-to-notebook, matching `/sync --dry-run`.
  `previewPull.ts` never passes a `status`, so it is unaffected.
- `renderNoteDiff` grew one optional `status` parameter rendered as ` (status)` on the path
  header, so `previewPull`'s call site is untouched and Phase 3's `CONFLICT` needs no further
  rendering change.

Original phase description follows, kept for reference:

On a second (or later) run, with a baseline now on disk: a note whose remote content differs
from the baseline but whose local content doesn't is labeled `(pull)`; a note whose local content
differs from the baseline but whose remote content doesn't is labeled `(push)`. A note unchanged
on both sides relative to the baseline is left out of the report, same as today.

Stop-safe: adds real value over Phase 1 — the user can now tell which direction a difference
would flow, without yet handling the case where both sides moved.

Write first: extend the e2e feature with the two named scenarios (remote changed / local
unchanged → `(pull)`; remote unchanged / local changed → `(push)`), each needing two
`/push --dry-run` invocations in sequence — run and confirm they fail because the label is
missing, not for any other reason. Then extend `previewPush.test.ts` with two-call unit
scenarios (call once to establish the baseline, change one side, call again, assert the label),
confirming each fails before implementing.

Then implement: extend `previewPush.ts`'s classification branch (baseline-defined case) and its
rendering (status suffix per note, e.g. `less.md (push)`).

### Phase 3 — Conflict detection — DONE

Pinned at the outermost layer first: a third scenario under the "later preview" rule (both sides
edited between the rule background's baseline-establishing run and a second one), watched red on
the missing `(CONFLICT)` label and the old summary line while the diff body itself already read
correctly — again with no new step definition needed, and again reusing the two existing edit
steps. Then `previewPush.test.ts`'s conflict, converged, mixed-count and plural cases, each
watched red before implementing `classify`'s both-changed branch and `renderDiffReport`'s
conflict count.

Learned during implementation:

- **Conflicts are counted apart from, not inside, "would change."** A conflict is not something a
  push could apply, so counting it as a note that "would change" would overstate what the command
  could do. The summary is a sentence per kind, joined by a space and each only present when
  non-zero: `1 note would change. 1 conflict.` for one of each, `1 conflict.` / `2 conflicts.`
  when conflicts are all there is. This keeps Phase 2's established "would change" phrasing as the
  base count rather than the plan's original illustrative "would push" — `/push --dry-run` reports
  both directions, so naming only one of them in the count would mislead.
- **`renderNoteDiff`'s diff direction needed no change, as Phase 2 predicted.** Widening
  `NoteDiffStatus` was enough: the generic `${path} (${status})` heading renders the label, and the
  `status === 'push'` check already leaves everything else — conflicts included —
  workspace-to-notebook. The label is uppercase (`CONFLICT`) so a conflict stands out from the
  lowercase directions in a report listing several notes, but the *status* stayed lowercase
  (`'conflict'`) and `renderNoteDiff` applies the casing, keeping it a presentation decision.
- **`renderDiffReport` now takes the reported notes as `{ diff, status }` entries** rather than
  rendered strings, and counts the conflicts itself. A parallel `conflictCount` parameter was tried
  first, but `changed.length - conflictCount` is an invariant nothing can enforce, and `previewPush`
  already had the richer data — so it hands that over instead of re-deriving a count from it.
  `previewPull` wraps each rendered diff in `{ diff }`, one line, no behavior change.
- **Phase 2's `leaves a note unlabeled while both sides differ from the baseline` guard test was
  made obsolete by this phase** — it pinned exactly the fall-through this phase replaces. Rewritten
  in place as `labels a note CONFLICT when both sides changed and diverged since the last run`,
  not kept alongside, since the two assertions contradict each other.
- **The converged-both-changed case already behaved correctly** (`classify` falls through to
  `'nothing'` when `local === remote`), but nothing pinned it: the nearest existing test covered
  *neither* side changing. Added as its own case, and it was the one new test that passed on
  arrival — kept as a regression guard rather than dropped.

Found in code review (first round), fixed before this phase was committed:

- **A conflict or a pull was reported once, then silently downgraded to a `(push)`.** Phase 1's
  "refresh the baseline from this run's export, always" rule meant the baseline caught up with the
  remote side of a difference it had just reported. The next run — with nothing edited by anyone —
  saw `remoteChanged` false and `localChanged` still true, and called the same untouched divergence
  a `(push)`: the stale-local-over-newer-remote push this feature exists to prevent, reachable by
  simply previewing twice. Fixed by making the baseline a *merge base* that only advances once the
  workspace has caught up (see the corrected rule in "Classification algorithm" above), pinned by
  `keeps reporting a conflict when the preview runs again with nothing edited` and
  `keeps reporting a pull when the preview runs again with nothing edited`, both watched red
  (`(push)`) against the old rule first. Two further guards keep the parts of Phase 1's behavior
  that were right: `advances the baseline once the workspace catches up to Doughnut` and
  `keeps the baseline to the notes the export still holds`.
- **A note missing locally was branded a difference — even a `(CONFLICT)` — with an empty diff
  side.** `workspace.get(path) ?? ''` read "no local file" as "empty local file", so a remote-only
  note (out of scope per Decision 6) counted as locally changed. Now such a path is skipped
  outright, pinned by `leaves a note missing from the workspace out of the report`. `previewPull`
  keeps its own `?? ''`: for a *pull*, a note missing locally is a file the pull would create, which
  is exactly what it should report.

Found in code review (second round), fixed before this phase was committed:

- **The same bug class survived once more, in the bootstrap case.** The first round's fix still
  exempted a path with no baseline entry (`agreed === undefined || workspace.get(path) === remote`),
  so a first-ever preview reporting an honest unlabeled difference still seeded the entry from that
  run's remote content — and the very next run, nothing edited in between, called it a `(push)`. The
  direction was fabricated: nothing had been learned between the two runs. The exemption is gone, so
  an entry is written only when the two sides actually agree. Pinned by
  `seeds the baseline only with the notes the two sides agree on` (a bootstrap run over one agreeing
  and one differing note leaves only the agreeing one in `baseline.json`) and
  `keeps a note unlabeled when the first preview found no history for it`, both watched red first —
  the second showing exactly the fabricated `less.md (push)`. It replaced Phase 1's
  `writes the baseline file with the exported content`, which pinned the buggy rule. The first
  round's two guards passed unchanged, as they should: both establish their baseline through
  genuine first-run agreement rather than a bootstrap difference.
- **Accepted consequence, deliberately pinned:** a note that differs on its very first run stays
  unlabeled on every later run too, until the two sides agree once. Honest per decision 3 — no merge
  base exists for it yet, so no direction can be claimed. The whole case table is in
  "Classification algorithm" above, walked to confirm no cell invents a direction.
- **Doc comments describing the pre-fix rule were corrected**, in `pushBaseline.ts` (its save
  function and the stored file's type) and `previewPush.ts` (`classify`, `nextBaseline`,
  `previewPush`), along with decision 2 above, which still stated the "refreshed on every run" rule
  a reader would hit before the correction recorded further down.

Original phase description follows, kept for reference:

When both local and remote content differ from the baseline **and** differ from each other,
label the note `(CONFLICT)` instead of `(pull)`/`(push)`, and use a distinct summary phrase
(e.g. `1 note would push. 1 conflict.`). When both changed but converged to the same content, the
note is left out of the report — nothing to reconcile.

Stop-safe: completes the design's core promise (diff-or-conflict) on top of Phases 1–2's
plumbing.

Write first: extend the e2e feature with the third named scenario (both sides changed →
conflict block with both diffs shown); run and confirm it fails because there is no conflict
label yet. Then extend `previewPush.test.ts` (conflict case, converged-no-conflict case),
confirming each fails before implementing.

Then implement: extend `previewPush.ts`'s classification (both-changed branch) and the
summary-line renderer.

### Phase 4 — Stale baseline for a different notebook (closing the loop)

Mostly covered by Phase 1's `pushBaseline.ts` (mismatch treated as absent, tested there already).
This phase is just an end-to-end check: reuse a workspace's `.doughnut-sync/baseline.json`
against a different notebook and confirm the command falls back to Phase 1's bootstrap behavior
rather than comparing against unrelated history.

Write first: confirm `previewPush.test.ts`'s notebookId-mismatch case (written in Phase 1)
actually covers this; add only what's missing there, watched red before green. No new e2e
scenario unless that unit coverage turns out insufficient to express the workspace-reuse angle —
if one is needed, write and red-confirm it first, same as every other phase.

Then implement: none expected beyond what Phase 1 already wrote.

### Naming each diff's sides — DONE, outside this plan's phases

Raised by the user after reviewing Phase 3's output: `-` and `+` alone cannot say which side a
line came from, because the direction flips per status — `-` is the workspace for an unlabeled,
`(pull)` or `(CONFLICT)` note, but Doughnut for a `(push)` one, and nothing in the report said so.
Decided (option chosen by the user out of four offered): name the two sides `git diff` style, on
every note, `--- <side the removed lines come from>` / `+++ <side the added lines come from>`.

Deliberately **not** folded into Phase 3: it changes `diffReport.ts`, which `/sync --dry-run` has
shipped on since Phase 1, so it is a change to already-released output rather than part of
conflict detection. Kept as its own commit, after Phase 3's.

- **The renderer became the only place that knows a side's name.** `renderNoteDiff` picks a
  `{ name, content }` pair per side and derives both the headers and the diff body from the same
  choice, so a future direction change cannot leave the labels behind.
- **A new `cli/tests/diffReport.test.ts` pins the renderer directly** — one case per status for the
  header direction and the uppercase conflict label, plus the summary-composition branches
  (changes only, conflicts only, both; singular and plural). This replaced growing
  `previewPush.test.ts` with more near-duplicate fixture runs: its `counts two conflicts in the
  plural` case was dropped in favor of the direct one, keeping only the mixed
  push-plus-conflict case there as an end-to-end check that classification feeds the count.
- **The e2e docstrings did need updating, contrary to the first guess.** A block asserted through
  `I should see the preview in past CLI assistant messages:` must be contiguous in the rendered
  terminal output, and the side headers are not adjacent to the `-`/`+` lines there. So each
  labeled scenario asserts two blocks: `path (status)` with its two side headers, then the body
  with the summary line. The redundant `I should see "less.md (push)"` single-line steps were
  dropped, since the block now covers the label. `cli_sync_dry_run.feature` needed no change at
  all, verified by running it.
- **Why they are not adjacent — measured, not guessed** (an earlier note here blamed a markdown
  rendering boundary, which was wrong: the preview reaches the transcript as a bare Ink
  `<Text>` node via `appendScrollbackAssistantTextMessage`
  (`sessionScrollback/sessionScrollbackAppendContext.tsx`), with no markdown renderer anywhere on
  that path — `AsyncAssistantFetchStage`'s `onSettled` string goes straight through). The three
  lines actually sitting between `+++ Doughnut` and `- Hello` are the diff's own **context
  lines**. `NotebookZipBuilder.java` builds every exported note as
  `---\ndoughnut_id: N\n---\n\n# <title>\n\n<body>`, so a body-only change sits on line 7 and
  `unifiedDiff.ts`'s `CONTEXT_LINES = 3` prints lines 4-6 above it: the blank line closing the
  frontmatter, the `# <title>` heading, and the blank line after it. Nothing renders a blank line
  into the report; `renderNoteDiff` emits the side headers immediately above the body.
  `cli_sync_dry_run.feature`'s header comment already recorded this ("An exported note carries
  frontmatter and a `# title` heading above its content, so a diff of the content is preceded by
  those as context lines") — the same note now sits in `cli_push_dry_run.feature`.
- **A single contiguous block was tried first and rejected on evidence.** Folding each scenario's
  two docstrings into one failed all four labeled scenarios, the assertion reporting the pattern
  `less\.md\r?\n  --- workspace\r?\n  \+\+\+ Doughnut\r?\n  - Hello…` unmatched. Spanning the gap
  would mean spelling the export's frontmatter-and-heading preamble into every scenario, which
  `cli_sync_dry_run.feature` deliberately declines to do — it would break the moment the export's
  shape changes, over something these scenarios are not about. Two blocks bracketing the preamble
  keep the assertion on what naming the sides is meant to prove.

- The real, mutating `/push` (story #6) — only its preview, here.
- Local-only notes (would become a new remote note) and remote-only notes (missing locally) —
  see Decision 6.
- Renames: matching is by identical relative path only. A remote title change that changes the
  exported path is treated as remote-new-path + local-old-path, same as `previewPull` today — no
  `doughnut_id`-based identity matching in this plan.
- The rest of the portable workspace epic
  (`.planning/notes/2026-07-24-portable-notebook-workspace.md`, `promoted: false`) beyond story
  #5.

## Verification

```bash
# CLI, focused
cd cli && CURSOR_DEV=true nix develop -c pnpm vitest run \
  tests/pushArgument.test.ts tests/previewPush.test.ts

# Targeted e2e for this feature
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature

# All of the CLI
cd cli && CURSOR_DEV=true nix develop -c pnpm test

# Lint and format
CURSOR_DEV=true nix develop -c pnpm lint:all
```

e2e needs `pnpm sut` running; if unsure, run `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`
first.

When each phase is done, follow the wrap-up in CLAUDE.md: Jidoka, post-change refactor, update
this plan (mark the phase done, prune anything it made obsolete), commit, push — before starting
the next phase.
