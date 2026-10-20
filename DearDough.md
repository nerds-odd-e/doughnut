# DearDough Process Findings

Reviewed 2026-10-12 (Asia/Tokyo). Active shared-process findings only; released
responses under observation live in the [watch list](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/near-term-watch-list.md).
Detailed observations, original releases and qualified inferences are retained
in the linked Open Dough catalog. Unknown execution provenance remains unknown.

- Highest allocated local number: 255. Removed numbers are never reused.
- Full source recovery before this cleanup: `0a8878e98be76b8e92203515d8f4719489b24169:DearDough.md`.
- Full source recovery before name adoption and compaction: `234f141b41332715edd5e4384a425bcb8f26e718:DearDough.md`; complete supplied evidence retained in Open Dough `dbe36711c0e4ff9421bc28afc53cb7477203e704:docs/maintainer/findings/`.
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

<a id="odf-264"></a>

## ODF-264 — Broad startup reads exceeded the response budget and forced repeated retrieval

Former local code: DD-233. Broad whole-reference and metadata batches exceeded the response budget and truncated required context before delegation, forcing repeated retrieval despite existing targeted-reading guidance. Seven distinct recorded executions; savings are unmeasured. This agent deviation is distinct from ODF-158’s required-reference cost. Open, unqueued. [Evidence and response](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/findings/ODF-264.md).

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

<a id="dd-251"></a>

## DD-251 — A plan removed the only remaining sign-in control before moving it

Slice planning ordered the removal of the old menus before the slice that moves the visitor's sign-in control into the new launcher. Those menus were the only place a visitor could sign in, so after the removal commit visitors would have had no sign-in until the next commit, and the signed-out E2E journeys might have failed on the story branch. The coordinator's destructive later-outcome check found the conflict before delegation and reordered the slices. Planning and readiness review had not found it.

### Occurrences
- Execution: SEED-078#story-1 (`.planning/slice-plans/003-donut-command-center/PLAN.md`, first implementation commit 15b6077ccd)
  - Timestamp: 2026-10-20T10:30:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.66
  - Evidence: the plan at f46a068 lists slice 3 "delete … VerticalMenu, HorizontalMenu" before slice 4 "`LoginButton` renders inside the panel and nowhere else"; `LoginButton` was used only by `HorizontalMenu.vue:17` and `VerticalMenu.vue:26`; the reorder was recorded in the plan's Current decisions in commit e4227c4.
  - Observed effect: the plan was corrected during execution before any implementation; the cost was one plan edit.
  - Inference: the planning premise check covered who consumed what was being removed, but not which user paths lived only in it. The timestamp is approximate, within the execution session.

<a id="dd-252"></a>

## DD-252 — Required build proof fails on formatting that implementers may not run

The plan required implementers to run the Vite build so `components.d.ts` is regenerated. Donut's build runs a Biome format check, but delegation forbids implementers from formatting, because the coordinator runs `format:changed` later. The slice 2 implementer therefore returned with the build failing on line-wrapping only, and build proof was confirmed only after the coordinator's formatting pass.

### Occurrences
- Execution: SEED-078#story-1 (`.planning/slice-plans/003-donut-command-center/PLAN.md`, first implementation commit 15b6077ccd)
  - Timestamp: 2026-10-20T10:10:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.66
  - Evidence: the slice 2 return says `pnpm -C frontend build` "exits 1, but only on the build's built-in Biome formatter check of three spec files"; the coordinator then ran `format:changed` ("Fixed 3 files") and the build.
  - Observed effect: one implementer proof was reported red and accepted on reasoning until the coordinator reran it; no defect escaped.
  - Inference: the shared rule that implementers do not format conflicts with any project proof command that also checks formatting. The timestamp is approximate.

<a id="dd-253"></a>

## DD-253 — Retargeting E2E page objects missed a direct label lookup of a moved control

When the always-visible menu was replaced by a panel that is rendered only while open, E2E callers were found by the names of the page-object helpers being retargeted. A separate direct `cy.findByLabelText('Resume')` in the recall page object was not one of those helpers, so it was missed. Local E2E for the named features and helper callers passed, and the one failure escaped to hosted CI.

### Occurrences
- Execution: SEED-078#story-1 (`.planning/slice-plans/003-donut-command-center/PLAN.md`, first implementation commit 15b6077ccd)
  - Timestamp: 2026-10-20T10:36:11+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.66
  - Evidence: CI run 39354041674 attempt 1 on 448d6fd, `recall/browse_answer_and_notes_while_recalling.feature` "Unable to find a label with the text of: Resume"; repaired in 0202d4a (`e2e_test/start/pageObjects/recallPage.ts`).
  - Observed effect: one CI failure and one repair cycle (stash, repair agent, refactor pass, publication).
  - Inference: the search covered the helpers being retargeted, but not every lookup of the moved control's accessible names. The relationship to ODF-272 (shared accessible-name consumers missed) is likely but unconfirmed.

<a id="dd-254"></a>

## DD-254 — A deviation from a recorded plan decision was accepted as a placement choice and lost the decision's purpose

The plan decided that each page renders one page bar outside its loading and error gates, with only page-specific contents conditional. The slice 3 implementer instead rendered a temporary bar while loading that a different bar replaced once loaded, and asked the coordinator to accept or reject that shape. The coordinator accepted it as a routine placement choice and then offered the same shape to slice 4. Each slice's proof checked that a launcher existed in each state, not that it kept working across the change from loading to loaded. When loading finished, the bar was replaced and a panel opened during loading closed.

### Occurrences
- Execution: SEED-078#correction-1 (plan recoverable at `413c7f4f0e:.planning/slice-plans/004-donut-menu-reach-and-keyboard/PLAN.md`, first implementation commit b664e2afc9)
  - Timestamp: 2026-10-20T11:43:50+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.66
  - Evidence: plan "Current decisions" (one `GlobalBar` outside the gates); slice 3 commit 333642b (`<GlobalBar v-if="!bootstrap" />`) and slice 4 commit 7ec9e67; slice 5 local E2E and CI run 39360839469 on 7ec9e67 (`assimilation_walkthrough`, 5 scenarios: "Expected to find element: `[role=\"region\"][aria-label=\"Donut menu panel\"]`"); repaired in 484ce25 with "keeps the Donut menu open when … finishes loading" tests that failed on 7ec9e67.
  - Observed effect: one CI failure, one repair cycle (stash, repair agent, a coordinator decision for book reading, refactor pass, publication), and a red trunk-bound branch tip for about 25 minutes.
  - Inference: the decision text did not state why one bar instance mattered, and acceptance treated a deviation from a recorded decision as routine placement, not as a change to that decision whose purpose needed checking. The timestamp is the slice 3 commit time.

<a id="dd-255"></a>

## DD-255 — Asking the refactor pass to verify one named promise caught a regression that slice proof missed

For slice 6, the coordinator asked the refactor agent to confirm, read-only unless broken, that choosing an Account action still closes the panel, including a link to the page already shown. Before slice 6, the Account list was teleported outside the panel, so every action counted as an outside click and closed it by accident; no test observed the same-page case. Moving the list inside the panel broke that case. The refactor agent added a test that failed and made the list emit a close.

### Occurrences
- Execution: SEED-078#correction-1 (plan recoverable at `413c7f4f0e:.planning/slice-plans/004-donut-menu-reach-and-keyboard/PLAN.md`, first implementation commit b664e2afc9)
  - Timestamp: 2026-10-20T12:16:59+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.66
  - Evidence: slice 6 commit 25ec462, `DonutMenu.spec.ts` "closes when the account destination already shown is chosen" (failed 20/21 against the slice 6 implementation), `AccountMenuItem.vue` `@choose`.
  - Observed effect: the regression was fixed before delivery at the cost of one refactor pass of about 10 minutes.
  - Inference: a useful practice when a promise was previously kept only by a side effect of the structure being removed. The fix added behavior inside the refactor pass, which the refactor contract does not normally do (compare ODF-268); the timestamp is the slice 6 commit time.
