# DearDough Process Findings

Shared findings reviewed 2026-11-13 (Asia/Singapore). The linked catalog records
own the occurrence evidence and current responses. See the [watch list](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/near-term-watch-list.md)
and [Donut’s project findings](DonutRetrospectiveFindings.md) for their separate records.

- Highest allocated local number: 500. Removed numbers are never reused.
- Shared-feedback compaction (2026-11-15): [exact post-local source](https://github.com/nerds-odd-e/doughnut/blob/5170deb585edd20cfdecf2e609dea9ae3a5a6e55/DearDough.md) and [retained catalog evidence](https://github.com/terryyin/open-dough/tree/5da5acc11ab3aa58fba24425a5d41213d23ad7b1/docs/maintainer/findings).
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

Retained the records-only extraction and its estimated effort. [Updated occurrence evidence](https://github.com/terryyin/open-dough/blob/5da5acc11ab3aa58fba24425a5d41213d23ad7b1/docs/maintainer/findings/ODF-189.md#odf-189).

<a id="odf-190"></a>

## ODF-190 — Planned observation route is unavailable

Former local code: DD-145. Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-190.md).

Retained the blocked 0.3.97 recall-history proof. The source plan’s owner-held route failed non-interactive reauthentication. [Updated occurrence evidence](https://github.com/terryyin/open-dough/blob/5da5acc11ab3aa58fba24425a5d41213d23ad7b1/docs/maintainer/findings/ODF-190.md#odf-190).

<a id="odf-195"></a>

## ODF-195 — Pre-existing size overrun handled inconsistently

Former local code: DD-160. Two refactor passes within one story treat barely touched, already oversized files differently, forcing an unrelated extraction in one and retaining another. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-195.md).

<a id="odf-141"></a>

## ODF-141 — Fixed refactor cost on small accepted slices

Former local code: DD-176. Every small slice launches a fresh full refactor agent even when the accepted diff needs no further changes, creating substantial repeated cost. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-141.md).

Retained four executions. The tracker-question review followed [Donut’s stricter wrap-up rule](DonutRetrospectiveFindings.md#records-only-refactor-reviews). [Updated occurrence evidence](https://github.com/terryyin/open-dough/blob/5da5acc11ab3aa58fba24425a5d41213d23ad7b1/docs/maintainer/findings/ODF-141.md#odf-141).

### Occurrences

- Execution: SEED-073#explore-reliable-recall-question-checks / ab7f298a81ebd58f8563ddc945af6b64c32625d3
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: `/root/fresh_generation_refactor`, `/root/fresh_comparison_refactor` and `/root/comparison_report_refactor` completion returns; accepted commits b36962bb1f, 5bed0c952c and 567b50c642.
  - Observed effect: three records-only independent passes reported approximately 12, 7 and 9 active minutes respectively; one extracted existing proof, one made no edit, and one corrected a stale acceptance phrase. All reused scientific proof.
  - Inference: the required fresh-agent pass has visible fixed overhead for small evidence increments. These are worker estimates, not instrumented durations or measured token costs; the checks also supplied link and preservation evidence.

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

Retained six executions, their distinct truncation effects and release limits. [Updated occurrence evidence](https://github.com/terryyin/open-dough/blob/5da5acc11ab3aa58fba24425a5d41213d23ad7b1/docs/maintainer/findings/ODF-264.md#odf-264).

### Occurrences

- Execution: SEED-130#rebalance-ci-e2e-shards / c594b3d5a135462865708ce7d3aedddaf9a074aa
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: 2026-11-16 execution conversation, startup tool results combining execution-location/runtime-setup and delegation/source-review/execution-decisions were marked truncated; the coordinator then retrieved story-obligations lines 68–200 and other required passages again.
  - Observed effect: required startup context was retrieved repeatedly before first implementation delegation. Later a broad consumer-trace dump was also truncated before a directed consumer read.
  - Inference: the batches exceeded the tool response budget despite existing targeted-retrieval guidance; this is an agent deviation. Additional reads are observed; their time and token cost were not measured.

- Execution: SEED-073#explore-reliable-recall-question-checks / ab7f298a81ebd58f8563ddc945af6b64c32625d3
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: resumed execution conversation on 2026-11-16, batch reading Execute Plan SKILL.md, PLAN.md and source-review.md reported a truncated 13,358-token result; the coordinator later reread skill lines 190–310.
  - Observed effect: already-established guidance was retrieved again during the Batch wait; the broad output obscured part of the repeated skill read.
  - Inference: another agent deviation from targeted retrieval and source-reuse guidance. Additional reading is observed; elapsed and token cost are unmeasured.

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

Retained the later cached-note review and bounded repair. [Updated occurrence evidence](https://github.com/terryyin/open-dough/blob/5da5acc11ab3aa58fba24425a5d41213d23ad7b1/docs/maintainer/findings/ODF-271.md#odf-271).

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

Retained the indirect Markdown autosave/trash consumer and its CI repair. [Updated occurrence evidence](https://github.com/terryyin/open-dough/blob/5da5acc11ab3aa58fba24425a5d41213d23ad7b1/docs/maintainer/findings/ODF-150.md#odf-150).

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

<a id="dd-479"></a>
<a id="dd-479--merged-repair-restores-were-misclassified-as-wholly-unapplied"></a>
<a id="odf-487"></a>

## ODF-487 — Merged repair restores were misclassified as wholly unapplied

Former local code: DD-479. A merged CI repair restore was reported as wholly unapplied when the retained draft had in fact been applied in part. Donut delivered its own exact-OID recovery, now in the [project recovery guide](docs/ci-repair-restoration.md). The shared helper remains open. [Evidence and response](https://github.com/terryyin/open-dough/blob/96e641815b9b7d957d97e03c612034d1c4093734/docs/maintainer/findings/ODF-487.md#odf-487). [Original source](https://github.com/nerds-odd-e/doughnut/blob/4783aec679dc0119cc6e43fe600324fde2d3e502/DearDough.md#L536).

<a id="dd-480"></a>

DD-480 was a Donut fixture defect, retired after direct scalar-routing proof on
the adopted `c371659ac7` repair. [Original project evidence](https://github.com/nerds-odd-e/doughnut/blob/e144d6f2c5b2d78305defe9d02d69e899e6a22e6/DonutRetrospectiveFindings.md#dd-480).

<a id="dd-481"></a>

DD-481 is a Donut project finding. [Evidence and disposition](DonutRetrospectiveFindings.md#dd-481).

<a id="dd-482"></a>
<a id="dd-482--an-unterminated-landed-plan-tail-made-appended-evidence-resemble-a-source-conflict"></a>
<a id="odf-489"></a>

## ODF-489 — An unterminated landed plan tail made appended evidence resemble a source conflict

Former local code: DD-482. An unterminated plan tail made appended observations look like a source conflict. Moving the observations away from the end let the original comparison pass. The execution found no substantive source amendment. [Evidence and response](https://github.com/terryyin/open-dough/blob/96e641815b9b7d957d97e03c612034d1c4093734/docs/maintainer/findings/ODF-489.md#odf-489). [Original source](https://github.com/nerds-odd-e/doughnut/blob/4783aec679dc0119cc6e43fe600324fde2d3e502/DearDough.md#L557).

<a id="dd-483"></a>

DD-483 is a Donut project finding. [Evidence and disposition](DonutRetrospectiveFindings.md#dd-483).

<a id="dd-484"></a>
<a id="dd-484--the-source-review-of-a-small-story-gave-137-rows-that-were-typed-and-recorded-one-at-a-time"></a>
<a id="odf-481"></a>

## ODF-481 — The source review of a small story gave 137 rows that were typed and recorded one at a time

Former local code: DD-484. In a single execution on Open Dough 0.3.96, a source review returned 137 rows and the coordinator made about 140 recording calls. The review caught a missing promise, and many rows were qualifications without an independent result. [Evidence and response](https://github.com/terryyin/open-dough/blob/96e641815b9b7d957d97e03c612034d1c4093734/docs/maintainer/findings/ODF-481.md#odf-481). [Original source](https://github.com/nerds-odd-e/doughnut/blob/4783aec679dc0119cc6e43fe600324fde2d3e502/DearDough.md#L574).

<a id="dd-485"></a>

## DD-485 — GitHub Actions observation ended before a rerun finished

The SEED-124 pronunciation execution on Open Dough 0.3.96 lost story-branch observation after the GitHub Jobs API returned HTTP 502 for run 43053073660, attempt 2. The coordinator read successful attempt 3 directly. The same execution later lost trunk observation to a TLS handshake timeout. The [retained observer evidence](https://github.com/terryyin/open-dough/blob/96e641815b9b7d957d97e03c612034d1c4093734/docs/maintainer/findings/ODF-121.md#odf-121) identifies both transport failures. See the [original source](https://github.com/nerds-odd-e/doughnut/blob/4783aec679dc0119cc6e43fe600324fde2d3e502/DearDough.md#L590) and [Donut’s package-install facet](DonutRetrospectiveFindings.md#dd-485).

<a id="dd-486"></a>
<a id="dd-486--a-checkpoint-go-ahead-did-not-let-the-session-push-the-release-tag"></a>
<a id="odf-492"></a>

## ODF-492 — A checkpoint go-ahead did not let the session push the release tag

Former local code: DD-486. The host required separate production-deployment authorization for the release tag after the story checkpoint had been answered. The release took one additional owner turn. [Evidence and response](https://github.com/terryyin/open-dough/blob/96e641815b9b7d957d97e03c612034d1c4093734/docs/maintainer/findings/ODF-492.md#odf-492). [Original source](https://github.com/nerds-odd-e/doughnut/blob/4783aec679dc0119cc6e43fe600324fde2d3e502/DearDough.md#L604).

## DD-487 — A combined, unlabelled command output was misread as an environment fact
The coordinator ran three environment probes in one shell command whose outputs carried no labels, then recorded in the plan that the Nix shell supplies `OPENAI_API_TOKEN`. The variable came from the interactive zsh profile instead.

### Occurrences
- Execution: SEED-073#validate-recall-question-integrity, plan 098, first commit 57115c1329
  - Timestamp: unknown (before 2026-11-16T07:48:31Z, the first paid send)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: plan "Decision before Take" edit in 57115c1329's parent working state; slice 1 return; corrected in 57115c1329 ("Current decisions")
  - Observed effect: the slice 1 agent's first send failed with a missing variable before any request; no charge; one plan correction.
  - Inference: labelling each probe's output, or one probe per call, would have avoided the false premise.

## DD-488 — Coordinator recheck of a subagent's summary figure before it reached the recommendation
The slice 6 agent reported that 6 of 32 verifier flags came from its choices differing from the key. Rereading the agent's own per-row JSON before writing the recommendation showed 8 of 32 (24 feasibility-only), and the plan was corrected.

### Occurrences
- Execution: SEED-073#validate-recall-question-integrity, plan 098, first commit 57115c1329
  - Timestamp: unknown (2026-11-16, between c1becf0562 and cd2eba2373)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: `PLAN.md` slice 6 result ("corrected at slice 8 from `slice6-summary.json`"), commit cd2eba2373
  - Observed effect: a wrong count was kept out of the owner-facing recommendation; the correction also exposed the choices-only rule (4 of 8 defects, 1 sound flag) that the recommendation relies on.
  - Inference: a useful practice for validation stories, where a subagent's prose summary feeds an owner decision.

## DD-493 — An uninstrumented active-time estimate cannot establish the slice bound

The fresh-generation attempt stopped with completed compatible evidence and an approximately 15-active-minute estimate, but no instrumented active/wait intervals. This leaves the original duration unverifiable; it does not establish an overrun. The coordinator preserved the evidence and split the remaining semantic comparison before continuing. Related to ODF-206, with a distinct timing-evidence gap rather than acceptance of a demonstrated overrun.

### Occurrences

- Execution: SEED-073#explore-reliable-recall-question-checks / ab7f298a81ebd58f8563ddc945af6b64c32625d3
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: [generation attempt and safe disposition](https://github.com/nerds-odd-e/doughnut/blob/b36962bb1f32635f3fa63d824aac0f9d61f449df/.planning/slice-plans/099-explore-reliable-recall-question-checks/fresh-generation-evidence.md#attempt-limit-and-safe-disposition); `/root/fresh_generation_checks` handback after 2026-11-16 UTC generation work. The exact limit-reaching time was not recorded.
  - Observed effect: the original combined slice remained incomplete and was split into generation and semantic-comparison outcomes. Later comparison/report slices recorded 636.307 and 525.708 active seconds; those measurements cannot recover the earlier duration.
  - Inference: host-clock active/wait intervals need to start with the attempt, counting concurrent authorship and unioning overlapping waits. Existing guidance required timing; this occurrence exposes missing execution evidence, not a need to change the product.

## DD-494 — Guarded fresh delivery refused a records-only execution-complete commit without a slice

After all slices were done, the coordinator published the plan's execution-complete record through guarded fresh delivery with `kind: planned` and no `plan` field, since no slice remained to mark done. The operation refused before preparation (`preparation-refused`, operation `context`: "planned work requires its workspace-relative plan and selected slice"). Supplying the last, already-done slice succeeded and left its status unchanged.

### Occurrences

- Execution: SEED-131#see-link-property-types-in-references / 04bc57930e0f59cc7ce2fa4bc49f89f4545d6c16
  - Timestamp: 2026-11-17 (Asia/Singapore; exact time not recorded)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.104
  - Evidence: execution-complete publication accepted as fcd05a8199d1c5894e8b6010198fea84283ea127 on `claude/see-link-property-types-in-references` after the refused first attempt.
  - Observed effect: one refused invocation and a retry; no commit, push or file change from the refusal.
  - Inference: the finish-or-stop guidance names increment delivery for this record but does not say which slice a records-only completion commit names; the cost was one tool round-trip.

## DD-495 — The plan's file-size premise skipped a document the story edits
The plan stated that no expected edit touched a file over the 250-line check, but the story's ADR 0001 amendment edited a 253-line glossary. The coordinator found it only at slice 3 delegation and had to ask the owner mid-execution.

### Occurrences
- Execution: SEED-073#judge-contested-question-by-answer-correctness, plan 102, first commit 2b70e2f6d9
  - Timestamp: unknown (2026-11-17, Asia/Singapore)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: plan 102 "Architecture and reuse" (file-size paragraph listed only Java files); `wc -l docs/adrs/0001-ubiquitous-language.md` = 253 at slice 3; owner answer recorded in the plan Learnings
  - Observed effect: the ADR edit was held while the rest of slice 3 ran; one owner question; the owner allowed the file to stay over 250.
  - Inference: the planning size check was applied to code files only; measuring every file the slices name, documents included, would have raised the question during planning.

## DD-496 — A slice's proof named an `@ignore` feature that no runner admits
Slice 2's proof command listed `cli_recall.feature`, which is tagged `@ignore` and `@interactiveCLI` and is refused by the isolated Cypress runner, so the five-spec command exited before running any test.

### Occurrences
- Execution: SEED-073#judge-contested-question-by-answer-correctness, plan 102, first commit 2b70e2f6d9
  - Timestamp: unknown (2026-11-17, Asia/Singapore)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: plan 102 slice 2 Proof; slice 2 return (first run exit 1, rerun with four specs, 8 passing; `pnpm -s test:gherkin-validation` for the ignored file)
  - Observed effect: one wasted proof launch; the ignored file's step removal is proven only by Gherkin validation.
  - Inference: the planning premise run covered one feature; checking each named spec's tags would have caught it.

## DD-497 — The refactor pass removed guards on reachable external data as "defensive programming"
The slice 3 refactor pass deleted the null stored-answer and out-of-range model position handling from the contest verdict, citing "no defensive programming". The answer-index column is nullable and the model's positions have no upper bound, so a learner's contest could return a server error. The previous check had handled both cases. The coordinator restored both guards with controller tests.

### Occurrences
- Execution: SEED-073#judge-contested-question-by-answer-correctness, plan 102, first commit 2b70e2f6d9
  - Timestamp: unknown (2026-11-17, Asia/Singapore)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: slice 3 refactor return ("Assumption to accept or reverse"); `V100000000__baseline.sql` `correct_answer_index` DEFAULT NULL; `RecallPromptContestControllerTest.Accepted.whenTheQuestionHasNoStoredAnswer` / `whenTheModelNamesAPositionOutsideTheChoices` in 58638c87e6
  - Observed effect: no regression shipped; the coordinator spent one extra check and two tests.
  - Inference: the refactor agent flagged its own assumption, which made the catch cheap; the rule against defensive code can be over-applied to data from storage or a model.

## DD-498 — AI reader returns were retained as coordinator paraphrases, not verbatim

In a validation study run with fresh AI reader contexts, the coordinator saved each reader's answer and judging returns to private files by hand, in a compact paraphrase of the hand-back, before the report writer decoded them. The owner-facing report's per-response marks trace to those paraphrases, not to the readers' own words.

### Occurrences
- Execution: SEED-073#retest-fixed-skill-spelling-objectives, plan 103, first commit 68ebdc1a9b
  - Timestamp: 2026-11-17 (between 68ebdc1a9b and 2b90d82410; exact time unknown)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: [plan 103 execution evidence](https://github.com/nerds-odd-e/doughnut/blob/6f02bb77ca188e4f19e1c33d510621c7a424090c/.planning/slice-plans/103-retest-fixed-skill-spelling-objectives/PLAN.md#execution-evidence); [retest report](.planning/seeds/fixed-skill-spelling-retest-evidence.md) "Provenance and method"; private `returns/C*-answer.md`/`C*-judge.md` (removed at closeout)
  - Observed effect: a spot check of four decoded marks against the mapping matched, but no verbatim reader record existed for the report writer or acceptance; one reader's misreading (brine slot 6) survived only because the paraphrase kept its reason.
  - Inference: asking readers to write their own return file, or saving the hand-back text unchanged, would make the "trace each judgment to its packet" proof direct and cost nothing. Copying only one packet into an isolated per-reader directory was a useful practice for keeping contexts key-hidden.

## DD-499 — A plan premise called a scratch directory Git-ignored without checking

Plan 103 placed private packets in "this execution worktree's Git-ignored `.planning/tmp/retest-fixed-skill/`". At execution, `git status --porcelain --ignored` showed `?? .planning/tmp/`: nothing ignored it. The coordinator added a `*` `.gitignore` inside the directory before writing private material.

### Occurrences
- Execution: SEED-073#retest-fixed-skill-spelling-objectives, plan 103, first commit 68ebdc1a9b
  - Timestamp: 2026-11-17 (before 68ebdc1a9b; exact time unknown)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: [plan 103 "Inputs and decisive premises"](https://github.com/nerds-odd-e/doughnut/blob/6f02bb77ca188e4f19e1c33d510621c7a424090c/.planning/slice-plans/103-retest-fixed-skill-spelling-objectives/PLAN.md#inputs-and-decisive-premises); coordinator check before slice 1 delegation
  - Observed effect: no private material was staged; the gap was caught before any sensitive file existed.
  - Inference: a privacy premise in a plan deserves the same literal observation as its input premises; the shared cache relies on its own inner `.gitignore` for the same reason.

## DD-500 — Wrap-up treats story scope limits as deferred promises

Story wrap-up deletes a story section "after queuing its deferred promises as one new story unless the developer drops them". A story's scope limits phrased "each deliverable on its own later" name unrelated possible extensions with no beneficiary or evaluable outcome, so a bundled story would fail the backlog's admission rule; the coordinator had to judge that they were context, not promises, and kept a one-line note in the seed instead.

### Occurrences
- Execution: SEED-131#label-every-property-link-in-references
  - Timestamp: 2026-11-17 (time unknown)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: seed SEED-131 section Scope "Scope limits, each deliverable on its own later" (before-cleanup commit); dough-story-wrap-up "Delete spent history"; dough-product-backlog "Add a feature story only with a named beneficiary and evaluable outcome"; the source-only reviewer also left those limits unnumbered.
  - Observed effect: no story was queued; the limits survive as a note under the seed's Ordering and Scope Reduction.
  - Inference: the wrap-up wording does not distinguish promises a story deferred from scope limits it never promised.

