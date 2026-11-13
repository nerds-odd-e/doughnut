# Final question-ownership schema upgrade

## Readiness and boundary decision

On 2026-11-13, the preflight at `34cf85c...` exposed work omitted from old
13's 5–8 minute editing estimate: populated engine proof, a deliberate startup
override and its post-application retirement. The smallest authorized result
remains prompt-owned question retention, cleanup of questions no prompt uses,
then physical retirement and removal of spent tooling. Consolidating these
three proof/recovery boundaries would not credibly fit ten editing minutes.
The first refinement split old 13 into three Behaviors. Actual full-suite
proof then disproved its shared-catalog premise; insert Structure 13 and shift
those Behaviors to 14–16, preserving completed 1–12. There are 16 slices.
Story resplit is recommended under the refinement skill; an optional developer
choice is pending while the already authorized isolation setup continues.

Carry forward the accepted V300000338 pre-DDL pattern, existing Flyway
repair-then-migrate lifecycle, tracker ownership and ADR/North Star decisions.
Add no archive, parallel owner, cascade or new shared framework. The complete
FK reading is mcq → recall_prompt CASCADE → conversation SET NULL, where the
delete walk stops. Answers/logs alone can survive lost prompts, so counts of
those rows alone are inadequate proof. Keep conversation/message/correction
fixtures too, observing the same asked graph and payload.

## Isolate the populated migration proof from shared test fixtures

Failed attempt: native 58778 found a package/bootstrap error, corrected by
moving the temporary test/helper to the established controllers package.
Native 34545 then ran 643 classes/3,200 tests: exactly two setup failures,
zero errors, two skips. Other committed fixtures left promptless IDs 6,7,8
at this class boundary. The prior post-suite zero-count observation did not
prove a clean catalog during the suite. Both cases stopped before creating
our graph or executing DML. All other cases passed. Active inspection/editing
and closure was conservatively 6–8 minutes; full waits were separate. This
learning remains attributable across the split, without resetting its cost.

The six attempt-owned files are hash-verified and parked in
`/tmp/donut-question-cleanup-parked` with a manifest; only their two profile
edits were reverted and four owned untracked files removed. Root planning
records and all shared rows remain. Failure archives:
`/tmp/donut-question-cleanup-bootstrap-failure` and
`/tmp/donut-question-cleanup-shared-catalog-failure`.

PFE reuses local worktree root administration, existing MakeMe/EntityPersister
REQUIRED persistence and committed transactions, plus the existing CI MySQL
wrapper and pinned action's root-password input. Entities/configuration have
no catalog qualifiers; retained XML shows unqualified ORM SQL. No second
Docker server, generic graph copier or new Spring context is needed.

Create one owned unique catalog, migrate canonical history through 357, and
grant only the actual named account returned by CURRENT_USER access to it.
Inside a committed transaction, clear the EntityManager, switch its retained
JDBC connection catalog, build/flush the MakeMe graph, verify actual target
rows explicitly before commit, then clear/restore the original catalog in
finally. A wrong ORM route must fail and roll back before committing. Use
independent target JDBC/Flyway connections afterward. Teardown revokes only
the unique-catalog grant and drops only that catalog; preserve normal history,
original account grants and every shared row. All actual target data is owned.

CI's current pinned action chooses root OR named-user setup, not both. Add an
optional known-root path to the existing wrapper, backend-job-only, using the
existing password secret without output. That path must also create/grant the
existing named account on its original test DB before tests; other callers
retain their default behavior. Pass the known admin credential only to the
backend test step. Do not retrieve random root passwords from logs.

Temporary Structure proof drives canonical pre-C Flyway replay with a real
owned MakeMe question/prompt graph and target reads, preserving shared data.
Full retained backend behavior must stay green. Local proof then publishes a
progress increment; actual branch CI must prove this resource path before
ordinary Structure completion and Behavior 14. This is the permitted early
probe of CI credentials/routing/teardown, not assumed platform success.
Editing budget 5–8 minutes, stop/refine above ten; engine/full-suite/CI waits
are separate. The immediate next Behavior owns destructive cleanup proof.

## Require deliberate cleanup readiness while preserving asked evidence

With B observed, run new canonical migrations above the checked actual tip:
first a Java readiness migration, then separate gated SQL cleanup. A positive
promptless count with default-off gate must throw before cleanup records a
successful no-op. After deliberate enablement, the same pending upgrade can
repair/retry and clean only MCQs unreferenced by ANY recall_prompt. Retain
default-off `1=0` placeholder configuration explicitly in every profile.

Reuse Structure 13's owned catalog and MakeMe routing for the complete graph.
Restore the compatible parked guard/DML/profile logic and adapt the two
pending cases to this real engine; no shared-catalog deletion is allowed.
Run canonical migrations/configuration, never mocked Context or engine.
Every target candidate is fixture-owned; require that invariant before DML.
Keep exact question/answer identities and preserve the original catalog.
The 6–8 minute failed setup attempt remains recorded above; this remaining
integration reuses its compatible algorithm and fixture intent, not its false
shared-catalog premise.

Prove default-off DML retains its populated inputs, pre-cleanup refusal with
remaining work, successful gate-on retry, exact intended selection and the
complete asked graph. Include unanswered references and same-tracker shared
questions, plus answered/excluded/logged question and conversation evidence.
Keep independent expected identities/payload outside builders. Teardown owns
only the isolated catalog and its temporary grant, using finally; no FK disabling.
DDL auto-commits must never occur inside an assumed rollback-only transaction.

Editing/cleanup budget 8–10 minutes, scrutinize above five and stop/refine above
ten. Full real-engine/backend waits are separate. Use backend:verify and
explicit worktree-schema ERD regeneration under the existing rules; retained
move/history/MCP/export/deletion tests must pass. The safe stop is a green
story-branch publication with defaults off. Do not release this intermediate
increment on populated production; it intentionally requires later C enablement.

## Preserve learner evidence through guarded physical schema retirement

After 14, add a higher Java migration that checks cleanup readiness before
ALTER, then drops the exclusive index and physical note_id column. It must
not depend on the temporary gate. Extend the same pending real-engine harness
through the complete ordered chain and a cleanup-incomplete refusal. A
temporary physical-state reading may establish refusal before ALTER; retain
no permanent absence assertion. Preserve exact asked identities/payload and
ordinary product regressions on the final schema.

The temporary harness owns the separate catalog's pre-C shape and complete
upgrade. Its resource teardown leaves the original catalog/history untouched. Keep serial command exclusivity; one backend invocation per checkout.
Regenerate the ERD from the explicitly assigned migrated catalog, never by hand.

The publisher pins the canonical startup script from the selected release.
Use an explicit temporary JVM Flyway-placeholder override there for the
reviewed C release; profile defaults remain off. Do not assume an external
startup-script override survives publication. Before human review, record a
fresh production selection count, full FK closure/readiness, complete local
proof and concrete retained landing/tag/SHA including enablement. That exact
go-ahead authorizes C, not B's earlier permission. Observe actual publication,
all active binaries, applied migrations and public/manual smokes. Health alone
is insufficient. Keep the pending harness/configuration until actual C.

Editing/cleanup budget 5–8 minutes by extending the accepted harness. Engine,
CI and human/release waits remain separate exceptions. Stop safely after
branch publication while C awaits its human gate. Applicable regression proof
must cover the actual candidate before any authorized live action.

## Keep normal startup and fresh upgrades after cleanup

After confirmed C, selectively delete the spent pre-cleanup Java guard and
gated SQL migration, both below the retained guarded DDL version. Delete their
exclusive harness/support and profile placeholder; restore the normal canonical
startup invocation. Do not edit committed migration bodies or rewrite the
baseline. Existing repair-before-migrate handles deleted applied versions;
their version numbers remain reserved. The retained DDL still transforms a
fresh historical baseline, with no missing placeholder dependency.

Prove fresh/current-schema replay and retained move/history/MCP/export/deletion
behavior through full backend:verify and the assigned catalog. Perform the
one-time whole-product removal sweep separately from proof, classifying
immutable historical migrations and dated evidence as the existing policy
requires. Preserve .planning/done provenance; add no absence checker, guard
or replacement product history note.

Editing/cleanup budget 4–6 minutes; verification/publication waits separate.
A repository edit alone does not remove the running production override.
Prepare the concrete neutral-startup landing/tag/SHA for human review under
the same release authority policy, then observe its actual publication,
all-active neutral startup, schema and smokes. Final acceptance/retrospective
and Story Wrap Up wait for this lifecycle outcome. Preserve the worktree,
branch, Taken assignment and plan through every retained checkpoint.

Retained preflight/proposals and the corrected second-catalog assumption:
`/tmp/donut-final-schema-preflight.md`,
`/tmp/donut-final-schema-plan-proposal.md`,
`/tmp/donut-final-schema-harness-readiness.md`.

[Actual read-only production baseline and selection count for C review](checkpoint-c-preparation.md).
