# DearDough Process Findings

Reviewed 2026-10-12 (Asia/Tokyo). Active shared-process findings only; released
responses under observation live in the [watch list](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/near-term-watch-list.md).
Detailed observations, original releases and qualified inferences are retained
in the linked Open Dough catalog. Unknown execution provenance remains unknown.

- Highest allocated local number: 237. Removed numbers are never reused.
- Full source recovery before this cleanup: `0a8878e98be76b8e92203515d8f4719489b24169:DearDough.md`.
- Project-owned findings remain in [Donut Retrospective Findings](DonutRetrospectiveFindings.md); their ownership review is unchanged.

<a id="odf-110"></a>

## ODF-110 — Incomplete readiness replays

Former local code: DD-109. Former local code: DD-163. Former local code: DD-168. Former local code: DD-171. Former local code: DD-207. A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop. Open after verified later recurrence; not resolved. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-110).

<a id="odf-121"></a>

## ODF-121 — Transient transport failure ends observation

Former local code: DD-115. A reported GitHub transport failure ends observation and leaves later revisions without notification coverage. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-121).

<a id="odf-106"></a>

## ODF-106 — Colliding plan numbers

Former local code: DD-118. Concurrent unpublished plan allocation produces two quick plans with the same numeric selector, making a number-only execution request ambiguous. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-106).

<a id="odf-074"></a>

## ODF-074 — Unverified planning premises

Former local code: DD-126. Former local code: DD-130. Former local code: DD-134. Former local code: DD-139. Former local code: DD-147. Former local code: DD-162. Former local code: DD-167. Former local code: DD-209. Concrete only-caller and host-state premises enter a plan without inspection, forcing a changed decision or stopped implementation when checked. Open after verified later recurrence; not resolved. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-074).

<a id="odf-147"></a>

## ODF-147 — Reasoned red proof leaves ineffective tests

Former local code: DD-127. An agent reasons that new tests would fail instead of observing them against pre-change behavior; round-trip or wrong-boundary tests can pass without the fix. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-147).

<a id="odf-100"></a>

## ODF-100 — Masked formatter failure

Former local code: DD-135. A formatter piped through tail returns the final pipeline stage's success, allowing subsequent delivery steps after formatter failure. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-100).

<a id="odf-152"></a>

## ODF-152 — File-size gate conflicts with approved intermediate scope

Former local code: DD-138. An absolute changed-file size check conflicts with an approved staged decomposition and mechanical edits to pre-existing oversized callers. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-152).

<a id="odf-189"></a>

## ODF-189 — Small additions force unrelated size extractions

Former local code: DD-144. A small addition crosses the numeric file-size ceiling and triggers relocation and re-proof of a substantial previously untouched responsibility. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-189).

<a id="odf-190"></a>

## ODF-190 — Planned observation route is unavailable

Former local code: DD-145. Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof. Released response; watch start unknown. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-190).

<a id="odf-195"></a>

## ODF-195 — Pre-existing size overrun handled inconsistently

Former local code: DD-160. Two refactor passes within one story treat barely touched, already oversized files differently, forcing an unrelated extraction in one and retaining another. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-195).

<a id="odf-141"></a>

## ODF-141 — Fixed refactor cost on small accepted slices

Former local code: DD-176. Every small slice launches a fresh full refactor agent even when the accepted diff needs no further changes, creating substantial repeated cost. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-141).

<a id="odf-208"></a>

## ODF-208 — Observer worker exits without a recorded cause

Former local code: DD-202. The CI observer's worker exits mid-execution without a terminal result or a reported transport error, ending notification coverage. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-208).

<a id="odf-213"></a>

## ODF-213 — Refinement reproduces a test mechanism already forbidden

Former local code: DD-204. Refinement makes reproducing a flaky mock the prerequisite for a remedy although the project’s existing rules already forbid that mock. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-213).

<a id="odf-214"></a>

## ODF-214 — A planned real observation loses a contract distinction

Former local code: DD-206. Replacing captured router push/replace calls with current-location proof cannot distinguish the behaviors the story promises to preserve. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-214).

<a id="odf-216"></a>

## ODF-216 — Landing retires a workspace with a remaining continuation

Former local code: DD-212. Explicitly authorized landing removes an execution workspace while the Taken story still has a final real-stack check and wrap-up owed. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-216).

<a id="odf-185"></a>

## ODF-185 — Reported story gap filed as a learning

Former local code: DD-214. A reported preservation gap or provisional behavior is accepted as a learning without checking the whole story and the later slices that depend on it. Released response; watch start unknown. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-185).

<a id="odf-142"></a>

## ODF-142 — Abbreviated delivery SHAs produce misleading refusals

Former local code: DD-216. An abbreviated published-base SHA is accepted as an argument but compared without normalization, causing misleading delivery refusal and fallback publication without observation. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-142).

<a id="odf-206"></a>

## ODF-206 — Over-budget slice not stopped

Former local code: DD-217. Former local code: DD-222. The slice time bound is checked only at hand-back, so a delegated slice runs past it and is accepted or extended without re-sizing. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-206).

<a id="odf-217"></a>

## ODF-217 — Unverified refactor proof effects

Former local code: DD-218. A refactor misclassifies or omits the proof effects of a change, leaving reached consumers unrun; some returns incorrectly call changed behavior identical or type-only. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-217).

<a id="odf-242"></a>

## ODF-242 — Correction queued after its repair already exists

Former local code: DD-219. Retrospective queues and takes a full correction story despite its exact repair already being committed on the same branch, producing a claim, workspace, plan and wrap-up but no product/test change. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-242).

<a id="odf-243"></a>

## ODF-243 — Missing story source is validated only after a gap

Former local code: DD-221. A plan without the mandatory Source link passes story-obligation list/check while its record is empty, then the first real obligation unexpectedly blocks delivery. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-243).

<a id="odf-244"></a>

## ODF-244 — Real microphone proof is assigned without an agent access route

Former local code: DD-223. A plan assigns a real-microphone visual observation to an unattended agent, then replaces it with synthetic screenshots and leaves the real-input check to the owner. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-244).

<a id="odf-245"></a>

## ODF-245 — Component lifetime changes miss captured state

Former local code: DD-224. Planning moves or reuses a session-owning component across note changes without reconciling its per-instance captures and termination, causing stale note targeting or a retained title lock. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-245).

<a id="odf-246"></a>

## ODF-246 — Observation-to-fix slices contradict scratch retention

Former local code: DD-228. An observation slice instructs scratch removal while the next slice requires its regression test, leaving the only passing baseline test untracked between deliveries. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-246).

<a id="odf-180"></a>

## ODF-180 — Measurement-slice delivery ownership is unclear

Former local code: DD-229. A measurement plan assigns record/prototype work to the coordinator while execution guidance still requires fresh implementation and refactor passes, producing case-by-case deviations. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-180).

<a id="odf-247"></a>

## ODF-247 — Plan proof omits promised session endings

Former local code: DD-231. A slice proof table omits explicit story end conditions, so implementers return it complete while listing those conditions untested; the coordinator needs second rounds to compare with the full story. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md#odf-247).

## DD-233 — Broad startup reads exceeded the response budget and forced repeated retrieval

The coordinator combined complete execution references and an unbounded tool-metadata listing, then batched another large context read. Both responses were truncated before first-slice delegation, so authoritative passages had to be retrieved again. The targeted-retrieval guidance already applied; this records an agent execution deviation rather than missing workflow guidance.

### Occurrences
- Execution: SEED-071#removal-proof-and-evidence-recurrence (first implementation commit 6fadfdbf13bd0e105042d0ff71cc404b36c7f577)
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: 0.3.60
  - Evidence: dashboard launch fb81e2c8-229c-473e-ba0c-0df74a951f5e, first two startup functions.exec responses on 2026-10-12 reported 20,868 and 20,631 original output tokens and truncation; subsequent calls retrieved the plan, seed, and individual execution references again before slice 1 delegation
  - Observed effect: the startup outputs did not expose all requested context; additional targeted reads were needed
  - Inference: limiting each read to the next execution boundary and bounding metadata discovery would avoid this duplication; no elapsed-time or total-token saving was measured
- Execution: SEED-075#story-1 (first implementation commit 723e69c4835c9124e3387ecc999559ec0bbac95d)
  - Timestamp: unknown
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.60
  - Evidence: dashboard launch 1c7f003f-4c5f-49e6-92f2-404ce5e48cf8 on 2026-10-12; two startup Bash reads that concatenated whole execution references returned "Output too large" (30.5KB and 32.2KB) with a 2KB preview, and each was then read again in full through a second call before slice 1 delegation
  - Observed effect: about 1,700 lines of references were loaded before a two-slice execution, two of the reads twice
  - Inference: reading only the passages the next boundary needed would have avoided the second calls; no token or time saving was measured

## DD-234 — A comparison of empty captures was printed as a match

During proof acceptance the coordinator compared two captured measurements with `diff ... && echo SAME`. The output filter had matched nothing, so both captures were empty and the command printed `SAME`. The coordinator noticed the empty files and repeated the measurement; a third attempt was needed because the test command ran from the wrong directory. An agent execution deviation, not missing workflow guidance.

### Occurrences
- Execution: SEED-075#story-1 (first implementation commit 723e69c4835c9124e3387ecc999559ec0bbac95d)
  - Timestamp: unknown
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.60
  - Evidence: dashboard launch 1c7f003f-4c5f-49e6-92f2-404ce5e48cf8 on 2026-10-12, slice 1 proof acceptance; four Bash runs of a temporary toolbar-position spec, the first two printing `SAME` for two 0-line captures, the third failing with `Command "frontend:test" not found`, the fourth showing identical control positions before and after the change
  - Observed effect: three extra measurement runs; the story obligation G1 was accepted only on the fourth, non-empty comparison
  - Inference: asserting that a capture is non-empty before comparing it would have exposed the first failure at once

## DD-235 — A later slice's failing example was already cured by the earlier slice

The plan recorded each symptom only on unchanged code. Slice 2's proof named the More options menu off screen at 390px and at 320px, but slice 1's corrected measurement already put the 390px menu inside the screen, so the 390px case passes with and without slice 2's change. Possibly related to ODF-074; the premise here was observed, only not re-observed after the earlier planned change.

### Occurrences
- Execution: SEED-075#story-1 (first implementation commit 723e69c4835c9124e3387ecc999559ec0bbac95d)
  - Timestamp: unknown
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.60
  - Evidence: `.planning/slice-plans/004-mobile-note-toolbar-one-row/PLAN.md` at 011af9ee1eb95af54fe65df5c1d2ff06821e8de2, settled premises (390px menu at left −206px on unchanged code) and slice 2 proof; slice 2 implementation return measured 45px to 325px at 390px before its change and −5px at 320px
  - Observed effect: one of the two planned real-layout menu cases does not discriminate the slice's change; the 320px case does
  - Inference: observing the later slice's symptom with the earlier slice's temporary edit applied would have shown which example carries the proof

## DD-236 — No obligation disposition fits an earlier defect the story leaves unchanged

An implementation return named a defect that existed before the story and that the story neither caused nor promised to fix. Story-obligation recording requires exactly one disposition for every named gap, and none describes "earlier defect, unchanged, reported onward": the coordinator recorded `no user cost`, whose wording says the gap costs the user nothing, although the defect itself does cost the user.

### Occurrences
- Execution: SEED-066#unified-voice-input-control (first implementation commit 94e79abf8aa8d658ceafead7ea6c0560e21c2349)
  - Timestamp: 2026-10-12T17:30:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.60
  - Evidence: `git show 6f73d976d5c36e91ba78d414b1487f2cf2204ce5:.planning/slice-plans/002-shared-voice-input-button/PLAN.md`, obligation G1; slice 4 return quoted there; `story-obligations.mjs check --slice 4` accepted the entry
  - Observed effect: the record's disposition reason had to explain that "no cost" means no cost added by this story; the defect reaches the owner only through the final report and product advice
  - Inference: one occurrence; whether a separate disposition is needed or the existing wording should cover this case is undecided

## DD-237 — Defect noticed in a refactor return is repaired by the coordinator after the refactor pass

A slice made one fragment list serve both answer matching and masking. Neither the plan's settled premises nor the implementation return covered a title with no text before its qualifier. The refactor return listed that shape as an "observation, not acted on". The coordinator then wrote a failing test, confirmed the defect, and changed production code itself after the refactor pass, with focused suites rerun but no fresh implementation or refactor pass and no full-suite rerun. The fix also changed one accepted answer for that title shape without an owner decision.

### Occurrences
- Execution: SEED-076#story-1 (first implementation commit 0e2fc686d5b008166c1ababdc8cb1c602448cfb7)
  - Timestamp: 2026-10-12T17:20:00+09:00 (approximate, from the session clock)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: plan `3972c2dbcc346016b0ca5e2c807d96f4ee71e0d5:.planning/slice-plans/003-mask-spelling-answer-before-title-qualifier/PLAN.md` Current decisions; test `spellingQuestionLeavesClueReadableWhenNoTitleTextPrecedesTheQualifier` failed before the guard with every word boundary masked for title `~(phase transition)`; refactor return section "Observation, not acted on"
  - Observed effect: the defect was caught before publication, by the coordinator rather than by planned proof; the post-refactor edit had no independent refactor review
  - Inference: when a rule is widened from matching to masking, checking the degenerate inputs of the shared value (empty fragment) in the plan's premises would have placed this in the implementation pass; generality is unverified from one occurrence
