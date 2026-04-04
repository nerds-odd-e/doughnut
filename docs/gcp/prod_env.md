# GCP Production environment

## 1. Interacting with gcloud CLI for cloud infrastructure management

- [Install `Google Cloud SDK`](https://cloud.google.com/sdk/docs/install)
- [Create App Server in GCloud Compute](infra/scripts/create-gcloud-app-compute.sh)
- Login to gcloud sdk: `gcloud auth login`
- Check your login: `gcloud auth list`
- Set/Point to gcloud dough project: `gcloud config set project carbon-syntax-298809`
- Check you can see the project as login user: `gcloud config list`

### 2. View/tail GCP VM instance logs

```bash
gcloud auth login
gcloud config set project carbon-syntax-298809
# Query GCP MIG instance/s health state and grep instance id of each GCP VM in MIG
infra/scripts/check-mig-doughnut-app-service-health.sh
# Expected output
# ❯ ./check-mig-doughnut-app-service-health.sh
# ---
# backend: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instanceGroups/doughnut-app-group
# status:
#  healthStatus:
#  - healthState: HEALTHY
#    instance: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instances/doughnut-app-group-0c2b
#    ipAddress: 10.142.0.7
#    port: 8081
#  - healthState: HEALTHY
#    instance: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instances/doughnut-app-group-2j9f
#    ipAddress: 10.142.0.8
#    port: 8081
#  kind: compute#backendServiceGroupHealth

# View instance logs - Take/use one of the above healthcheck report instance id for next command (e.g. doughnut-app-group-2j9f)
infra/scripts/view-mig-doughnut-app-instance-logs.sh doughnut-app-group-2j9f

# Tail instance logs - Take/use one of the above healthcheck report instance id for next command (e.g. doughnut-app-group-2j9f)
infra/scripts/tail-mig-doughnut-app-instance-logs.sh doughnut-app-group-2j9f
```

### 3. Building/refreshing doughnut-app MIG VM instance/s base image with Packer + GoogleCompute builder

We use packer + googlecompute builder + shell provisioner to construct and materialise base VM image to speed up deployment and control our OS patches and dependent packages and libraries upgrades

- [Packer](https://www.packer.io)
- [packer googlecompute builder](https://www.packer.io/docs/builders/googlecompute)
- [SaltStack](https://docs.saltproject.io/en/latest/)

#### How-to

From `infra` directory, run the following:

Login to dough GCP project account with `gcloud auth login`
Configure gcloud CLI to project ID with `gcloud config set project carbon-syntax-298809`

```bash
cd infra
export GCLOUDSDK_CORE_PROJECT="$(gcloud config get-value project)"
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/carbon-syntax-298809-f31377ba77a9.json
PACKER_LOG=1 packer build packer.json
```

Expect to see following log line towards end of Packer build stdout log:
`--> googlecompute: A disk image was created: doughnut-debian12-mysql84-base-saltstack`

## 4. Production Database (Cloud SQL for MySQL)

- Instance: `doughnut-db-instance` (Cloud SQL, MySQL 8.4)
- Private DNS target for app: `db-server` (maps to the instance's private IP)
- Vector support: enabled via Cloud SQL flag `cloudsql_vector=on`

Enable/verify vector flag:

```bash
gcloud sql instances patch doughnut-db-instance \
  --database-flags=cloudsql_vector=on
gcloud sql instances describe doughnut-db-instance \
  --format="table(settings.databaseFlags[].name, settings.databaseFlags[].value)"
```

## 5. CI/CD: conditional backend deploy

Green `main` builds may **skip** GCS jar upload and MIG rollout when the jar hash matches the last successful deploy record. To **force** upload + rolling replace anyway, use the commit-message token and merge caveats in [conditional-backend-deploy.md](conditional-backend-deploy.md).

## 6. Frontend static in GCS (CI publish + prod LB)

Each green `Package-artifacts` run on `main` uploads the SPA tree (Vite output under `frontend/dist/` after `pnpm bundle:all`) to:

`gs://<GCS_FRONTEND_BUCKET>/frontend/<GITHUB_SHA>/`

The CLI install binary goes to `gs://<GCS_FRONTEND_BUCKET>/doughnut-cli-latest/doughnut`. Deploy artifacts (fat jar, `deploy/last-successful-deploy.json`) use **`GCS_BUCKET`** only. Upload scripts: `infra/gcp/scripts/upload-frontend-static-to-gcs.sh`, `infra/gcp/scripts/upload-cli-binary-to-gcs.sh`.

**Prod routing:** HTTPS load balancer sends static paths to a **backend bucket** (and optional Cloud CDN); API, OAuth, `/attachments`, `/logout`, `/install`, etc. stay on the MIG. Full runbook (including a one-page release checklist): [prod-frontend-static-lb.md](prod-frontend-static-lb.md).

**Local dev / Cypress (ports and LB — source of truth):**

| Port | Role |
|------|------|
| **2525** | Mountebank |
| **9081** | Spring (sut / E2E profile) |
| **5173** | Local LB (`scripts/local-lb.mjs`) — browser and Cypress **`baseUrl`** **`http://localhost:5173`** |
| **5174** | Vite dev server — only when using **`pnpm sut`** / **`pnpm local:lb:vite`** |

**Readiness:** **`GET http://127.0.0.1:5173/__lb__/ready`** → **200** (Spring health probed from the LB; use for **`wait-on`** / automation; set **`NO_PROXY=127.0.0.1,localhost`** in CI to avoid proxy issues on loopback).

**Scripts:** **`pnpm local:lb`** — static from **`frontend/dist`** + Spring **9081** (no Vite). **`pnpm local:lb:vite`** — same LB with **`LOCAL_LB_VITE_UPSTREAM=http://127.0.0.1:5174`** for UI + HMR (**`frontend/vite.config.ts`** `server.port`). **CI** and **`pnpm test`** use **`local:lb`**; **`pnpm sut`** uses **`local:lb:vite`** + **`frontend:sut`**. Build static first when needed: **`pnpm frontend:build`** or **`pnpm bundle:all`**. **`/doughnut-cli-latest/doughnut`** is served from **`cli/dist/doughnut-cli.bundle.mjs`** (**`pnpm cli:bundle`** — **`pnpm sut`** / **`pnpm test`** run this after install). Full env list: header on **`scripts/local-lb.mjs`** (`LOCAL_LB_STATIC_ROOT`, `LOCAL_LB_BACKEND`, `LOCAL_LB_VITE_UPSTREAM`, `LOCAL_LB_LISTEN_PORT`, `LOCAL_LB_ROUTING_JSON`).

**Verify the stack:** **`pnpm sut:healthcheck`**. If unhealthy or stray processes: **`pnpm sut:restart`** or start **`pnpm sut`**. With Nix (typical local agent): **`CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`** / **`… sut:restart`** — see **`CLAUDE.md`**.

## 7. Book PDF storage (GCS, prod)

Attached notebook PDFs are stored in GCS when the backend runs with Spring profile **`prod`** (`GcsBookStorage` + VM **Application Default Credentials**). Production uses bucket **`doughnut-book-pdf-carbon-syntax-298809`** (the short name **`books`** is not available globally on GCS). Prod sets **`doughnut.book-pdf.gcs.bucket`** in [`backend/src/main/resources/application.yml`](../../backend/src/main/resources/application.yml) (prod profile). Optional **`doughnut.book-pdf.gcs.object-prefix`** is unset by default.

**Global names:** GCS bucket names are globally unique. If you recreate this environment in another project, pick a unique bucket name and keep **`application.yml`** and this section in sync.

**Create the bucket** (project **`carbon-syntax-298809`**, region aligned with the MIG in **`us-east1`**):

```bash
gcloud config set project carbon-syntax-298809
gcloud storage buckets create gs://doughnut-book-pdf-carbon-syntax-298809 \
  --project=carbon-syntax-298809 \
  --location=us-east1 \
  --uniform-bucket-level-access
```

**IAM:** The app VM uses the instance default **Compute Engine service account** `220715781008-compute@developer.gserviceaccount.com` (pattern: `PROJECT_NUMBER-compute@developer.gserviceaccount.com` for project **`carbon-syntax-298809`**). Grant **`roles/storage.objectAdmin`** on the bucket so attach and download can **`put`** and **`get`** objects:

```bash
gcloud storage buckets add-iam-policy-binding gs://doughnut-book-pdf-carbon-syntax-298809 \
  --project=carbon-syntax-298809 \
  --member="serviceAccount:220715781008-compute@developer.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"
```

**Upload limit:** Book attach uses Spring **`spring.servlet.multipart.max-file-size`** (10MB in [`application.yml`](../../backend/src/main/resources/application.yml)); oversize requests get HTTP **413** with an **`ApiError`** JSON body (same shape as other API errors).

**GCS orphans:** Deleting or replacing a **Book** row does **not** remove the object in GCS; orphan object cleanup is **not** implemented yet.

Operational note: creating a Cloud SQL VECTOR index may fail with
"Vector index: not enough data to train" if the table has too few embeddings.
Run index creation after sufficient data exists.
