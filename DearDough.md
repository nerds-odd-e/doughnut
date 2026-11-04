# DearDough Process Findings

Source maintenance on 2026-10-28 retained collected evidence before shortening at Open Dough `32b5ae0b4414d755bcde4ef6cd822774d7450771:docs/maintainer/findings/`. The complete post-local source is `cc8b0fe361c07a8a6532d1a74aa0b41ba18146f0:DearDough.md`; linked source records share that revision. Unknown execution releases and local facets remain qualified.


Reviewed 2026-10-25 (Asia/Tokyo). Active shared-process findings only; released
responses under observation live in the [watch list](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/near-term-watch-list.md).
Detailed observations, original releases and qualified inferences are retained
in the linked Open Dough catalog. Unknown execution provenance remains unknown.

- Highest allocated local number: 344. Removed numbers are never reused.
- Full source recovery before this cleanup: `0a8878e98be76b8e92203515d8f4719489b24169:DearDough.md`.
- Shared-feedback compaction recovery (2026-11-03): `0373e4129d8dbc29189e08bdcb0075969eee0f2c:DearDough.md`; its rows are retained at the linked Open Dough finding owners.
- Full source recovery before name adoption and compaction: `234f141b41332715edd5e4384a425bcb8f26e718:DearDough.md`; complete supplied evidence retained in Open Dough `dbe36711c0e4ff9421bc28afc53cb7477203e704:docs/maintainer/findings/`.
- Project-owned findings and their current assessment remain in [Donut Retrospective Findings](DonutRetrospectiveFindings.md).
- Full source recovery before this adoption and compaction: `9e98f0874d9519997be4579a5fe4edde8997f496:DearDough.md`; newly supplied evidence is retained in Open Dough `341293c09f1115b8ad3412fc6da63679ce563457:docs/maintainer/findings/`.
- Full source recovery before the 2026-10-22 adoption and compaction: `0c25e73cfa672359d493549a8930851bba72f8a8:DearDough.md`; newly supplied evidence retained in Open Dough `eeefa1588ba0b04c6573384de520e1374e7e7afa` before compaction.

<a id="odf-110"></a>

## ODF-110 — Incomplete readiness replays

Former local code: DD-109. Former local code: DD-163. Former local code: DD-168. Former local code: DD-171. Former local code: DD-207. A replay resolves the named readiness seam without exercising the rest of the slice's promised journey, leaving a later operation to force a scope stop. Open after verified later recurrence; not resolved. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-110.md).

<a id="odf-121"></a>

## ODF-121 — Transient transport failure ends observation

Former local code: DD-115. A reported GitHub transport failure ends observation and leaves later revisions without notification coverage. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-121.md).

### Occurrences

Distinct executions, original timestamps/releases/tools, observed effects and qualified inferences are [retained at ODF-121](https://github.com/terryyin/open-dough/blob/7716badfa29258ebcd299c3f61c0966a7c6a75d1/docs/maintainer/findings/ODF-121.md); rereading adds no occurrence.


<a id="odf-106"></a>

## ODF-106 — Colliding plan numbers

Former local code: DD-118. Concurrent unpublished plan allocation produces two quick plans with the same numeric selector, making a number-only execution request ambiguous. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-106.md).

<a id="odf-074"></a>

## ODF-074 — Unverified planning premises

Former local code: DD-126. Former local code: DD-130. Former local code: DD-134. Former local code: DD-139. Former local code: DD-147. Former local code: DD-162. Former local code: DD-167. Former local code: DD-209. Former local code: DD-337. Concrete only-caller and host-state premises enter a plan without inspection, forcing a changed decision or stopped implementation when checked. Open after verified later recurrence; not resolved. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-074.md).

<a id="dd-337"></a>
<a id="dd-337--plan-premises-were-checked-in-a-different-form-from-how-they-were-used"></a>

The plan recorded tooling and data definitions as settled without exercising them as used: it observed `python3 -c "import pandas"` but prescribed `python3 -I`, which disables the user site where pandas is installed; and it defined session span from `answer.promptCreatedAt`, which the real export shows is generated ahead (90th percentile about 5 days before the answer). The slice 1 implementer found both; the coordinator replaced the commands and the span definition in the plan before slice 2. The early data-inventory slice is what made the data premise cheap to correct.

### Occurrences
- Execution: SEED-100#story-2; first implementation 275504cfc10cad5418f9f54f5a70dc26c331b6da
  - Timestamp: unknown (2026-11-03, slice 1 return before 275504cfc1)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.82
  - Evidence: da02e73b4a PLAN.md Current decisions "Tooling" and "Day-level measures" and premise table row 3; corrected in 275504cfc1 and 51b2d705fc PLAN.md; slice 2 also changed the answer start to include away and detour time after 4 of 65 days clipped.
  - Observed effect: one implementer workaround (`-E -B -p no:cacheprovider`) and two definition changes; no published result was wrong.
  - Inference: running a premise check with the exact command form, and reading a few real rows before fixing a data definition, would likely have avoided both.

<a id="odf-147"></a>

## ODF-147 — Reasoned red proof leaves ineffective tests

Former local code: DD-127. An agent reasons that new tests would fail instead of observing them against pre-change behavior; round-trip or wrong-boundary tests can pass without the fix. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-147.md).

<a id="odf-100"></a>

## ODF-100 — Masked formatter failure

Former local code: DD-135. A formatter piped through tail returns the final pipeline stage's success, allowing subsequent delivery steps after formatter failure. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-100.md).

<a id="odf-152"></a>

## ODF-152 — File-size gate conflicts with approved intermediate scope

Former local code: DD-138. An absolute changed-file size check conflicts with an approved staged decomposition and mechanical edits to pre-existing oversized callers. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-152.md).

<a id="odf-189"></a>

## ODF-189 — Small additions force unrelated size extractions

Former local code: DD-144. A small addition crosses the numeric file-size ceiling and triggers relocation and re-proof of a substantial previously untouched responsibility. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-189.md).

### Occurrences

Distinct executions, original timestamps/releases/tools, observed effects and qualified inferences are [retained at ODF-189](https://github.com/terryyin/open-dough/blob/7716badfa29258ebcd299c3f61c0966a7c6a75d1/docs/maintainer/findings/ODF-189.md); rereading adds no occurrence.


<a id="odf-190"></a>

## ODF-190 — Planned observation route is unavailable

Former local code: DD-145. Planning names an observation route or log source that is not available to the executing project, causing owner probing or loss of the intended proof. Released response; watch start unknown. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-190.md).

<a id="odf-195"></a>

## ODF-195 — Pre-existing size overrun handled inconsistently

Former local code: DD-160. Two refactor passes within one story treat barely touched, already oversized files differently, forcing an unrelated extraction in one and retaining another. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-195.md).

<a id="odf-141"></a>

## ODF-141 — Fixed refactor cost on small accepted slices

Former local code: DD-176. Every small slice launches a fresh full refactor agent even when the accepted diff needs no further changes, creating substantial repeated cost. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-141.md).

### Occurrences

Distinct executions, original timestamps/releases/tools, observed effects and qualified inferences are [retained at ODF-141](https://github.com/terryyin/open-dough/blob/7716badfa29258ebcd299c3f61c0966a7c6a75d1/docs/maintainer/findings/ODF-141.md); rereading adds no occurrence.

Distinct executions, original provenance, observations and qualified inferences are
[retained at ODF-141](https://github.com/terryyin/open-dough/blob/a29f625255a3368eb721249c01df96e0f98edfd4/docs/maintainer/findings/ODF-141.md); source recovery remains in this project’s pre-compaction revision.

<a id="odf-208"></a>

## ODF-208 — Observer worker exits without a recorded cause

Former local code: DD-202. The CI observer's worker exits mid-execution without a terminal result or a reported transport error, ending notification coverage. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-208.md).

<a id="odf-213"></a>

## ODF-213 — Refinement reproduces a test mechanism already forbidden

Former local code: DD-204. Refinement makes reproducing a flaky mock the prerequisite for a remedy although the project’s existing rules already forbid that mock. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-213.md).

<a id="odf-214"></a>

## ODF-214 — A planned real observation loses a contract distinction

Former local code: DD-206. Replacing captured router push/replace calls with current-location proof cannot distinguish the behaviors the story promises to preserve. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-214.md).

<a id="odf-216"></a>

## ODF-216 — Landing retires a workspace with a remaining continuation

Former local code: DD-212. Explicitly authorized landing removes an execution workspace while the Taken story still has a final real-stack check and wrap-up owed. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-216.md).

<a id="odf-185"></a>

## ODF-185 — Reported story gap filed as a learning

Former local code: DD-214. A reported preservation gap or provisional behavior is accepted as a learning without checking the whole story and the later slices that depend on it. Released response; watch start unknown. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-185.md).

<a id="odf-142"></a>

## ODF-142 — Abbreviated delivery SHAs produce misleading refusals

Former local code: DD-216. An abbreviated published-base SHA is accepted as an argument but compared without normalization, causing misleading delivery refusal and fallback publication without observation. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-142.md).

<a id="odf-206"></a>

## ODF-206 — Over-budget slice not stopped

Former local code: DD-217. Former local code: DD-222. The slice time bound is checked only at hand-back, so a delegated slice runs past it and is accepted or extended without re-sizing. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-206.md).

<a id="odf-217"></a>

## ODF-217 — Unverified refactor proof effects

Former local code: DD-218. A refactor misclassifies or omits the proof effects of a change, leaving reached consumers unrun; some returns incorrectly call changed behavior identical or type-only. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-217.md).

<a id="odf-242"></a>

## ODF-242 — Correction queued after its repair already exists

Former local code: DD-219. Retrospective queues and takes a full correction story despite its exact repair already being committed on the same branch, producing a claim, workspace, plan and wrap-up but no product/test change. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-242.md).

<a id="odf-243"></a>

## ODF-243 — Missing story source is validated only after a gap

Former local code: DD-221. A plan without the mandatory Source link passes story-obligation list/check while its record is empty, then the first real obligation unexpectedly blocks delivery. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-243.md).

<a id="odf-244"></a>

## ODF-244 — Real microphone proof is assigned without an agent access route

Former local code: DD-223. A plan assigns a real-microphone visual observation to an unattended agent, then replaces it with synthetic screenshots and leaves the real-input check to the owner. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-244.md).

<a id="odf-245"></a>

## ODF-245 — Component lifetime changes miss captured state

Former local code: DD-224. Planning moves or reuses a session-owning component across note changes without reconciling its per-instance captures and termination, causing stale note targeting or a retained title lock. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-245.md).

<a id="odf-246"></a>

## ODF-246 — Observation-to-fix slices contradict scratch retention

Former local code: DD-228. An observation slice instructs scratch removal while the next slice requires its regression test, leaving the only passing baseline test untracked between deliveries. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-246.md).

<a id="odf-180"></a>

## ODF-180 — Measurement-slice delivery ownership is unclear

Former local code: DD-229. A measurement plan assigns record/prototype work to the coordinator while execution guidance still requires fresh implementation and refactor passes, producing case-by-case deviations. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-180.md).

<a id="odf-247"></a>

## ODF-247 — Plan proof omits promised session endings

Former local code: DD-231. A slice proof table omits explicit story end conditions, so implementers return it complete while listing those conditions untested; the coordinator needs second rounds to compare with the full story. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-247.md).

<a id="odf-264"></a>

## ODF-264 — Broad startup reads exceeded the response budget and forced repeated retrieval

Former local code: DD-233. Broad whole-reference and metadata batches exceeded the response budget and truncated required context before delegation, forcing repeated retrieval despite existing targeted-reading guidance. Execution provenance and counts remain with the linked evidence owner; savings are unmeasured. This agent deviation is distinct from ODF-158’s required-reference cost. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-264.md).

### Occurrences

Distinct executions, original timestamps/releases/tools, observed effects and qualified inferences are [retained at ODF-264](https://github.com/terryyin/open-dough/blob/7716badfa29258ebcd299c3f61c0966a7c6a75d1/docs/maintainer/findings/ODF-264.md); rereading adds no occurrence.


<a id="odf-265"></a>

## ODF-265 — A comparison of empty captures was printed as a match

Former local code: DD-234. A proof comparison printed SAME because both filtered measurement captures were empty. The coordinator noticed and reran it; accepted proof came from the fourth, non-empty comparison. One SEED-075#story-1 execution, also represented by ODF-264 and ODF-266. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-265.md).

<a id="odf-266"></a>

## ODF-266 — A later slice's failing example was already cured by the earlier slice

Former local code: DD-235. Planning observed later-slice symptoms only on unchanged code; an earlier slice already cured the 390px menu case, so that case did not discriminate the later edit. The 320px case still did. One SEED-075#story-1 execution, shared with ODF-264 and ODF-265; the relationship to ODF-187 and ODF-074 remains uncertain. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-266.md).

<a id="odf-267"></a>

## ODF-267 — No obligation disposition fits an earlier defect the story leaves unchanged

Former local code: DD-236. The required obligation vocabulary had no truthful disposition for an earlier defect left unchanged by the story. The coordinator used no user cost, then qualified that the defect still costs the user. One 0.3.60 execution; this vocabulary gap is distinct from ODF-223’s unsupported path judgment and ODF-270’s pending-decision case. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-267.md).

<a id="odf-268"></a>

## ODF-268 — Defect noticed in a refactor return is repaired by the coordinator after the refactor pass

Former local code: DD-237. A refactor return named an unhandled empty-title-fragment case; the coordinator then wrote red proof and changed behavior after the refactor, without a fresh independent pass or full-suite rerun, and changed one accepted answer without an owner decision. Caught before publication. One execution, release unknown; relationship to ODF-219 remains uncertain. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-268.md).

<a id="odf-269"></a>

## ODF-269 — A plan took an ADR's rebuild statement as a fact about stored history

Former local code: DD-238. Planning treated an ADR’s rebuild-from-logs statement as proof of complete historical coverage. Older state predated log writing; the owner changed the starting-state decision during execution, adding a slice and replacing an entry point already written. One SEED-073#story-7 execution, release unknown; relationship to ODF-074 remains uncertain. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-269.md).

<a id="odf-270"></a>

## ODF-270 — No obligation disposition fits a gap that waits for an owner decision

Former local code: DD-239. The required obligation vocabulary could not keep a gap open for an owner decision. The coordinator used interim until the last slice despite that slice having no responsibility for it; other no-user-cost labels needed cost qualifications. Related to former DD-236 (ODF-267), with a distinct pending-decision problem. One SEED-073#story-7 execution, release unknown. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-270.md).

<a id="odf-271"></a>

## ODF-271 — A cross-slice defect passed every slice acceptance and was found by an aggregate review

Former local code: DD-240. Acceptance reasoned that a repeat confirmation stayed stable despite the return naming two exclusions on one tracker as untested. Their interaction restored an earlier excluded attempt’s effect. An independent aggregate review found the branch defect and the owner authorized slice 11’s repair. One SEED-073#story-7 execution, release unknown; relationship to ODF-223 remains uncertain. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-271.md).

<a id="odf-272"></a>

## ODF-272 — Shared accessible-name consumers were missed before delivery

Former local code: DD-248. Naming a shared modal’s outer dismissal Close collided with an existing footer Close. Selected frontend proof passed while seven ambiguous-button failures across four E2E features escaped to two CI runs. The outer dismissal was repaired to Close modal and existing E2E consumers passed unchanged. One SEED-073#story-9 execution, release unknown; relationships to ODF-150 and ODF-107 remain uncertain. The product repair does not establish shared prevention. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-272.md).

<a id="odf-273"></a>

## ODF-273 — Full verification started before the intended edits settled

Former local code: DD-249. Required whole-suite verification began while intended test or template edits were still changing, requiring another run on settled source. One SEED-073#story-2 execution reports an extra full backend run and premature frontend/typecheck results. This sequencing deviation excludes necessary reruns after newly discovered gaps and differs from ODF-225’s repair-stash invalidation. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-273.md).

<a id="odf-308"></a>

<a id="dd-251"></a>

## ODF-308 — A plan removed the only remaining sign-in control before moving it

Former local code: DD-251. A plan removed the sole sign-in route one slice before its replacement. One SEED-078#story-1 execution on 0.3.66; corrected before implementation at the cost of one plan edit. Open, unqueued; relationship to ODF-210 remains qualified. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-308.md).

<a id="odf-309"></a>

<a id="dd-252"></a>

## ODF-309 — Required build proof fails on formatting that implementers may not run

Former local code: DD-252. Required build/generation proof checks formatting before coordinator-owned formatting, producing red proof or manual layout repair before that stage. Two SEED-078 executions (story-1 and story-5), first on 0.3.66 and second release unknown; repeated builds, no escaped defect. Distinct from ODF-285’s observed formatter-ownership violation. Open, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-309.md).

### Occurrences

Distinct executions, original timestamps/releases/tools, observed effects and qualified inferences are [retained at ODF-309](https://github.com/terryyin/open-dough/blob/7716badfa29258ebcd299c3f61c0966a7c6a75d1/docs/maintainer/findings/ODF-309.md); rereading adds no occurrence.


<a id="odf-310"></a>

<a id="dd-253"></a>

## ODF-310 — Retargeting E2E page objects missed a direct label lookup of a moved control

Former local code: DD-253. Helper-name searches missed direct accessible-label lookups of newly hidden controls. Two SEED-078 executions (story-1 and story-5), also represented by ODF-309, each needed a CI repair; first reports 0.3.66, second release unknown. Product repairs do not establish shared prevention. Relationships to ODF-150 and ODF-272 remain uncertain. Open, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-310.md).

<a id="odf-311"></a>

<a id="dd-254"></a>

## ODF-311 — A deviation from a recorded plan decision was accepted as a placement choice and lost the decision's purpose

Former local code: DD-254. Acceptance treated a deviation from an explicit stable-component placement decision as routine layout; per-state presence proof missed losing an open panel when loading completed. One SEED-078#correction-1 execution on 0.3.66; CI failure and repair, about 25 minutes with a red tip. Product repair 484ce25 does not prove shared acceptance prevention. Relationships to ODF-245 and ODF-290 remain qualified. Open, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-311.md).

<a id="odf-312"></a>

<a id="dd-255"></a>

## ODF-312 — Asking the refactor pass to verify one named promise caught a regression that slice proof missed

Former local code: DD-255. A named promise check caught a same-page Account-action closure regression previously masked by incidental outside-click behavior. Useful successful practice: fixed before delivery in an approximately ten-minute refactor pass. The refactor agent also added behavior beyond its normal role, a qualified concern related to ODF-268. Same SEED-078#correction-1 execution as ODF-311, on 0.3.66; do not count the successful check as an escaped defect. Retained, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-312.md).

<a id="odf-313"></a>

<a id="dd-256"></a>

## ODF-313 — A planned removal sweep promised an empty reading for a term that already matched retained code

Former local code: DD-256. A planned empty removal sweep used a pattern that already matched retained code. One SEED-078#story-2 execution on 0.3.66; narrowed acceptance reading, one judgment and plan note, no defect. Relationship to ODF-074 remains uncertain. Open, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-313.md).

<a id="odf-314"></a>

<a id="dd-257"></a>

## ODF-314 — An implementation return omitted plan-owned proof rows listed in its delegation, and the returned rows found a defect

Former local code: DD-257. An implementation return omitted proof rows already named by both plan and delegation. Coordinator comparison returned the gap; missing proof then caught header overflow before delivery, adding about six implementation minutes. One SEED-078#story-3 execution on 0.3.66, shared with ODF-206. Distinct from ODF-247’s omission in the plan itself. Open, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-314.md).

<a id="odf-315"></a>

<a id="dd-258"></a>

## ODF-315 — A rollover rule left the probe's own running state unconsidered and was caught from the implementer's gap list

Former local code: DD-258. A rollover rule omitted the offered probe already running; the implementer disclosed the gap and the coordinator added a rule before asking whether the owner wanted it. One SEED-079#story-1 execution on 0.3.66; about four extra minutes, then the owner reverted the rule that day as an extreme edge case. No delivered defect established; the owner’s simple-rule decision does not authorize restoring the extension. Retained, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-315.md).

<a id="odf-316"></a>

<a id="dd-274"></a>

## ODF-316 — Consumer discovery treats execution evidence as canonical work

Former local code: DD-274. Consumer discovery treated execution-evidence Identity lines and non-story anchors as canonical work, producing duplicate-home and unreadable-story warnings. One SEED-080#story-1 execution, release unknown; source inspection found no current dependency agreements and preserved unrelated SEED-066 evidence. No dependency was silently satisfied. Relationship to other discovery/readiness findings remains uncertain. Open, unqueued. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-316.md).

<a id="odf-150"></a>

## ODF-150 — Implementation proof misses indirect consumers

Former local code: DD-319. Acceptance traced the unchanged transport API but not the existing E2E journeys whose preconditions the explicit question-load recovery changed; seven failures reached CI in SEED-081#story-2 and were repaired in the execution. Distinct from ODF-272 and ODF-310. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-150.md).

### Occurrences

Distinct executions, original timestamps/releases/tools, observed effects and qualified inferences are [retained at ODF-150](https://github.com/terryyin/open-dough/blob/7716badfa29258ebcd299c3f61c0966a7c6a75d1/docs/maintainer/findings/ODF-150.md); rereading adds no occurrence.

Distinct executions, original provenance, observations and qualified inferences are
[retained at ODF-150](https://github.com/terryyin/open-dough/blob/692bf5162b86fca9cbebc1c57c98efd227198e9d/docs/maintainer/findings/ODF-150.md); source recovery remains in this project’s pre-compaction revision.

<a id="odf-323"></a>

## ODF-323 — A caller-written plan-status mutation survives a later delivery refusal

Former local code: DD-320. Guarded delivery applied the coordinator's plan update before its staged-whitespace check refused; the retry needed an idempotent update. One SEED-082#story-1 execution, 0.3.67; no delivered defect. [Evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-323.md).

<a id="dd-324"></a>

## ODF-331 — Implementer takes a red baseline by overwriting a shared-checkout file

Former local code: DD-324.

An implementer obtains a red baseline by swapping committed bytes into the live shared checkout, creating interruption and concurrent-proof exposure.

[Retained evidence and current response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-331.md). Original occurrence provenance and distinct effects are recoverable in Open Dough `ccf309a69459284de3f1d45f2de813e9180c2971:docs/maintainer/findings/ODF-331.md` and this project's source snapshot `d5cb4a923b16973028820eb9cb1f4d811b4ef4f6:DearDough.md`. Name adoption is not resolution.

<a id="dd-332--formatting-expands-a-file-after-the-last-size-check"></a>

## ODF-218 — Formatting expands a file after the last size check

Former local code: DD-332.

A pre-format refactor can approve a file below the maintained 250-line limit, then coordinator formatting expands it beyond that limit before the guarded operation commits/pushes. The final size invariant needs checking after the mutating formatter as well as during refactor.

### Occurrences

Distinct executions, original timestamps/releases/tools, observed effects and qualified inferences are [retained at ODF-218](https://github.com/terryyin/open-dough/blob/7716badfa29258ebcd299c3f61c0966a7c6a75d1/docs/maintainer/findings/ODF-218.md); rereading adds no occurrence.

## ODF-333 — Lifecycle duties enter the slice acceptance gate

Former local code: DD-333.

A source-only review of the complete implementation-and-landing task returned both product requirements and later publication/CI/reporting duties. The coordinator initially allocated all 187 clauses to the last implementation slice; the pre-commit gate then required observations for duties that could only follow that commit and execution completion. The coordinator separated 18 future-phase clauses into G3, explicitly retaining their mandatory continuation without claiming they had passed. This was a coordinator allocation mismatch; whether guidance should distinguish lifecycle clauses earlier remains a proposal, not an established shared defect. Relationship to ODF-267/270 is uncertain: this case has settled authority and a later lifecycle owner, rather than an unchanged defect or pending human decision.

### Occurrences
Distinct executions, original provenance, observations and qualified inferences are
[retained at ODF-333](https://github.com/terryyin/open-dough/blob/a29f625255a3368eb721249c01df96e0f98edfd4/docs/maintainer/findings/ODF-333.md); source recovery remains in this project’s pre-compaction revision.

## ODF-413 — A commitment was recorded as observed before its output was inspected

Former local code: DD-334.

During slice 1 acceptance, the coordinator recorded the source-review commitment "The `GET` is described in OpenAPI" as observed, pointing at generated output that did not exist yet. The API client was regenerated afterwards, during delivery, and nobody read the generated schemas. The generated spec reused the existing entity `Answer` schema for the nested export record, without three of its fields. The slice 2 implementer noticed a possible name clash for its own new record; the coordinator then inspected the spec, found the slice 1 defect, and opened obligation G1 to return it to slice 2. The shared problem is recording the observation before reading its output; the Springdoc schema-name collision that produced the wrong contract is Donut's and is recorded as DD-335 in [Donut Retrospective Findings](DonutRetrospectiveFindings.md#dd-335).

### Occurrences
Distinct executions, original provenance, observations and qualified inferences are
[retained at ODF-413](https://github.com/terryyin/open-dough/blob/a29f625255a3368eb721249c01df96e0f98edfd4/docs/maintainer/findings/ODF-413.md); source recovery remains in this project’s pre-compaction revision.

## ODF-139 — A plan narrowed a story commitment and only the source-only review exposed it

Former local code: DD-336.

The story required comparing the probe against two baselines; the plan's slice 3 limited that comparison to rows the relation found supported ("unsupported rows are left as they are"). Slice 2 found no supported row on the real data, so following the plan would have left the report with no baseline comparison. The coordinator noticed because the source-only review listed the baseline commitment unqualified, corrected the plan in place, and briefed the implementer to compare every row. The practice that worked: allocating the independent review's commitments, not the plan's slice text, as the acceptance list.

### Occurrences

SEED-100#story-2, first implementation `275504cfc1`. The 0.3.82 source-only review exposed the narrowed comparison before slice 3 delegation, without implementation rework. [Retained evidence](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-139.md) and [original source](https://github.com/nerds-odd-e/doughnut/blob/c4f485b9a8f3f1b0951789141732b55b52b51654/DearDough.md).

## ODF-252 — Execution was launched before an owner-held start input existed

Former local code: DD-338.

The plan states execution starts only when the owner's export file is outside the repository or a token is supplied. The dashboard launched execution with neither available; the coordinator searched, found nothing, and stopped with an attention report before any slice. The owner then supplied a token in the same session. Readiness recorded the story as ready without distinguishing an owner-held input that must exist at launch.

### Occurrences

SEED-100#story-2, first implementation `275504cfc1`. The 0.3.82 launch stopped before slice 1 and required one owner round trip. [Retained evidence](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-252.md) and [original source](https://github.com/nerds-odd-e/doughnut/blob/c4f485b9a8f3f1b0951789141732b55b52b51654/DearDough.md).

## DD-339 — A plan's lettered slice numbers were unreadable to the obligations script

The slice plan named slices `3a`, `3b`, `6a` and `6b`. At the first obligations listing the execution script refused the plan with `unreadable-slices` ("An ordered-slices heading is not in the established “### N. name” form") and `unknown-slice`. The coordinator renumbered all slices 1–8 in the plan and re-allocated the source-review table before delegating slice 1.

### Occurrences
- Execution: SEED-101#story-1, first implementation `44fb2233e0`
  - Timestamp: 2026-11-04T00:45:00Z (approximate; before the slice 1 commit)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.82
  - Evidence: plan `.planning/slice-plans/003-order-property-assimilation-by-change-time/PLAN.md` at `642aa7aace` (lettered headings) versus `44fb2233e0` (renumbered); `story-obligations.mjs list --slice 1` refusal in the coordinator transcript.
  - Observed effect: one mechanical renumbering of about 25 plan references before any implementation; slice planning had accepted headings that execution cannot read.

## DD-340 — A plan decision chose a costlier mechanism than the story's own one-step case, and existing cost guards rejected it

The story's Architecture names a web save "the one-step case (previous content vs new content)". The plan decided the web save would run the Git commit-range dater over the appended commit. The implementer built that, and four `NotebookGitWebContentSaveCostControllerTest` guards failed (tree fetches 39 against a cap of 13). The implementer switched to the story's previous-versus-saved comparison, which the coordinator accepted and recorded as the plan's revised "Web save" decision.

### Occurrences
- Execution: SEED-101#story-1, first implementation `44fb2233e0`
  - Timestamp: 2026-11-04T03:30:00Z (approximate; slice 5, commit `c66303158d`)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.82
  - Evidence: slice 5 implementation return (failed guards, then redesign); plan "Web save" decision at `642aa7aace` versus `c66303158d`; story Architecture bullet "One dating mechanism".
  - Observed effect: one discarded implementation inside a roughly 28-minute slice run; existing save-cost guards were the only check that caught it.
  - Inference: planning did not consult the save path's existing cost tests when it placed new work on that path.

## DD-341 — A plan's no-op shortcut contradicted a story clause, and the implementer's gap list exposed it

The plan said "A push that changes no note documents runs no dater". Story clause 6.1 requires "an edit followed by a revert inside one push" to be dated at the revert commit. A push containing only that edit and revert has no net change, so it took the publisher's early return and left the old date. The slice 6 implementer named this as an open question in its gaps. The coordinator returned it to the slice under that clause, and two more agent rounds moved dating to the single acceptance point that all three accepting push paths reach. Related to ODF-139 (a plan narrowing a story commitment); here the source review allocated 6.1 to the slice, and the implementer's gap report exposed the contradiction.

### Occurrences
- Execution: SEED-101#story-1, first implementation `44fb2233e0`
  - Timestamp: 2026-11-04T04:20:00Z (approximate; slice 6, commit `82d047703e`)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.82
  - Evidence: plan "Push" decision at `642aa7aace`; obligation G1 in the plan at `82d047703e`; tests `anEditRevertedInTheSamePushIsDatedAtTheRevert` and `anEditRevertedInAPushThatOnlyMovesAFolderIsDatedAtTheRevert`.
  - Observed effect: two extra delegated rounds inside slice 6; the final design has one call site instead of three.

## DD-342 — The coordinator sent a shell placeholder to the source-only reviewer

The coordinator wrote the source-only reviewer's prompt with `$(placeholder)` where the story text belonged. The prompt was not run through a shell, so nothing was substituted. The coordinator noticed after launch, stopped the reviewer before it returned anything, and relaunched it with the verbatim story text.

### Occurrences
- Execution: SEED-101#story-1, first implementation `44fb2233e0`
  - Timestamp: 2026-11-04T00:40:00Z (approximate; before the slice 1 commit)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.82
  - Evidence: coordinator transcript (stopped reviewer task, relaunch); `source-review.md` launch note records the stopped first launch.
  - Observed effect: one wasted agent launch, with no effect on the review's input.

## DD-343 — Implementers proved discrimination by disabling the new call

In slices 4–7 the implementers showed that the new tests depend on the change. Each temporarily disabled the decisive line (the dating call, the committer-time read, or the creation-time fallback), recorded the failing assertion, and restored the line. In slice 6 one such probe also proved which early-return path a test exercised. The coordinator accepted these as discriminating evidence without rerunning them.

### Occurrences
- Execution: SEED-101#story-1, first implementation `44fb2233e0`
  - Timestamp: 2026-11-04T05:30:00Z (approximate; slices 4–7)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.82
  - Evidence: slice 4–7 implementation returns (an author-time swap failing 4 of 4; a removed web dating call failing the e2e; a removed push dating call failing 4 of 5; a removed fallback failing the rebuild test).
  - Observed effect: acceptance relied on observed failures rather than test names, at the cost of one extra focused run per probe.

## DD-344 — A test-removal plan checked only the story's examples and dropped a test's other observation

The plan for SEED-101#story-2 slice 4 listed `NotePropertyIndexPlannerTest.changedValues_ignores_reordering_and_reformatting_of_the_same_parsed_values` for deletion as a repeat of the controller reorder test. Its decisive premise mapped every story-1 key example to a surviving test, but no one checked what else each deleted test observed. That test was also the only observation that quoting a scalar or switching list style does not re-date a value. The slice 4 implementer named the loss in its gaps after deleting it. The coordinator restored a reformat-only case (`changedValues_ignores_reformatting_of_the_same_parsed_values`) before delivery.

### Occurrences
- Execution: SEED-101#story-2, first implementation `9cdb6d998d`
  - Timestamp: 2026-11-04T08:20:00Z (approximate; slice 4 acceptance)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.82
  - Evidence: `4b4e3ca939:.planning/slice-plans/004-date-property-values-on-every-web-path/PLAN.md`, "Decisive premises" (the last row) and slice 4; slice 4 implementation return, "Gap"; commit `47794ae3e0`.
  - Observed effect: a learner-visible behavior (reformatting keeps a value's date) briefly lost its only test; it cost one extra focused run.
  - Inference: proving a removal per story example does not cover every observation of each deleted test. Checking each deleted assertion for a surviving owner would have caught this at planning.
