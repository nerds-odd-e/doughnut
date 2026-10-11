# DearDough Process Findings

Retained material shared-process findings, reviewed 2026-10-09. Only an explicit
queued follow-up is planned work; other entries are open and unqueued. A retained
released response is not proof of effectiveness. Unknown provenance stays unknown.
[Response status](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md).
Full pre-trim evidence: `8830c682704aac3bbb34bf9b1204da8feba042ca:DearDough.md`. Older narratives live in Git, not a second archive.

- Highest allocated local number: 231. Removed local codes are never reused.

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

### Occurrences
- Execution: SEED-066#simplify-voice-input-feedback (first implementation commit dd22b17de2)
  - Timestamp: 2026-10-10T19:57:33+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `6f6f3d32c9:.planning/slice-plans/063-simplify-voice-input-feedback/PLAN.md` slice 2 ("Delete … `noteAudioToolsSavedContentTestSupport.ts`"), slice 4 ("cut from `e2e_test/fixtures/harvard.wav` or `lecture.wav`", "2026-10-06 measurement"), slice 2 proof ("no toast"); slice 2, 3 and 4 agent returns in the execution conversation
  - Observed effect: the test-support file named for deletion is imported by the preservation and language-joining specs, so the implementer kept it; neither fixture holds 60 s of speech (18.4 s and 3.1 s), so the probe looped one; the cited measurement is dated 2026-10-03 in the seed; "no toast" could not be observed in slice 2 because no panel code raised a toast until slice 3, so it moved there as a story obligation
  - Inference: each premise was a one-command check at planning time (an import search, a duration read, a date read); none stopped a slice, and each cost a plan correction during delivery

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

### Occurrences
- Execution: SEED-066#simplify-voice-input-feedback (first implementation commit dd22b17de2)
  - Timestamp: 2026-10-10T19:57:33+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: slice 1 implementation return in the execution conversation: "The exit code I captured was the pipe's, not `vue-tsc`'s, so the evidence is the empty output"; the coordinator's slice 2, 3 and 5 briefs give the type-check command with `; echo "exit=$?"`
  - Observed effect: the coordinator accepted slice 1's type check on empty output without a rerun; later slices returned real exit codes once the brief spelled the command out
  - Inference: the first brief asked for a type check "if the frontend has a quick one" without a literal command, so the agent chose the pipe

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

### Occurrences
- Execution: SEED-066#simplify-voice-input-feedback (first implementation commit dd22b17de2)
  - Timestamp: 2026-10-10T19:57:33+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: refactor returns for slices 1 and 5 ("none — already clean", about 80,000 and 53,000 sub-agent tokens); refactor returns for slices 2 and 3 with edits (a spec over the file-size limit split, a duplicated `noToastShown` helper merged, an absence sentence removed from the doc)
  - Observed effect: two of four refactor passes made no edit; slice 5's diff was one constant, two spec files and one doc line
  - Inference: the two passes that edited found things the implementation returns had not, so the cost question is about the smallest slices only
- Execution: SEED-066#single-button-voice-input (first implementation commit 5c3ca7033b)
  - Timestamp: 2026-10-10T21:48:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: refactor return for slice 1 ("none — already clean", about 62,000 sub-agent tokens, 43 s); refactor returns for slices 2, 3 and 4 with edits (a toolbar-panel composable folded into the Assimilate view, a recorder listener scoped to the recording, a spec over the file-size limit split, leaving-note cases gathered in one spec)
  - Observed effect: one of four refactor passes made no edit; slice 1 was a two-file structural move
  - Inference: as in the earlier execution, the passes on the behavior slices found real work; only the smallest slice's pass returned nothing

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

## DD-221 — A slice plan linked its story without the `**Source:**` field, so the first story-obligation check refused

Plan 063 as written by slice planning named its story as "Work item: … ([story](…))". The story-obligations script reads the selected story from a `**Source:**` link; with an empty obligation record it passes, so the missing field surfaced only when the first obligation was recorded.

### Occurrences
- Execution: SEED-066#simplify-voice-input-feedback (first implementation commit dd22b17de2)
  - Timestamp: 2026-10-10T20:10:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `6f6f3d32c9:.planning/slice-plans/063-simplify-voice-input-feedback/PLAN.md` lines 3–4; `story-obligations.mjs check --slice 2` result `{"reason":"missing-story-source"}` in the execution conversation; `.claude/skills/dough-story-refinement/references/planning.md` ("Its `**Source:**` link names the selected story section"); `list --slice 1` and `check --slice 1` both passed on the same plan
  - Observed effect: slice 2's done transition was blocked until the coordinator rewrote the plan header during delivery; one extra plan edit and check
  - Inference: the script validates the link only when an obligation exists, so a plan can pass delegation checks for every slice and still fail at the first recorded gap

## DD-222 — A removal-plus-replacement slice sized at about 10 minutes took about 29 minutes of implementation

Plan 064 sized slice 2 (new toolbar button, toolbar wiring, deletion of the panel, retargeting seven specs and the E2E page object) at about 10 minutes and gave the reason it could not be split: the product cannot hold the change halfway.

### Occurrences
- Execution: SEED-066#single-button-voice-input (first implementation commit 5c3ca7033b)
  - Timestamp: 2026-10-10T22:17:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `7becda17c4:.planning/slice-plans/064-single-button-voice-input/PLAN.md` slice 2 ("About 10 min"); the implementation agent's usage record (1,749 s, about 258,000 sub-agent tokens, 52 tool uses); its return reporting one Vitest run that printed nothing for over 600 s before being ended and rerun in about 11 s; refactor pass about 6 minutes more
  - Observed effect: the slice ran about three times its estimate; about 10 of the 29 minutes were the stalled test run, whose cause is unknown; the slice touched 53 files
  - Inference: the stated reason for not splitting held, so the estimate rather than the boundary was wrong; a 53-file removal with renamed specs is not a 10-minute change even when mechanical
- Execution: SEED-066#unobtrusive-selection-aware-spoken-title (first implementation commit 57a659f018)
  - Timestamp: 2026-10-11T08:00:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `7d062d701f:.planning/slice-plans/065-unobtrusive-spoken-title/PLAN.md` slice 5 ("About 8 min"); the implementation agent's usage record (787 s, about 175,000 sub-agent tokens, 44 tool uses) and its return ("roughly 25–30 minutes with the observations"); refactor pass about 2.5 minutes more
  - Observed effect: the slice ran well over its estimate; it carried the control rewrite, two placements, five spec files, the E2E page object, two documentation sections, a held-stack real-click observation and three obligations received from earlier slices
  - Inference: the estimate counted the code change only; the held-stack observation and the received obligations were known when the slice was delegated and were not added to it

## DD-223 — A plan left a look to be "checked by eye on the dev server" with a real microphone, which an unattended agent could not do

Plan 064 recorded as not observed how the live waveform reads inside a toolbar-sized button and assigned the check to slice 2 on the dev server. Matching with ODF-190 (a prescribed observation whose access route did not exist) is uncertain: here the route exists for a person at the machine, not for the agent.

### Occurrences
- Execution: SEED-066#single-button-voice-input (first implementation commit 5c3ca7033b)
  - Timestamp: 2026-10-10T22:20:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `70a1b54232:.planning/slice-plans/064-single-button-voice-input/PLAN.md` "Premises observed" last item; slice 2 return ("The look of the waveform with a real microphone on a dev server is unobserved"); the coordinator's request for enlarged Chromium screenshots with a synthetic level saved under the job's temporary directory, and its reading of them; plan obligation G3
  - Observed effect: the look was judged from screenshots of the mounted button fed a synthetic level; the real-microphone check was left to the owner and reported at completion
  - Inference: screenshots saved outside the checkout gave the coordinator an inspectable look at low cost; they do not show how the bars scale to a real voice

## DD-224 — A plan mounted a session bound to one note in an always-mounted control without noticing it would write to the first note

Plan 064 moved the recording session from a panel opened per use to a button that stays mounted while the toolbar's note changes. The session captures its note when created. The plan's own structural slice recorded that fact as a learning, and the next slice's design did not account for it.

### Occurrences
- Execution: SEED-066#single-button-voice-input (first implementation commit 5c3ca7033b)
  - Timestamp: 2026-10-10T22:17:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `5c3ca7033b:.planning/slice-plans/064-single-button-voice-input/PLAN.md` Learnings ("captures the note at call time") and "Design for the button"; slice 2 return, decision 1 ("Voice input clicked on a second note visited in the same page session would dictate into the first note"); the added spec "dictates into the note on the page when voice input starts after moving to another note", reported to fail without the fix; slice 4 replacing the fix with a key on the note's id
  - Observed effect: the implementer found the defect mid-slice, added a watch and a test outside the plan, and slice 4 later replaced the watch
  - Inference: the plan's navigation premise covered an open panel surviving a note change, not an idle control; a slice that changes a component's lifetime needs its per-instance captures re-read
- Execution: SEED-066#read-only-voice-input-with-insertion-feedback (first implementation commit 7557329339)
  - Timestamp: 2026-10-11T11:15:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: plan 067 slice 2 and its proof table at `783d3f96cd` (no observation for leaving the note while a title is listening); slice 2 return, second round ("moving to another existing note while listening left that note's title locked"); `frontend/tests/notes/NoteShow.spokenTitleLeavingNote.spec.ts` at `f4bba8d679`, reported to fail before the fix; plan 067 obligation G10 at `e87920d731`
  - Observed effect: the title's read-only lock stayed on the title editor reused for the next note; the coordinator asked for the observation after reading `NoteShow.vue` for keys, and the implementer keyed the speak control on the note's id and ended the session on a note change
  - Inference: same lifetime question as the first occurrence, on the title this time; the plan reused the `busy` signal without asking what ends it when the page moves between cached notes

## DD-225 — A removal sweep's search terms matched a dated observation record, so the implementer reworded history to make the sweep return nothing

Plan 064's sweep reading required searches for the removed control names to return nothing over the product. One name appeared in `docs/voice-input-observations.md`, a dated record of journeys run with that control.

### Occurrences
- Execution: SEED-066#single-button-voice-input (first implementation commit 5c3ca7033b)
  - Timestamp: 2026-10-10T22:17:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: slice 2 return, decision 3 (five mentions reworded to "requested (mid-speech) conversion", heading renamed, a seed link's anchor broken); the coordinator's anchor repair in `.planning/seeds/SEED-066-voice-input.md`; slice 2 refactor return ("a truthful rewrite means deleting or re-running the observation, which is an owner decision")
  - Observed effect: a dated observation now describes an author-requested conversion the product does not offer; one planning link broke and was repaired during delivery; the question was left for the owner
  - Inference: the removal rule and the plan's sweep did not say whether a dated observation record counts as documentation to sweep, to delete, or to leave

## DD-226 — The documented frontend typecheck passed while the build's plain `tsc` failed, so CI caught a test-only type error

The project's frontend proof rule names `vue-tsc --noEmit`. `pnpm -C frontend build` also runs plain `tsc` through the vite checker, which types every `*.vue` import through `tests/shims-vue.d.ts` and so sees no exposed component members.

### Occurrences
- Execution: SEED-066#unobtrusive-selection-aware-spoken-title (first implementation commit 57a659f018)
  - Timestamp: 2026-10-11T07:45:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: slice 2 refactor return (replaced `as unknown as {…}` casts with `InstanceType<typeof SeamlessTextEditor>` and `wrapper.vm.insertAtSelection`, "typecheck clean"); CI run 38091750938 on `1e5e7efa49` (TS2722 in `seamlessTextEditorTestSupport.ts` and `PathNameEditor.spec.ts`, two jobs failed at the build step); repair `5e346c5791`; the repair agent's reading of `frontend/vite.config.ts` and `pnpm -C frontend exec tsc --noEmit` exiting 2 at the failed commit while `vue-tsc --noEmit` exited 0
  - Observed effect: one failed CI run; slice 3's unfinished work was set aside and restored around a repair, a repair agent and a refactor agent ran; later slices added `pnpm -C frontend build` to their proof
  - Inference: the proof rule in the `frontend` skill is narrower than the CI step it stands for; a refactor pass tidied a cast that was there for a reason no comment or rule stated

## DD-227 — A slice plan asked for a test that the removed status line is absent, against the project's removal rule

Plan 065 slice 5 listed "no `[role="status"]` anywhere" among its proof for removing the status line. The project's principle 7 forbids a check that a removed thing is absent.

### Occurrences
- Execution: SEED-066#unobtrusive-selection-aware-spoken-title (first implementation commit 57a659f018)
  - Timestamp: 2026-10-11T08:00:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `7d062d701f:.planning/slice-plans/065-unobtrusive-spoken-title/PLAN.md` slice 5 Proof; `CLAUDE.md` principle 7 ("no check that it is absent"); the coordinator's slice 5 brief ("principle 7 overrides that: do not add absence assertions"); slice 5 return, decision 2
  - Observed effect: the coordinator caught it while writing the brief and the test was not written; the plan had passed slice planning and a readiness assessment with the instruction in it
  - Inference: planning checked the removal as deletion plus sweep in its scope text but not in each slice's proof list

## DD-228 — A plan's observation slice said to remove its scratch changes while the next slice said to keep its test

Plan 066 slice 1 was stop-safe by "record learning and remove scratch changes; no product change ships", and slice 2 said "Retain the real HTTP startup regression from slice 1". The plan did not say where the probe test lives between the two deliveries.

### Occurrences
- Execution: SEED-067#start-e2e-after-migrations (first implementation commit f297a916c5)
  - Timestamp: 2026-10-11T09:05:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `1a654d0999:.planning/slice-plans/066-migration-aware-http-readiness/PLAN.md` slices 1 and 2; slice 1 brief ("Leave the harness test in the working tree, uncommitted"); commit `0a7ca0a799` (plan only) while `HealthCheckStartupReadinessTest.java` stayed untracked until `f297a916c5`
  - Observed effect: the coordinator chose to publish a plan-only commit and carry the passing baseline test untracked into slice 2; nothing was lost, and slice 2 reused it without rebuilding
  - Inference: a test that asserts the defect cannot be committed, so an observation slice followed by its fix has no stated hand-over; an interruption between the two deliveries would have left the only copy in an untracked file

## DD-229 — Slices that only observe or record had no change for the refactor step, and the coordinator skipped it without guidance

Plan 066 slices 1, 3 and 4 delivered only plan records. Slice wrap-up lists a fresh post-change-refactor agent as step 1 of every delivery and names no case with nothing to refactor.

### Occurrences
- Execution: SEED-067#start-e2e-after-migrations (first implementation commit f297a916c5)
  - Timestamp: 2026-10-11T09:20:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `.claude/skills/dough-execute-plan/references/wrap-up.md` "Deliver the change" step 1; commits `0a7ca0a799`, `fe3a14172b`, `af62c7cd0a` (each one file, the plan); one refactor agent ran, for `f297a916c5`
  - Observed effect: three deliveries skipped the refactor agent on the coordinator's own judgment; the one refactor that ran found and removed a triplicated datasource URL
  - Inference: spawning a refactor agent for a plan-only commit would have cost a full agent run for no possible finding; the guidance leaves that as an unstated deviation

## DD-230 — An environment-only Flyway callback with a MySQL named lock held a real startup migration open for full-stack proof

To observe the runner during a migration, the execution supplied `beforeMigrate.sql` through `SPRING_FLYWAY_LOCATIONS` from a directory outside the checkout; the file waited on a named lock that a separate session held and released. No product or test code and no delay option was added.

### Occurrences
- Execution: SEED-067#start-e2e-after-migrations (first implementation commit f297a916c5)
  - Timestamp: 2026-10-11T09:18:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: plan 066 Learnings "Slice 3" and "Slice 4" at `af62c7cd0a`; `sut.log` lines 148 to 234 in the execution worktree (`Executing SQL callback: beforeMigrate`, `Successfully applied 41 migrations`)
  - Observed effect: both `pnpm e2e:hold` and `pnpm cy:run` were observed returning 503 during the held migration and resetting test data only after the first 200; each slice took about five minutes including startup
  - Inference: useful practice: a timing fault was proved at the real boundary with a release barrier instead of a sleep or a stand-in response; it depends on the runner passing its environment to the backend, which was confirmed with `ps eww`

## DD-231 — A plan's proof table left out endings the story states, and implementers returned the slices complete with those endings untested

The story lists how a voice-input session ends or never starts: the microphone cannot start, nothing was heard, leaving the note. Plan 067's proof table named the landing place, the read-only state, failure and retry, but not those endings. Each implementer delivered the table and listed the endings as untested in a complete report.

### Occurrences
- Execution: SEED-066#read-only-voice-input-with-insertion-feedback (first implementation commit 7557329339)
  - Timestamp: 2026-10-11T11:00:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: plan 067 "Outside-in proof ownership" at `783d3f96cd`; slice 1 first return, "Not covered by a test" (microphone cannot start, nothing heard with an editor open, Quill re-enabled on leaving); the three tests added in the second round at `7557329339`; the DD-224 occurrence of this execution for the title
  - Observed effect: slices 1 and 2 each needed a second implementation round after the coordinator compared the return with the story's Scope; the body endings already held, and the title ending was a real defect
  - Inference: the delegation carried the proof table as the promise list; carrying the story's session-end list beside it would likely have saved both rounds
