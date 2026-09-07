# Recover and coordinate application releases

Source: [SEED-013 Story 2](../../seeds/SEED-013-version-tag-production-releases.md#story-2).
Status: planned; Story 1 merged to main at a6f2745b8e.
Predecessor: implemented tag release path; see scripts/ci/application-release*.mjs
and docs/gcp/conditional-backend-deploy.md.
Readiness: updated from Story 1 implementation and merge observations; ready for
a fresh execute-plan worktree. Defer only post-merge platform observations to parent.

Active execution: branch `codex/release-recovery-ordering`; checkout
`/Users/terryyin/.codex/worktrees/8079/doughnut`; CI observer coordinator
`story2-8079`, yielded cell `16`, PTY session `70450`, PID `46090`, mailbox
`/tmp/donut-ci-501/watch-BMLO0z`.

## Goal and scope

Remove the single-release manual coordination rule after tag releases work.
Support immutable retries, artifact regeneration, successful duplicate no-ops,
newest pending version selection and protection against an older release replacing
a newer one. Keep one active application deployment and let it finish. Preserve
exact-SHA successful CI, complete artifact admission, application payload and
independent CLI behavior from Story 1.

This story owns the durable application outcome record and event-driven waiting.
It does not add staging, automatic CI retries/version bumps, automatic schema
rollback, cross-stream CLI ordering or a general release scheduler. Forward
correction still uses a tested revert and new patch tag.

## Execution decisions

- Reuse Story 1's real release command, CI/artifact admission, source checkout,
  publisher and focused tests. Keep the tag-only bounded wait operational until
  final cutover; do not suspend the working release feature during this story.
- Use a separate application-state object in the existing private GCS deploy
  bucket, holding admitted tag, raw Git refOid, peeled SHA, CI run/attempt and
  publishing/succeeded outcome. Backend last-successful-deploy.json only describes backend hashes and
  must remain separate. Persist identity before production writes, success only
  after all publication operations finish, including when MIG is skipped.
- Extend the existing publisher with this outcome ownership; do not create a
  reusable state framework. Workflow concurrency is the single-writer owner.
  Object-not-found means initial state; transport/permission/parse errors do not.
- Bootstrap existing published application identity under that same concurrency
  owner before global reconciliation is enabled. Use a verified successful
  publication job/step from Story 1's app workflow, with tag and SHA; a green CI
  run or successful no-publication workflow is not proof of deployment. Latest
  CLI-only publication is not application state. If the required evidence is
  unavailable, fail visibly and require the operator to identify the published
  release; never silently treat an existing release as undeployed. A verified
  absence of prior tag-driven application publication permits initial empty
  tracking; legacy automatic main deployments have no application tag to replay.
  Recheck bounded workflow history at activation, not just the current tag list.
  On 2026-09-07 the deploy.yml push-event API returned total_count 0 and remote
  v* tag listing was empty. This observation is not an enduring empty-state
  assumption: Story 1 can publish before this story activates.
- A publishing/incomplete record permits the same tag/SHA to retry after CI and
  artifacts are revalidated. A fresh successful run for that SHA may replace the
  recorded CI identity. Successful duplicate identity is checked before artifact
  download and produces no writes, so it cannot overwrite an independent CLI.
- Verify raw tag object and peeled commit against the frozen/persisted identity;
  forced update or tag deletion must not create a new release request. Older versions cannot
  replace higher admitted/deployed state. Normal corrections use higher versions.
- Add tag and ci.yml main workflow_run completion as wakeups for one reconciliation
  command. Pending/failed CI reports waiting/blocked and returns without sleeping;
  a later completion re-evaluates current state. Remove Story 1's bounded loop at
  cutover. Do not add automatic CI reruns or another background service.
- Inside workflow-level deploy-production concurrency, cancel-in-progress false,
  select the highest numeric qualifying main tag before evaluating its readiness.
  Any surviving wakeup reconciles current pending tags, regardless of its own
  event's age/SHA/conclusion. Thus an old arriving event cannot erase the only
  useful pending wakeup. A higher pending version with unfinished CI supersedes
  lower pending versions. Freeze the active admitted release through completion.
- Keep source/control checkout separation and exact artifact identity. Preserve
  existing diagnostics but add already-released, identity-mismatch, superseded
  and artifact-recovery outcomes. Keep long operational instructions in the
  existing GCP runbook. No repository ruleset or production IAM changes assumed.
- Preserve [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) routing
  ownership and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  error policy. There are no database changes or new storage-engine experiments.

## Implementation findings that constrain these leaves

- `scripts/ci/application-release-ci.mjs` already exports `querySelectedCi` and
  supports `--once`: reuse exact repository/workflow/main-push/SHA selection,
  pagination, latest attempt and error context. Only `waitForSelectedCi` and its
  timer/cancellation tests are temporary. At cutover replace their callers/tests
  with immediate-return event outcomes; keep all exact-CI admission coverage.
- `scripts/ci/application-release.mjs` currently owns event identity and
  `--verify-ref`; it is a command, not an import-safe helper. Extract only a needed
  shared identity boundary when reconciliation calls it. Raw `refOid` detects an
  annotated-tag replacement even when its peeled SHA is unchanged. Per-invocation
  movement checks already work; leaf 4 adds durable cross-attempt identity.
- `infra/gcp/scripts/publish-application.sh` preflights all three artifacts,
  validates the selected checkout, prepares its URL map and verifies the remote
  ref immediately before writes. Keep tracking around that proven boundary;
  invalid payload/routing/source must not become a partially published release.
  Keep current orchestration, selected routing/startup/force-token inputs and
  workflow's explicit `GITHUB_SHA="$RELEASE_SHA"` shell assignment.
- Three existing download-artifact actions own artifact transport. Check successful
  duplicates before those actions; artifact recovery improves diagnostics and
  replay proof, not archive downloading. Reading state earlier requires existing
  GCP authentication in admission, under the same workflow concurrency owner.
- The backend hash record cannot represent a frontend-only application release.
  No durable application ledger exists. Bootstrap uses successful publication-step
  evidence (selected identity), not green CI, whole-workflow success or CLI state.
- The first tag-only source must contain that workflow trigger. The default-branch
  CI-completion adapter will allow reconciliation of older main sources with
  available successful exact-SHA CI; neither CI history nor artifacts are promised
  recoverable beyond GitHub retention.

## Proof ownership

Extend Story 1's public command suite with the smallest fake external object store
and controlled event/run fixtures. Use real Git for tag movement and real publisher
subprocesses; fake GCS/GitHub/MIG calls only. Do not build a fake Actions scheduler.

| Promise | Leaves | Observation |
|---|---|---|
| Complete application outcome, including frontend-only, without changing backend record | 1 | Ordered record/upload trace and failure residue |
| Existing publication is recognized on upgrade | 2a–2b | Bounded history is classified from publication-step evidence, then seeds state with zero uploads |
| Successful duplicate does not replace application or CLI | 3 | Same identity replay produces no writes, even with expired artifacts |
| Moved identity rejects before writes | 4 | Actual changed tag/current record fixture |
| Interrupted attempt retries without changing tag/SHA | 5 | Failure then same-identity completion with current validation |
| Regenerated exact-SHA artifacts resume failed release | 6 | Missing-artifact result → new successful CI run → resumed same version |
| Highest numeric pending version, independent of event delivery | 7 | Reversed v1.3.9/v1.3.10 wakeups and pending newer CI |
| Older retry/late CI cannot downgrade deployed version | 8 | Higher stored version blocks writes even if its tag disappears |
| One active deployment finishes; pending wakeup survives; waiting holds no runner | 9–10 | Serialized wiring and event-driven public-command replay |
| Original single-tag release/CLI paths stay intact; advanced runbook matches behavior | 10 | Existing regression suite plus updated operations walkthrough |

## Ordered leaves

### 1. Record the complete application publication outcome
Type: Behavior
Status: done
Proof: Publisher saves admitted identity before uploads and success after all
commands; a failed command leaves publishing state. Backend-skip data variation
still records app success without modifying backend hash bookkeeping.

Behavior: A validated app release publishes → record whether the whole release
finished. Wrap Story 1's publisher in one application-state boundary; use the
existing serialized workflow and private bucket. Preserve working tag releases.

Learning: the existing publisher is the cohesive outcome owner after its payload,
source, routing and ref preflights. Two writes to the private deploy object record
`publishing` before payload writes and `succeeded` after the complete publisher;
MIG-skip success leaves backend hash bookkeeping untouched.

### 2a. Classify existing application publication evidence
Type: Behavior
Status: planned
Proof: Controlled workflow-history and log fixtures return a published identity
only when the Deploy publication step succeeded and the admission log supplies
tag, raw refOid, peeled SHA and selected CI identity. CI-only, CLI-only and
no-publication successes do not qualify. Zero application tags plus zero deploy
push-event runs is verified empty; incomplete, missing or unparsable relevant
history is ambiguous.

Behavior: Application tracking is absent → inspect bounded deploy workflow
history and existing admission JSON logs → classify a verified published
identity, verified empty installation or ambiguous history without writing
production state. Existing `writeReleaseOutput` JSON is the identity transport;
the jobs API alone is insufficient because it omits job outputs.

### 2b. Initialize application tracking from classified evidence
Type: Behavior
Status: planned
Proof: Existing state remains byte-for-byte unchanged. A verified publication
classification seeds succeeded application state and a verified-empty
classification persists an initialized-empty state, both with zero payload/CLI/
backend writes. Ambiguous history fails visibly with zero state or payload writes.

Behavior: Tracking is introduced to an installation → under the existing
application concurrency owner, preserve established state or initialize the
same application-state object from the evidence classification. Never infer an
application version from green CI, whole-workflow status, CLI state or legacy
automatic main deployment. Require operator-supplied published-release identity
when relevant history cannot be verified.

Refinement learning: the first leaf-2 attempt spent about 13 minutes on mandatory
reading and boundary analysis, made no edits and hit the hard sizing trigger.
The disproved assumption was that GitHub's jobs API exposes Story 1 job outputs;
it exposes step conclusions, while the existing admission commands log their
identity JSON. Classification and state initialization therefore need separate
proof loops. On 2026-09-07 activation evidence was rechecked: remote `v*` tags
were empty and deploy.yml push-event history had `total_count: 0`; that observed
empty case is not an enduring assumption.

### 3. Make completed-release replays a no-op
Type: Behavior
Status: planned
Proof: Same successful tag/SHA replay performs no artifact download or production
write, including when old artifacts expired or CLI was independently updated.

Behavior: A completed release is encountered again → report already released.
Put this successful identity check before artifact admission. No other recovery
or ordering rule belongs in this leaf.

### 4. Reject a moved release identity
Type: Behavior
Status: planned
Proof: Retarget a real fixture tag after a failed attempt, including changing an
annotated tag object without changing its peeled SHA; the public entry rejects
before production writes. Reuse existing in-invocation movement proof.

Behavior: A release tag no longer matches its persisted refOid/SHA → fail
with an identity mismatch. Reuse record and Git fixtures; reject forced-update
and deletion inputs. Do not mutate tag protection rules or CLI policy.

### 5. Retry an interrupted release with the same tag
Type: Behavior
Status: planned
Proof: External failure after one upload leaves incomplete state; a same-tag/SHA
retry revalidates CI/artifacts, repeats permitted uploads and records success.

Behavior: A current release failed partway through → retry its immutable identity
→ complete that release. No compensating rollback or cross-service transaction.
Reuse existing outcome writes; add only interrupted-state admission and its
same-tag retry runbook example.

### 6. Recover a release whose CI artifacts are unavailable
Type: Behavior
Status: planned
Proof: Missing/expired artifacts cause zero writes and actionable diagnostics;
a newer successful CI attempt for the same SHA supplies the payload on retry.

Behavior: Release artifacts cannot be retrieved → guide the maintainer to rerun
that exact commit's CI → resume the same tag after success. Do not silently
rebuild, use newer main artifacts or promise to rerun history GitHub has discarded.
If historical CI cannot be recovered, use a newly tested correction and new patch.

### 7. Keep the highest pending version despite event order
Type: Behavior
Status: planned
Proof: Reversed v1.3.9/v1.3.10 wakeups always select v1.3.10 and wait if its CI
is unfinished; late older completion re-evaluates that same pending selection.

Behavior: Several application tags are pending → retain the highest numeric
version for the next release. Add reconciliation at the existing command boundary;
exercise it locally before exposing the new trigger. Never select based only on
incoming event order. Do not interrupt the active command's frozen identity.

### 8. Prevent older retries and late CI from replacing newer state
Type: Behavior
Status: planned
Proof: Higher persisted version rejects a stale attempt with zero writes, even
if the newer tag is absent from the current tag listing.

Behavior: An older candidate reaches admission after a newer release → report
superseded and retain the newer application. Reuse numeric comparison and record
lookup; this protects stored state beyond leaf 7's pending-tag selection.

### 9. Connect reconciliation without changing the active trigger
Type: Structure
Status: planned
Proof: Parsed workflow contracts and existing command tests cover both adapters:
current bounded tag wait and new immediate-return reconciliation. One application
concurrency group encloses selection, admission, publication and record writes.

Internal change: Factor the proven common admission/publication call so the new
tag/CI wakeup adapter can use it. Keep the working tag-only adapter active until
leaf 10. This preparation immediately enables that Behavior; no dual permanent
release engine, new state format or second lock is introduced.

### 10. Enable recovery and overlapping-release coordination
Type: Behavior
Status: planned
Proof: Replayed tag-first/CI-first, old-wakeup, interrupted-retry and newer-pending
fixtures produce the agreed public-command outcomes. Existing Story 1/CLI tests
remain green and parsed wiring shows no active-run cancellation or blocking wait.

Behavior: Maintainer submits overlapping tags or retries a failed release → the
workflow completes the active release, then reconciles the newest pending eligible
version without duplicate publication or downgrade.
Enable CI-completion wakeups and the new tag adapter; remove the bounded wait
and old adapter in the same green commit. Require verified initial tracking before
first reconciliation. Do not filter by incoming-event success/SHA before pending
selection. Replace the one-release-at-a-time restriction in the runbook with
retry, duplicate, moved-tag, pending-version and forward-correction instructions.

## Verification, sizing and wrap-up

Each leaf targets about five minutes including its focused proof and cleanup,
medium confidence. No advance sizing exceptions. The actual Story 1
interfaces are recorded above; refine an actual overrun in place without changing
the story scope. Existing guard coverage should reduce leaf 4 and 6 work.
If bootstrap evidence requires a broader investigation, fail visibly at that
boundary and refine the leaf rather than inventing production state.

```bash
CURSOR_DEV=true nix develop -c bash scripts/test/application-release.test
CURSOR_DEV=true nix develop -c bash scripts/test/upload-cli-binary-to-gcs.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/deploy-backend-jar-to-gcp-mig.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/upload-frontend-static-to-gcs.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/apply-doughnut-app-service-url-map-wiring.test
```

Use named scenarios per leaf and repeat other existing deployment regressions
only when their boundary changes. Before final cutover run both stories' focused
checks together. Local tests prove owned decisions, not the GitHub scheduler or
production credentials; no production tag or Slack message is a test fixture.

Execute via execute-plan: Jidoka → fresh post-change-refactor agent → API generation
only if needed → coordinator format:changed once → PLAN update → commit/push →
asynchronous CI observation. Review at five minutes and finer-decompose at ten
unless actual focused-test runtime explains it. Do not run this plan alongside
Story 1. After completion, clean up this plan and only its spent seed detail.

## Split provenance and platform constraints

Former leaves 6–7 and 10–14 belong here; recovery/operator guidance from former
16 is distributed into their owning behaviors. Bootstrap and event cutover make
this a safe upgrade from an independently finished Story 1. Former global waiting
and ordering assumptions no longer leak into the narrowed predecessor plan.
All accepted final promises are retained. Story 1's 73-test wrapper and combined
deployment regressions passed. The main merge started donut CI run 34071938563;
remote deploy.yml is tag-only and no deployment was observed for that push.
Real production credentials/scheduling remain operator confirmation, not local
proof. No release tags were created for verification.

Platform references already checked during the original planning:

- [Concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency): queued event order is not numeric version priority.
- [workflow_run](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_run): default-branch workflow, explicit selected SHA and completion/rerun handling.
- [Workflow runs API](https://docs.github.com/en/rest/actions/workflow-runs#list-workflow-runs-for-a-workflow): exact-SHA run lookup with pagination.
