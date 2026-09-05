# Slice Wrap-up

The coordinator owns this sequence after the implementer returns with complete
focused proof, no intentional non-`@wip` CI failure, and uncommitted changes.
Reuse the handed-off proof without rerunning it unless the handoff is missing or
ambiguous, later wrap-up work changed the covered boundary, or the slice closes
a broader integration proof the handoff did not run. Rerun for one of those
invalidation reasons; do not randomly sample proof. Treat a placeholder,
abbreviation, or paraphrase as missing or ambiguous. First try to recover the
literal original command from the available implementer handoff without
rerunning it; recovered complete proof remains reusable.

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
4. Run `./scripts/run.sh pnpm format:changed` directly. Run it once on the
   routine path after refactor/API generation and let the repository command
   select affected components; a planning-only tree is a valid no-op. Do not
   pre-filter paths or spawn a `format-changed` agent. Apply an unambiguous
   mechanical repair directly and repeat only when that intervening repair
   invalidated preparation. Stop for semantic or design judgment.
5. Update the plan (and SUMMARY if present), never `.planning/STATE.md`: record
   brief relevant learnings, mark the slice done, prune obsolete detail, and
   adjust future leaves. If a linked story decomposition became stale, add an
   `awaiting story-decomposition review` note naming the seed/story and affected
   field without altering sibling stories. This later PLAN-only edit does not
   trigger a second formatting pass.
6. If post-slice learning needs developer judgment, commit and push safe work so
   far, then return a Jidoka stop with the required decision.
7. Commit only CI-safe work. Review the diff, prefer staging all changes so none
   remain local, and make a partial commit only deliberately. The hook runs
   check-only `pnpm lint:changed` on staged components. Resolve mechanical
   findings directly; stop for semantic/design judgment. Do not run standalone
   `lint:changed`. If a hook repair invalidates preparation, rerun the direct
   formatting command before restaging and retrying.
8. Push with `git push`. A successful push completes routine wrap-up. Start the
   nonblocking CI observer for the pushed revision per
   [ci-monitor.md](ci-monitor.md), then continue the plan. Handle delivered CI
   failures through its pause/stash/repair/resume protocol; never wait for CI
   or CD to finish. Apply the same rule after pushing a CI repair.
