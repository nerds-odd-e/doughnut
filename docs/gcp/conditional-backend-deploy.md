# Conditional backend deploy (GCS jar + MIG)

**See also:** [prod-frontend-static-lb.md](prod-frontend-static-lb.md) for SPA/CLI buckets, URL map, and frontend rollback (Deploy always applies the URL map; jar rollout is what this page describes).

On a green `main` pipeline, the **Deploy** job runs `infra/gcp/scripts/deploy-backend-jar-to-gcp-mig.sh`. That script compares the built fat jar’s SHA-256 to `gs://<bucket>/deploy/last-successful-deploy.json`. When they match, it **skips** uploading the jar and **skips** the MIG rolling replace, and leaves the record unchanged.

## Last successful deploy record

After a **full** deploy (upload + rolling replace), CI writes JSON to `deploy/last-successful-deploy.json` with `sha256`, `git_sha`, and `recorded_at`. The record is updated only when both steps succeed.

## MIG template / startup changes without a new jar

The deploy script only compares the **fat jar** hash to the record. Changes that affect VMs but **not** the built jar—startup scripts, instance templates, metadata, or edits made only in the GCP console—do **not** change that comparison. A green pipeline can still **skip** uploading the jar and **skip** the MIG rolling replace.

To roll the MIG in those cases, use **force full deploy** (next section: `force-deployment: true` on the tip of `main`), run the deploy script with **`FORCE_FULL_DEPLOY=1`**, or perform a **manual** rolling replace / template update in GCP.

## Force a full deploy: `force-deployment: true`

Include the following in the **subject or body** of the commit that ends up as **`GITHUB_SHA` on `main`** (the commit CI checks out for the Deploy job):

```text
force-deployment: true
```

Matching is case-sensitive. Extra spaces around `:` are allowed (for example `force-deployment : true`).

CI sets `FORCE_FULL_DEPLOY=1` for the deploy script when that pattern appears in **`git log -1 --format=%B`** for `$GITHUB_SHA` — i.e. the **tip commit of the push** only.

### Multi-commit pushes

If you push several commits at once, only the **latest** commit’s message is inspected. Put the line on that commit (or push it alone).

### Merge methods

- **Squash merge:** add the line to the squash commit title or description.
- **Merge commit:** the line must appear in the **merge commit message** (the default merge message usually does not include the PR description).
- **Rebase merge:** put the line on the rebased tip commit message.

## Manual / local script runs

To bypass the hash skip when invoking the script yourself:

```bash
FORCE_FULL_DEPLOY=1 GITHUB_SHA="$(git rev-parse HEAD)" \
  GCS_BUCKET=… ARTIFACT=doughnut VERSION=0.0.1-SNAPSHOT \
  infra/gcp/scripts/deploy-backend-jar-to-gcp-mig.sh
```

Use normal GCP credentials and a jar path the script can find (or set `DEPLOY_JAR_PATH`).

## Other recovery

If prod already runs the intended jar but `deploy/last-successful-deploy.json` in **`GCS_BUCKET`** is wrong or missing, either:

- Upload a corrected JSON object with the matching `sha256` (and consistent `git_sha` / `recorded_at` if you use them), or
- Remove the object so the next deploy treats the record as absent and performs a full upload + MIG rollout (coordinate with the team; traffic impact depends on template changes).

Do not confuse this object with frontend trees under **`GCS_FRONTEND_BUCKET`**; only the deploy bucket holds the jar deploy record.
