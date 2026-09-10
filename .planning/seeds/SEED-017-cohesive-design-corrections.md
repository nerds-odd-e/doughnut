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
The remaining stories start with deletion composition (2a) and move
composition (2b), then the existing infrastructure corrections (3–4).
The product backlog owns priority.

## Open product decision

Should every web mutation of Portable content advance accepted Git history, or
should the product deliberately expose a partial synchronization boundary?
Normal web authoring participation needs separate user outcomes; do not silently
rebuild accepted history from live state. This question does not block the
queued corrections.

<a id="story-2a"></a>

### Story 2a: Publish note deletions alongside compatible note changes

- **Goal:** notebook owners can remove obsolete notes in one coherent commit,
  including alongside content edits, without splitting
  each deletion into its own publication.
- **Scope:** compose ordinary-note deletions with existing ordinary-note
  modifications at represented destinations, including deletion-only batches.
  Deletion-plus-addition combinations with uncertain identity are deferred for
  this increment as described below. Preserve existing deletion semantics, retained
  note identities and learning data, and atomic publication of the complete
  candidate tree and accepted head. An invalid member rejects the whole
  publication without changing accepted content or learning data.
- **Identity boundary:** same-path edits retain identity. Existing supported
  identity-preserving moves must continue to work; a removed/added equal-blob
  correspondence must not be reclassified as independent deletion/creation just
  because companion changes exist. Multiple possible correspondences must not
  silently lose identity. Considering all changed notes is necessary even when
  this increment does not promise to publish composed moves.
- **Deferred promises:** new folders, broader folder operations, composed
  identity-preserving moves (Story 2b), and changed-content move inference. These
  are delivery boundaries, not reasons to introduce new blanket refusals or
  regress already supported behavior.
- **Key examples:**
  - Accepted A, B and C → publish a commit deleting A and B and editing C →
    both deletions and C's edit are accepted together; C retains its identity
    and learning data. Multiple deletions without a companion edit work too.
  - A deletion batch contains an otherwise invalid companion change → publish
    → the entire publication is rejected and the accepted state stays unchanged.
  - A removed note and added note have possible identity correspondence, with
    unrelated edits also present → publication must preserve proven identity
    through supported move behavior or refuse with an actionable reason; it
    must not fall back to deleting and recreating that note. Ambiguous multiple
    exact matches refuse atomically.
  These examples establish composition and identity safety, not a fixed number
  of operations or an exhaustive catalogue of eligible commit shapes.
- **Evaluation:** owners observe the complete accepted tree through publication
  and a receiving clone. Proof must establish atomic acceptance/rejection and
  preserved retained-note identities and learning data through public behavior.
  Reuse existing proofs; replace obsolete isolated-deletion expectations while
  retaining independently justified validation and identity safeguards.
- **Planning and execution handoff — explicit user requirement (2026-09-11):**
  this is an incremental step toward the complete feature. Generalize the
  existing publication solution into one cohesive treatment of compatible note
  operations; do not add a parallel deletion-batch pipeline, duplicate identity
  or application rules, or dispatch by the exact example's counts/sequence.
  Examples are evidence for domain rules. Slice planning must carry this
  requirement explicitly, identify the existing concept that owns each rule
  and the obsolete branches/tests/guidance to replace, and evaluate the
  cumulative solution, including implicated unchanged code. Execution and its
  refactoring/review must verify that the result integrates with existing
  additions, edits, deletions and move handling. This does not authorize
  implementing every deferred capability or prescribing a new framework.
- **Safe stopping point:** deletion composition remains useful without move
  composition. No functional dependency on the folder story is asserted.
- **Planning boundary — mixed additions/deletions:** Git trees alone cannot reliably
  distinguish independent delete/create from a move with rewritten content.
  [Proposed ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md#infer-private-identity-from-commit-to-commit-changes)
  calls for conservative refusal; it is proposed, not an Accepted constraint.
  Following the recommendation and the user's instruction to proceed with
  planning, use the conservative boundary: defer identity-uncertain
  deletion-plus-addition combinations for this increment, narrowing the earlier
  addition promise while delivering deletion batches with edits. This is the
  working scope for this plan, not approval of ADR 0002. Do not
  silently decide that unequal blobs prove independent new identity, invent a
  similarity threshold, or leave "compatible additions" undefined in a PLAN.
- **Refinement status:** refined for the conservative planning scope above.
  Executable plan: [Publish compatible note deletions](../quick/099-publish-compatible-note-deletions/PLAN.md).

<a id="story-2b"></a>

### Story 2b: Publish unambiguous note moves alongside compatible note changes

- **Goal:** notebook owners can rename or relocate a note while making other
  compatible note changes in the same commit, retaining its learning history.
- **Scope:** unambiguous unchanged-content note moves/renames among compatible
  ordinary-note operations at represented destinations. Determine correspondence
  from the complete candidate set, independent of total diff entry count.
  Changed-content moves, folder-subtree expansion and new-folder destinations
  remain deferred promises.
- **Key example:** publish A's equal-content rename to D alongside an unrelated
  edit to B. D retains A's identity and learning data, B retains its identity,
  and both changes become accepted atomically. Ambiguous equal-blob candidates
  must not silently lose identity through deletion/creation.
- **Evaluation:** publication and a receiving clone expose the complete result;
  ambiguous correspondence or an invalid destination leaves state unchanged.
- **Safe stopping point:** owners can compose proven note moves without broader
  folder-move or changed-content identity inference.
- **Refinement status:** retained from the former combined Story 2; identity
  ambiguity and composition boundaries need refinement before slice planning.
  Ordered after 2a to establish ordinary-note composition first.

<a id="story-3"></a>

### Story 3: Declare isolated test capabilities in one place

- **Kind:** bounded architectural correction preserving the existing isolated
  test capability. This is not a new test-admission feature. Keep this section
  as its canonical refinement input for later slice planning.
- **Goal:** maintainers can understand and change an approved spec's resource
  requirements in one place; developers retain safe, independent test runs.
  Incremental workflow delivery should accumulate one coherent resource model,
  rather than require lifecycle branches for each delivered story.
- **Evidence:** the current `scripts/isolated-openai-mock.mjs` exports the
  completion feature filename; `isolated-cypress-spec-selection.mjs` imports it
  to build the allowlist; `isolated-cypress.mjs` compares the selected filename
  against it to start a private mock. This reverses the dependency between
  resource lifecycle and spec policy. The audit's F3 records its incremental
  provenance. Unclear instructions are a plausible contributing explanation,
  not an established sole cause. Specific feature names in an admission
  registry or test fixture are appropriate; the resource mechanism owning
  those names is the evidenced defect.
- **Scope:** consolidate approval and spec-to-resource requirements into one
  small authoritative registry. Preserve the same four approved workflows:
  note editing, web-created-note CLI, MCP search/graph, and OpenAI completion.
  All continue to use the owning SUT/allocation and runner lease. Only completion
  currently requires the private OpenAI mock. Selection resolves the approved
  spec and its requirements; orchestration uses those requirements to acquire
  existing resources. Resource lifecycle modules know ownership/configuration,
  not feature paths. Derived lists and diagnostics must not become a second
  independently maintained admission or capability table.
- **Architecture boundary:** spec policy owns which resources an approved
  workflow needs; selection owns normalization and admission; orchestration
  owns acquisition, endpoint exposure and cleanup; existing resource modules
  own resource lifecycle and ownership checks. These are responsibilities, not
  prescribed new classes/files. Setup-time selection and `before:run` selection
  must consult the same policy, retaining current normalization and discovery
  behavior and making mock endpoints available when Cypress needs them.
  Reuse the existing runner and lifecycle machinery rather than cloning it.
- **Preserved constraints:** one approved spec per isolated run; unknown or
  multiple selected specs refuse before test reset/execution. Unknown specs
  must not default to a no-mock capability. Preserve owning-SUT health checks,
  runner exclusivity, private endpoints, foreign-resource refusal and isolation
  from shared defaults. Failures remain observable by the runner; cleanup
  releases acquired owned resources and the lease on completion, failure and
  cancellation. Retain composed Cypress completion handlers and current timing
  of setup-time endpoint exposure.
- **Key examples:**
  - An owner selects an approved no-mock workflow → starts its isolated run →
    it uses the owning SUT and lease without starting a private OpenAI mock.
  - An owner selects completion → starts its isolated run → its declared
    requirement supplies the owned private mock and endpoint; completion,
    failure or cancellation releases that run's resources while a peer remains
    unaffected. Existing failure observation remains effective after startup.
  - An unknown spec or several specs are selected → admission is evaluated →
    the run refuses before test reset/execution, without shared-service fallback.
  - A maintainer inspects an approved spec's requirements → finds one policy
    entry → no resource module or orchestration filename comparison must be
    kept in sync with it. Future new admission still needs its own safety proof.
- **Evaluation:** use existing runner/Cypress boundary tests for selection,
  resource acquisition, endpoint exposure, ownership refusal and cleanup;
  reuse existing completion, mock-failure and cancellation proofs, including
  the 073/093 lifecycle evidence, adding only missing observable coverage.
  One representative live isolated run must confirm owned-resource release
  and lease reuse. Structural review establishes that production feature-path
  policy has one owner and lifecycle modules no longer depend on it; green
  workflow tests alone do not prove this architectural correction. Do not
  create tests per new internal helper or rerun unrelated E2E scenarios.
- **Planning and execution handoff — explicit user intent (2026-09-11):**
  generalize the existing solution into a cohesive, consistent system, not a
  mirror of story decomposition. Plan the shared policy/dependency correction,
  not separate implementations for note editing, CLI, MCP and completion.
  Identify obsolete filename exports/comparisons and their affected consumers
  to migrate, including harnesses and fixtures; remove superseded production
  rules rather than retaining competing compatibility paths. Review the
  cumulative final design and implicated unchanged code for duplication.
  Structure slices may directly own this evidenced correction with preserved
  public behavior; do not invent new user behavior to justify refactoring.
- **Deferred/excluded:** new spec admissions, multiple-spec runs, new mock
  providers, a generic plugin/resource framework, changed process termination
  mechanics (Story 4), environment allocation redesign, and notebook publication.
  Do not add capabilities merely to fill a speculative future abstraction.
- **Accepted ADRs:** [0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md)
  preserves environment and ownership boundaries;
  [0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md)
  preserves visible failures and purposeful handling. No conflicting decision
  or new ADR approval is needed for this bounded dependency correction.
- **Safe stopping point:** the same four workflows run with one policy owner
  and unchanged safety/lifecycle guarantees even if no further specs are added.
- **Effort hypothesis:** M, medium confidence; setup-time versus `before:run`
  resource timing is the main integration consideration for slice planning.
- **Dependencies and parallelism:** no functional dependency on Story 2a/2b.
  Can execute alongside notebook publication in a separate owning worktree
  after planning. Coordinate shared seed/backlog edits and do not overlap
  mutable test resources. Avoid concurrent edits to the lifecycle mechanism
  selected for Story 4.
- **Refinement status:** refined; no unresolved product-scope decisions.
  Executable plan: [Declare isolated test capabilities in one place](../quick/100-declare-isolated-test-capabilities/PLAN.md).

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

Story 2a has refined behavior and an executable plan using the conservative
mixed-addition boundary. Story 3 has an executable plan for its bounded
architectural correction. Stories 2b and 4 remain refinement input.
The product backlog records the selected order.

For each eventual executable plan, require a short final-design account: which domain concept owns the rule, which old handlers/tests/docs disappear, which invariants justify remaining refusals, and which public proof demonstrates composition. An example is evidence of a rule, not the name or dispatch key of a production algorithm. A cardinality limit requires a real product, identity or resource reason. Review the aggregate final diff and implicated unchanged code, not just each slice in isolation.

Apply Accepted ADRs on portable representation, failure handling and environment isolation (0004, 0006, 0007). Keep fail-loud refusals for unsupported or ambiguous semantics; remove only restrictions shown to arise from example-shaped delivery contracts. Follow normal repository refactoring, formatting and test requirements when execution is separately requested. This audit changes no Open Dough rules or skills; upstream process work remains separate.

## Why this pattern survived

Historical plans often explicitly made unsupported neighboring examples part of the contract. Execution could therefore satisfy the plan while creating an inconsistent combined capability. Later stories added another allowed case; shared low-level helpers preserved the higher-level scenario gate. Behavior-preserving cleanup could not remove a rejection that planning and tests had made a required behavior. F1/F2 show that directly. F3/F4 are narrower cohesion misses and do not establish the same intent or severity.

This evidence supports fixing both the affected mechanisms and how future plans distinguish an example from an invariant. It does not support discarding all five days of work, blaming every small story, or claiming every specialized adapter is a defect.
