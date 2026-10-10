# DearDough Process Findings

Retained material shared-process findings, reviewed 2026-10-09. Only an explicit
queued follow-up is planned work; other entries are open and unqueued. A retained
released response is not proof of effectiveness. Unknown provenance stays unknown.
[Response status](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md).
Full pre-trim evidence: `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`. Older narratives live in Git, not a second archive.

- Highest allocated local number: 220. Removed local codes are never reused.

Detailed retained observations are consolidated in the linked Open Dough
record; headings and former local aliases preserve traceability. Review date:
2026-10-09 (Asia/Tokyo).

## ODF-110 — A readiness replay observed only the plan's named seam, not the rest of the slice's journey

Former local code: DD-109.
Former local code: DD-163.
Former local code: DD-168.
Former local code: DD-171.

A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop.

Follow-up: delivered, unreleased: [Observe decisive planning premises through the full promised journey](https://github.com/terryyin/open-dough/blob/c1875574c4f0b5a6703d7a3e45e981e5e9cd9227/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey.

Evidence and response: [ODF-110](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-110).

## ODF-121 — One transient GitHub TLS timeout ended CI observation for the rest of the execution

Former local code: DD-115.

A reported GitHub transport failure ends observation and leaves later revisions without notification coverage.

Follow-up: Open, unqueued.

Evidence and response: [ODF-121](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-121).

## ODF-106 — Two concurrent executions allocated the same slice-plan number from different bases

Former local code: DD-118.

Concurrent unpublished plan allocation produces two quick plans with the same numeric selector, making a number-only execution request ambiguous.

Follow-up: Open, unqueued.

Evidence and response: [ODF-106](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-106).

## ODF-074 — A plan said the changed script had no test, and nobody searched for one before delivery, so CI caught the stale test

Former local code: DD-126.
Former local code: DD-130.
Former local code: DD-134.
Former local code: DD-139.
Former local code: DD-147.
Former local code: DD-162.
Former local code: DD-167.

Concrete only-caller and host-state premises enter a plan without inspection, forcing a changed decision or stopped implementation when checked.

Follow-up: delivered, unreleased: [Observe decisive planning premises through the full promised journey](https://github.com/terryyin/open-dough/blob/c1875574c4f0b5a6703d7a3e45e981e5e9cd9227/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey.

Evidence and response: [ODF-074](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-074).

## ODF-147 — An implementer reasoned that a new test would fail instead of running it red, and one of its tests could not fail

Former local code: DD-127.

An agent reasons that new tests would fail instead of observing them against pre-change behavior; round-trip or wrong-boundary tests can pass without the fix.

Follow-up: Open, unqueued.

Evidence and response: [ODF-147](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-147).

## ODF-100 — Agents reported vue-tsc's exit code from a pipe into `tail`, so the coordinator had to rerun the typecheck

Former local code: DD-135.

A formatter piped through tail returns the final pipeline stage's success, allowing subsequent delivery steps after formatter failure.

Follow-up: Open, unqueued.

Evidence and response: [ODF-100](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-100).

## ODF-152 — The file-size rule conflicted with an approved staged simplification and mechanical callers

Former local code: DD-138.

An absolute changed-file size check conflicts with an approved staged decomposition and mechanical edits to pre-existing oversized callers.

Follow-up: Open, unqueued.

Evidence and response: [ODF-152](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-152).

## ODF-189 — A four-line repository tip-over forced extracting an unrelated assimilation query block

Former local code: DD-144.

A small addition crosses the numeric file-size ceiling and triggers relocation and re-proof of a substantial previously untouched responsibility.

Follow-up: Open, unqueued.

Evidence and response: [ODF-189](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-189).

## ODF-190 — The plan prescribed production observations whose access route or log source did not exist, and whose results could not change the approach

Former local code: DD-145.

Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof.

Follow-up: delivered, unreleased: [Observe decisive planning premises through the full promised journey](https://github.com/terryyin/open-dough/blob/c1875574c4f0b5a6703d7a3e45e981e5e9cd9227/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey.

Evidence and response: [ODF-190](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-190).

## ODF-195 — The file-size check split an untouched block in one slice of an execution and was waived in a later slice

Former local code: DD-160.

Two refactor passes within one story treat barely touched, already oversized files differently, forcing an unrelated extraction in one and retaining another.

Follow-up: Open, unqueued.

Evidence and response: [ODF-195](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-195).

## ODF-141 — A fresh refactor agent per slice returned "no edits" on three of eight small slices

Former local code: DD-176.

Every small slice launches a fresh full refactor agent even when the accepted diff needs no further changes, creating substantial repeated cost.

Follow-up: Open, unqueued.

Evidence and response: [ODF-141](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-141).

## ODF-208 — The CI observer's worker exited without a terminal result, with no recorded cause

Former local code: DD-202.

The CI observer's worker exits mid-execution without a terminal result or a reported transport error, ending notification coverage.

Follow-up: Open, unqueued.

Evidence and response: [ODF-208](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-208).

## ODF-213 — Refinement planned a search to reproduce a flaky mock, but the project's test rules already forbade that mock

Former local code: DD-204.

Refinement makes reproducing a flaky mock the prerequisite for a remedy although the project’s existing rules already forbid that mock.

Follow-up: Open, unqueued.

Evidence and response: [ODF-213](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-213).

## ODF-214 — A slice plan told the implementer to replace a push/replace assertion with a current-location check, which drops the replace

Former local code: DD-206.

Replacing captured router push/replace calls with current-location proof cannot distinguish the behaviors the story promises to preserve.

Follow-up: Open, unqueued.

Evidence and response: [ODF-214](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-214).

## ODF-110 — A plan's proof for a CLI key example checked only that the server answered, not the authenticated step the example needed

Former local code: DD-207.

A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop.

Follow-up: delivered, unreleased: [Observe decisive planning premises through the full promised journey](https://github.com/terryyin/open-dough/blob/c1875574c4f0b5a6703d7a3e45e981e5e9cd9227/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey.

Evidence and response: [ODF-110](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-110).

## ODF-074 — A slice made the conversion step reject on failure; the existing catch also covered a save the story excluded

Former local code: DD-209.

Concrete only-caller and host-state premises enter a plan without inspection, forcing a changed decision or stopped implementation when checked.

Follow-up: delivered, unreleased: [Observe decisive planning premises through the full promised journey](https://github.com/terryyin/open-dough/blob/c1875574c4f0b5a6703d7a3e45e981e5e9cd9227/.planning/seeds/SEED-108-planning-observations-cover-promised-journeys.md#observe-promised-journey) — SEED-108#observe-promised-journey.

Evidence and response: [ODF-074](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-074).

## ODF-216 — Landing retired the execution workspace while its final slice still needed a continuation

Former local code: DD-212.

Explicitly authorized landing removes an execution workspace while the Taken story still has a final real-stack check and wrap-up owed.

Follow-up: Open, unqueued.

Evidence and response: [ODF-216](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-216).

## ODF-185 — The coordinator accepted an interim "Record shows again during Stop" note; a later slice made it a wrong-result race

Former local code: DD-214.

A reported preservation gap or provisional behavior is accepted as a learning without checking the whole story and the later slices that depend on it.

Follow-up: queued, not resolved: [Keep reported gaps owned through the story's remaining slices](https://github.com/terryyin/open-dough/blob/main/.planning/seeds/SEED-125-story-gap-acceptance.md#keep-reported-gaps-owned) — SEED-125#keep-reported-gaps-owned.

Evidence and response: [ODF-185](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-185).

## ODF-142 — Managed delivery stopped with a misleading rebase error when given an abbreviated previously-published base

Former local code: DD-216.

An abbreviated published-base SHA is accepted as an argument but compared without normalization, causing misleading delivery refusal and fallback publication without observation.

Follow-up: Open, unqueued.

Evidence and response: [ODF-142](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-142).

## ODF-206 — Tooling slices sized at 5–8 minutes ran 10.5–12 minutes because real proof needed fresh disposable worktrees

Former local code: DD-217.

The slice time bound is checked only at hand-back, so a delegated slice runs past it and is accepted or extended without re-sizing.

Follow-up: Open, unqueued.

Evidence and response: [ODF-206](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-206).

## DD-218 — A post-change refactor chained page-object helpers with arrow-function `this` and skipped the E2E path that uses that chain

A cohesion edit rewrote typed referenced-title save to call `this.chooseReferencedTitleSave` from an arrow property on the note page object, then skipped E2E because the spoken Keep scenario still called the helper directly. Existing typed Keep/Update wiki-link scenarios failed in CI with `this` undefined.

### Occurrences
- Execution: SEED-066#rename-with-spoken-title
  - Timestamp: 2026-10-10T07:50:00+09:00
  - Tool: Cursor
  - Model: unknown
  - Open Dough release: 0.3.57
  - Evidence: refactor agent `823365da-5587-41e8-8079-137bdc24f9b8` (reported skipping E2E after changing `saveReferencedNoteTitle` to `this.chooseReferencedTitleSave`); `ca98162b615cd38552770bd615720d3a05aead31:e2e_test/start/pageObjects/notePage.ts`; CI run https://github.com/nerds-odd-e/doughnut/actions/runs/38001567782 (`TypeError: Cannot read properties of undefined (reading 'chooseReferencedTitleSave')` on `wiki_link.feature` / `property_wiki_link.feature`)
  - Observed effect: four note-topology E2E scenarios failed on the published slice-2 SHA while focused `record_live_audio.feature` proof stayed green
  - Inference: arrow-property `this` is not the page object; proof that only exercises the direct helper call cannot validate the rewritten typed-save chain

## DD-219 — A correction story was queued and Taken for a CI failure whose repair was already on the branch

The retrospective of the spoken-title story planned a correction for the typed referenced-title save regression after the CI repair for it had been committed. The correction's plan noted the repair "may already contain" the fix, yet the story was still queued first and executed as its own story.

### Occurrences
- Execution: SEED-066#restore-typed-referenced-title-save
  - Timestamp: 2026-10-10T08:38:32+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: repair `07f888578ee96b7248e08ba202a5a252fe26ae8b` (08:07:20) precedes queue commit `b4bb2f90a0d74a938dcadb98e8f279bbf09263e0` (08:38:32); plan `dcc482a586a4d50d86af8f24ade29aed791c7498:.planning/slice-plans/013-restore-typed-referenced-title-save/PLAN.md` slice 1 "confirm at Take before rewriting"; plan evidence commit `f8f718da651d033034ed4b420e4077634949ef53` changes the plan only
  - Observed effect: the execution took a claim, prepared a workspace, ran four E2E specs (28 scenarios, all green) and delivered no product or test change
  - Inference: the E2E confirmation had value, but it could have closed the predecessor's repair without a separate story, claim, plan and wrap-up

## DD-220 — A plan's literal focused command cut the source story text off before its last key examples

Plan 013's focused commands read each recorded story with a fixed line range (`sed -n '151,200p'`). The dictation story at `595e2eb5d9` runs past line 200, so the range ends before its last three key examples, including the one removal example the demonstration existed to observe ("Advanced Options … no Processing Instructions field"). The plan's premise table recorded only the story's start line as observed.

### Occurrences
- Execution: SEED-071#removals-follow-project-rule
  - Timestamp: 2026-10-10T14:15:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.57
  - Evidence: `618ee6f7cc:.planning/slice-plans/013-removal-proof-in-guidance/PLAN.md` "Focused commands" and premise row "Both recorded story shapes are recoverable with their anchors"; `git show 595e2eb5d9:.planning/seeds/SEED-066-voice-input.md | sed -n '198,202p'` (examples continue past line 200); coordinator's slice-2 extraction, first by line range, then by anchor after reading the file's tail
  - Observed effect: the coordinator's first two extractions ended mid-sentence; reading the tail caught it and one more extraction by anchor to the next heading gave the full story before any agent received it
  - Inference: a line range fixes the start that was observed and guesses the end; extracting from the anchor to the next anchor would not depend on that guess. Cost here was one extra tool call; unnoticed, the demonstration would have run on a story missing three examples
