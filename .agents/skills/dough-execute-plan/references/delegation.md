# Delegate a slice

Assign each planned slice, or the one quick slice, to a fresh implementation
agent. Use a general-purpose agent, or `gsd-executor` when this project uses
`/gsd-execute-phase`. Implement locally only for a single interactive slice.
The coordinator retains
[wrap-up](wrap-up.md); an execution tool does not take over that responsibility.

Give the agent:

- The selected execution checkout and branch. For planned execution, pass the
  complete retained execution identity; for quick execution, pass the location
  retained in the conversation. Require all implementation commands and edits
  to run there rather than relying on the agent's inherited working directory.
- The execution source and current slice with mapped promises and observations,
  including replacement and lifecycle obligations. For planned execution, pass
  the plan path and its selected-story or bounded-correction source. Also pass
  any relevant existing-solution finding and candidate evidence from the plan,
  plus new evidence that triggered a PFE revisit; a fresh agent does not repeat
  a still-valid search merely because delegation occurred. For a correction,
  pass its complete plan-owned
  [correction input](../../dough-story-refinement/references/planning.md#choose-the-planning-level)
  rather than requiring a seed. For quick execution, pass the canonical story,
  the explicit instruction to execute without slice planning, and the relevant
  conversation context; require no plan or substitute execution record. Omit
  unrelated plan or conversation history.
- Any North Star topic cited by the delegated work and the evidence supporting
  it. Require the agent to return contrary evidence through [execution
  decisions](execution-decisions.md#resolve-conflicting-recorded-direction),
  without changing the topic or continuing the affected path.
- For planned execution continuing an oversized quick attempt, the remaining-work
  plan plus the preserved completed work and proof and any incomplete-change
  disposition needed to identify the true starting boundary. Require the agent
  not to repeat completed compatible work or its unchanged proof. Treat the
  quick attempt and planned continuation as one execution, not two handoffs with
  independent histories.
- [Execution decisions](execution-decisions.md), this project's slice budget and
  exceptions, workflow precedence, and literal focused commands with the runtime
  wrapper. Require relevant proof; broaden testing only when the slice, project
  workflow, or human requires it.
- Ownership of the slice's changes. State that other agents may share the
  execution checkout and their work must be preserved.
- A stop before coordinator delivery: no commit, push, marking a planned slice
  done, refactor pass, selective formatting, or independent hook-owned lint
  command.
- The [CI pause and resume contract](ci-monitor.md#pause-and-resume-writers).

Require uncommitted changes with passing focused proof, a stop requiring human
judgment, or an oversized-slice report under execution decisions. An implementation
return does not establish slice completion.

For each passing focused command, use:

```text
proof:
  command: <literal complete focused command>
  covers: <observable behavior or paths covered>
  result: pass
```

Connect proof to the planned slice's or quick story's promises. Placeholders,
abbreviations, and paraphrases are ambiguous evidence. Report uncovered behavior
as incomplete implementation; the refactor pass must not supply missing behavior.
