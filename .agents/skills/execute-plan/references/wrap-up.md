# Slice Wrap-up

Check coverage under `planning.mdc`'s Proof decisions, including
replacement/lifecycle obligations. For claimed promises, inspect the relevant
assertion and only enough setup to identify the exercised boundary; compare that
expected result with the promise. Do not treat the test name, passing command,
or `proof:` summary as sufficient. A contradictory or insufficient observation is
incomplete implementation: name the promise and observation gap and return it
to implementation before refactor or acceptance. Refactor may report gaps but
cannot supply missing behavior. Require CI-safe uncommitted work: no deliberate
red; unfinished E2E stays `@wip`. Do not run full CI before commit.

Accept adequate inspected `proof:` evidence by default; add no mandatory
handoff fields. Reuse an earlier inspection while the promise and covered
boundary remain unchanged. Rerun only for a missing/ambiguous handoff, a
wrap-up-changed boundary, or a broader integration proof the slice closes but
the handoff omitted. Placeholders, abbreviations, and paraphrases are ambiguous:
recover the literal command from the original handoff when available. Reuse
adequate/recovered proof; never randomly sample.

1. Spawn a fresh general-purpose sub-agent to read and run
   `.agents/skills/post-change-refactor/SKILL.md` end-to-end. Pass only the slice
   text, plan path, implementer's compact `proof:` block(s), Nix/Cloud VM rule,
   no-commit constraint, and required completion markers. It must decide whether
   to edit before running tests: with no refactor edits, run no tests and report
   `skipped — no refactor edits`; with edits, rerun only the handed-off proof
   command(s) invalidated by those edits, or name and run a replacement when an
   edit moved the covered boundary. Explicitly forbid `format:changed` and
   standalone `lint:changed`.
2. Proceed only on `## REFACTOR COMPLETE`; stop without committing on a Jidoka
   stop or missing marker.
3. Run **generate-api-client** when backend controller or DTO signatures changed.
4. Run `./scripts/run.sh pnpm format:changed` directly once after refactor/API
   generation; require success before staging/committing. Let it select
   components (planning-only is a valid no-op); no pre-filtering or formatting
   agent. Repair mechanical failures and repeat only if that repair invalidates
   preparation. Stop for semantic/design judgment.
5. Update the plan (and SUMMARY if present), never `.planning/STATE.md`: record
   brief learnings, mark the slice done, prune obsolete detail, and adjust
   future leaves. If linked story understanding became stale, add an
   `awaiting story review` note naming the seed/story and affected field; route
   via `problem-decomposition.mdc` without altering sibling stories. This PLAN
   edit does not trigger a second formatting pass.
6. If post-slice learning needs developer judgment, commit and push safe work,
   then return a Jidoka stop with the required decision.
7. Commit only CI-safe work. Stage only owned files or separable owned hunks;
   inspect the staged diff. Whole-change staging is allowed only when all content
   is owned. Ordinary unrelated unstaged files need no approval. If unrelated
   content is already staged or hunk ownership is ambiguous, resolve the boundary
   with that owner first; do not co-commit or silently unstage, reset, or revert
   another task's index or worktree. The hook runs check-only `pnpm lint:changed`
   on staged components; it must not format or mutate the index. Resolve
   mechanical findings; stop for semantic/design judgment. Do not run standalone
   `lint:changed`. If a hook repair invalidates preparation, rerun the direct
   formatting command before restaging and retrying.
8. Push with `git push`. Success completes routine wrap-up. Keep the existing
   nonblocking CI observer per [ci-monitor.md](ci-monitor.md); it discovers this
   push without new setup. Continue the plan and handle delivered CI failures
   through pause/stash/repair/resume; never wait for CI or CD. Same after
   pushing a CI repair.
