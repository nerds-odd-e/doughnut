# DearDough Process Findings

Shared findings reviewed 2026-11-08 (Asia/Singapore). The linked catalog records
own the occurrence evidence and current responses. See the [watch list](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/near-term-watch-list.md)
and [Donut’s project findings](DonutRetrospectiveFindings.md) for their separate records.

- Highest allocated local number: 353. Removed numbers are never reused.
- [Full pre-compaction source and earlier recovery locators](https://github.com/nerds-odd-e/doughnut/blob/0436d21c6e59f75157ab29394e273f8af02fe3f9/DearDough.md).
- [Open Dough catalog at evidence retention](https://github.com/terryyin/open-dough/tree/784bf7f574cda84eefe505142262e58d42be6cf4/docs/maintainer/findings).

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

### Occurrences
- Execution: SEED-113#assimilate-notes-with-a-clear-default-and-result (first implementation commit 9868d40b3c)
  - Timestamp: unknown (2026-11-09 Asia/Singapore, slices 1 to 5)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: plan slices targeted 5 minutes (scrutinized 5–8, hard 10); implementer agent runs took 1,749 s, 1,554 s, 934 s, 1,857 s and 2,093 s including runner waits; slice 2 return: "roughly 15–20 minutes, above the 5–8 minute window"; slice 4 return: "Over the 5-minute target … not oversized".
  - Observed effect: every slice overran at hand-back and was accepted with the implementer's stated reasons; none was re-sized.
  - Inference: runner and full-suite waits are part of the overrun, so the budget's runner exception left the active-work time unmeasured.

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

## ODF-314 — An implementation return omitted plan-owned proof rows listed in its delegation, and the returned rows found a defect

Former local code: DD-257. An implementation return omitted proof rows already named by both plan and delegation. Coordinator comparison returned the gap; missing proof then caught header overflow before delivery, adding about six implementation minutes. One SEED-078#story-3 execution on 0.3.66, shared with ODF-206. Distinct from ODF-247’s omission in the plan itself. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-314.md).

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

## DD-347 — Guarded delivery refused a renamed file's old path listed as owned

A slice that moved files with `git mv` listed both old and new paths in `staging.ownedPaths`. Guarded fresh delivery failed at staging with `pathspec ... did not match any files`, because the deletions were already staged. Listing the old paths in `stagedPaths` succeeded on retry.

### Occurrences
- Execution: SEED-109#complete-test-inventory-and-script-proof (first implementation commit e2256907b2)
  - Timestamp: unknown (2026-11-08 Asia/Singapore, slice 2 delivery)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: `execution-increment-delivery.mjs fresh` result `preparation-failed`, operation `staging`; retry published 716cfa4f3d.
  - Observed effect: one refused attempt; formatting already ran; no work lost.
  - Inference: the `ownedPaths`/`stagedPaths` split does not say where a rename's removed path belongs; low cost, likely recurring for move slices.

## DD-348 — A consumer trace deferred a stale doc line to an out-of-scope later slice

Slice 1 added an Intent location without CI proof. Its consumer trace found `docs/ci-proof-summary.md`, which lists the locations without proof, and deferred it to "slice 2 CLI CI proof", although CLI CI proof was outside the story. The coordinator accepted the return without checking that doc, and the stale line shipped until slice 3 corrected it. Possibly related to ODF-150.

### Occurrences
- Execution: SEED-109#complete-test-inventory-and-script-proof (first implementation commit e2256907b2)
  - Timestamp: unknown (2026-11-08 Asia/Singapore, slices 1 to 3)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: slice 1 return's "deferred to slice 2" disposition; e2256907b2 left `docs/ci-proof-summary.md` naming only `scripts` and `infra`; d122555f9f added `cli/tests`.
  - Observed effect: an inaccurate doc line for two slice increments; no CI or product effect.
  - Inference: a disposition naming a later slice was accepted without checking that slice owned the change.

## DD-349 — Agent shells lacked the environment that repository commands rely on

The coordinator's and agents' non-interactive shells do not load `~/.zshrc`, which exports `NIX_CONFIG` (flakes) and `OPENAI_API_TOKEN`. `./scripts/run.sh` refused to start Nix, and mocked-OpenAI E2E features failed because the backend reads its key only from `OPENAI_API_TOKEN`.

### Occurrences
- Execution: SEED-113#assimilate-notes-with-a-clear-default-and-result (first implementation commit 9868d40b3c)
  - Timestamp: unknown (2026-11-09 Asia/Singapore, execution start and slice 1 E2E)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: `./scripts/run.sh bash scripts/worktree_setup.sh` → "experimental Nix feature 'flakes' is disabled"; slice 1 E2E `sut.log`: `IllegalStateException: This request requires apiKey` (8 refinement, 10 question-generation errors); reruns with an exported placeholder passed.
  - Observed effect: one refused setup command and a diagnosis at startup; two E2E features failed and were rerun; every later brief had to carry both exports.
  - Inference: the wrappers depend on interactive-shell environment; a wrapper-supplied flag and mock token would remove the dependency.

## DD-350 — The coordinator required the full frontend suite in every slice

The plan asked for the full frontend suite on the final implementation. The coordinator's briefs required it once per slice, beyond the focused-check time the delegation rule names for the changed surface's suite.

### Occurrences
- Execution: SEED-113#assimilate-notes-with-a-clear-default-and-result (first implementation commit 9868d40b3c)
  - Timestamp: unknown (2026-11-09 Asia/Singapore, slices 1 to 5)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: slice briefs: "Run the full frontend suite … once on your final implementation"; returns report full-suite runs of 140 s (slice 2), 143 s (slice 4) and 262 s (slice 5), plus slices 1 and 3; slice 2 return: load peaked at 34.9 after its full frontend run, delaying its E2E launch.
  - Observed effect: five full-suite runs on a shared, loaded host; no recorded failure was caught only by a full-suite run.
  - Inference: per-slice focused specs plus one final full run would likely have given the same evidence at lower cost.

## DD-351 — A disclosure slice built a width rule that the docking slice replaced

The plan split the compact optional-modes disclosure (slice 4) from the bottom-bar docking (slice 5) and left the final threshold to slice 5. Slice 4 therefore built its own panel-width measurement and test setup, and slice 5 replaced both.

### Occurrences
- Execution: SEED-113#assimilate-notes-with-a-clear-default-and-result (first implementation commit 9868d40b3c)
  - Timestamp: unknown (2026-11-09 Asia/Singapore, slices 4 and 5)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: `e61af632d9` added `useNarrowerThan` inside `NoteAssimilationModes` with a 720 px panel threshold and attached 1280 px panel test mounts; `0d97720312` moved measurement to `NoteShow` (768 px note column), deleted the constant and restored `assimilationPanelTestSupport.ts` to its slice-3 bytes.
  - Observed effect: part of a 1,857 s slice-4 run and its proof were discarded one slice later; the delivered result is correct.
  - Inference: a slice that needs a provisional rule owned by a later slice may need the measurement owner decided in the plan.

## DD-352 — Successive slices extended an inherited reload mechanism that an aggregate review found unnecessary

Slices 1, 2 and 5 moved, extended and duplicated the panel's reload-and-rebuild after stay-on-note actions. Each slice's acceptance treated the rebuild as given. The retrospective's independent outcome review found that the reloaded data holds nothing the panel shows. Possibly related to ODF-271.

### Occurrences
- Execution: SEED-113#assimilate-notes-with-a-clear-default-and-result (first implementation commit 9868d40b3c)
  - Timestamp: unknown (2026-11-09 Asia/Singapore, slices 1 to 5 and retrospective)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: `NoteShow.vue` `onAssimilationReloadNeeded`/`assimilationPanelKey` (slices 1, 5); Understanding success emits `reloadNeeded` (slice 2 learning); retrospective probe with a no-op handler passed 39 tests and failed only `isConnected === false`; correction planned as SEED-113#keep-the-assimilation-panel-in-place-after-staying-actions.
  - Observed effect: an extra note request per stay-on-note action, an unhandled reload rejection path, and focus lost after a compact optional addition shipped across five increments.
  - Inference: the plan's Architecture designed the result's lifetime around the rebuild, so acceptance had no prompt to question it.

## DD-353 — Disposable break checks confirmed that each new assertion catches its defect

Implementers temporarily broke the product behavior each new mounted assertion claimed to observe, confirmed the expected test failed, and restored the source before returning.

### Occurrences
- Execution: SEED-113#assimilate-notes-with-a-clear-default-and-result (first implementation commit 9868d40b3c)
  - Timestamp: unknown (2026-11-09 Asia/Singapore, slices 1, 2, 4 and 5)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: slice 1: slot before the title → title bottom 526 vs action top 256; slice 2: removing the `watch(noteId)` reset failed exactly the two reset tests; slice 4: four checks (focus return, Escape, close-on-action, row CSS); slice 5: eight checks (sticky, overlay, popover direction, heading size, read-only guard, threshold 720, bar above blocker, no bar).
  - Observed effect: each new geometry or state assertion was shown to discriminate its defect before acceptance.
  - Inference: useful practice; it cost a few focused test runs per slice and was not required by the briefs.
