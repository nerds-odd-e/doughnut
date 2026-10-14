---
id: SEED-074
status: dormant
planted: 2026-10-12
planted_during: owner-directed backlog shaping after the OpenKB implementation assessment
trigger_when: testing whether deliberately authored Donut knowledge improves an external agent task
scope: medium
---

# SEED-074: Prove one useful capability from Donut knowledge

## Why This Matters

For a Donut author using an external agent, selected knowledge should improve
the quality of a real task. A plausible generated skill file alone does not
establish that improvement. The owner explicitly wants this experiment to be
independent of the learning/refinement and retrieval epic.

The 2026-10-12 assessment of main at `e851a7682d` found existing notebook Git
checkouts, HTTP/MCP context access, and commissioned tutoring requests with
feedback. No skill-effectiveness experiment was run. These existing paths are
enough to try one task without building new query, hosting, or skill infrastructure.

## Alternatives and Decision

- **Task prompt alone:** Cheapest baseline; it may already be sufficient.
- **Attach selected notes:** Strongest simpler alternative. If it performs as
  well as a procedure, use it and do not build a skill-generation feature.
- **Selected direction:** Compare both baselines with one small reviewed
  procedure grounded in the same notes. Use a current Git checkout for source
  reading and record its revision so retrieval changes cannot obscure the result.
- **General skill factory:** Deferred until a task-level benefit is demonstrated.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery, following
[SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
This is a non-executable experiment story, not authority to run it now.

<a id="story-1"></a>

### Decide whether an acceptance-test review procedure benefits from Donut notes

**Identity:** SEED-074#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** The owner can decide, from one recorded comparison, whether
compiling selected ATDD notes into a reviewed agent procedure makes an
acceptance-test proposal review more useful than the task prompt alone or the
same notes attached. The outcome is a trustworthy keep/change/stop decision; a
reusable procedure is retained only when that decision is keep.

**Scope — required:**

- **Inputs fixed before any run:** a small set of the owner's actual ATDD
  notes (about five to ten), read from a Git checkout of their notebook at one
  recorded commit; three acceptance-test proposals (one ordinary, one
  exceptional, one missing a requirement the review should ask for); and, per
  proposal, the owner's answer key written before any output exists (the
  important problems, and for the third proposal the missing requirement).
- **One procedure document**, drafted from the selected notes and reviewed by
  the owner before the runs. It names inputs, decision rules, output shape,
  and when to ask for missing evidence. Every rule traces to a selected note;
  it adds no ATDD knowledge the notes do not hold, so the comparison measures
  compilation rather than extra knowledge.
- **Nine single runs:** each proposal under three conditions — prompt alone;
  prompt plus the note contents; prompt plus the same note contents plus the
  procedure. Same model and agent harness, chosen at execution and recorded;
  a fresh session per run; identical proposal text; notes passed as file
  contents from the recorded commit, not through Donut retrieval.
- **Blind judging:** the owner scores each output against its answer key
  with condition labels hidden until the scores are recorded: important
  problems found and missed, unsupported criticisms added, whether each piece
  of advice is actionable, and for the third proposal whether the output asked
  for the missing requirement instead of assuming one. Latency and cost are
  recorded per run.
- **Output:** a dated evidence record (note list and commit, proposals,
  answer keys, procedure revision, model, prompts, raw outputs, scores,
  latency, cost) and a keep/change/stop recommendation with reasons under the
  owner's decision rule (2026-10-14): keep when the procedure condition beats
  notes-attached on at least two of the three proposals and is worse on none;
  stop when notes-attached equals or beats it; change when the result is mixed
  and the owner can name what to adjust. On keep, the procedure is saved as a
  note in the owner's notebook under the existing Markdown profile; otherwise
  it stays in the evidence record only.

**Scope — deferred promises** (not built or verified here): repeated runs or
further models for statistical confidence; other review tasks; packaging the
procedure as an external agent skill file; any generator, scheduler, or
note-to-skill pipeline; measuring the author's own assimilation.

**Scope — rejection constraints:** no new product code, MCP tool, hosted
service, query feature, or skill exporter (owner instruction, 2026-10-12).
Attached notes and the evidence record carry no private learning history
([ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
keeps learning data out of Portable content).

**Key examples:**

1. **Ordinary proposal.** Pre: the answer key for proposal 1 lists two
   important problems, for example a scenario that asserts implementation
   detail and a missing failure path. Trigger: the three runs. Result: each
   output is scored on which of the two it found, any criticism the answer key
   does not support, and whether each piece of advice is actionable. An output
   that rewrites the proposal instead of reviewing it is scored only on what
   the rewrite shows it found; a rewrite is not required.
2. **Exceptional proposal.** Pre: proposal 2 breaks a common ATDD rule on
   purpose, and the selected notes explain why that is right in its context.
   Trigger: the three runs. Result: a condition that criticizes the deliberate
   choice records an unsupported criticism; a condition that accepts it with a
   reason from the notes records a found judgment.
3. **Insufficient evidence.** Pre: proposal 3 omits a requirement needed to
   judge one scenario, and the answer key says the correct behavior is to ask
   for it. Trigger: the three runs. Result: an output that asks for that
   requirement and withholds judgment on that scenario scores "asked"; one
   that assumes the requirement and criticizes on that basis scores an
   unsupported criticism; one that asks about everything and judges nothing
   scores not actionable.
4. **Procedure grounding.** Pre: the drafted procedure contains a rule no
   selected note supports. Trigger: the owner's review before the runs.
   Result: the rule is removed or the supporting note joins the selected set;
   no run starts before the review passes.
5. **Fixed source revision.** Pre: the runs use note contents from commit
   `X`. Trigger: a selected note changes in Donut during the experiment.
   Result: later runs still use commit `X`; the change does not enter the
   comparison and the record names `X`.
6. **Decision.** Pre: all nine scores, latencies, and costs are recorded.
   Trigger: the owner applies the agreed decision rule. Result: keep, change,
   or stop is recorded with reasons. A finding of no benefit closes the story
   as a valid decision and claims no feature.

**Open decisions (owner, before planning continues):**

1. **Source notes.** Which notebook holds the ATDD notes, where its Git
   checkout is or how to clone it, and which notes form the selected set. None
   of the nine notebook checkouts under `~/git/notebooks` holds ATDD notes
   outside `_trash` folders (checked 2026-10-14).
2. **Review cases.** The three proposals and their answer keys. Recommended:
   reuse real proposals from past training or review work, anonymized, with
   the answer keys written before any run.

- **Value / learning:** Distinguish the benefit of the knowledge itself from
  the benefit of compiling it into a working method.
- **Effort hypothesis:** M, low confidence until notes and cases are selected;
  keep one bounded comparison within L rather than expanding to other tasks.
- **Depends on:** None of the epic's stories. Use existing clone/pull and
  ordinary source reading; no new MCP tool, hosted service, query feature, or
  product skill exporter is required.
- **Safe stopping point:** Recoverable evidence and a decision remain useful
  even if no reusable skill is retained and all epic work is deferred.

## Constraints and Open Decisions

- A generated procedure is not proof of the author's assimilation or mastery.
- Do not publish private learning history with knowledge exports. A saved
  artifact inside a Donut notebook follows the existing Markdown profile;
  external skill packaging is a derived output, not another notebook format.
- No numeric quality gain has been assumed; the story's decision rule is a
  blind, case-by-case comparison, not a measured gain.

## Ordering and Scope Reduction

Queue this behind the direct learning-loop and question-context outcomes,
without a dependency on them. It can be selected sooner as a cheap learning
opportunity. Do not broaden it into a skill factory, scheduler, dependency
tracking service, or automatic note-to-skill pipeline.

## When to Surface

When the owner has a recurring acceptance-test review task and a small usable
set of Donut notes, regardless of the other epic's delivery status.

## Breadcrumbs

- Owner's independent-capability-experiment instruction, 2026-10-12.
- [Related but nonblocking epic](SEED-073-learning-refinement-and-retrieval.md).
- [Commissioned learning protocol](../../docs/commissioned-learning-session-protocol.md).
- [Git content authority](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  and [Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
