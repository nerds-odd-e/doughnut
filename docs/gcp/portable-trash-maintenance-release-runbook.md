# Portable trash maintenance release runbook

**See also:** [conditional-backend-deploy.md](conditional-backend-deploy.md) for
the ordinary release/deploy flow, whose backend-rollout step this one-time path
replaces (only for this release);
[deploying-mig-startup-script-updates.md](deploying-mig-startup-script-updates.md)
for how instance templates are normally created and rolled out via the
ordinary route this path does not use.

This is a minimal, one-time runbook for the portable-trash legacy-trash schema
upgrade (SEED-009 story 37,
`.planning/quick/123-safe-portable-trash-upgrade/PLAN.md`). It covers plan
slices 7-9: entering a verified quiescent maintenance state, keeping old
application binaries from resuming writes after that point, and routing
ordinary publication through that protected sequence for this one release.
Slice 10 will add backup/preflight/recovery guidance here. Story 39 removes
this document, the maintenance scripts, and the migration-only code they
protect after production success (see the plan's temporary removal
inventory).

## What `enter-maintenance-mode.sh` does

`infra/gcp/scripts/enter-maintenance-mode.sh` is called from
`deploy-backend-jar-to-gcp-mig.sh`'s maintenance-protected rollout (slice 9)
for this one-time release, in place of the ordinary
create-template-then-rolling-replace route (`update-mig-startup-script.sh`).
It remains runnable standalone (e.g. for a manual operator run):

1. **Set the MIG's instance template** to the already-created,
   portable-trash-compatible template (`MAINTENANCE_INSTANCE_TEMPLATE`, no
   default — a missing value fails loudly rather than silently leaving
   whatever template the MIG already has):

   ```bash
   gcloud compute instance-groups managed set-instance-template doughnut-app-group \
     --template="$MAINTENANCE_INSTANCE_TEMPLATE" \
     --zone=us-east1-b
   ```

2. **Stop every instance** in `doughnut-app-group`:

   ```bash
   gcloud compute instance-groups managed stop-instances doughnut-app-group \
     --zone=us-east1-b --all-instances
   ```

3. **Wait, then poll** `list-instances --format=value(instanceStatus)` until
   none report `RUNNING|STOPPING|PROVISIONING|STAGING|REPAIRING`, or a
   timeout is hit.

The exit code is the only signal this script produces: `0` means quiescence
was verified and migration is permitted; any non-zero exit, including a
timed-out poll, means migration must **not** proceed. No new state is
written by the script itself — step 1's instance-template assignment is
GCP's own MIG state, described below.

The backend's HTTP handlers and its in-process `@Scheduled` background jobs
(`QuestionGenerationBatchMaintenanceJob`, `EmbeddingMaintenanceJob` — see
`backend/src/main/java/com/odde/donut/services`) run inside the same JVM per
MIG instance; there is no separate worker fleet. Stopping the MIG's instances
therefore excludes both surfaces of schema-dependent writers, not incoming
HTTP alone.

## Env

| Variable | Default | Meaning |
| --- | --- | --- |
| `ZONE` | `us-east1-b` | MIG zone |
| `MIG_NAME` | `doughnut-app-group` | MIG name |
| `MAINTENANCE_INSTANCE_TEMPLATE` | *(required)* | Name of the already-created compatible instance template |
| `MAINTENANCE_STOP_GRACE_SECONDS` | `10` | Wait after issuing stop before the first quiescence check |
| `MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS` | `300` | Poll timeout |
| `MAINTENANCE_QUIESCENCE_POLL_SECONDS` | `5` | Poll interval |

## Cloud-owned controls this slice relies on

The plan requires using existing cloud lifecycle controls for the bounded
maintenance window — not a shell trap or an HTTP gate — because those would
only protect writers while this script's own process stays alive. The actual
controls, and why the ordering above closes the gap slice 7 alone left open:

- **`instanceTemplate` is a field GCP stores on the MIG resource itself**, set
  by `set-instance-template`. It survives this script's process dying,
  survives a network drop, survives an operator's Ctrl-C — it is durable
  the instant the `gcloud` call in step 1 returns, regardless of what happens
  next.
- **Autohealing** (`infra/gcp/scripts/add-mig-autohealing.sh`) recreates an
  instance that fails its `/api/healthcheck` 3 times in a row (~90s). A
  recreated instance is provisioned from the MIG's **current** instance
  template — documented GCP behavior, not specific to this repo.
- **The MIG's update policy** (`infra/gcp/scripts/configure-mig-update-policy.sh`)
  is `PROACTIVE` / `most-disruptive-action=replace`: the MIG's own control
  loop works to converge every instance toward whatever template is
  currently assigned, replacing instances itself without a separate
  `rolling-action replace` call.

Because step 1 runs and durably commits *before* step 2 stops any instance,
every path that can bring an instance back up for this MIG after that point —
autohealing recreating a stopped instance, the update policy's own proactive
convergence, or an operator's manual retry/recreate — necessarily boots the
compatible template, never the old one. This is why `set-instance-template`
precedes `stop-instances` rather than following it or being combined with an
explicit rolling replace: a plain `set-instance-template` call does not by
itself touch already-running instances (they keep running their current
template until something recreates or replaces them), so it commits the
precondition without also forcing disruptive replacement of instances this
script is about to stop anyway.

## Operational caveat: the instance template does not pin the jar

The startup script baked into every instance template
(`infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh`) always
downloads the jar from the **same fixed GCS path**,
`gs://<bucket>/backend_app_jar/donut-0.0.1-SNAPSHOT.jar` — see
[conditional-backend-deploy.md](conditional-backend-deploy.md#release-application-versions):
"Application tags do not change ... the jar's `donut-0.0.1-SNAPSHOT.jar`
name and GCS destinations." Ordinary deploys overwrite that same object on
every release; the startup script's text does not itself encode a jar
version or hash.

This means instance-template identity alone does not select which jar bytes
an instance runs — swapping the template is the durable, cloud-owned control
over *which template* any future instance boots from, but the **compatible
jar must already be uploaded to that fixed GCS path** before any instance
that could restart under this or any template does so.
`deploy-backend-jar-to-gcp-mig.sh` (slice 9) satisfies this: `gsutil cp` of
the jar to `backend_app_jar/donut-0.0.1-SNAPSHOT.jar` already ran, unchanged,
before the new instance template is even created, which itself precedes the
`set-instance-template` step above — not only after `enter-maintenance-mode.sh`
exits successfully.

## How `deploy-backend-jar-to-gcp-mig.sh` routes through this for slice 9

For this one release only, `deploy-backend-jar-to-gcp-mig.sh` replaces its
ordinary `update-mig-startup-script.sh` call (create template, then
immediately `rolling-action replace`) with:

1. `create-mig-instance-template-for-maintenance.sh` — duplicates only the
   template-creation `gcloud compute instance-templates create` call from
   `update-mig-startup-script.sh`; prints the new template name on stdout
   and does **not** assign it to the MIG or trigger a replace.
2. `enter-maintenance-mode.sh` with `MAINTENANCE_INSTANCE_TEMPLATE` set to
   that new template name — sets the template, stops every instance, and
   verifies quiescence, as described above.
3. `exit-maintenance-mode.sh` — the new resume counterpart; runs
   `gcloud compute instance-groups managed start-instances doughnut-app-group
   --zone=us-east1-b --all-instances` so the compatible template actually
   boots and serves again.
4. `check-mig-rollout.sh` and `app-instance-healthcheck.sh` — unchanged,
   confirming the restarted instances reached the new template version and
   are healthy before the deploy is recorded as successful.

The ordinary `rolling-action replace` call is never issued for this release.
`publish-application.sh`'s frontend/CLI upload and release-outcome recording
are unchanged; only this one backend-rollout step is replaced. Story 39
reverts this once production has succeeded (see the plan's temporary removal
inventory).

## Proof

`scripts/ci/application-release-maintenance-fixtures.mjs` /
`application-release-maintenance.test.mjs` trace fake `gcloud` invocations
and assert: `set-instance-template` precedes `stop-instances`, which
precedes quiescence polling; a failure between the template assignment and
the stop leaves the compatible template durably set (verified via an
independent, later `describe` call reading the same fake persisted MIG
state) even though migration is not permitted; and a missing
`MAINTENANCE_INSTANCE_TEMPLATE` fails before any `gcloud` call is made,
leaving the prior template untouched. These trace fixtures cannot simulate
GCP's actual autohealing daemon or update-policy control loop; they prove
this script's own command ordering establishes the precondition that makes
the GCP-side guarantee, described above, hold.

`scripts/ci/application-release-publication-fixtures.mjs` /
`application-release-publication.test.mjs`'s `forced` scenario extends this
at the actual publication entry point (`publish-application.sh`, as invoked
by `.github/workflows/deploy.yml`'s `publish` step): it asserts the full
maintenance-protected order (jar upload → template created →
set-instance-template → stop-instances → quiescence poll → start-instances →
rollout wait → healthcheck → record) and that no `rolling-action replace`
call appears anywhere in the trace; the `skip` scenario asserts none of the
maintenance/rollout calls run at all when the hash-compare skip applies. As
with the maintenance fixtures, this is local fake-cloud proof — it does not
by itself prove GCP-side isolation.
