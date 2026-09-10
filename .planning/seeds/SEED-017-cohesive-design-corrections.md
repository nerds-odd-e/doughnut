---
id: SEED-017
status: dormant
planted: 2026-09-10
planted_during: Five-day completed-plan design retrospective
trigger_when: Improving Git workflows and verification cohesion
scope: L
---

# Five-day plan audit and cohesive-design correction plan

## Result and audit boundary

**Original audit findings (F1–F4):** two user-visible composition restrictions (F1/F2), one misplaced infrastructure policy (F3), and one duplicated lifecycle mechanism (F4). These are four root causes, not a defect count obtained by adding every affected plan. Several other plans improved cohesion or were superseded successfully.

Window: **2026-09-05 11:02 through 2026-09-10 11:02, Asia/Singapore**, a fixed rolling 120 hours. Current code inspected at **`6876f46de098cf6b41dfcae7b2a2a0bebb7c16ec`**. Inventory contains **66 distinct completed/closed plan identities: 54 reviewed and 12 explicitly excluded**. There are 68 historical PLAN paths because two plans were renamed. Reused numeric IDs refer to different plans; the full directory names below disambiguate them.

The original five-day inventory excluded the initial-publication family (plans
078–087, 092 and 094); preserve that historical audit boundary.

## Method and limits

Recovered tracked PLAN history under `.planning/quick` and `.planning/phases`, including deletions, renames and merged branches, using `git log --all --full-history --diff-merges=first-parent` with the fixed window. Inspected historical plan/closure evidence, associated implementation/proof changes and their present mechanisms. No completed phase PLAN was found in this window. Compared cumulative behavior, not filenames or class counts alone.

The table gives **closure/provenance dates**, which can lag the last behavior change. A deletion was not by itself treated as proof of delivery: slice state, implementation or explicit delivered/closure evidence was checked. Some last pre-deletion documents had stale planned statuses; final commits also implemented or recorded those remaining outcomes. For example, 050 relocation closes at `040ff8c689`, and 095 closes at `6876f46de0` with persistence/concurrent-E2E observations. Plan 093 remains present and done. Former 050 release-freeze is 051; former 069 mock-isolation is 070. No independent completed 090/091 PLAN artifact was recovered; do not invent missing plan identities from numeric gaps. The recent initial-publication correction described as 091 is excluded regardless.

This is a targeted **decomposition-to-design audit**, not a full correctness/security audit of every changed line. “No finding” means no concrete remaining instance of this pattern was established in the inspected concept. Local Git refs supply the history; no remote fetch or claim about unavailable branches. Full execution conversations for these other plans are unavailable, so causal attribution is to plan contracts and code evolution, not inferred agent deliberations. Later working-tree planning changes were preserved and not treated as executed code.

Verification: a temporary real Git repository exercised the current CLI history classifier through `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx /tmp/donut-plan-audit/probe.ts`. Results are below. This is a focused classifier probe, not a full CLI/server end-to-end run. Backend and infrastructure findings are source/test/history inspection; no application databases, running environments, production code, skills or completed plans were modified. Broad suites were not run for this read-only review.

## Original audit findings

### F1 — High: pull recognizes example-sized histories instead of compatible changes

Current evidence: [notebookLocalCandidate.ts](/Users/terryyin/git/doughnut/cli/src/commands/notebook/notebookLocalCandidate.ts:109), [notebookAcceptedAdditionInterval.ts](/Users/terryyin/git/doughnut/cli/src/commands/notebook/notebookAcceptedAdditionInterval.ts:47), and [notebookAcceptedInterval.ts](/Users/terryyin/git/doughnut/cli/src/commands/notebook/notebookAcceptedInterval.ts).

The local classifier dispatches by one versus two changed paths. The two-note branch requires exactly one accepted save of a different note, and refuses an already-based candidate. Addition history has separate recognizers for exactly one addition and exactly one addition followed by one save of that same note. The predicates are about the demonstration sequence/count, even where ancestry and changed operation kinds are otherwise compatible.

Focused current-code results:

| Local unpublished commit | Accepted history after shared base | Decision |
| --- | --- | --- |
| Edit A and B | Save C once | Rebase |
| Edit A and B | Save C twice | Reject |
| Edit A and B | No new accepted commit | Reject |
| Edit A | Save C twice | Rebase |
| Edit A | Add D, then save D once | Rebase |
| Edit A | Add D, then save D twice | Reject as structural change at D |

Impact: ordinary additional saves or another independently edited note make a previously supported workflow fail. In particular, an already-based batch has nothing to reconcile yet is refused. This does not establish data loss today; the demonstrated failure is refusal and misleading recovery work.

Provenance: 053/056 established single-local-note reconciliation and conflicts; 062 addition introduced the one-addition case; 065 added the addition-then-save recognizer; 066 introduced the two-note branch; 068 documented its rejection boundary. Relevant implementation anchors include `896b409156`, `1675d58cb9`, `565e0acde7`, `1945a62286`; 069 (`eb3e8ecc35`) adds exact-subtree replay with a scalar local path. Plans 063/064 show the inconsistency with already-generalized batch publication.

Safety interaction: [notebookPullRebase.ts](/Users/terryyin/git/doughnut/cli/src/commands/notebook/notebookPullRebase.ts:102) can absorb final-LF-only conflicts via whole-commit `rebase --skip`. Broader eligible local change sets must never discard other non-conflicting edits. Preserve every companion change; skip only when the complete remaining commit is redundant. Plan 076's exact equivalence safeguard is not itself evidence of a bad abstraction.

### F2 — High: ordinary-note operations cannot compose within one publication

Current evidence: [NotebookGitProposalTreeShape.java](/Users/terryyin/git/doughnut/backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalTreeShape.java:104). `requireAllowedNoteChanges` permits additions/modifications as a collection but rejects any deletion with another change. `detectEqualBlobRename` returns immediately unless the entire changeset contains exactly two entries. The rejection is explicitly pinned by [NotebookGitDeletionRejectionControllerTest.java](/Users/terryyin/git/doughnut/backend/src/test/java/com/odde/donut/controllers/NotebookGitDeletionRejectionControllerTest.java:28).

Simple consequences: deleting A and B together fails; renaming A without content changes while editing unrelated B fails. The second operation does not make the first rename's blob correspondence ambiguous, yet it prevents recognition because identity analysis is tied to the whole example shape.

Provenance: 045 (`d14948b845`) established isolated deletion; 049 (`6e983e55a9`) established isolated equal-content rename; 050 (`7d42867d91`, `f0e6f8713b`) correctly reused identity mutation for relocation but retained that outer gate. 052 documents the boundary. 016 and 063 already support collection application for other operation kinds. 059's exact folder relocation is a related structural boundary, not a separate license to guess identity.

These limits were explicitly part of the planned contracts. This is **design debt encoded during planning**, not evidence that implementation disobeyed its plan. Removing them changes supported behavior and must be captured as a correction story.

### F3 — Medium: isolated E2E resource policy knows demonstration filenames

Current evidence: [isolated-cypress-spec-selection.mjs](/Users/terryyin/git/doughnut/scripts/isolated-cypress-spec-selection.mjs), [isolated-openai-mock.mjs](/Users/terryyin/git/doughnut/scripts/isolated-openai-mock.mjs:30), [isolated-cypress.mjs](/Users/terryyin/git/doughnut/scripts/isolated-cypress.mjs:105).

The runner's closed registration contains four specific feature files. The OpenAI resource implementation exports one of those file identities, and orchestration decides whether to acquire mock resources by comparing the selected filename with that constant. The general resource concept therefore owns knowledge of a particular example.

Provenance: 061 established focused isolated admission; 070 implemented OpenAI setup for its selected feature; 088 admitted the CLI feature; 089 (`25d16928bf`) admitted MCP. 073's recording proof and 093's event-composition repair are useful shared lifecycle work, not additional findings.

The allowlist itself is **not proven wrong**: it prevents unverified tests from resetting or mocking shared services. No claim is made that all other feature files can safely run today. The specific finding is the reversed dependency and scattered filename-driven resource policy. Consequence: changing or adding an approved spec's capability requires awareness across runner and resource code, encouraging another example-specific branch.

Correction target: one small approved-spec registry declares required isolated capabilities; selection resolves a descriptor and orchestration acquires its resources. Resource lifecycle modules do not know feature filenames. Preserve the current four admissions, one-spec execution and unknown-spec refusal. This correction adds no newly admitted tests, external mocks or generic plugin framework.

### F4 — Medium: Development and SUT implement the same termination lifecycle twice

Current evidence: [sut-owned-process-tree.mjs](/Users/terryyin/git/doughnut/scripts/sut-owned-process-tree.mjs) and [development-owned-process-tree.mjs](/Users/terryyin/git/doughnut/scripts/development-owned-process-tree.mjs).

Both capture descendants, signal descendants/group with TERM, poll for owned-tree disappearance, escalate with KILL and fail after a bounded wait. Both contain their own missing-process handling and wait/termination progression. Sharing descendant enumeration does not share that state machine.

Provenance: 072 (`392599bf16`, `d191a6b33d`) established SUT descendant shutdown. 095 (`149809803a`) added the parallel Development implementation. Their ChildProcess-versus-recorded-PID entry points and ownership authentication legitimately differ. Other Development runtime concerns already reuse shared helpers; the finding is this particular duplicated mechanism, not the existence of two environments.

Impact is maintenance divergence: a fix to escalation, liveness or failure propagation requires changing two implementations. No process leak was reproduced by this audit.

Correction target: share termination of an already-verified owned process tree; retain separate ownership verification and thin caller adapters. Preserve peer safety, TERM-before-KILL, bounded failure and callers' observation of asynchronous failure. Do not combine environment state, ports, databases or ownership records.

## Plan-by-plan inventory

Each row is one plan identity. Dates are September 2026 closure/provenance dates; SHAs are local Git anchors, not assertions that a single commit contains the complete execution. Read historical plans with `git show <sha>^:.planning/quick/<full-name>/PLAN.md` for deletion anchors, and inspect that commit's delivered status/proof as well. For retained 093, use `git show 6adf381f84:.planning/quick/093-release-private-openai-mock-after-cypress/PLAN.md`.

| Plan directory | Closure/provenance | Current-state assessment |
| --- | --- | --- |
| 004-publish-local-note-content | 09-06 · `06e38695ce` | Superseded limit: single-note publication grew into shared changeset application in 016/063. No separate current finding; F2 covers remaining deletion/rename restrictions. |
| 005-report-notebook-publish-rejections | 09-06 · `06e38695ce` | No finding: CLI reports server rejection reasons through the common publication boundary. |
| 006-publish-from-conformant-clone-fixture | 09-06 · `06e38695ce` | No finding: installed-clone proof exercises the real publication flow; fixture shape is test data. |
| 007-observe-ci-once-per-execution | 09-05 · `9a5f46f0f2` | No finding: observer owns execution/run identities through a common lifecycle; host adapters reflect delivery capabilities. |
| 007-receive-web-note-content | 09-06 · `a5278d667f` | Later extended: common accepted snapshots and fast-forward pull remain cohesive. Divergent-history restrictions are assessed under F1, not charged again to this foundation. |
| 008-preserve-ci-observer-boundaries | 09-06 · `513f69b139` | No finding: retained worker/run identity, delivery acknowledgement and bounded shutdown are lifecycle rules. |
| 008-preserve-promises-through-execution | 09-05 · `e6ea35d4f9` | No product-code finding: workflow guidance preserves promises. Guidance alone did not prevent later example-shaped contracts. |
| 009-harden-web-note-receive-evidence | 09-06 · `3e41fb8e5e` | No finding: concurrency fixture isolation and bundle ancestry proofs improve the shared boundary. |
| 010-create-local-note | 09-06 · `d2d8fb9ff5` | Later generalized: note construction is shared and additions are applied as a collection. F2 concerns their composition with deletions, not another creation implementation. |
| 014-preserve-task-owned-edits | 09-06 · `ffa9b40e3d` | No product-code finding: changes concern attribution and one-writer review ownership. |
| 015-enforce-decide-first-refactor-handoffs | 09-06 · `72701489a7` | No new mapping finding: observer recovery uses retained identity; remaining changes are execution/proof guidance. |
| 016-publish-several-note-changes | 09-06 · `6b6bf5ddd8` | Positive generalization: additions/modifications share collection application and atomic persistence. Remaining structural combinations are F2. |
| 017-consistent-published-frontmatter | 09-06 · `74a384f7f4` | No finding: general YAML parsing and metadata/body separation; nested metadata is not implemented per example key. |
| 017-identify-publication-destination-conflicts | 09-06 · `f0fd4bed63` | No finding: destination conflicts are validated at the common publication boundary, including retained deleted destinations. |
| 018-report-access-denial-as-forbidden | 09-06 · `9977898b0d` | No finding: shared REST exception handling yields forbidden consistently. |
| 019-close-search-on-result-activation | 09-06 · `d0111cd7f0` | No finding: common result activation closes search across callers. |
| 045-publish-local-note-deletion | 09-07 · `cad4b44737` | F2: whole-commit isolated-deletion restriction remains and rejects multiple deletions or deletion plus unrelated edits. |
| 046-version-tag-production-releases | 09-07 · `891eff8ef9` | No finding: release selection/state is shared; earlier orchestration was superseded by reconciliation, not retained per-version handlers. |
| 047-reliable-login-with-browser-history | 09-07 · `97999a207a` | No finding: history normalization and bad-request recovery are general mechanisms; storage bounds are explicit resource policy. |
| 048-release-recovery-and-ordering | 09-07 · `0dd29768ea` | No finding: reconciliation derives ordering from version/immutable identity and persistent state; recovery examples are not separate release algorithms. |
| 049-publish-local-note-move | 09-07 · `eaa59f5d69` | F2: equal-blob rename recognition requires the entire changeset to contain exactly two entries. Identity preservation itself is appropriate. |
| 050-relocate-local-note | 09-07 · `040ff8c689` | F2 inherited: final-destination validation reuses same-note mutation, but the whole-commit rename gate still blocks composition with unrelated edits. |
| 051-freeze-pending-release-identities | 09-07 · `4c703786cf` | No finding: immutable candidate identity is a common release-state invariant. Formerly numbered 050. |
| 052-clarify-note-rename-publication | 09-07 · `63dbba7f74` | F2 symptom: diagnostics/docs describe isolated rename eligibility. They need alignment when the underlying contract changes, not an independent redesign. |
| 053-reconcile-local-web-content | 09-07 · `f3aec9da7f` | F1 origin: local-content rebase was framed around one local note. Current one/two-note branching preserves that example boundary. |
| 054-configured-worktree-backend-tests | 09-07 · `63dbba7f74` | No finding: suite datasource/ownership are shared configuration; explicit opt-in was subsequently superseded by automatic routing. |
| 055-worktree-suite-datasource-for-lock-tests | 09-07 · `63dbba7f74` | No finding: lock proofs use the configured suite database rather than a test-specific hardcoded database. |
| 056-resolve-overlapping-note-edits | 09-07 · `63dbba7f74` | F1 affected: conflict continuation is reusable, but eligibility limits its reach. Broader change sets must retain all companion edits. |
| 057-automatic-worktree-backend-tests | 09-07 · `63dbba7f74` | No finding: common worktree target selection and admission replace manual setup. |
| 058-exclusive-worktree-checkout-lock | 09-07 · `63dbba7f74` | No finding: one active owner and stale-owner evidence are real concurrency invariants. |
| 059-publish-folder-relocation | 09-08 · `6130c0be9a` | Related F2 boundary: exact whole-subtree correspondence is intentionally conservative identity evidence. No separate request to generalize ambiguous folder moves; preserve this path during correction. |
| 060-ordinary-worktree-backend-commands | 09-08 · `05ad9753c9` | No finding: ordinary commands route through shared ownership/datasource selection. |
| 061-concurrent-worktree-browser-e2e | 09-08 · `9e7b8e5f99` | F3 foundation: isolated runner is general, but spec admission/resource selection became coupled to feature filenames as later examples were added. |
| 062-pull-note-addition-with-local-edit | 09-08 · `2f032ecfc5` | F1: accepted addition history is recognized through an exact single-addition scenario. |
| 062-web-note-creation-local-refinement | 09-08 · `05ad9753c9` | No new finding: WebNoteCreationService uses shared construction, projection and accepted-snapshot persistence. Represented-folder checks protect projection consistency. |
| 063-existing-note-batch-publication | 09-08 · `04336bd793` | Positive generalization: ordinary existing-note publication is a collection. F1 explains why pull remains narrower than publish. |
| 064-existing-note-batch-guidance | 09-08 · `0b3a73eb53` | F1 symptom: guidance honestly documents batch-publish versus restricted pull; update it with the engine correction. |
| 065-local-edit-over-web-creation-save | 09-08 · `0b3a73eb53` | F1: new exact addition-then-one-save recognizer extends the scenario catalogue rather than an accepted-history model. |
| 066-local-edit-batch-rebase | 09-08 · `9e7b8e5f99` | F1: separate two-note algorithm allows exactly one save of a different third note; rejects already-based batches. |
| 067-isolated-browser-allocation-safeguards | 09-08 · `7057a95fa6` | No finding: allocated-port and owner checks enforce peer safety across worktrees. |
| 068-batch-pull-refusal-guidance | 09-08 · `0a797844b9` | F1 symptom: rejection messages and proofs entrench the two-note/one-save boundary; not a separate mechanism to fix. |
| 069-local-note-edit-across-folder-move | 09-08 · `6b32ce0e04` | F1 interface residue: exact-subtree replay carries a scalar localPath. Keep identity-safe move support; ordinary history correction must not accidentally broaden structural replay. |
| 070-isolated-openai-browser-mocks | 09-08 · `63423ccc48` | F3: mock-resource module exports a particular feature path and orchestration selects resources by that identity. Formerly numbered 069. |
| 071-web-autosave-commit-batching | 09-08 · `4df82944d9` | No finding: batching uses common note/head/time state. The ordinary-note eligibility and time window are deliberate policy, not literal fixture identities. |
| 072-owned-sut-descendant-shutdown | 09-08 · `67cc92e469` | F4 shared concept: establishes termination mechanism subsequently duplicated by 095; not evidence this original plan itself introduced duplication. |
| 073-exclusive-openai-recording-proof | 09-09 · `428e021e60` | No new finding: paired recordings verify isolation through common owners; F3 remains in admission, not in this proof. |
| 074-web-autosave-clock-precision | 09-08 · `76f8cb70b0` | No finding: timestamp precision correction applies across autosave state, not one timing example. |
| 075-retire-worktree-databases | 09-08 · `1f723707fb` | No finding: generic disposable identity, admission and database evidence govern retirement. |
| 076-absorb-equivalent-final-newline-edit | 09-08 · `858835291e` | No independent mapping finding: exact LF equivalence and single-conflict safeguards protect whole-commit skipping. F1 must preserve companion edits when broadening eligibility. |
| 077-retirement-admission-and-process-evidence | 09-08 · `dbc1bd7039` | No finding: evidence checks fail closed for ambiguous ownership; not per-worktree exceptions. |
| 078-create-readme-only-folder | 09-09 · `f506502e0b` | Excluded from original audit. |
| 079-initial-notebook-and-folder-readmes | 09-09 · `e9b56195eb` | Excluded from original audit. |
| 080-require-empty-notebook-for-initial-readmes | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 081-initial-folder-with-one-note | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 082-require-ordinary-note-in-initial-tree | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 083-initial-notebook-readme | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 084-cohere-initial-notebook-readme-publication | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 085-minimal-initial-container-with-note | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 086-cohere-proposal-binding-persistence | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 087-next-small-initial-readme-note-trees | 09-09 · `cd59ea24af` | Excluded from original audit. |
| 088-isolated-cli-web-note-e2e | 09-09 · `cd59ea24af` | F3: another literal spec registration added. CLI uses the owning environment correctly; issue is where resource policy lives. |
| 089-isolated-mcp-services-e2e | 09-09 · `960c4cc2ba` | F3: another literal spec registration. Awaiting MCP client shutdown is a valid lifecycle fix, not an example-shaped workaround. |
| 092-small-initial-notebook-layouts | 09-09 · `524017c50a` | Excluded from original audit. |
| 093-release-private-openai-mock-after-cypress | 09-10 · `6adf381f84` | No new finding: common event composition fixes overwritten cleanup callbacks. Current PLAN explicitly records done and live proof. |
| 094-initial-notes-with-relationship | 09-10 · `cea9985238` | Excluded from original audit. |
| 095-persistent-development-environment | 09-10 · `6876f46de0` | F4: separate Development termination state machine duplicates SUT descendant/TERM/wait/KILL logic. Other runtime helpers are already shared. |

## Correction plan

The active stories below cover independent correction outcomes. Each retains
its own scope and proof; this seed is not a cross-subsystem executable plan.
The container-publication proof correction is first in the queue, followed by
Stories 1–4 in their developer-selected order.

<a id="story-11"></a>

### Story 11: Consolidate root container publication proof

- **For / why:** developers need one maintained controller proof for
  container-only root publication, avoiding duplicate database and Git-bundle
  round trips for the same behavior.
- **Goal:** retain proof that notebook and root Folder Readmes publish as the
  exact authored Git state with zero Note rows in one container-tree owner.
- **Scope:** remove the overlapping successful root-Readme method from the
  notebook-structure controller test. Preserve that class's mixed-content,
  rejection and document-role scenarios, plus the notebook-Readme-only proof
  and installed CLI journey. Product behavior remains unchanged.
- **Evaluation:** the retained container-tree controller test owns the exact
  authored head, tree, paths, content, root ancestry and zero-Note observations,
  and the required backend suite passes after consolidation.
- **Key example:** given an empty Git-backed notebook, when a proposal contains
  a notebook Readme and root Folder Readmes, publication preserves the authored
  content and Git state, creates the root Folders and no Notes, without a second
  controller scenario owning the same observations.
- **Exclusions:** production changes, rejection or mixed-content cleanup, API,
  schema, CLI, E2E and broader test-suite changes.
- **Execution plan:** [quick/099](../quick/099-consolidate-root-container-publication-proof/PLAN.md).
- **Safe stopping point:** the remaining successful container-only proof has one
  owner independently of later Git workflow corrections.

## Open product decision

Should every web mutation of Portable content advance accepted Git history, or
should the product deliberately expose a partial synchronization boundary?
Normal web authoring participation needs separate user outcomes; do not silently
rebuild accepted history from live state. This question does not block the
queued corrections.

<a id="story-1"></a>

### Story 1: Receive compatible accepted history without example-count restrictions

- **For / why:** notebook owners should keep local content edits while receiving compatible accepted edits and additions; another save should not arbitrarily change eligibility.
- **Scope:** one unpublished ordinary content-edit commit containing a nonempty set of paths; a linear accepted interval of ordinary content edits and additions to valid represented destinations. Eligibility follows operation semantics and ancestry, independently of path count and save count. Preserve existing exact-subtree replay without extending it.
- **Design:** one local change-set representation; inspect accepted commits in order against the preceding accepted tree; one common rebase/conflict lifecycle. Remove the one/two-note and addition-then-save recognizers after replacement, including redundant negative tests and instructions that prescribe reducing work to an example.
- **Delivery sequence:** first make whole-commit conflict/skip handling safe for a change set; then unify content-only eligibility, including already-based batches; then apply the same interval model to additions/content saves; finally align diagnostics and user guidance with semantic rejection reasons. Keep safety and successful-case proofs in the same delivery as each behavioral change.
- **Evaluation:** the six probe cases above follow compatibility rather than count; test different path cardinalities and repeated/reordered independent saves. At the real `notebook pull` boundary, confirm content, accepted/main ancestry, continued conflicts, abort and all companion edits. Include an LF-equivalent conflict plus another valuable edit. Dirty checkout, unrelated/merge/multiple-unpublished history and unsupported structural operations remain safe refusals with local work intact.
- **Stop-safe outcome:** ordinary content-only histories work consistently before addition handling is broadened; no interim path-count branch becomes a permanent acceptance rule.
- **Effort hypothesis:** M–L, medium confidence; conflict state and complete-commit preservation need executable slice sizing. No database experiment is required for the CLI classifier itself.
- **Depends on:** no functional dependency on initial publication.

<a id="story-2"></a>

### Story 2: Publish compatible ordinary-note operations as one atomic changeset

- **For / why:** notebook owners can make one coherent edit that deletes or relocates notes alongside other compatible changes without artificially splitting Git commits.
- **Scope:** existing ordinary-note additions/modifications/deletions and unambiguous unchanged-content note relocation/rename, using current represented destination and identity rules. Keep current initial README/Relationship handling and exact folder-subtree publication behavior outside this correction.
- **Design:** derive typed operations from the full diff; resolve identity correspondence using the complete candidate set, not total entry count; validate final destinations and apply once under existing accepted-head locking/transaction ownership. Retain existing note construction, mutation and binding persistence.
- **Delivery sequence:** expose one complete ordinary-note changeset through validation/application; support compatible deletion collections; recognize unambiguous equal-content moves within that collection; prove the complete result and align diagnostics. Do not introduce “two deletions” or “one rename plus one edit” dispatchers.
- **Evaluation:** through proposal/controller and installed CLI boundaries, publish multiple independent deletions and a rename plus an unrelated edit; vary counts/order. Assert final content/location, unchanged identities and learning data, single accepted head, retry behavior and a receiving clone's result. Ambiguous equal-blob candidates, reserved destinations, stale head, drift and late invalid members must leave the entire state unchanged.
- **Stop-safe outcome:** deletion collections are useful independently; uncertain identity remains refused until correspondence has proof. Never silently downgrade a move to deletion/creation to make a fixture pass.
- **Effort hypothesis:** L, lower confidence because identity ambiguity and atomic effects require refinement before executable slices.
- **Depends on:** no functional dependence on Story 1; preserve the initial-tree publication contract while changing shared proposal code.

<a id="story-3"></a>

### Story 3: Declare isolated test capabilities in one place

- **For / why:** maintainers can see and change an approved test's resource requirements without editing resource lifecycle code for that example.
- **Scope/design:** a small approved-spec descriptor registry drives common acquisition/cleanup; retain the same admitted specs and safety rules. Resource modules accept capabilities/configuration, not feature identities.
- **Delivery sequence:** consolidate existing spec-to-capability policy; route runner/mock selection through it; remove filename exports/comparisons from resource lifecycle modules and update callers/tests.
- **Evaluation:** existing no-mock and private-OpenAI runs use their respective owned resources; unknown and multiple specs still refuse. At runner/Cypress lifecycle boundaries, prove success, failure and cancellation release the correct resources and preserve peer ownership. Reuse 073/093 proofs; add only missing observable coverage.
- **Stop-safe outcome:** the same four verified workflows continue to work with one policy owner. Adding more spec admissions is a later decision.
- **Effort hypothesis:** M, medium confidence. Needs one representative isolated-run proof; do not rerun every unrelated E2E scenario.
- **Depends on:** none.

<a id="story-4"></a>

### Story 4: Keep one owned-process termination mechanism

- **For / why:** maintainers can fix shutdown behavior once while developers retain safe independent Development and SUT lifecycles.
- **Scope/design:** extract the existing common termination progression behind adapters for ChildProcess and recorded PID/group callers. Ownership proof stays with each environment; no generic service-management framework.
- **Delivery sequence:** move the common already-owned-tree mechanism to one module; connect both adapters; remove duplicated loops/signalling progression and prove callers receive failures.
- **Evaluation:** existing public stop/restart tests for both callers cover normal exit, TERM-resistant descendants, disappeared processes and bounded failure. One representative process-level check confirms owned descendants exit while an unrelated peer remains alive. Preserve environment-specific ownership refusal before signalling.
- **Stop-safe outcome:** both environment entry points still work and use one mechanism. No migration of persistent data or ownership records.
- **Effort hypothesis:** S–M, medium confidence; the adapter contract is the main uncertainty.
- **Depends on:** none.

## Execution readiness and design checks

Story 11 has an executable plan. Stories 1–4 remain refinement input; Story 2
needs particular attention to identity ambiguity before execution.

For each eventual executable plan, require a short final-design account: which domain concept owns the rule, which old handlers/tests/docs disappear, which invariants justify remaining refusals, and which public proof demonstrates composition. An example is evidence of a rule, not the name or dispatch key of a production algorithm. A cardinality limit requires a real product, identity or resource reason. Review the aggregate final diff and implicated unchanged code, not just each slice in isolation.

Apply Accepted ADRs on portable representation, failure handling and environment isolation (0004, 0006, 0007). Keep fail-loud refusals for unsupported or ambiguous semantics; remove only restrictions shown to arise from example-shaped delivery contracts. Follow normal repository refactoring, formatting and test requirements when execution is separately requested. This audit changes no Open Dough rules or skills; upstream process work remains separate.

## Why this pattern survived

Historical plans often explicitly made unsupported neighboring examples part of the contract. Execution could therefore satisfy the plan while creating an inconsistent combined capability. Later stories added another allowed case; shared low-level helpers preserved the higher-level scenario gate. Behavior-preserving cleanup could not remove a rejection that planning and tests had made a required behavior. F1/F2 show that directly. F3/F4 are narrower cohesion misses and do not establish the same intent or severity.

This evidence supports fixing both the affected mechanisms and how future plans distinguish an example from an invariant. It does not support discarding all five days of work, blaming every small story, or claiming every specialized adapter is a defect.
