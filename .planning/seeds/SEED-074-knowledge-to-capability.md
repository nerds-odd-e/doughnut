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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** The owner can decide whether turning selected ATDD knowledge
  into an agent procedure makes acceptance-test proposal reviews more useful.
- **Scope:** Select a small, owner-reviewed set of actual ATDD notes and
  representative proposals. Draft one procedure specifying inputs, decision
  rules, output, and when to ask for missing evidence. Compare task prompt alone,
  task plus notes, and task plus the same notes and procedure, under comparable
  model and input conditions. Include ordinary, exceptional, and insufficient-
  evidence cases. Record source revisions, outputs, human judgments, latency,
  and cost. Deliver a keep/change/stop recommendation and the reviewed procedure
  only if it earns its additional complexity.
- **Evaluation:** The owner judges which important proposal problems each
  condition finds, which unsupported criticisms it adds, and whether its advice
  is actionable. Include a case where the correct behavior is to ask for a
  missing requirement. Success is a trustworthy decision; an honest finding of
  no benefit is valid and does not become a feature-delivery claim.
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

- The owner must select or confirm the actual source notes and review cases
  before execution; this assessment did not inspect a personal ATDD notebook.
- A generated procedure is not proof of the author's assimilation or mastery.
- Do not publish private learning history with knowledge exports. A saved
  artifact inside a Donut notebook follows the existing Markdown profile;
  external skill packaging is a derived output, not another notebook format.
- Agree the human evaluation rubric and acceptable added effort before the
  comparison. No numeric quality gain has been assumed.

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
