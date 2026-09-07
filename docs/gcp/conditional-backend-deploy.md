# Application releases and conditional backend deploy

**See also:** [prod-frontend-static-lb.md](prod-frontend-static-lb.md) for SPA/CLI buckets, URL map, and frontend publication (Deploy always applies the URL map; jar rollout is what this page describes).

Production is published only by a stable `vMAJOR.MINOR.PATCH` tag through the
[deploy workflow](../../.github/workflows/deploy.yml). Ordinary `main` pushes run
CI and publish nothing. A release selects the tag's exact main commit, even when
main has advanced. Two-component tags such as `v1.2`, prereleases such as
`v1.2.3-rc.1` and unrelated tags do not deploy the application.
The workflow waits up to **60 minutes** for that commit's
latest applicable `ci.yml` main-push run/attempt to succeed. Failed or cancelled
CI fails admission; it never substitutes an older green run or another commit.

## Release one version

1. Coordinate **one application release at a time**; wait until its workflow
   finishes and inspect its result before issuing another version. The workflow
   uses one non-canceling production concurrency group; it is not a version queue
   or a duplicate/retry recovery mechanism.
2. Choose an exact commit on `main` with successful CI and available jar, frontend
   and CLI artifacts. A tag may arrive before CI finishes; the bounded wait handles
   that ordering. Use increasing versions and immutable tags; do not move/delete
   tags, rerun old releases, or overlap release requests.
3. Confirm the selected source contains the tag-triggered `deploy.yml`. **The first
   release must include this workflow cutover.** Older pre-cutover source does not
   acquire a tag trigger when main changes. Later, a tested earlier main commit
   containing the trigger can be selected without selecting main's current tip.
4. Create and push one new version, substituting the selected full SHA and next
   unused increasing version:

   ```bash
   git fetch origin main --tags
   git tag -a v1.2.3 <FULL_TESTED_MAIN_SHA> -m 'Release v1.2.3'
   git push origin refs/tags/v1.2.3
   ```

5. Inspect **donut deploy** admission outputs for the chosen tag, SHA and CI run/
   attempt, then the publication result. All three artifacts must be available
   from that run before any production upload. The tag ref is checked again before
   writes. The selected source supplies routing, startup script and force token;
   publication runs the current main orchestration pinned at admission.
6. Confirm the SPA/CLI and backend health as described in the
   [frontend runbook](prod-frontend-static-lb.md#smoke-checks-after-a-change).
   Local tests do not prove GitHub scheduling, artifact service or GCP credentials;
   the first actual release confirms those platform paths.

Publication uploads the frontend, then the bundled CLI, then applies routing and
conditionally deploys the backend. Independent `cli-*` releases retain their own
tag-derived CLI version. Application tags do not change the validated CLI bundle's
embedded version or the jar's `donut-0.0.1-SNAPSHOT.jar` name and GCS destinations.

The backend script compares the fat jar's SHA-256 and selected MIG startup script's
SHA-256 to `gs://<bucket>/deploy/last-successful-deploy.json`. Matching hashes skip
jar upload and MIG rolling replace and leave the record unchanged. Frontend and
CLI publication and URL-map application still run.

## Failed release and correction

Inspect the reported tag, SHA, CI run/attempt and failure stage. Missing/expired
artifacts or failed validation stop before uploads. Later publication failure can
leave a partial frontend/CLI/backend change; this workflow has no automatic
rollback or durable application release ledger. Do not rerun the old release or
retarget its tag. Commit a correction or revert on main, test that new commit, then
release it under the **next patch version**, after the previous workflow has ended.
There is **no automatic schema rollback**; assess existing production schema and
migration compatibility when preparing that correction/revert.

## Last successful deploy record

After a **full** deploy (upload + rolling replace), the deploy workflow waits until the MIG reaches the new instance template version (`infra/gcp/scripts/check-mig-rollout.sh`), then probes `https://doughnut.odd-e.com/api/healthcheck` via `infra/gcp/scripts/app-instance-healthcheck.sh`. Only on success does it write JSON to `deploy/last-successful-deploy.json` with `sha256`, `startup_script_sha256`, `git_sha`, and `recorded_at`. A failed wait or probe fails the deploy workflow and leaves the record unchanged.

## MIG template / startup changes without a new jar

The deploy script compares the **fat jar** hash and committed startup script hash to the record. Changes to `infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` trigger a full deploy even when the jar is unchanged.

Changes made only in the GCP console, instance templates, or other metadata still do not change either committed hash. To roll the MIG in those cases, use **force full deploy** (next section: `force-deployment: true` on the selected release commit), run the deploy script with **`FORCE_FULL_DEPLOY=1`**, or perform a **manual** rolling replace / template update in GCP.

## Force a full deploy: `force-deployment: true`

Include the following in the subject or body of the **selected tagged commit**:

```text
force-deployment: true
```

Matching is case-sensitive; extra spaces around `:` are allowed. The publisher
reads `git log -1 --format=%B` for the selected SHA after CI admission. Other commits
in the push, the newer main tip and the annotated tag message do not force rollout.
For a squash or merge commit, put the token in that resulting commit's message;
for rebase, put it in the selected rebased commit. Test and tag that exact commit.

## Manual / local script runs

To bypass the hash skip when invoking the script yourself:

```bash
FORCE_FULL_DEPLOY=1 GITHUB_SHA="$(git rev-parse HEAD)" \
  GCS_BUCKET=… ARTIFACT=donut VERSION=0.0.1-SNAPSHOT \
  infra/gcp/scripts/deploy-backend-jar-to-gcp-mig.sh
```

Use normal GCP credentials and a jar path the script can find (or set `DEPLOY_JAR_PATH`).

## Other recovery

If prod already runs the intended jar/startup script but `deploy/last-successful-deploy.json` in **`GCS_BUCKET`** is wrong or missing, either:

- Upload a corrected JSON object with matching `sha256` and `startup_script_sha256` (and consistent `git_sha` / `recorded_at` if you use them), or
- Remove the object so the next deploy treats the record as absent and performs a full upload + MIG rollout (coordinate with the team; traffic impact depends on template changes).

Do not confuse this object with frontend trees under **`GCS_FRONTEND_BUCKET`**; only the deploy bucket holds the jar deploy record.
