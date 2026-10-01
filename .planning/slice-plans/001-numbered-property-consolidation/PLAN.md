# Consolidate numbered properties while retaining their learning

**Identity:** SEED-063#story-2
**Source:** [Existing numbered property keys become one list, and their trackers follow](../../seeds/SEED-063-track-property-values-separately.md#story-2)

## Goal and authority

Authors see one meaningful property containing their associations. Learners
retain the identity, history and next recall of each distinct tracked
association. The owner permits dropping redundant trackers at a duplicate
destination; the retained tracker keeps its own history without merging.

The owner invoked dough-execute-plan for this story on 2026-10-01. Execution
is authorized in Story Branch Mode; application release remains separately
authorized. The published claim is Taken.

## Execution context

Read [EXECUTION.md](EXECUTION.md) for retained execution identity, preparation,
publication and observer state.

## Required execution basis

Read [BASIS.md](BASIS.md) for scope, current decisions, architectural
constraints, decisive premises, baseline proof and review learnings before
executing these slices. Those requirements remain part of this plan.

## Ordered slices and outside-in proof

Each slice targets about 5 minutes including local edits and focused cleanup.
Slices 4, 8 and 10 are scrutinized cohesive outcomes targeting 5–8 active minutes:
their content/history or transaction promises must be proved together. The
mandatory complete backend suite and isolated process boot are explicit
verification-wait exceptions when they exceed that target; do not use them to
hide more implementation. At >10 active minutes, stop and finer-decompose the
remaining work with recorded learning before continuing.

### 1. Startup services are usable after schema migration
Type: Behavior
Status: done
Accepted proof: see [slice 1](EXECUTION.md#slice-1-accepted-proof).
Proof: An isolated bootstrap probe delivers the actual startup event; observes
Flyway completion before a consumer invokes the real accepted-change service
and reads the migrated tracker schema. Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.

Behavior: Disposable schema and a representative bound notebook → real event
delivery → injected services can open and close a no-change notebook operation
after schema migration, without an outer Flyway transaction or extra Git commit.
Read current column limits, collation and unique keys in the same test engine.
Keep the probe in the test harness; introduce no live transformation caller.
If ordering, injection, schema or independent transactions differ from the
observed contracts, stop dependent slices and revise this plan. This is early
evidence, not a substitute for the later migration journey.

### 2. One exact-family transformation owns consolidation
Type: Structure
Status: done
Accepted proof: see [slice 2](EXECUTION.md#slice-2-accepted-proof).
Proof: Pure contract tests on the existing authored-frontmatter boundary prove
numeric grouping/list/ordering and preserve unrelated bytes. Include quoted
leading/trailing whitespace keys and numeric suffixes beyond Integer range;
case and whitespace-distinct authored bases stay distinct. Run the full backend
suite. Slice 3 immediately consumes its transformed content/focus mapping.

Change: Reuse the parked prototype (see EXECUTION.md), correcting exact authored
suffix recognition in PropertyKeyNaming without changing trimmed structural
recognition. Numeric suffix size is not a new eligibility restriction. Keep one
suffix domain owner. Preserve source ranges through NoteLeadingFrontmatter,
whose current verbatim split normalizes BOM/line endings. Consolidation retains
shared codec scalar meanings, returns source-to-final focuses and diagnostic-only
outcomes for genuinely unsupported or ambiguous shapes. No preview API or
migration framework. Preserve scalar-only structural and word-suffix keys.

### 3. Unmappable notebook operations leave all state unchanged
Type: Behavior
Status: planned
Proof: Invoke the notebook migration on an orphan item focus, an empty source
list with a persisted scalar focus, a tracked empty destination and an oversized
tracked destination. Observe a diagnostic and unchanged content/accepted history
for every case. Run the full backend suite with real committed readers.

Behavior: Any persisted focus cannot follow its source → preflight the complete
notebook operation before mutation → diagnostic, no partial content or learning
change. Reuse the parked operation and diagnostic proof. Keep only this leaf's
proof active; the canonical learned journey remains parked for slice 4. The
same operation/mapping owner immediately enables slice 4, without a live caller.
The owner-required cleanup is queued as SEED-063#story-3; removal awaits
confirmed deployed migration completion.

### 4. A learned scalar family becomes one accepted list
Type: Behavior
Status: planned
Continuation: restore the parked canonical test and read its initial tracker
state from committed MySQL before migration; do not compare unpersisted FSRS
floats against rounded database values.
Proof: Migration notebook operation → `NoteController`/tracker history reads
and downloaded bundle → list content, retained note/tracker IDs, histories and
next recall, refreshed property/reference indexes, one descendant commit and
no private learning data in Portable content. Run the full backend suite.

Behavior: Learned `example of` and `example of 2` with distinct scalar values →
migrate → both original trackers focus on their values under the base. Apply
content and learning changes through the existing accepted-change transaction.
Keep the startup caller absent until slice 13.

### 5. Sparse and already-listed families use the same rule
Type: Behavior
Status: planned
Proof: Invoke the migration on missing-base/out-of-order suffixes and an
existing base list; observe the selected order and tracker destinations through
the same boundary. Include an untracked `url 2` and retained structural/word
keys in the canonical content contract; run the full backend suite.

Behavior: Absent base or existing list with repeated values → migrate → one
ordered list with stable deduplication; existing list-item trackers remain at
their item, and scalar trackers move only when their original value is known.
Do not treat a tracked list as a scalar or clone its tracker onto all items.

### 6. Every learner's persisted tracker follows
Type: Behavior
Status: planned
Proof: Two learners with distinct tracking states → one migration operation →
both query their original tracker IDs at the new focuses; an unrelated tracker
retains its focus. Run the full backend suite.

Behavior: Other learners and inactive persisted trackers of a migrated note →
migrate → all mapped trackers follow; active-only UI filtering is not reused.
Do not repeatedly assert canonical history fields already owned by slice 4.

### 7. Restoring Trash retains the migrated association
Type: Behavior
Status: planned
Proof: Trash a learned note through `NoteController`, migrate, download its
`_trash` content, undo Trash through the controller → restored list and retained
tracker/history. Use the existing real Trash fixture pattern and full suite.

Behavior: A numbered-family note is already in Trash → migration then restore
→ the legacy convention does not return. Enumerate complete stored content,
not only available notes; do not recreate Trash ancestry rules.

### 8. Duplicate destinations retain one tracker safely
Type: Behavior
Status: planned
Proof: Duplicate mapping → migration → the deterministic survivor keeps its
history/schedule; only redundant trackers and their normal dependent data are
removed. Seed every tracker FK child and the prompt/conversation edge. Include
inactive and collation-equivalent destinations in the real MySQL fixture.
Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.

Behavior: Final destinations collide by the schema's rule → retain the existing
destination tracker, or otherwise the lowest-ID candidate → drop redundant
trackers before updating the survivors. Do not drop different learners/types,
merge histories, or relax uniqueness. Recheck the current FK closure against
`information_schema` before implementing hard deletion. Failure leaves the
complete accepted operation unchanged, including the deletion fan-out.

### 9. In-notebook selectors still resolve after consolidation
Type: Behavior
Status: planned
Proof: A uniquely resolving `#prop:` selector in body/frontmatter → migration →
controller wiki resolution selects the base, visible text is retained, derived
source rows match authored content and downloaded Markdown has the new selector.
For a tracked list item containing the selector, observe retained tracker ID
at the rewritten value. Run the full backend suite.

Behavior: A note refers to a removed suffixed key → migrate the target family
and retarget only resolved selectors in the complete operation. Preserve the
existing resolver's source-scope and ambiguity rules. Use shared reference
rewrites and tracker mapping; add no alternate link syntax or saved destination
authority.

### 10. Cross-notebook referrers publish in the same operation
Type: Behavior
Status: planned
Proof: Target and referrer in separate bound notebooks → migration → both
downloaded descendant trees agree with controller reads and retained learning.
The changed notebook set is computed before locking and reverified under lock.
Run the full backend suite.

Behavior: A resolved selector is authored in another stored notebook → complete
migration operation → one descendant per changed notebook in the same
transaction. Include affected source-value trackers, using the same duplicate
rule. This is system migration authority, not a user web action's ownership
filter. Do not publish a target change before its required reference rewrites.

### 11. Late publication failure rolls back the whole migration
Type: Behavior
Status: planned
Proof: Reuse the existing failing-binding-save/committed-reader pattern at the
migration boundary. After late failure, inspect original notes, tracker
identities/focuses and all deletion fan-out, derived references, accepted heads
and native object rows from a fresh committed reader and reopened bundle.
Run the full backend suite.

Behavior: Failure after content projection and redundant deletion but before
final accepted binding save → all affected notebooks and private learning
remain at their pre-operation state. The injected failure verifies the
business atomicity promise; it adds no generic failure-recovery framework.

### 12. A resumed run changes only remaining legacy content
Type: Behavior
Status: planned
Proof: Run migration twice → stable content, tracker IDs/history and accepted
heads on the second run. Interrupt a two-notebook run after the first committed
operation → invoke again → first remains unchanged, second finishes. Run the
full backend suite with separately committed readers.

Behavior: Already-consolidated content or a partially completed run → retry →
no new duplicate commit/deletion/history reset. Use separately proxied
transactions and derive eligibility from current authored content. A late
infrastructure failure may surface loudly; the next run resumes safely.

### 13. Startup invokes the proved complete migration
Type: Behavior
Status: planned
Proof: Extend slice 1's real event probe to seed a learned numbered family and
consume the actual production-configured runner. Startup after Flyway →
controller/database/downloaded-tree observations satisfy slice 4. Deliver
startup again and concurrently invoke the runner from a separate transaction
→ the unchanged head and survivor prove no double application. Run
`CURSOR_DEV=true nix develop -c pnpm backend:verify`.

Behavior: Application starts with legacy content → after schema migration,
iterate notebooks through the proved operation, with per-operation locks and
fresh reads → eligible content is consolidated. Keep startup synchronous under
the existing failure lifecycle; add no fire-and-forget task. Report remaining
unmappable operations rather than claiming full completion. Document the
startup entry, deterministic duplicate exception, retry and completion check
in the maintained migration/operations guidance. This slice activates the
caller only after preceding behavior is safe.
