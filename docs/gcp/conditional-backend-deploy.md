# Conditional backend deploy (GCS jar + MIG)

On a green `main` pipeline, the **Deploy** job runs `infra/gcp/scripts/deploy-backend-jar-to-gcp-mig.sh`. That script compares the built fat jar’s SHA-256 to `gs://<bucket>/deploy/last-successful-deploy.json`. When they match, it **skips** uploading the jar and **skips** the MIG rolling replace, and leaves the record unchanged.

## Last successful deploy record

After a **full** deploy (upload + rolling replace), CI writes JSON to `deploy/last-successful-deploy.json` with `sha256`, `git_sha`, and `recorded_at`. The record is updated only when both steps succeed.

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

If prod matches the intended jar but the record is wrong or missing, you can fix or remove `deploy/last-successful-deploy.json` in GCS as described in the project deploy plan (`ongoing/conditional-deploy-gcs-frontend.md`).
