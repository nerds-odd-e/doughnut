# Slice Delegation

Use a fresh general-purpose sub-agent (or GSD `gsd-executor` when inside
`/gsd-execute-phase`). Keep wrap-up coordinator-owned; do not rely on
`gsd-executor` to run local post-change-refactor.

The implementer prompt must include:

1. The plan path and current slice text, but not the full plan history or Jidoka
   list.
2. A Jidoka stop for value/design forks, missing credentials, undiagnosed
   unrelated failures, or ambiguity.
3. `problem-decomposition.mdc`, `planning.mdc`, and `gsd-coexistence.mdc`,
   including the ~5-minute fuzzy / >10-minute hard split budget, relevant-test
   proof, no commit on red, no deliberately broken CI, and capability naming.
   Do not run a broader suite unless the slice's proof names that suite.
4. A hard stop before wrap-up: do not commit, push, mark the plan done, run
   post-change-refactor, run `format:changed`, or run standalone `lint:changed`.
   Leave relevant tests green and the tree uncommitted.
5. `revert_and_refine` when the slice is too big; the coordinator will invoke
   **slice-plan-refinement** on the existing PLAN.
6. `CURSOR_DEV=true nix develop -c <command>` except on Cloud VM; Git needs no
   Nix prefix.
7. A short return: ready for wrap-up with one or more compact proof blocks,
   Jidoka stop, or reverted and ready for refinement. Do not claim the slice is
   done in Git terms. Use this repeatable shape for every green focused command:

   ```text
   proof:
     command: <exact focused test command>
     covers: <behavior or paths this command covers>
     result: pass
   ```

   The command must be literal and complete. A placeholder, abbreviation, or
   paraphrase is missing or ambiguous proof.
8. Cooperate with coordinator CI pauses: stop editing, finish or terminate
   write-capable commands, and return `## PAUSED FOR CI` with current slice,
   changed/untracked paths, exact completed proof, incomplete commands, and
   next action. Stay idle until explicitly resumed. After resume, reread files
   affected by the repair and rerun only invalidated proof. Other agents share
   the checkout; never revert their work.

Resume context remains in the plan on disk.
