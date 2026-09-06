# Slice Wrap-up

Apply `planning.mdc`'s Proof decisions, including replacement/lifecycle coverage.
Compare each promise with its assertion and enough setup to identify the boundary;
test names, passing commands, and `proof:` summaries alone are insufficient.
Return contradictory or incomplete observations to implementation before refactor or
acceptance, naming the promise and gap. Refactor may report gaps, not supply missing
behavior. Require CI-safe uncommitted work: no deliberate red; unfinished E2E stays
`@wip`. Do not run full CI before commit.

Accept adequate inspected `proof:` without new handoff fields; reuse inspections
while promises and boundaries remain unchanged. Rerun only for missing or ambiguous
handoffs, wrap-up-changed boundaries, or omitted broader integration proof the
slice closes. Recover literal commands from original handoffs when available:
placeholders, abbreviations, and paraphrases are ambiguous. Reuse adequate/recovered
proof; never randomly sample.

1. Spawn a fresh general-purpose sub-agent to read and run
   `.agents/skills/post-change-refactor/SKILL.md` end-to-end. Pass only the slice
   text, plan path, implementer's compact `proof:` block(s), Nix/Cloud VM rule,
   no-commit constraint, required completion markers, and this clause: decide
   whether to edit before tests; with no refactor edits, run no tests and report
   `skipped — no refactor edits`; with edits, rerun only the handed-off proof
   commands invalidated, or name, explain, and run a focused replacement for a
   moved boundary.
   Correct contradictory additions before dispatch. Explicit developer verification
   requests remain authoritative. Forbid `format:changed` and standalone
   `lint:changed`.
2. Check the existing edit/test report against that clause and require
   `## REFACTOR COMPLETE`; stop without committing for Jidoka or a missing marker.
   Apply the evidence gate above to gaps. Report unnecessary prior runs as process
   deviations, reuse valid proof, and correct delegation; never repeat tests/review
   to manufacture compliance. A pass cannot erase an actual failure; retain
   `execute-plan`'s failure diagnosis and routing.
3. Run **generate-api-client** when backend controller or DTO signatures changed.
4. Run `./scripts/run.sh pnpm format:changed` directly once after refactor/API
   generation; require success before staging/committing. Let it select components
   (planning-only may be a no-op); no pre-filtering or formatting agent. Repair
   mechanical failures; repeat only when repair invalidates preparation. Stop for
   semantic/design judgment.
5. Update the plan (and SUMMARY if present), never `.planning/STATE.md`: record
   brief learnings, mark done, prune obsolete detail, and adjust future leaves.
   If linked story understanding became stale, add an
   `awaiting story review` note naming the seed/story and affected field; route
   via `problem-decomposition.mdc` without altering sibling stories. This PLAN
   edit triggers no second formatting pass.
6. If post-slice learning needs developer judgment, commit and push safe work,
   then return a Jidoka stop with the required decision.
7. Commit only CI-safe work. Stage only owned files or separable owned hunks;
   inspect the staged diff. Stage everything only when all content is owned.
   Unrelated unstaged files need no approval. For unrelated staged content or
   ambiguous hunk ownership, resolve the boundary
   with that owner first; do not co-commit or silently unstage, reset, or revert
   another task's index or worktree. The hook runs check-only `pnpm lint:changed`
   on staged components without formatting or index mutation. Resolve
   mechanical findings; stop for semantic/design judgment. Do not run standalone
   `lint:changed`. If a hook repair invalidates preparation, rerun the direct
   formatting command before restaging and retrying.
8. `git push` success completes routine wrap-up. Keep the nonblocking observer
   per [ci-monitor.md](ci-monitor.md); it discovers pushes without new setup.
   Continue, handling delivered CI failures through pause/stash/repair/resume;
   never wait for CI or CD, including after pushing a CI repair.
