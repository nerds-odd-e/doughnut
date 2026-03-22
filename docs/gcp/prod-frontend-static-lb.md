# Production: SPA static from GCS (LB + CDN), API from MIG

Runbook for **phase 5** of the GCS frontend plan: one browser-facing hostname, HTTPS load balancer path rules, backend bucket (and optional Cloud CDN) for the Vue build, managed instance group (MIG) for Spring Boot.

**Related:** CI uploads each commit’s tree to `gs://<GCS_BUCKET>/frontend/<GITHUB_SHA>/` ([`infra/gcp/scripts/upload-frontend-static-to-gcs.sh`](../../infra/gcp/scripts/upload-frontend-static-to-gcs.sh)). Bucket name in CI is `GCS_BUCKET` (e.g. `dough-01` in [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)).

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
| GCS bucket (CI + LB) | `dough-01` |
| Backend bucket (CDN on) | `doughnut-frontend-backend-bucket` → `dough-01` |
| URL map (HTTPS) | `doughnut-app-service-map` — YAML in repo: [`infra/gcp/url-maps/doughnut-app-service-map.yaml`](../../infra/gcp/url-maps/doughnut-app-service-map.yaml) |
| HTTPS target proxy | `doughnut-app-service-map-target-proxy-2` → above URL map |
| MIG backend service | `doughnut-app-service` (default path matcher + API traffic) |
| GCS read for LB + CDN | `gs://dough-01` has `roles/storage.objectViewer` for `allUsers` so the backend bucket (and Cloud CDN cache fills) can read objects. The default Compute SA and `service-220715781008@compute-system.iam.gserviceaccount.com` are also granted `objectViewer`; with **Cloud CDN** enabled, Google’s docs require `service-<PROJECT_NUMBER>@cloud-cdn-fill.iam.gserviceaccount.com` (created after the first signed URL key on a backend bucket in the project) when the bucket is not publicly readable. |

### Why the bucket is world-readable today (and what to do instead)

Serving static through a **backend bucket** on the **same** multi-purpose bucket as jars (`backend_app_jar/`), `deploy/`, `db_backups/`, etc. means **`allUsers` objectViewer exposes every object prefix** to unauthenticated reads if someone knows or guesses object names. That is acceptable only as a **stopgap**.

**Preferred:** Create a **frontend-only** GCS bucket (only trees under `frontend/<GITHUB_SHA>/`), make that bucket publicly readable (or keep it private and grant only the Compute + CDN fill principals), point `doughnut-frontend-backend-bucket` at that bucket, set CI to upload static there (separate env from the deploy jar bucket), then **remove** `allUsers` from `dough-01`.

**Org constraint:** If your org forbids `allUsers` with IAM **conditions** (`PublicResourceAllowConditionCheck`), you cannot scope “public read only under `frontend/`” on a single bucket via conditional bindings; a dedicated bucket avoids that.

**Promote a new frontend build:** confirm `gs://dough-01/frontend/<GITHUB_SHA>/` exists, update every `pathPrefixRewrite` in the YAML to that SHA (and add path rules for any **new root-level** static files next to `index.html`, same as `odd-e.ico` / `odd-e.png` today), then:

```bash
gcloud config set project carbon-syntax-298809
gcloud compute url-maps import doughnut-app-service-map \
  --source=infra/gcp/url-maps/doughnut-app-service-map.yaml --global --quiet
```

HTTP forwarding (`doughnut-app-web-map-http`) is unchanged and does not use this map.

---

## Choosing the **active** frontend revision

CI writes **immutable** prefixes: `frontend/<GITHUB_SHA>/`. The load balancer must map browser paths into **one** of those prefixes.

**Recommended:** URL map **path prefix rewrite** for routes that go to the backend bucket: rewrite incoming paths so the backend bucket sees `frontend/<ACTIVE_SHA>/…` (replace `<ACTIVE_SHA>` when you promote a build).

- **Promote:** Update the URL map (or the IaC that owns it) so the rewrite uses the new full commit SHA after you confirm the object prefix exists in GCS.
- **Rollback:** Point the rewrite back to a previous `<GITHUB_SHA>` that still exists under `frontend/`.

**Alternatives (tradeoffs):**

- **`latest` JSON in GCS:** Useful as a human-readable pointer; the HTTPS LB does not read it natively—you would still need Terraform/gcloud or a small service to apply it to the URL map.
- **Separate static hostname (e.g. `cdn.`)** — possible, but then you must design **CORS**, **cookies** (`Domain` / `SameSite`), and **OAuth redirect URIs** explicitly. Prefer single hostname unless you have a strong reason.

---

## Path routing (what must hit the MIG)

Order **specific** paths **before** the catch-all that sends traffic to GCS.

The **canonical backend path list** used by the local prod-topology proxy and by CI checks against this URL map lives in [`infra/gcp/path-routing/backend-path-hints.json`](../../infra/gcp/path-routing/backend-path-hints.json) (`pnpm validate:path-routing`).

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

**Default service:** Often still the MIG during migration; once static is fully on GCS + CDN, the **default** may be the backend bucket, provided SPA deep links are handled (below).

---

## Path routing (what hits GCS)

Built app uses Vite `base: '/'` ([`frontend/vite.config.ts`](../../frontend/vite.config.ts)): **root-relative** asset URLs (`/assets/…`).

Minimum to send to the **backend bucket** (with prefix rewrite to `frontend/<ACTIVE_SHA>/`):

- `/assets/*` — hashed JS/CSS/media
- `/` and `/index.html` — SPA entry
- Any other **real files** present at bucket root for that build (e.g. favicon if present)

---

## SPA deep links (`/d/…`, `/n…`)

There is **no** object per client route under `frontend/<sha>/`. The URL map still sends these paths to the **MIG** (path matcher default). Spring must not serve **classpath** `index.html` for them when that build can differ from the **active GCS** tree (e.g. frontend-only deploy + conditional MIG skip): the HTML would reference new chunk names while `/assets/*` still maps to GCS for the older SHA → **404 on JS**.

**Current behavior:** In **prod**, [`ApplicationController`](../../backend/src/main/java/com/odde/doughnut/controllers/ApplicationController.java) handles `/d/**` and `/n**` by HTTP‑fetching the public site root (`doughnut.spa-public-base-url`, overridable with `DOUGHNUT_SPA_PUBLIC_BASE_URL`) so the shell matches what `/` gets from GCS. **Non‑prod** keeps forwarding to classpath `index.html`.

**Alternative (infra-only):** Route deep links to the backend bucket and use **Cloud CDN custom error response** (404 → `index.html` in the active prefix), with strict path rules so `/api/*` never hits the bucket.

---

## Backend bucket, IAM, CDN

1. **Backend bucket** points at the **same** GCS bucket CI uses (`GCS_BUCKET`), or a dedicated bucket synced from it—team choice.
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

---

## Smoke checks (after a change)

From a shell (replace `https://your-prod-host`):

```bash
BASE=https://your-prod-host
curl -sfI "$BASE/" | head -5
curl -sfI "$BASE/assets/" | head -5   # may 403/404 listing; instead hit a real asset URL from View Source
curl -sf "$BASE/api/healthcheck"
```

Confirm: **HTML/JS** responses are from the **expected** revision (e.g. unique hash in a chunk filename or build metadata if you log it), **healthcheck** returns OK from the MIG, login and **attachments** still work.

---

## Spring Boot / jar (this phase vs phase 7)

For **phase 5**, it is acceptable for the **jar to still contain** the same static tree as today: the LB should steer browser traffic to GCS first. **Phase 7** (optional) can remove embedded SPA from the prod jar to shrink deploy surface; conditional MIG skip remains **jar-hash** based only.
