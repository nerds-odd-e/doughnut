# Slice Wrap-up

The coordinator first checks coverage under `planning.mdc`'s Proof decisions,
including applicable replacement/lifecycle obligations. Return behavioral gaps
to implementation before refactor or acceptance, including gaps refactor finds.
Require CI-safe uncommitted work: no deliberate red; unfinished E2E stays `@wip`.
Do not run full CI before commit.

Accept the `proof:` handoff from `delegation.md` by default. Rerun only for a
missing/ambiguous handoff, a boundary changed by wrap-up, or a broader integration
proof the slice closes but the handoff omitted. Placeholders, abbreviations, and
paraphrases are ambiguous: first recover the literal command from the original
handoff if available. Reuse adequate/recovered proof; never randomly sample it.

1. Spawn a fresh general-purpose sub-agent to read and run
   `.agents/skills/post-change-refactor/SKILL.md` end-to-end. Pass only the slice
   text, plan path, implementer's compact `proof:` block(s), Nix/Cloud VM rule,
   no-commit constraint, and required completion markers. Restate that it must
   decide whether to edit before running tests: with no refactor edits, run no
   tests and report `skipped — no refactor edits`; with edits, rerun only the
   handed-off proof command(s) invalidated by those edits, or name and run a
   replacement when an edit moved the covered boundary. Explicitly forbid
   `format:changed` and standalone `lint:changed`; formatting and lint ownership
   remain with the coordinator.
2. Proceed only on `## REFACTOR COMPLETE`; stop without committing on a Jidoka
   stop or missing marker.
3. Run **generate-api-client** when backend controller or DTO signatures changed.
4. Run `./scripts/run.sh pnpm format:changed` directly once after refactor/API
   generation; require success before staging/committing. Let the command select
   components (planning-only is a valid no-op); no pre-filtering or formatting
   agent. Repair mechanical failures and repeat only if that repair invalidates
   preparation. Stop for semantic/design judgment.
5. Update the plan (and SUMMARY if present), never `.planning/STATE.md`: record
   brief relevant learnings, mark the slice done, prune obsolete detail, and
   adjust future leaves. If linked story understanding became stale, add an
   `awaiting story review` note naming the seed/story and affected field; route
   via `problem-decomposition.mdc` without altering sibling stories. This PLAN
   edit does not trigger a second formatting pass.
6. If post-slice learning needs developer judgment, commit and push safe work so
   far, then return a Jidoka stop with the required decision.
7. Commit only CI-safe work. Review the diff, prefer staging all changes so none
   remain local, and make a partial commit only deliberately. The hook runs
   check-only `pnpm lint:changed` on staged components; it must not format or
   mutate the Git index. Resolve mechanical findings; stop for semantic/design
   judgment. Do not run standalone
   `lint:changed`. If a hook repair invalidates preparation, rerun the direct
   formatting command before restaging and retrying.
8. Push with `git push`. A successful push completes routine wrap-up. Keep the
   execution's existing nonblocking CI observer running per
   [ci-monitor.md](ci-monitor.md); it discovers this push without new setup.
   Continue the plan and handle delivered CI failures through the shared
   pause/stash/repair/resume protocol; never wait for CI or CD to finish. Apply
   the same rule after pushing a CI repair.
