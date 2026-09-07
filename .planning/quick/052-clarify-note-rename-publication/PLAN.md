# Clarify identity-preserving note rename publication

Status: in progress; slice 1 is delivered.
Source: [SEED-009 Story 6](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6).
Execution observer: `/root`, checkout `/Users/terryyin/git/doughnut`,
`nerds-odd-e/doughnut` `main`, mailbox `/tmp/donut-ci-501/watch-goWo0j`, PID `34160`.

## Goal and scope

An owner following the existing Git publication workflow can understand how
to publish a same-folder rename and a later edit, what happens to authored
links, and which destination is blocked when a deleted note reserves a title.
This corrects guidance and diagnostics within delivered Story 6.

Preserve the existing unchanged-content, same-parent, single-direct-child
acceptance boundary and note identity, authored bytes, transaction, learning
state, and local-work protection. No new commands, automatic recovery, link
rewrites, aliases, restore support, or cross-folder movement. Story 12 and
its existing Plan 50 remain separate. No API schema or DDL changes.

## Review provenance and findings

Recovered execution-ready Plan 49 from `a83e68efb6` at
`.planning/quick/049-publish-local-note-move/PLAN.md`. All 12 leaves are done
at `980114d23d`; `eaa59f5d69` marks the story delivered and removes the plan.
Its stale top-level planning boilerplate does not override that completion
evidence. Review boundary: `eaa59f5d69`, also HEAD at review time; the working
tree was clean. No later fix resolves the findings below.

| Included commit | Evidence / contribution |
| --- | --- |
| `9355a20007` | Leaf 1: raw blob IDs in tree classification |
| `6e983e55a9` | Leaf 2: rename recognition, same-note mutation and acceptance/rejection proof |
| `1a166ebfea` | Leaf 3: private associations and unchanged identical copy |
| `83e9e6daa3` | Leaf 4: late acceptance failure rollback |
| `fcdb3f3c0a` | Leaf 5: installed checkout rename harness |
| `d2ccb4e6f8` | Leaf 6: installed CLI publication scenario |
| `e5fa653236` | Leaf 7: clone guidance and output assertions |
| `bde458d888` | Leaf 8: authored referrer preservation |
| `185a004bfe` | Leaf 9: invalid destination / mode rejection |
| `53ad422e09` | Leaf 10: deleted destination rejection |
| `295bedeb06` | Leaf 11: later edit updates original identity |
| `980114d23d` | Leaf 12: second checkout receives rename and edit |
| `eaa59f5d69` | Completed-story / backlog / plan cleanup |

Each leaf commit updates Plan 49. Inspected and excluded the intervening
`1da3418779`: release-planning changes only. Reviewed selected execution
patches together and product files at the final boundary; the product-only
net diff `9355a20007^..eaa59f5d69 -- backend cli e2e_test` is equivalent
because the excluded commit changes none of those paths. Planning-only edits
are provenance, not product-quality findings.

1. **P2 — Incomplete rename instructions.** `cli/src/nonInteractiveCli.ts:96`
   says to commit the rename then edit separately, but omits publishing and
   accepting the rename before editing. Following that sequence can leave
   two unpublished commits, rejected by `notebookPublishAncestry.ts:39`;
   the existing ancestry suite explicitly proves this rejection. The sentence
   about leaving links authored modifies deletion, so it does not explain
   that rename leaves old-path links unchanged and potentially unresolved.
   Original leaf 7 explicitly promised both pieces of guidance. Current clone
   unit and installed-feature assertions repeat the incomplete text.
2. **P3 — Deleted-title rename error lacks destination context.**
   `NotebookGitProposalPublisher.java:190` propagates the placement exception
   without the path context already supplied for additions. The generic
   message says a title exists "here" without naming the title or folder;
   `notebookPublishSubmission.ts` displays only `ApiError.message`.
   `NotebookGitDeletedDestinationControllerTest.java:109` pins that generic
   message. The collision is correctly rejected and rolled back; this is a
   diagnostic improvement, not an identity/atomicity defect. The old plan
   recorded the asymmetry but did not resolve it.

No concrete identity, rollback, ancestry, or reference-resolution defect was
found. Reviewed cumulative residue, duplication, naming, unnecessary helpers,
and changed-file sizes; no consequential refactoring work remains to plan.

Retrospective verification: `CURSOR_DEV=true nix develop -c pnpm -C cli exec
vitest run tests/notebookClone.test.ts tests/notebookPublish.test.ts
tests/notebookPull.test.ts` passed all 52 tests across three files (23.11s).
Backend and installed E2E proof were inspected, not rerun during this review.

## Outside-in proof ownership

| Promise | Owning leaf / observation |
| --- | --- |
| Rename must be accepted before a later content edit | 1: actual clone output explicitly describes commit → publish/accept → edit → commit/publish |
| Rename leaves authored links unchanged, possibly unresolved | 1: actual clone output explains the consequence; no automatic repair promised |
| Deleted destination rejection names the path and retains its cause | 2: public controller error has destination path, original conflict reason/type/deleted ID and cause |
| Rejected rename preserves source and accepted state | 2: existing committed collision fixture and binding observations remain green |

## Ordered slices

### 1. Explain the usable rename-then-edit workflow
Type: Behavior
Status: done
Proof: Drive CLI `run` via the existing clone-output test and observe complete
instructions; keep the installed feature's output expectation aligned.

Behavior: Owner clones a notebook → reads the existing next steps → can
follow the supported rename publication sequence with its link consequence.
Say to commit and publish the unchanged rename, wait for acceptance, then
edit and separately commit/publish. Explain that referring links are left
authored and old-path links may no longer resolve. Preserve the supported
same-folder scope and existing rejection/local-work policies.

Extend `cli/tests/notebookClone.test.ts` at its existing observable boundary;
update `e2e_test/features/cli/cli_notebook_clone.feature` where it asserts the
guidance. Avoid another verbatim-output test or new help abstraction.
Run `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.test.ts`.
Run the focused installed feature with
`CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.

Learning: the first installed-feature run reused an ignored stale version-keyed
0.5.2 CLI bundle cache and therefore showed the pre-change output. Parking that
generated cache forced a current-source rebuild; the rerun passed all scenarios.

### 2. Identify the reserved destination in a rename rejection
Type: Behavior
Status: planned
Proof: Existing public-controller deleted-destination fixture observes the
requested Portable path alongside the original error semantics and unchanged
persisted state.

Behavior: An accepted deletion reserves a same-folder destination title →
owner publishes a rename into that path → rejection names the destination
and existing remedy while leaving the source and accepted binding unchanged.
Reuse `NotebookGitProposalPublisher.withContext` around the placement failure;
retain error type, deleted note ID, original fields and cause as additions do.
Extend the existing rename case in
`NotebookGitDeletedDestinationControllerTest`, keeping its rollback assertions.
No new CLI error mapping is needed: its existing submission code displays the
server message. Keep production files within the 250-line limit through
cohesive local cleanup if needed; introduce no generic failure framework.
Execution verification follows the backend rule:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Decisions and readiness

Accepted ADR 0004 governs authored filenames/content/references; ADR 0006
permits enriching a business error with useful context while retaining cause.
Neither slice changes architecture or the story boundary.

Two stop-safe Behavior leaves, no preparatory Structure. Ready for direct
execution; each implementation is small and uses an existing proof boundary.
Target about five minutes of active work per leaf; record actual backend or
E2E runtime separately if that alone exceeds the target. Apply the normal
overrun and execution wrap-up rules when execution is requested.

Process retrospective remains explicitly excluded from this execution.
