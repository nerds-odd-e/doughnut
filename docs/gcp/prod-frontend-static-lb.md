# Production: SPA static from GCS (LB + CDN), API from MIG

Runbook for production static hosting: one browser-facing hostname, HTTPS load balancer path rules, backend bucket (and optional Cloud CDN) for the Vue build, managed instance group (MIG) for Spring Boot.

**Related:** CI uploads each commit’s SPA tree to `gs://<GCS_FRONTEND_BUCKET>/frontend/<GITHUB_SHA>/` and the CLI install binary to `gs://<GCS_FRONTEND_BUCKET>/doughnut-cli-latest/doughnut` ([`upload-frontend-static-to-gcs.sh`](../../infra/gcp/scripts/upload-frontend-static-to-gcs.sh), [`upload-cli-binary-to-gcs.sh`](../../infra/gcp/scripts/upload-cli-binary-to-gcs.sh)). In [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), `GCS_FRONTEND_BUCKET` is the public static bucket (e.g. `dough-frontend-01`); `GCS_BUCKET` is deploy-only (jars, `deploy/`, etc., e.g. `dough-01`).

## Release at a glance

| Step | What happens |
|------|----------------|
| Green `main` **Package-artifacts** | SPA → `gs://<GCS_FRONTEND_BUCKET>/frontend/<GITHUB_SHA>/`; CLI → same bucket; fat jar + `deploy/last-successful-deploy.json` → `GCS_BUCKET` only. |
| Green `main` **Deploy** | Always runs [`apply-doughnut-app-service-url-map.sh`](../../infra/gcp/scripts/apply-doughnut-app-service-url-map.sh) so the LB serves that pipeline’s `frontend/<GITHUB_SHA>/` (including frontend-only commits). |
| Backend MIG | Jar upload + rolling replace only when the jar hash differs from the record — [conditional-backend-deploy.md](conditional-backend-deploy.md). |
| Routing edits | Change [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json); CI must pass `pnpm validate:path-routing`. |
| **Frontend rollback** | Render + validate + `gcloud url-maps import` for an older SHA whose `frontend/<SHA>/` still exists (commands under [Recovery / manual import](#cutover-checklist-new-frontend-bucket) below). |
| **Backend rollback / bad record** | Redeploy a known-good jar, use `force-deployment: true`, or fix `deploy/last-successful-deploy.json` in `GCS_BUCKET` — [conditional-backend-deploy.md](conditional-backend-deploy.md). |

---

## Goals

- **Single origin** for the browser (existing prod domain): cookies, OAuth redirects, and same-site behavior stay simple.
- **Static assets and SPA shell** served from **GCS** in front of the domain (via LB, not `storage.googleapis.com` as the script origin).
- **API, auth, attachments, install script** stay on the **MIG** backend service.

Do **not** treat `https://storage.googleapis.com/...` as the primary UI origin: use your domain + LB to avoid CORS and cache/versioning issues for module scripts.

---

## GCP building blocks (project `carbon-syntax-298809`)

| Resource | Name / note |
|----------|-------------|
| GCS bucket (public static: SPA + CLI) | `dough-frontend-01` — CI `GCS_FRONTEND_BUCKET` |
| GCS bucket (deploy / private ops) | `dough-01` — CI `GCS_BUCKET` (jars, `deploy/`, etc.; **no** `allUsers` needed for prod UI) |
| Backend bucket (CDN on) | `doughnut-frontend-backend-bucket` → **`dough-frontend-01`** (not the deploy bucket) |
| URL map (HTTPS) | `doughnut-app-service-map` — routing source [`infra/gcp/path-routing/doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json); CI renders YAML with `GITHUB_SHA` and imports after each green `main` run |
| HTTPS target proxy | `doughnut-app-service-map-target-proxy-2` → above URL map |
| MIG backend service | `doughnut-app-service` (default path matcher + API traffic) |
| GCS read for LB + CDN | Prefer **`roles/storage.objectViewer`** on `dough-frontend-01` for `allUsers` **only on this bucket**, or keep the bucket private and grant the default Compute SA, `service-<PROJECT_NUMBER>@compute-system.iam.gserviceaccount.com`, and (with Cloud CDN) `service-<PROJECT_NUMBER>@cloud-cdn-fill.iam.gserviceaccount.com`. With **Cloud CDN** enabled, Google’s docs require the CDN fill SA when the bucket is not publicly readable. |

### Cutover checklist (new frontend bucket)

Do this **before** the first green `main` run that uses `GCS_FRONTEND_BUCKET`:

1. Create `gs://dough-frontend-01` (or your chosen name; keep [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) in sync).
2. Point global backend bucket **`doughnut-frontend-backend-bucket`** at that GCS bucket (replace any attachment to `dough-01` for static serving).
3. Grant IAM on **that** bucket for LB + CDN as in the table above.
4. Optionally copy existing `frontend/*` and `doughnut-cli-latest/*` objects from the old bucket if you need rollback SHAs to resolve before the next CI upload.
5. Remove **`allUsers`** (and any overly broad read) from **`dough-01`** once nothing public depends on it.

**Org constraint:** If your org forbids `allUsers` with IAM **conditions** (`PublicResourceAllowConditionCheck`), you cannot scope “public read only under `frontend/`” on one mixed bucket via conditional bindings; a **dedicated** frontend bucket avoids that.

**Normal release:** On green `main`, CI runs [`apply-doughnut-app-service-url-map.sh`](../../infra/gcp/scripts/apply-doughnut-app-service-url-map.sh) (render from `doughnut-routing.json` + `pnpm validate:path-routing` equivalent + `gcloud compute url-maps import`) so the LB serves `frontend/<GITHUB_SHA>/` for that pipeline.

**Recovery / manual import:** Render for a known commit (40-char SHA) whose tree exists under `gs://<GCS_FRONTEND_BUCKET>/frontend/<SHA>/`, validate, then import:

```bash
gcloud config set project carbon-syntax-298809
node infra/gcp/url-maps/renderDoughnutAppServiceUrlMap.mjs --sha <FULL_GITHUB_SHA> --write /tmp/url-map.yaml
pnpm exec node scripts/validate-url-map-static-vs-backend-hints.mjs --url-map /tmp/url-map.yaml
gcloud compute url-maps import doughnut-app-service-map --source=/tmp/url-map.yaml --global --quiet
```

Add entries under `gcpUrlMap.staticPathRules` in [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json) for any **new root-level** static files (same pattern as `odd-e.ico` / `odd-e.png`), merge to `main`, and let CI apply the updated map.

HTTP forwarding (`doughnut-app-web-map-http`) is unchanged and does not use this map.

---

## Choosing the **active** frontend revision

CI writes **immutable** prefixes: `frontend/<GITHUB_SHA>/`. See [Release at a glance](#release-at-a-glance) for the automatic path; rollback uses the [manual import](#cutover-checklist-new-frontend-bucket) commands above.

**Alternatives (tradeoffs):**

- **`latest` JSON in GCS:** Useful as a human-readable pointer; the HTTPS LB does not read it natively—you would still need Terraform/gcloud or a small service to apply it to the URL map.
- **Separate static hostname (e.g. `cdn.`)** — possible, but then you must design **CORS**, **cookies** (`Domain` / `SameSite`), and **OAuth redirect URIs** explicitly. Prefer single hostname unless you have a strong reason.

---

## Path routing (what must hit the MIG)

Order **specific** paths **before** the catch-all that sends traffic to GCS.

The **canonical routing** for prod URL-map generation, local LB backend classification, mandatory static probes, and CI validation lives in [`infra/gcp/path-routing/doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json) (`backendPathHints`, `gcpUrlMap.staticPathRules`, `mandatoryStaticBucketProbes`, `localProxy`). `pnpm validate:path-routing` checks the URL map YAML **generated** from that file (dummy SHA when no `--url-map`). It also ensures root-level static paths implied by [`frontend/index.html`](../../frontend/index.html), [`frontend/public/`](../../frontend/public/), and (when present) `frontend/dist/index.html` are covered by a static pathRule. Unit tests: `pnpm test:path-routing`.

Send to the **backend service (MIG)** at least:

| Path prefix / pattern | Why |
|----------------------|-----|
| `/api/*` | REST API |
| `/attachments/*` | [`AttachmentController`](../../backend/src/main/java/com/odde/doughnut/controllers/AttachmentController.java) |
| `/logout` | Spring Security logout ([`CommonConfiguration`](../../backend/src/main/java/com/odde/doughnut/configs/CommonConfiguration.java)) |
| `/users/identify` | Prod auth entry ([`ApplicationController`](../../backend/src/main/java/com/odde/doughnut/controllers/ApplicationController.java)) |
| `/install` | CLI install script ([`InstallController`](../../backend/src/main/java/com/odde/doughnut/controllers/InstallController.java)) |
| `/oauth2/*`, `/login/oauth2/*` (and any other OAuth paths your Spring Security config uses) | OAuth2 login |

**Optional:** `/robots.txt` can be served from GCS if you upload it in the static tree, or left on the MIG.

**Default service:** In the rendered map, `defaultService` is the MIG (`doughnut-app-service`). Traffic that does not match a static path rule goes to Spring (API, OAuth, deep-link shell fetch, etc.).

---

## Path routing (what hits GCS)

Built app uses Vite `base: '/'` ([`frontend/vite.config.ts`](../../frontend/vite.config.ts)): **root-relative** asset URLs (`/assets/…`).

Minimum to send to the **backend bucket** (with prefix rewrite to `frontend/<ACTIVE_SHA>/`):

- `/assets/*` — hashed JS/CSS/media
- `/` and `/index.html` — SPA entry
- Any other **real files** present at bucket root for that build (e.g. favicon if present)
- `/doughnut-cli-latest/*` — CLI install binary at bucket prefix `doughnut-cli-latest/` (CI: [`upload-cli-binary-to-gcs.sh`](../../infra/gcp/scripts/upload-cli-binary-to-gcs.sh); not embedded in the Spring jar)

---

## SPA deep links (`/d/…`, `/n…`)

There is **no** object per client route under `frontend/<sha>/`. The URL map still sends these paths to the **MIG** (path matcher default). Spring must not serve **classpath** `index.html` for them when that build can differ from the **active GCS** tree (e.g. frontend-only deploy + conditional MIG skip): the HTML would reference new chunk names while `/assets/*` still maps to GCS for the older SHA → **404 on JS**.

**Current behavior:** In **prod**, [`ApplicationController`](../../backend/src/main/java/com/odde/doughnut/controllers/ApplicationController.java) handles `/d/**` and `/n**` by HTTP‑fetching the public site root (`doughnut.spa-public-base-url`, overridable with `DOUGHNUT_SPA_PUBLIC_BASE_URL`) so the shell matches what `/` gets from GCS. **Non‑prod** keeps forwarding to classpath `index.html`.

**Alternative (infra-only):** Route deep links to the backend bucket and use **Cloud CDN custom error response** (404 → `index.html` in the active prefix), with strict path rules so `/api/*` never hits the bucket.

---

## Backend bucket, IAM, CDN

1. **Backend bucket** points at **`GCS_FRONTEND_BUCKET`** (the bucket CI uploads SPA + CLI into), not at the deploy bucket (`GCS_BUCKET`).
2. **LB service account** needs `storage.objectViewer` on that bucket (see [Backend buckets](https://cloud.google.com/load-balancing/docs/backend-bucket) IAM).
3. **Cloud CDN** (optional but typical): enable on the backend bucket service; tune **cache mode** and **TTLs**:
   - Hashed assets under `/assets/` → long cache safe.
   - `index.html` → short TTL or cache bypass so new deployments are visible quickly after you change `<ACTIVE_SHA>` or error-page behavior.

Set **Cache-Control** at upload time if you want CDN to respect origins; today [`upload-frontend-static-to-gcs.sh`](../../infra/gcp/scripts/upload-frontend-static-to-gcs.sh) uses `gsutil rsync` without custom metadata—add `-h "Cache-Control:…"` if you need stronger CDN alignment.

---

## CORS on the bucket

With **single hostname** and everything same-origin through the LB, bucket CORS is usually **unnecessary** for the main app. If you ever serve static from a **second** origin, define CORS and cookies deliberately.

---

## TLS, DNS

Attach the existing managed SSL certificate and forwarding rule to the URL map. No change to the **public hostname** requirement beyond adding path rules and an extra backend.

---

## Who can change this

Treat URL map / backend edits as **production infrastructure**: restrict to project admins or the same process as MIG and jar deploy. Prefer **IaC or scripted updates** so the active `<ACTIVE_SHA>` is reviewable (PR or change ticket).

### GitHub Actions deploy SA and `url-maps import`

CI runs [`apply-doughnut-app-service-url-map.sh`](../../infra/gcp/scripts/apply-doughnut-app-service-url-map.sh), which calls `gcloud compute url-maps import`. Grant the deploy service account (e.g. `doughnut-ci-gcp-deploy-svc-acc@…`) **`roles/compute.loadBalancerAdmin`** on the **project**: that single role includes **`compute.urlMaps.update`** plus **`compute.backendServices.use`** and **`compute.backendBuckets.use`** on the backends referenced by the map (separate per-resource bindings are not required).

If Deploy returns **403** mentioning `compute.urlMaps.update` or `compute.backendServices.use`, run [`grant-doughnut-ci-deploy-url-map-iam.sh`](../../infra/gcp/scripts/grant-doughnut-ci-deploy-url-map-iam.sh) (or add the same project binding manually). Override SA with `CI_DEPLOY_GCP_SA=…@….iam.gserviceaccount.com` if needed.

---

## Smoke checks (after a change)

From a shell (replace `https://your-prod-host`):

```bash
BASE=https://your-prod-host
curl -sfI "$BASE/" | head -5
curl -sfI "$BASE/assets/" | head -5   # may 403/404 listing; instead hit a real asset URL from View Source
curl -sf "$BASE/api/healthcheck"
curl -sfI "$BASE/doughnut-cli-latest/doughnut" | head -5
```

Confirm: **HTML/JS** responses are from the **expected** revision (e.g. unique hash in a chunk filename or build metadata if you log it), **healthcheck** returns OK from the MIG, login and **attachments** still work, and the **CLI** URL returns the bundle from **`GCS_FRONTEND_BUCKET`** (via the LB), not the deploy bucket.

---

## Spring Boot jar (no SPA or CLI inside)

The **deployable boot jar** does **not** embed `classpath:/static/**` (no SPA, no CLI). **Conditional MIG skip** compares only the **jar** hash to `deploy/last-successful-deploy.json`. Each green **`Package-artifacts`** run uploads **frontend** and **CLI** to **`GCS_FRONTEND_BUCKET`**; jars and the deploy record stay on **`GCS_BUCKET`**.
