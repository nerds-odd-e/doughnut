# Explain why a notebook publication was rejected

Source: [SEED-009, Story 2](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-2),
and the completed [publish plan](../004-publish-local-note-content/PLAN.md).
Status: complete.

## Story contract

A notebook owner whose committed local edit is rejected needs the actual reason
so they can correct the content or understand a synchronization conflict.

Example: the accepted Git head matches the checkout, but a web edit has changed
the current projection → `donut notebook publish <directory>` → a nonzero exit
displays the server's projection-drift explanation. The local commit, refs,
index, and worktree remain intact.

The same response-presentation contract applies to invalid Markdown, unsupported
tree changes, and a publication losing the race after its initial download.
Success and 401/403 permission-denial behavior remain supported.

This corrects the original story's actionable rejection path. It adds no pull,
merge, rebase, automatic retry, server validation policy, or synchronization
capability. CI watcher improvements are separate process proposals and are not
included here.

## Outside-in proof

The CLI `run` submission cases use real temporary Git checkouts and mocked HTTP
responses shaped like the backend `ApiError`. They assert terminal error text
and nonzero exit for 400 validation and 409 conflict variations, preserve local
state, cover the HTTP-status fallback for a response without an API message,
and retain success and permission-denial regression coverage.

## Ordered slices

### 1. Report the server's reason for rejecting publication
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.test.ts tests/notebookClone.test.ts`.

Behavior: an eligible bound checkout receives a rejected publication response
→ publish → report its actionable reason and exit unsuccessfully while leaving
the local checkout intact.

Learning: the existing exception presentation can carry both the backend
`ApiError.message` and an honest HTTP-status fallback, so no generic response
framework or backend/API change was needed. Eligibility-only tests now use
successful submissions instead of pinning the retired interim refusal.

## Decisions and wrap-up

- ADR 0001 governs terminology; ADR 0006 favors propagating or improving useful
  failure messages. No architectural policy change is needed.
- No API generation, backend suite, migration, or broad E2E run is indicated.

## Completion record

- Backend rejection reasons and status-only fallbacks now reach the notebook
  owner through the CLI's existing error presentation.
- Local refs, index, and worktree remain unchanged on rejection.
- The focused 26-test publish/clone proof passed after the refactor review.
