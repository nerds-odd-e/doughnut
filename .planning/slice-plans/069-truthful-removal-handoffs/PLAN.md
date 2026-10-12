# Truthful removal handoffs

**Identity:** SEED-071#removal-proof-and-evidence-recurrence
**Source:** [Make Donut removal handoffs preserve valid proof and truthful evidence](../../seeds/SEED-071-reliable-project-feedback.md#removal-proof-and-evidence-recurrence)

## Goal and scope

Donut's owner can accept a removal handoff whose proof covers surviving
behavior and whose retained evidence remains truthful and usable. Correct the
Donut-local proof-item guidance, then use it in this story's real documentation
cleanup from plan to implementation, independent refactor review, and acceptance.

The owner accepted deletion of the dated observation material dependent on the
retired mid-speech control, with dependent-link repair and preservation of
independently meaningful evidence. Delete it transitively; do not disguise its
action with a synonym. Git retains the historical material. This choice does
not establish that the original observations were false.

Included: `AGENTS.md` / `CLAUDE.md` principle 7, the Donut-authored
`unit-testing` skill, `docs/voice-input-observations.md`, and their actual
consumers, notably the bounded discovery section in SEED-066 and the
observations link in `docs/voice-input.md`.

Excluded: public `dough-*` skills or scripts, generic readiness/proof machinery,
a permanent checker or absence test, new voice behavior, new paid-service
observations, unrelated retrospective findings, and reopening the completed
predecessor. Runtime code and tests need no edits for this documentation change.

## Existing owners and observed premises

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/make-donut-removal-handoffs-preserve-valid-proof`,
base `0a8878e98be76b8e92203515d8f4719489b24169`, read 2026-10-12. Execution
uses the checkout established by its own authorized start; these readings are
reusable only while their document boundaries remain unchanged.

- `git show b96f234fb8:AGENTS.md` and
  `git show 7d062d701f:.planning/slice-plans/065-unobtrusive-spoken-title/PLAN.md`:
  the October 10 rule was present; the later plan cited it in scope but its
  proof table and slice 5 prescribed an absence assertion. This is a concrete
  contradiction at the proof-item consumer. The agent's internal cause is
  unknown and is not an implementation premise.
- PFE: change the existing local rule owners. Principle 7 owns deletion and
  sweep, and `unit-testing` owns local assertion style. Public refactor guidance
  already defers to project test style. No new policy document, workflow hook,
  or proof registry is needed. `.planning/NORTH-STAR.md` governs product
  architecture and real-service audio proof; this work changes neither and
  needs no new architectural decision.
- `git show 7becda17c4 -- docs/voice-input-observations.md` exposes the five
  substitutions and heading change. Reading the current entire observation
  document confirms that its dates, revisions, limitations, inputs, and results
  distinguish historical evidence from current product behavior. Its navigation
  section refers to the orchard input in the affected section; later positive
  comparisons also refer back to those sessions. Those dependencies must be
  resolved in the same deletion, while the October 6 measurements are independent.
- `rg -n 'voice-input-observations|requested-mid|requested-conversion' .planning docs`
  reaches the SEED-066 passage anchor, its separate navigation anchor, and the
  maintained voice-input document's whole-file link. Reading each destination
  confirms the links currently resolve. Link preservation is proof of usable
  retained evidence, not a proxy for observing the product again.

These are document-consumption premises, observed by reading the actual rules,
proof lists, observations, and link targets. No runtime test, environment, or
paid-service premise is used. Guidance effectiveness is the outcome to establish
during execution, not a premise claimed by the existence of guidance.

## Outside-in proof

| Promise | Owning slice and observation |
| --- | --- |
| A mixed change carries surviving behavior proof consistently from the proof table through its slice and brief | 1: one-time review of the historical mixed-change example using the revised local rule; retain button-state and error-feedback observations across all three forms |
| An actively enforced read-only restriction keeps its rejection proof | 1: the same review preserves the authorization boundary's distinct proof |
| Retained evidence keeps accurate provenance, results, limitations, and usable links | 2: compare retained observations with the execution base and follow each affected local Markdown link/anchor |
| The local rule reaches a real implementation and acceptance handoff | 2: coordinator brief, implementer return, fresh refactor review, and accepted diff carry the same evidence-preservation proof; record their results here |

The historical example is a bounded review, not a new product test or evidence
that later work will always comply. Slice 2 supplies the real removal handoff.
No frontend/backend tests are added or rerun solely for prose changes; retained
runtime tests stay intact. The one-time sweep below is an acceptance reading,
separate from the retained proof above.

## Ordered slices

### 1. Removal briefs carry the proof of surviving behavior
Type: Behavior
Status: planned
Proof: a one-time review of the mixed-change example demonstrates that the
proof table, slice Proof, and implementation brief agree on button states,
error feedback, and the active read-only boundary. Record the reviewed input,
resulting brief, and classification in this plan; keep no permanent harness.

Behavior: a coordinator prepares a mixed change under Donut's local rules →
each proof item is carried into the handoff according to the surviving promise
it establishes, with the sweep recorded separately. A genuine product refusal
keeps its rejection proof. When an inherited item contradicts the local rule,
the coordinator corrects the plan and brief before implementation; a downstream
role encountering it skips and reports it as principle 7 already requires.

Change the existing local guidance, keeping `AGENTS.md` and `CLAUDE.md` in sync
and the test-style detail with `unit-testing`. Include dated evidence handling:
preserve truthful retained observations; delete dependent obsolete evidence and
its consumers under an accepted disposition, never rewrite an action solely
to clear a search. An unresolved evidence-retention decision stays with its
owner. Do not invent a blanket exception to the removal rule.

Use the historical plan named above as the challenging input. This reading
must exercise individual proof items, not merely restate its scope paragraph.
The next slice consumes the resulting rule in an actual delivery.

Sizing: about 5 minutes for editing and the focused reading, plus normal
delivery overhead. Stop-safe: the clarified guidance is usable independently;
the approved evidence cleanup remains explicitly owned by slice 2.

### 2. Retained voice evidence stays truthful through a real removal handoff
Type: Behavior
Status: planned
Proof: retained navigation and responsiveness observations keep their dates,
revision qualifications, input meaning, results, limitations, and working
links. Compare those facts with the execution base, read the resulting document
as a standalone account, and follow the affected Markdown links. Record the
coordinator brief, implementer result, independent refactor result, and final
accepted observation here with their revision boundaries.

Behavior: the approved dated-evidence disposition reaches an implementation
brief under slice 1's guidance → the delivered document contains independently
meaningful observations, and consumers reach the evidence they describe.
The brief and acceptance use that same preservation proof.

Delete the affected passage section and material solely dependent on it,
including SEED-066's corresponding discovery bullet and incoming anchor.
Follow the navigation input reference and later positive comparisons: retain
independent facts without changing their meaning; make an input description
self-contained from already recorded facts when necessary. Delete any dependent
claim that cannot stand alone rather than inventing a new observation. Keep
the navigation anchor, independent October 6 measurement table, and the
whole-file documentation link usable. Do not add a historical replacement note.

One-time acceptance sweep reading: search the product, documentation, and agent
guidance for `Write text now`, `requested mid-speech conversion`,
`requested-mid-speech-conversion`, and the corresponding SEED-066 `Flush`
reference. Inspect matches by meaning rather than deleting unrelated uses.
The retired control and its dependent journey must have no product residue.
This active plan, its source, and the open finding are temporary execution and
review evidence, cleaned up by ordinary retrospective/wrap-up; they are not
maintained product documentation or an excuse to retain product traces. Record
the literal command, scope, classified matches, and result here as a reading,
never as a test. Keep the preservation observations above as the proof.

Sizing: about 8 minutes, scrutinized because shared input descriptions cross
sections. The edits and link reading form one cohesive evidence outcome;
splitting them would leave dangling or misleading evidence. At 10 minutes,
apply the repository overrun rule before continuing. Stop-safe when all affected
links and independently retained observations are coherent in the same commit.

## Current decisions and delivery

- Both leaves are Behavior: their external consumers are Donut's contributors
  and readers of its evidence. Each has one observation loop. The common rule
  is proof of what survives plus a one-time sweep reading; evidence disposition
  does not add another proof system.
- No permanent negative-assertion detector, generic Open Dough change, fresh
  paid-service recording, or unrelated documentation cleanup is required.
- Follow `AGENTS.md` execution wrap-up: Jidoka, a fresh
  `dough-post-change-refactor` agent, API generation only if needed, coordinator
  `./scripts/run.sh pnpm format:changed` once, plan evidence update, commit with
  the independent check-only hook, and publication through `dough-execute-plan`.
  No API generation is anticipated. The refactor review in slice 2 is the
  ordinary required review, not an additional role or extra gate.
- Focused verification is document reading and link resolution because the
  changed behavior is guidance and evidence consumption. Apply the repository's
  normal whitespace check. Product suites become relevant only if execution
  discovers and authorizes a product-code change; such a change is not planned.
- During execution, record the actual handoff results and limits here. Do not
  close the recurrence on the historical review alone or claim universal future
  compliance. Retrospective and story wrap-up own findings disposition and
  spent history cleanup after the real handoff is accepted.

## Learnings

None beyond the preparation observations above.
