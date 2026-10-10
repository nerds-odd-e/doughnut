# Deliver Donut removals under the project's deletion rule

## Source

- Story: [Deliver Donut removals under the project's deletion rule](../../seeds/SEED-071-reliable-project-feedback.md#removals-follow-project-rule)
- Identity: SEED-071#removals-follow-project-rule
- Finding: [DD-205](../../../DonutRetrospectiveFindings.md#local-removal-proof)
- Preparation workspace: `/Users/terryyin/git/doughnut/.worktrees/deliver-donut-removals-under-the-project-s-delet`
- Branch: `claude/deliver-donut-removals-under-the-project-s-delet`
- Preparing agent: Kaoru-chan; established assignment revision `bca15146b20eea7909ad449875e031e5a51a71f8`.
- Publication target: `origin/main`; integration checkout: `/Users/terryyin/git/doughnut`.
- Authority: the current request is planning only. This plan does not Take,
  execute, commit to trunk, publish, or release the preparation assignment.

## Goal and scope

A removal handed to Donut's planning and delivery roles produces deletion,
transitive cleanup, and the whole-product sweep, with proof of the behavior
that survives. Nobody writes, reviews, or deletes an observation of the
removed thing.

The gap, from the story's refinement finding: the shared proof-ownership step
asks the planner to map every promise, including "X is deleted", to proof.
Principle 7 says what a removal must not leave and nowhere says what proof a
removal promise owns, so both DD-205 planners mapped the deletion promise to
an observation of the removed thing, and the implementer wrote it.

Included: one positive statement of what proof a removal owns, in `AGENTS.md`
and `CLAUDE.md` principle 7 (kept identical) and in the always-on
`unit-testing` skill's assertion rules, which reach the implementer and
refactorer; and a one-time demonstration that a fresh agent, given the two
recorded removal shapes and the corrected guidance, writes proof of the
surviving behavior only.

Excluded, per the story: the shared `dough-*` skills, whose proof-ownership
step stays as it is; any check that Donut artifacts contain no absence test;
a plan template or proof format; restating the existing "every role" bullet
elsewhere. A refusal the product actively enforces is not a removal and keeps
its tests, as principle 7 already says.

Assumptions: the demonstration's produced plan text is inspected and
discarded; it is never committed and gains no `.planning/` entry.

## Existing solutions and architectural constraints

PFE search covered Donut-authored guidance that a planner or implementer reads
when writing proof: `AGENTS.md`/`CLAUDE.md` principles, `.agents/agent-map.md`,
the stack and testing skills, `docs/`, and `.planning/NORTH-STAR.md`.

- Principle 7 (`AGENTS.md:30–35`, `CLAUDE.md:30–35`, identical) is the only
  Donut statement of the removal rule. Unlike principles 1–5 it names no
  detail home; its details are inline. The new statement extends this
  bullet list; it does not create a new document.
- `.agents/skills/unit-testing/SKILL.md` is the always-on skill for every
  coding session (`CLAUDE.md`). Its "Focused assertions" list already holds
  the neighbouring rule "Prefer asserting the positive signal over 'not the
  other message/alert'". The removal rule for tests is one more bullet there,
  as the assertion-side detail. `.claude/skills/unit-testing` is a symlink to
  it, so one edit reaches both discovery paths.
- The shared `dough-story-refinement/references/executable-proof.md` and
  `dough-slice-planning/SKILL.md` are installed Open Dough skills and stay
  unchanged. The sweep reading that plan 006 already used (row "Start writes
  no `dev.pid`": a `git grep` over the product returning nothing, "no test
  asserts the file's absence") is the existing shape the guidance now names.
- No Accepted ADR governs agent guidance; no North Star topic is warranted.
  Nothing in product code changes.

Skills to load during execution: `unit-testing` (it is also the file edited).

## Outside-in proof

Key examples are the story's, in order.

| Promise (example) | Owner | Observable proof |
| --- | --- | --- |
| Guidance states what proof a removal owns, where planners and test writers read it | 1 | Read principle 7 in both files and the `unit-testing` "Focused assertions" list; `diff` of the principle 7 blocks of `AGENTS.md` and `CLAUDE.md` is empty. |
| Dictation shape: proof names the surviving behavior and a sweep reading; no row about the Responses API (1) | 2 | A fresh agent, given the story text at `595e2eb5d9` and told to read the current guidance, writes only a Goal and an Outside-in proof table to a temporary file; the coordinator reads it: every row observes surviving or replacement behavior or is a one-time sweep reading, and no row observes the removed rewrite or client call. |
| PID-file shape: deletion plus sweep, replacement start and stop proof, one-time `git grep` reading; no "leftover `dev.pid` is ignored" test (2) | 2 | Same demonstration with the story text at `cd1b2f1694`; no row prescribes a test or fixture built around `dev.pid`. |
| An implementer skips and reports a step asking for an absence assertion (3) | kept | Principle 7's existing "every role" bullet, unchanged; no new proof. |
| An enforced refusal keeps its tests (4) | kept | Principle 7's existing last bullet, unchanged; no new proof. |

Focused commands:

```sh
diff <(sed -n '/^7\. Removals/,/^$/p' AGENTS.md) <(sed -n '/^7\. Removals/,/^$/p' CLAUDE.md)
git show 595e2eb5d9:.planning/seeds/SEED-066-voice-input.md | sed -n '151,200p'
git show cd1b2f1694:.planning/seeds/SEED-069-agent-development-tooling.md | sed -n '33,110p'
```

The demonstration agents write under `$CLAUDE_JOB_DIR/tmp` (or another path
outside the repository), run no product-backlog recorder, announce nothing,
and touch no Git state. Their output is evidence for the plan's Learnings,
then deleted. No lint or test gate applies to a guidance-only change beyond
the commit hook.

## Decisive premises and observations

Observed on 2026-10-10 at revision `51bb751744` (only planning prose differs
from trunk).

| Premise | Consumed by | Literal observation and result |
| --- | --- | --- |
| Principle 7 is identical in `AGENTS.md` and `CLAUDE.md` | 1 | The `diff` command above printed nothing. True. |
| The `unit-testing` skill has a "Focused assertions" list with the positive-signal rule, and one edit reaches both discovery paths | 1 | `grep -n "Prefer asserting the \*\*positive\*\*" .agents/skills/unit-testing/SKILL.md` → line 24; `readlink .claude/skills/unit-testing` → `../../.agents/skills/unit-testing`. True. |
| Planners read principle 7 when writing proof, so the gap is wording, not reach | 1, 2 | `git show 9ad1cedcc0:.planning/slice-plans/008-keep-every-transcribed-sentence/PLAN.md` line 15 cites "CLAUDE.md principle 7" while line 91 prescribes "no Responses API call is made". True. |
| The shared proof-ownership step maps every promise, including a deletion, to proof | 1 | Read `executable-proof.md`: "Map every checkable final-state promise … to an owning slice and observable proof". True. |
| Both recorded story shapes are recoverable with their anchors | 2 | `git show 595e2eb5d9:.planning/seeds/SEED-066-voice-input.md` has `keep-every-transcribed-sentence` at line 151 with the removal decision; `git show cd1b2f1694:.planning/seeds/SEED-069-agent-development-tooling.md` has `reliable-development-stack-lifecycle` at line 33. True. |
| The demonstration is unpaid and side-effect-free | 2 | Agents read the repository and write to a temporary path; no OpenAI call, no service, no Git write. By construction of the instruction in slice 2. |

Whether the new wording changes a fresh agent's proof is the story's own
question; slice 2 is the observation that settles it, so no probe precedes it.

## Current decisions and cumulative design

- One rule, stated once in each reader's home: a removal's proof is the
  retained or replacement tests of the behavior that survives, plus a
  one-time sweep reading at acceptance (a search for the removed names over
  the product returns nothing) recorded in the plan as a reading. Proof of a
  removal names nothing about the removed thing. Principle 7 states it for
  refinement and planning; the `unit-testing` bullet states the test-writing
  consequence and points back to principle 7 rather than repeating it.
- Principle 7 cites `unit-testing` as its detail home for the assertion
  side, matching how principles 1–5 cite their skills. Keep the principle
  text identical in `AGENTS.md` and `CLAUDE.md`.
- The demonstration asks only for the Goal and proof table, not a whole
  plan: the proof mapping is where the gap occurs, and a full planning run
  would cost more than the story's budget without adding evidence. Both
  shapes run in parallel.
- If a demonstration still produces an observation of the removed thing,
  revise the wording once from what the agent wrote and rerun that shape.
  A second failure stops for the owner with both outputs quoted.

## Ordered slices

### 1. Guidance names the proof a removal owns
Type: Behavior
Status: done
Proof: read the three files; the principle 7 `diff` between `AGENTS.md` and
`CLAUDE.md` is empty; the `unit-testing` bullet sits in "Focused assertions".

Accepted 2026-10-10: one added line in each of `AGENTS.md:34`, `CLAUDE.md:34`
("Proof of a removal" bullet, citing the `unit-testing` skill) and
`.agents/skills/unit-testing/SKILL.md:25`; the `diff` command printed nothing.

Learning: the story's sentence "proof of a removal names nothing about the
removed thing" contradicts the sweep reading beside it, which must name the
removed names. The principle bullet reads "Apart from that reading, the proof
names nothing about the removed thing".

Behavior: a planner or test writer reads principle 7 or the `unit-testing`
skill → the text says a removal's proof is the surviving or replacement
behavior's tests plus a one-time sweep reading recorded as a reading, and
that proof of a removal names nothing about the removed thing → a deletion
promise has a named proof shape that is not an absence check.

### 2. A fresh agent writes surviving-behavior proof for both recorded removals
Type: Behavior
Status: planned
Proof: two temporary proof tables, one per recorded shape, each read by the
coordinator against the story's examples 1 and 2; the Learnings record the
result in a sentence each; the temporary files are deleted.

Behavior: a fresh agent with no memory of DD-205 is given the recorded story
text of one removal and told to read `CLAUDE.md`, the `unit-testing` skill,
and the executable-proof reference, then write a Goal and an Outside-in proof
table → every row observes surviving or replacement behavior or is a
one-time sweep reading; no row observes the removed thing. On a failure,
apply the single revision decision above.
