# DearDough Process Findings

Shared findings reviewed 2026-11-10 (Asia/Singapore). The linked catalog records
own the occurrence evidence and current responses. See the [watch list](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/near-term-watch-list.md)
and [Donut’s project findings](DonutRetrospectiveFindings.md) for their separate records.

- Highest allocated local number: 482. Removed numbers are never reused.
- [Full pre-compaction source and earlier recovery locators](https://github.com/nerds-odd-e/doughnut/blob/0436d21c6e59f75157ab29394e273f8af02fe3f9/DearDough.md).
- [Open Dough catalog at evidence retention](https://github.com/terryyin/open-dough/tree/784bf7f574cda84eefe505142262e58d42be6cf4/docs/maintainer/findings).
- [Source before the 2026-11-10 compaction](https://github.com/nerds-odd-e/doughnut/blob/d3eb0ef73aca13a5ed229738f92dbe44ee12bf95/DearDough.md); its rows are retained at the [Open Dough catalog](https://github.com/terryyin/open-dough/tree/5afe6fee8fe34322752e9e44911d72eb31dd4991/docs/maintainer/findings). DD-443 (disposable break checks) was pruned as successful practice, not a problem.

<a id="odf-110"></a>

## ODF-110 — Incomplete readiness replays

Former local code: DD-109. Former local code: DD-163. Former local code: DD-168. Former local code: DD-171. Former local code: DD-207. A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-110.md).

<a id="odf-121"></a>

## ODF-121 — Transient transport failure ends observation

Former local code: DD-115. A reported GitHub transport failure ends observation and leaves later revisions without notification coverage. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-121.md).

<a id="odf-106"></a>

## ODF-106 — Colliding plan numbers

Former local code: DD-118. Concurrent unpublished plan allocation produces two quick plans with the same numeric selector, making a number-only execution request ambiguous. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-106.md).

<a id="odf-074"></a>
<a id="dd-337"></a>
<a id="dd-337--plan-premises-were-checked-in-a-different-form-from-how-they-were-used"></a>

## ODF-074 — Unverified planning premises

Former local code: DD-126. Former local code: DD-130. Former local code: DD-134. Former local code: DD-139. Former local code: DD-147. Former local code: DD-162. Former local code: DD-167. Former local code: DD-209. Former local code: DD-337. Concrete premises enter a plan after a proxy check that omits the consuming conditions. SEED-100's Python isolation and span premises required review. In SEED-108, the scratch test missed a Stability-derived due time overriding the builder's explicit time. The 0.3.90 planning transcript verifies use of the response rule. The test-builder repair landed in `5002ad7856`. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-074.md).

<a id="odf-147"></a>

## ODF-147 — Reasoned red proof leaves ineffective tests

Former local code: DD-127. An agent reasons that new tests would fail instead of observing them against pre-change behavior; round-trip or wrong-boundary tests can pass without the fix. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-147.md).

<a id="odf-100"></a>

## ODF-100 — Masked formatter failure

Former local code: DD-135. A formatter piped through tail returns the final pipeline stage's success, allowing subsequent delivery steps after formatter failure. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-100.md).

<a id="odf-152"></a>

## ODF-152 — File-size gate conflicts with approved intermediate scope

Former local code: DD-138. An absolute changed-file size check conflicts with an approved staged decomposition and mechanical edits to pre-existing oversized callers. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-152.md).

<a id="odf-189"></a>

## ODF-189 — Small additions force unrelated size extractions

Former local code: DD-144. A small addition crosses the numeric file-size ceiling and triggers relocation and re-proof of a substantial previously untouched responsibility. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-189.md).

<a id="odf-190"></a>

## ODF-190 — Planned observation route is unavailable

Former local code: DD-145. Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-190.md).

<a id="odf-195"></a>

## ODF-195 — Pre-existing size overrun handled inconsistently

Former local code: DD-160. Two refactor passes within one story treat barely touched, already oversized files differently, forcing an unrelated extraction in one and retaining another. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-195.md).

<a id="odf-141"></a>

## ODF-141 — Fixed refactor cost on small accepted slices

Former local code: DD-176. Every small slice launches a fresh full refactor agent even when the accepted diff needs no further changes, creating substantial repeated cost. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-141.md).

### Occurrences
- Execution: SEED-118#export-a-note-with-recent-quiz-questions (first implementation `7089269e2e`)
  - Timestamp: 2026-11-10T15:48:00+08:00
  - Tool: Cursor
  - Open Dough release: 0.3.94
  - Evidence: plan `089-export-recent-note-questions` slices 1–4; published SHAs `7089269e2e`, `1c6d3e9945`, `011385133d`, `52e74feaa5`; refactor agents `0ae1c0d1` (slice 1, 15:48), `7fe03ae0` (slice 2, 16:03), `56d8503d` (slice 3, 16:15), `c7d4aaa4` (slice 4, 16:28) under parent `75e2007b-148e-453a-91c5-435dd0684d36`
  - Observed effect: four consecutive post-slice refactor agents. Slices 1–2 returned `none — already clean` / no edits. Slice 3 collocated latest-selection folding into `RecallPromptRepository` and synced OpenAPI description. Slice 4 trimmed sibling test assertions only (production unchanged).
  - Inference: same fixed per-slice refactor launch cost as ODF-141; two empty passes and one test-doc-only pass paid the full agent cost. Slice 3’s cohesion edit shows the pass is not always empty. Token/time cost of the empty passes was not separately measured here.

<a id="odf-208"></a>

## ODF-208 — Observer worker exits without a recorded cause

Former local code: DD-202. The CI observer's worker exits mid-execution without a terminal result or a reported transport error, ending notification coverage. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-208.md).

<a id="odf-213"></a>

## ODF-213 — Refinement reproduces a test mechanism already forbidden

Former local code: DD-204. Refinement makes reproducing a flaky mock the prerequisite for a remedy although the project’s existing rules already forbid that mock. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-213.md).

<a id="odf-214"></a>

## ODF-214 — A planned real observation loses a contract distinction

Former local code: DD-206. Replacing captured router push/replace calls with current-location proof cannot distinguish the behaviors the story promises to preserve. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-214.md).

<a id="odf-216"></a>

## ODF-216 — Landing retires a workspace with a remaining continuation

Former local code: DD-212. Explicitly authorized landing removes an execution workspace while the Taken story still has a final real-stack check and wrap-up owed. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-216.md).

<a id="odf-185"></a>

## ODF-185 — Reported story gap filed as a learning

Former local code: DD-214. A reported preservation gap or provisional behavior is accepted as a learning without checking the whole story and the later slices that depend on it. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-185.md).

<a id="odf-142"></a>

## ODF-142 — Abbreviated delivery SHAs produce misleading refusals

Former local code: DD-216. An abbreviated published-base SHA is accepted as an argument but compared without normalization, causing misleading delivery refusal and fallback publication without observation. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-142.md).

<a id="odf-206"></a>

## ODF-206 — Over-budget slice not stopped

Former local code: DD-217. Former local code: DD-222. The slice time bound is checked only at hand-back, so a delegated slice runs past it and is accepted or extended without re-sizing. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-206.md).

<a id="odf-217"></a>

## ODF-217 — Unverified refactor proof effects

Former local code: DD-218. A refactor misclassifies or omits the proof effects of a change, leaving reached consumers unrun; some returns incorrectly call changed behavior identical or type-only. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-217.md).

<a id="odf-242"></a>

## ODF-242 — Correction queued after its repair already exists

Former local code: DD-219. Retrospective queues and takes a full correction story despite its exact repair already being committed on the same branch, producing a claim, workspace, plan and wrap-up but no product/test change. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-242.md).

<a id="odf-243"></a>

## ODF-243 — Missing story source is validated only after a gap

Former local code: DD-221. A plan without the mandatory Source link passes story-obligation list/check while its record is empty, then the first real obligation unexpectedly blocks delivery. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-243.md).

<a id="odf-244"></a>

## ODF-244 — Real microphone proof is assigned without an agent access route

Former local code: DD-223. A plan assigns a real-microphone visual observation to an unattended agent, then replaces it with synthetic screenshots and leaves the real-input check to the owner. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-244.md).

<a id="odf-245"></a>

## ODF-245 — Component lifetime changes miss captured state

Former local code: DD-224. Planning moves or reuses a session-owning component across note changes without reconciling its per-instance captures and termination, causing stale note targeting or a retained title lock. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-245.md).

<a id="odf-246"></a>

## ODF-246 — Observation-to-fix slices contradict scratch retention

Former local code: DD-228. An observation slice instructs scratch removal while the next slice requires its regression test, leaving the only passing baseline test untracked between deliveries. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-246.md).

<a id="odf-180"></a>

## ODF-180 — Measurement-slice delivery ownership is unclear

Former local code: DD-229. A measurement plan assigns record/prototype work to the coordinator while execution guidance still requires fresh implementation and refactor passes, producing case-by-case deviations. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-180.md).

<a id="odf-247"></a>

## ODF-247 — Plan proof omits promised session endings

Former local code: DD-231. A slice proof table omits explicit story end conditions, so implementers return it complete while listing those conditions untested; the coordinator needs second rounds to compare with the full story. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-247.md).

<a id="odf-264"></a>

## ODF-264 — Broad startup reads exceeded the response budget and forced repeated retrieval

Former local code: DD-233. Broad whole-reference and metadata batches exceeded the response budget and truncated required context before delegation, forcing repeated retrieval despite existing targeted-reading guidance. Execution provenance and counts remain with the linked evidence owner; savings are unmeasured. This agent deviation is distinct from ODF-158’s required-reference cost. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-264.md).

### Occurrences
- Execution: SEED-117#find-the-note-and-property-after-recall; first implementation 8b9e70d9fb0f6ec7f11b30595278c29c6709f2c4
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: launch 5a2c1855-2aed-40c0-bd75-6e21ae06957b conversation, 2026-11-10; startup combined unit-testing/agent-map/plan/package read returned original token count 11774 with truncation; subsequent combined plan/seed/trace/backlog read returned 12465 with truncation. Retrospective combined full ADR read repeated the problem (12411 tokens).
  - Observed effect: coordinator retrieved missing source/guidance again; targeted passages were sufficient when used.
  - Inference: avoidable agent retrieval deviation recurred despite targeted-reading guidance; token/time savings are unmeasured.

### Occurrences
- Execution: SEED-121#publish-large-notebooks-without-losing-history
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: 2026-11-10 resumed execution; coordinator tool results combined whole Execute Plan finish/CI/source/obligation references, then aggregate retrospective traces, exceeding output budgets and requiring targeted retrieval. Git recovery:59db9d970d2be39e5d7825e26118bbc455e52a0f retains the reviewed source/proof homes.
  - Observed effect: multiple truncated responses and repeated reads during an otherwise settled owner journey; no measured token/time saving is claimed.
  - Inference: coordinator batching repeated this known deviation despite targeted-reading guidance; the product outcome remained independently verified.

<a id="odf-265"></a>

## ODF-265 — A comparison of empty captures was printed as a match

Former local code: DD-234. A proof comparison printed SAME because both filtered measurement captures were empty. The coordinator noticed and reran it; accepted proof came from the fourth, non-empty comparison. One SEED-075#story-1 execution, also represented by ODF-264 and ODF-266. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-265.md).

<a id="odf-266"></a>

## ODF-266 — A later slice's failing example was already cured by the earlier slice

Former local code: DD-235. Planning observed later-slice symptoms only on unchanged code; an earlier slice already cured the 390px menu case, so that case did not discriminate the later edit. The 320px case still did. One SEED-075#story-1 execution, shared with ODF-264 and ODF-265; the relationship to ODF-187 and ODF-074 remains uncertain. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-266.md).

<a id="odf-267"></a>

## ODF-267 — No obligation disposition fits an earlier defect the story leaves unchanged

Former local code: DD-236. The required obligation vocabulary had no truthful disposition for an earlier defect left unchanged by the story. The coordinator used no user cost, then qualified that the defect still costs the user. One 0.3.60 execution; this vocabulary gap is distinct from ODF-223’s unsupported path judgment and ODF-270’s pending-decision case. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-267.md).

<a id="odf-268"></a>

## ODF-268 — Defect noticed in a refactor return is repaired by the coordinator after the refactor pass

Former local code: DD-237. A refactor return named an unhandled empty-title-fragment case; the coordinator then wrote red proof and changed behavior after the refactor, without a fresh independent pass or full-suite rerun, and changed one accepted answer without an owner decision. Caught before publication. One execution, release unknown; relationship to ODF-219 remains uncertain. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-268.md).

<a id="odf-269"></a>

## ODF-269 — A plan took an ADR's rebuild statement as a fact about stored history

Former local code: DD-238. Planning treated an ADR’s rebuild-from-logs statement as proof of complete historical coverage. Older state predated log writing; the owner changed the starting-state decision during execution, adding a slice and replacing an entry point already written. One SEED-073#story-7 execution, release unknown; relationship to ODF-074 remains uncertain. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-269.md).

<a id="odf-270"></a>

## ODF-270 — No obligation disposition fits a gap that waits for an owner decision

Former local code: DD-239. The required obligation vocabulary could not keep a gap open for an owner decision. The coordinator used interim until the last slice despite that slice having no responsibility for it; other no-user-cost labels needed cost qualifications. Related to former DD-236 (ODF-267), with a distinct pending-decision problem. One SEED-073#story-7 execution, release unknown. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-270.md).

<a id="odf-271"></a>

## ODF-271 — A cross-slice defect passed every slice acceptance and was found by an aggregate review

Former local code: DD-240. Acceptance reasoned that a repeat confirmation stayed stable despite the return naming two exclusions on one tracker as untested. Their interaction restored an earlier excluded attempt’s effect. An independent aggregate review found the branch defect and the owner authorized slice 11’s repair. One SEED-073#story-7 execution, release unknown; relationship to ODF-223 remains uncertain. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-271.md).

### Occurrences
- Execution: SEED-119#highlight-and-link-to-a-specific-property-item / 3349feafb0163ea7c0a72d54562b5638a9fa1b52
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: modified; review snapshot88061832511d83ae3b7cc0197c827bfd57d145de; base release unknown
  - Evidence: 2026-11-10, isolated cached-note review `/tmp/seed119-cache-review-result.log`, G8/refinement1eab56a4becd601c665bcbd05cdc113058177325, maintained NoteTextContent.propertyHistoryNoteChange.spec.ts.
  - Observed effect: original eight slices,407files/2514 tests and exact branch CI passed, then a mounted Cancel/note-reuse check deleted prior-note11 instead of current22. Related same-note history after Cancel also bypassed a new warning. The inherited guard memoization had no correct note/action lifetime; this execution exposed it through item removal. The original completion judgment was withdrawn and one bounded same-story leaf restored the matching-history promise with pending-only reads and three regressions;408files/2517 tests and both types passed.
  - Inference: the prior per-slice proof omitted reused-editor/current-history continuation. This establishes an inherited weakness reached by the feature, not that this execution introduced the original cache.

<a id="odf-272"></a>

## ODF-272 — Shared accessible-name consumers were missed before delivery

Former local code: DD-248. Naming a shared modal’s outer dismissal Close collided with an existing footer Close. Selected frontend proof passed while seven ambiguous-button failures across four E2E features escaped to two CI runs. The outer dismissal was repaired to Close modal and existing E2E consumers passed unchanged. One SEED-073#story-9 execution, release unknown; relationships to ODF-150 and ODF-107 remain uncertain. The product repair does not establish shared prevention. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-272.md).

<a id="odf-273"></a>

## ODF-273 — Full verification started before the intended edits settled

Former local code: DD-249. Required whole-suite verification began while intended test or template edits were still changing, requiring another run on settled source. One SEED-073#story-2 execution reports an extra full backend run and premature frontend/typecheck results. This sequencing deviation excludes necessary reruns after newly discovered gaps and differs from ODF-225’s repair-stash invalidation. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-273.md).

<a id="odf-308"></a>
<a id="dd-251"></a>

## ODF-308 — A plan removed the only remaining sign-in control before moving it

Former local code: DD-251. A plan removed the sole sign-in route one slice before its replacement. One SEED-078#story-1 execution on 0.3.66; corrected before implementation at the cost of one plan edit. Relationship to ODF-210 remains qualified. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-308.md).

<a id="odf-309"></a>
<a id="dd-252"></a>

## ODF-309 — Required build proof fails on formatting that implementers may not run

Former local code: DD-252. Required build/generation proof checks formatting before coordinator-owned formatting, producing red proof or manual layout repair before that stage. Two SEED-078 executions (story-1 and story-5), first on 0.3.66 and second release unknown; repeated builds, no escaped defect. Distinct from ODF-285’s observed formatter-ownership violation. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-309.md).

<a id="odf-310"></a>
<a id="dd-253"></a>

## ODF-310 — Retargeting E2E page objects missed a direct label lookup of a moved control

Former local code: DD-253. Helper-name searches missed direct accessible-label lookups of newly hidden controls. Two SEED-078 executions (story-1 and story-5), also represented by ODF-309, each needed a CI repair; first reports 0.3.66, second release unknown. Product repairs do not establish shared prevention. Relationships to ODF-150 and ODF-272 remain uncertain. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-310.md).

<a id="odf-311"></a>
<a id="dd-254"></a>

## ODF-311 — A deviation from a recorded plan decision was accepted as a placement choice and lost the decision's purpose

Former local code: DD-254. Acceptance treated a deviation from an explicit stable-component placement decision as routine layout; per-state presence proof missed losing an open panel when loading completed. One SEED-078#correction-1 execution on 0.3.66; CI failure and repair, about 25 minutes with a red tip. Product repair 484ce25 does not prove shared acceptance prevention. Relationships to ODF-245 and ODF-290 remain qualified. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-311.md).

<a id="odf-312"></a>
<a id="dd-255"></a>

## ODF-312 — Asking the refactor pass to verify one named promise caught a regression that slice proof missed

Former local code: DD-255. A named promise check caught a same-page Account-action closure regression previously masked by incidental outside-click behavior. Useful successful practice: fixed before delivery in an approximately ten-minute refactor pass. The refactor agent also added behavior beyond its normal role, a qualified concern related to ODF-268. Same SEED-078#correction-1 execution as ODF-311, on 0.3.66; do not count the successful check as an escaped defect. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-312.md).

<a id="odf-313"></a>
<a id="dd-256"></a>

## ODF-313 — A planned removal sweep promised an empty reading for a term that already matched retained code

Former local code: DD-256. A planned empty removal sweep used a pattern that already matched retained code. One SEED-078#story-2 execution on 0.3.66; narrowed acceptance reading, one judgment and plan note, no defect. Relationship to ODF-074 remains uncertain. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-313.md).

<a id="odf-314"></a>
<a id="dd-257"></a>
<a id="dd-446"></a>

## ODF-314 — An implementation return omitted plan-owned proof rows listed in its delegation, and the returned rows found a defect

Former local code: DD-257. Former local code: DD-446 (SEED-113#keep-the-assimilation-panel-in-place-after-staying-actions only). An implementation return omitted proof rows already named by both plan and delegation. Coordinator comparison returned the gap; missing proof then caught header overflow before delivery, adding about six implementation minutes. One SEED-078#story-3 execution on 0.3.66, shared with ODF-206. Distinct from ODF-247’s omission in the plan itself. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-314.md).

<a id="odf-315"></a>
<a id="dd-258"></a>

## ODF-315 — A rollover rule left the probe's own running state unconsidered and was caught from the implementer's gap list

Former local code: DD-258. A rollover rule omitted the offered probe already running; the implementer disclosed the gap and the coordinator added a rule before asking whether the owner wanted it. One SEED-079#story-1 execution on 0.3.66; about four extra minutes, then the owner reverted the rule that day as an extreme edge case. No delivered defect established; the owner’s simple-rule decision does not authorize restoring the extension. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-315.md).

<a id="odf-316"></a>
<a id="dd-274"></a>

## ODF-316 — Consumer discovery treats execution evidence as canonical work

Former local code: DD-274. Consumer discovery treated execution-evidence Identity lines and non-story anchors as canonical work, producing duplicate-home and unreadable-story warnings. One SEED-080#story-1 execution, release unknown; source inspection found no current dependency agreements and preserved unrelated SEED-066 evidence. No dependency was silently satisfied. Relationship to other discovery/readiness findings remains uncertain. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-316.md).

<a id="odf-150"></a>

## ODF-150 — Implementation proof misses indirect consumers

Former local code: DD-319. Acceptance traced the unchanged transport API but not the existing E2E journeys whose preconditions the explicit question-load recovery changed; seven failures reached CI in SEED-081#story-2 and were repaired in the execution. Distinct from ODF-272 and ODF-310. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-150.md).

### Occurrences
- Execution: SEED-119#highlight-and-link-to-a-specific-property-item / 3349feafb0163ea7c0a72d54562b5638a9fa1b52
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: modified; execution revision 8ab86f53f02c4cdb243ff0a2b34567d3a7f6c8a8; base release unknown
  - Evidence: 2026-11-10; CI run42770072237/attempt1 frontend job129675536405, autosaveTrash expected3 saves/got2; local deterministic red and repair c5134729baf5379be70bb458200128998793efd8.
  - Observed effect: stricter unknown-history refusal reached ordinary Markdown frontmatter removal in an existing mounted autosave/trash case omitted from slice7's indirect-consumer proof. Missing getNoteInfo data blocked its third save. Known-empty-history HTTP setup repaired the test; original expectations and exact third payload passed, with no production workaround.
  - Inference: symbol-based tracing did not expose the indirect edit-to-property-removal path. The owned CI repair restored proof; shared prevention remains unestablished.

<a id="odf-323"></a>

## ODF-323 — A caller-written plan-status mutation survives a later delivery refusal

Former local code: DD-320. Guarded delivery applied the coordinator's plan update before its staged-whitespace check refused; the retry needed an idempotent update. One SEED-082#story-1 execution, 0.3.67; no delivered defect. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-323.md).

<a id="dd-324"></a>

## ODF-331 — Implementer takes a red baseline by overwriting a shared-checkout file

Former local code: DD-324. An implementer obtained a red baseline by swapping committed bytes into a live shared checkout, exposing concurrent work and proof to interruption. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-331.md).

<a id="dd-332--formatting-expands-a-file-after-the-last-size-check"></a>

## ODF-218 — Formatting expands a file after the last size check

Former local code: DD-332. The mandatory formatter can expand a file past the 250-line limit after the independent size check. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-218.md).

## ODF-333 — Lifecycle duties enter the slice acceptance gate

Former local code: DD-333. The last slice initially required observations of 18 future-phase duties. Reallocating those duties let acceptance proceed while preserving the mandatory continuation. The report establishes a coordinator allocation mismatch; shared responsibility and the relationship to ODF-267/270 are qualified. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-333.md).

## ODF-413 — A commitment was recorded as observed before its output was inspected

Former local code: DD-334. The coordinator marked the OpenAPI commitment observed before generating or inspecting the output. Later inspection found three missing fields and opened obligation G1. The schema collision belongs to [DD-335 in Donut’s project findings](DonutRetrospectiveFindings.md#dd-335). [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-413.md).

## ODF-139 — A plan narrowed a story commitment and only the source-only review exposed it

Former local code: DD-336. Slice 3 narrowed the comparison to supported rows, and the run found none. The 0.3.82 source-only review preserved both baselines for all rows before implementation in SEED-100#story-2. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-139.md).

## ODF-252 — Execution was launched before an owner-held start input existed

Former local code: DD-338. SEED-100#story-2 launched with neither the required export nor a token. On 0.3.82, work stopped before slice 1 until the owner supplied the input. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-252.md).

## ODF-431 — A plan's lettered slice numbers were unreadable to the obligations script

Former local code: DD-339. Lettered slices failed the obligations reader before delegation. Sequential numbers and reallocated proof rows restored acceptance. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-431.md).

## ODF-432 — A plan decision chose a costlier mechanism than the story's own one-step case, and existing cost guards rejected it

Former local code: DD-340. The plan picked Git commit-range dating for a one-step web save. Four cost guards failed (39 tree fetches, cap 13); the comparison specified by the story passed. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-432.md).

## ODF-433 — A plan's no-op shortcut contradicted a story clause, and the implementer's gap list exposed it

Former local code: DD-341. The plan's no-net-change shortcut skipped dating an edit followed by a revert. The slice 6 gap report exposed the contradiction; two further rounds moved dating to the common acceptance point. The relation to ODF-139 remains qualified. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-433.md).

## ODF-434 — The coordinator sent a shell placeholder to the source-only reviewer

Former local code: DD-342. The source-only reviewer received a literal shell placeholder for the story. The coordinator stopped it and relaunched with verbatim text before it returned. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-434.md).

## ODF-435 — A test-removal plan checked only the story's examples and dropped a test's other observation

Former local code: DD-344. A deletion mapped every key example but dropped the only reformatting observation. The implementer's gaps exposed it; a reformat-only case was restored before delivery. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-435.md).

## ODF-436 — The source-review link form did not fit a plan whose Source section is a list

Former local code: DD-345. The documented Source review link failed when written as a list item under a Source heading. The obligations reader accepted the bare line. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-436.md).

## ODF-437 — A token-only removal sweep left natural-language wording that still described the deleted feature

Former local code: DD-346. A removal sweep checked deleted symbols and paths, while maintained docs still described local proof in ordinary prose. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-437.md).

<a id="odf-337"></a>
<a id="dd-347"></a>

## ODF-337 — Guarded delivery refused a renamed file's old path listed as owned

Former local code: DD-347. Guarded delivery refused a `git mv` source listed in `ownedPaths`; `stagedPaths` succeeded. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-337.md).

<a id="odf-471"></a>
<a id="dd-348"></a>

## ODF-471 — A consumer trace deferred a stale doc line to an out-of-scope later slice

Former local code: DD-348. A consumer-trace disposition deferred a stale doc to a later slice that did not own it. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-471.md).

<a id="odf-472"></a>
<a id="dd-438"></a>

## ODF-472 — A plan's leftover check named a temporary folder that changes with every Nix shell

Former local code: DD-438. A plan’s leftover check named a per-shell temporary folder. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-472.md).

<a id="odf-473"></a>
<a id="dd-440"></a>

## ODF-473 — The coordinator required the full frontend suite in every slice

Former local code: DD-440. Coordinator briefs required the full frontend suite in every slice. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-473.md).

<a id="odf-474"></a>
<a id="dd-441"></a>

## ODF-474 — A disclosure slice built a width rule that the docking slice replaced

Former local code: DD-441. A slice built a provisional width rule that the owning later slice replaced. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-474.md).

<a id="odf-475"></a>
<a id="dd-442"></a>

## ODF-475 — Successive slices extended an inherited reload mechanism that an aggregate review found unnecessary

Former local code: DD-442. Successive slices extended an inherited reload mechanism that an aggregate review found unnecessary. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-475.md).

<a id="odf-476"></a>
<a id="dd-444"></a>

## ODF-476 — CI observation was armed before CLI authentication was verified

Former local code: DD-444. CI observation was armed before CLI authentication was verified. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-476.md).

<a id="odf-477"></a>
<a id="dd-445"></a>

## ODF-477 — A repair publication started before a source amendment was handled

Former local code: DD-445. A repair publication started before an `amended` freshness result was handled. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-477.md).

<a id="odf-478"></a>
<a id="dd-447"></a>

## ODF-478 — Slice delivery published without that slice's source-review observations

Former local code: DD-447. A slice was published without its source-review observations. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-478.md).

## DD-479 — Merged repair restores were misclassified as wholly unapplied

Exact equality with the saved tree classified every merged file as unapplied when each also contained accepted repair edits. The restore had actually auto-merged draft content and reported a conflict. Its stored false-none then blocked exact stash cleanup and required a user-triggered resume.

### Occurrences
- Execution: SEED-119#highlight-and-link-to-a-specific-property-item / 3349feafb0163ea7c0a72d54562b5638a9fa1b52
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: 2026-11-10; affected snapshot c371659ac7121d1e83128c616fa1c0b391ac0867; ci-repair-stash restore/drop for OID266306cfa8064e45118b76c6581083a63bbcb2fd; unfinished dashboard receipt57246283-fdda-482d-8908-b608c63f8e4b; user requested fixing and retrying; repair b3ed6f70ea538888919593b2a13fb9a14b0e9c4f.
  - Observed effect: all draft edits were preserved, but the helper returned applied:none and then unapplied. New real-Git regression reproduced it. The local fix identifies actual unmerged paths as partial and supplies an explicit reviewed-OID resolution with checkout/index/conflict guards; ordinary no-apply refusal remains. Three recovery tests and25 CI-proof tests passed, and the retained real entry resolved/dropped by exact OID. A later CI pause restored its five saved paths and dropped only its own entry automatically.
  - Inference: tree inequality proves a merge differs, not that nothing applied. The local repair is delivered; propagation through the shared guidance release remains outside this product execution.

## DD-480 — An HTTP fixture invented the meaning of an empty assimilation value

The frontend treated any supplied propertyValue, including empty string, as a list item and wrote a passing blank-item fixture. The existing backend explicitly uses empty string for scalar units.

### Occurrences
- Execution: SEED-119#highlight-and-link-to-a-specific-property-item / 3349feafb0163ea7c0a72d54562b5638a9fa1b52
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown; execution-time snapshot fa51f486fca13f9f3c85eaebbec2ed93be8e8522
  - Evidence: 2026-11-10; CI run42762324814/attempt1 E2E job129650701015, Skip expected /n6/p/topic but received /n6/p/topic/i/1/~; backend PropertyFocus/AssimilationUnit documentation; repair c371659ac7121d1e83128c616fa1c0b391ac0867.
  - Observed effect: selected mocked proof and list continuation E2E passed before the existing scalar walkthrough failed. Positive scalar-empty red reproduced the defect; nonempty raw values retain item routing and manual blank selection remains a separate input path. The corrected workspace fixture,37 focused tests, both types and10 real E2E scenarios passed.
  - Inference: reuse of the key/value shape did not establish the wire sentinel. Read the producing contract before choosing a branch or manufacturing fixture variants; one observed execution does not establish recurrence.

## DD-481 — Full local frontend proof lost the browser connection despite fewer workers

A two-worker full frontend run ended before all files loaded. The existing CI configuration already serializes file imports and uses an orchestrator GC reporter; selecting that configuration completed the full required proof without assertion retries.

### Occurrences
- Execution: SEED-120#export-a-note-from-the-highlighted-property / 401bc9ff4a2bf02f4c4e922f2b87b1fb8b4e030f
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown; execution guidance snapshot 884a4fe8e463a1ea32120c7589cf808a82162285
  - Evidence: 2026-11-10 execution conversation; `/tmp/export-property-frontend-tests.log` (start18:17:41 Asia/Singapore), `/tmp/export-property-frontend-tests-serial.log`, `/tmp/export-ui-frontend.log`; retained acceptance at `7df14ec0c6:.planning/slice-plans/089-export-a-note-from-the-highlighted-property/source-review.md`; `frontend/vitest.config.ts` fileParallelism/orchestratorGcReporter.
  - Observed effect: `./scripts/run.sh pnpm frontend:test --maxWorkers=2` ended after259/408 files and1,950 passing tests with VITEST_BROWSER_CONNECTION_CLOSED (114.11s), without an assertion failure. `CI=true ./scripts/run.sh pnpm frontend:test --maxWorkers=2 --retry=0` then passed408files/2,521tests in262.65s and the next slice passed409files/2,528tests in245.98s. All runs were headless on the shared16CPU/48GiB local host; no production or test behavior was changed to obtain those passes.
  - Inference: worker reduction alone did not prevent this documented orchestrator failure mode. The existing serialized-import/GC mode is a supported bounded alternative for full local proof on a busy host. One failed run does not establish recurrence or a universal memory threshold; consider documenting the invocation in project testing guidance.

## DD-482 — An unterminated landed plan tail made appended evidence resemble a source conflict

Source freshness's three-way containment check treated coordinator-owned observations appended after an identical landed, unterminated final disposition as overlapping source edits. No new human story/plan amendment existed. Keep the substantive conflict gate; assess newline-aware comparison or clearer record-edit diagnostics without weakening it.

### Occurrences
- Execution: SEED-121#publish-large-notebooks-without-losing-history
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: 2026-11-10 slice14 acceptance; Take0715d7d1b2b25d0ca224a3909e14cdf43fad0d42, landed plan0019fa5e4ada4cc630b90131580dadd83fb84d8a, accepted record59db9d970d2be39e5d7825e26118bbc455e52a0f; source-freshness-comparison.mjs compareThreeWay/compareRecord and source-freshness.mjs fromContainedBase. Raw coordinator results retained three conflict refusals; read-only git merge-file localized the conflict to the same G6 disposition's EOF plus appended G7–G9.
  - Observed effect: extra source checks/inspection and record relayout. Preserving landed bytes and placing new obligations before existing G1 produced an unchanged comparison; no source scope, accepted proof or product behavior changed.
  - Inference: the unterminated tail and same-EOF additions explain this instance; other source-conflict behavior was not investigated. Coordinator condensation also complicated the first comparison and was partly reversed; exact cost is unmeasured.
