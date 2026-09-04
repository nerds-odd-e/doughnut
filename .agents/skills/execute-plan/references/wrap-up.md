# Slice Wrap-up

The coordinator owns this sequence after the implementer returns with relevant
tests green, no intentional non-`@wip` CI failure, and uncommitted changes.

1. Spawn a fresh Task (`generalPurpose`) to read and run
   `.agents/skills/post-change-refactor/SKILL.md` end-to-end. Pass only the slice
   text, plan path, Nix/Cloud VM rule, no-commit constraint, and required
   completion markers.
2. Proceed only on `## REFACTOR COMPLETE`; stop without committing on a Jidoka
   stop or missing marker.
3. Run **generate-api-client** when backend controller or DTO signatures changed.
4. Spawn a fresh minimal-context Task (`generalPurpose`) to read and run
   `.agents/skills/format-changed/SKILL.md` end-to-end. Pass only the skill path,
   instruction to prepare current changes, and Nix/Cloud VM rule. It must not
   stage, commit, push, revert, or update the plan.
5. Proceed only on `## FORMAT CHANGED COMPLETE`; stop without staging or
   committing on a Jidoka stop or missing marker.
6. Update the plan (and SUMMARY if present), never `.planning/STATE.md`: record
   brief relevant learnings, mark the slice done, prune obsolete detail, and
   adjust future leaves. If a linked story decomposition became stale, add an
   `awaiting story-decomposition review` note naming the seed/story and affected
   field without altering sibling stories.
7. If post-slice learning needs developer judgment, commit and push safe work so
   far, then return a Jidoka stop with the required decision.
8. Commit only CI-safe work. Review the diff, prefer staging all changes so none
   remain local, and make a partial commit only deliberately. The hook runs
   check-only `pnpm lint:changed` on staged components. Resolve mechanical
   findings directly; stop for semantic/design judgment. After any file change,
   rerun the fresh format-changed step before restaging and retrying.
9. Push with `git push`.
