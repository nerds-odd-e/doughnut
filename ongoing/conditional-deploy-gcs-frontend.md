# Conditional backend deploy, reproducible jar, GCS frontend, E2E parity

Informal plan; delete or archive when done.

## Goals

1. **Skip backend GCP deploy** (jar upload + MIG replace) when the deployable backend jar is **byte-identical** to the last **successful** production deploy, using a **recorded hash** (not only “GCS object exists”). **Frontend static upload to GCS is not conditional**—each green pipeline publishes static under the commit prefix; no skip-by-hash for the frontend artifact.
2. **Reproducible builds** so the comparison is trustworthy (same sources → same jar hash for a given build environment).
3. **Guardrail (implemented):** If the **`main`** commit CI deploys (`GITHUB_SHA`) has a message containing **`force-deployment: true`** (subject or body; optional spaces around `:`), always run the full deploy path when the job runs, **even when** the jar hash matches the last successful deploy record—subject to existing job success/`needs`. See `docs/gcp/conditional-backend-deploy.md`.
4. **Later:** Ship the **frontend static assets from GCS** (or CDN in front of it) so **frontend-only** changes do not require a **backend MIG** rollout in prod.
5. **E2E:** After (4), tests should mirror **prod routing**: the UI is **not** served from the jar (local static server and/or proxy), and how the browser reaches **static vs API** matches prod—whether that is **one hostname** (path-based LB) or a deliberate **split-origin** layout—without using real GCS in CI.

---

## Phase 1 — Reproducible fat jar

**Outcome:** Building the same commit twice (e.g. locally in the nix env) yields the **same SHA-256** for `doughnut-0.0.1-SNAPSHOT.jar` (or the agreed deploy artifact name).

**Scope:** Gradle/Spring Boot packaging only (the jar that includes embedded static assets today). Document any intentional non-reproducible inputs (timestamps, property files) and eliminate or normalize them.

**User/system value:** Trustworthy artifact hashing for all later phases.

### Phase 1 implementation (done)

- **Gradle:** `JavaCompile` uses UTF-8; all `AbstractArchiveTask` tasks (including `bootJar`) use `preserveFileTimestamps = false` so ZIP entry times do not depend on filesystem metadata. (`reproducibleFileOrdering` is not available on Spring Boot’s `BootJar` task type, so it is not set globally.)
- **Regression test:** `backend/scripts/boot-jar-reproducible.sh` — asserts those Gradle lines stay in `backend/build.gradle`, then runs two `clean bootJar` passes with `--no-build-cache`, pinned CLI bundle env, and a short sleep between passes; compares SHA-256. CI runs it from the **Backend unit tests** job after `pnpm bundle:all` and a prod `build`, via `BOOT_JAR_REPRO_SKIP_BUNDLE=1`.

**Inputs that can change the jar without a source change (documented):**

- **CLI bundle** (`pnpm cli:bundle`): `--define` inlines `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `CLI_VERSION` from the environment at build time. Same commit on CI with the same env yields the same bytes; a local `.env.local` or different `CLI_VERSION` can differ from CI.

---

## Phase 2 — Last successful deploy record + conditional deploy

**Outcome:** On `main` green pipeline, **if** the new jar’s hash **equals** the hash in the **last successful deploy record**, **skip** uploading the jar to GCS **and skip** MIG rolling replace. Otherwise perform upload + replace as today, then **update the record** only after deploy steps succeed.

**Record shape (intent, not prescription):** Store at least `sha256` and ideally `git_sha` + timestamp in a small object in GCS (e.g. next to the jar or a dedicated `deploy/` prefix), or another single source of truth the workflow can read/write with the same credentials. The record represents **“what we intend prod to run after a good deploy”**, not “hash of whatever is in the bucket” alone—so a failed replace does not advance the record.

**Edge case (document in implementation):** If the bucket already has jar A but a previous **replace never finished**, comparing only to the record (updated only on success) avoids falsely “skipping” when you still need a replace. Optionally add a manual “bump record without upload” playbook for rare recovery.

**User/system value:** Fewer unnecessary rollouts and less blast radius when nothing deployable changed.

### Phase 2 implementation (done)

- **Workflow:** `.github/workflows/ci.yml` `Deploy` job runs `infra/gcp/scripts/deploy-backend-jar-to-gcp-mig.sh` after downloading the packaged jar artifact.
- **Record:** `gs://<GCS_BUCKET>/deploy/last-successful-deploy.json` — JSON with `sha256`, `git_sha`, `recorded_at` (ISO8601 UTC). Written **only** after `gsutil cp` of the jar and `perform-rolling-replace-app-mig.sh` both succeed.
- **Skip path:** If the jar’s SHA-256 equals `sha256` in the record, the script exits without uploading or rolling replace (record unchanged).
- **First run / missing record:** No object or unreadable JSON → full deploy; record created on success.
- **Recovery:** If prod is already on the intended jar but the record is missing or wrong, you can upload a correct JSON to `deploy/last-successful-deploy.json` (same `sha256` as the jar you intend) to re-enable skipping without a rollout—or delete the object to force the next green pipeline to deploy.

---

## Phase 3 — Commit-message force deploy

**Outcome:** On the **`Deploy` job** (after the same `needs` as today), if the triggering **`push`** indicates a force, run the **full** path (upload + rolling replace) **even when** the jar SHA-256 equals the last successful deploy record. Otherwise keep phase 2 skip behavior.

**Token (concrete):** Treat the deploy as forced when the full message (subject + body) of at least one relevant commit matches a documented pattern, e.g. substring **`force-deployment: true`** (or a small regex allowing optional spaces around `:`). Document exact rules in the runbook; optional: **`workflow_dispatch`** with a “force full deploy” input later for ad-hoc reruns without a commit.

**Where CI gets the message:** Prefer `github.event.head_commit.message` for the **tip** of the push, or `git log` over `before..after` after checkout if we choose to honor **every commit in the batch** (see caveats). Pass a boolean or `FORCE_FULL_DEPLOY=1` into `deploy-backend-jar-to-gcp-mig.sh` so the script only skips on hash match when force is off.

**Caveats (document for authors):**

- **Tip-only (simplest):** If only the **latest** commit on the push is inspected, a multi-commit push must put the token on the **last** commit, or force won’t apply.
- **Batch scan:** Scanning `github.event.commits` or `git log before..after` covers the whole push; note GitHub’s **`commits` array is capped at 20** — larger pushes need `git log`.
- **Merge style:** **Squash merge:** put the token in the squash title/description. **Merge commit:** token must appear in the **merge commit message** (default text often won’t include PR body). **Rebase merge:** token on the rebased tip commit message.
- **`head_commit`:** Rare push shapes leave it empty; treat as “no force” if missing.

**User/system value:** Escape hatch for “must roll VMs” without a jar change (or drift recovery), no repo-root sentinel file to add/remove.

### Phase 3 implementation (done)

- **Workflow:** `.github/workflows/ci.yml` `Deploy` job runs `git log -1 --format=%B "$GITHUB_SHA"`; if the message matches `force-deployment[[:space:]]*:[[:space:]]*true` (grep extended regex), it sets `FORCE_FULL_DEPLOY=1` before `deploy-backend-jar-to-gcp-mig.sh`.
- **Script:** `infra/gcp/scripts/deploy-backend-jar-to-gcp-mig.sh` — when the jar hash equals the record, skips unless `FORCE_FULL_DEPLOY=1`.
- **Tests:** `scripts/test/deploy-backend-jar-to-gcp-mig.sh.test` — `test_force_full_deploy_when_record_matches_jar_hash`.
- **Docs:** `docs/gcp/conditional-backend-deploy.md`; overview link from `docs/gcp/prod_env.md` §5.

---

## Phase 4 — Frontend publish to GCS (versioned or content-addressed)

**Outcome:** CI produces a **frontend build** and uploads it to GCS under a **predictable layout** (e.g. version or content hash prefix) suitable for a CDN or load balancer to serve. **Prod** traffic for static assets hits that location; **API** still hits the backend.

**Dependencies:** Decide URL layout (`base` / asset paths), cache headers, and whether HTML is immutable per release or updated in place. Align with how the browser resolves `index.html` vs chunk URLs.

**User/system value:** Frontend releases no longer require baking the same bundle into the jar for prod (prep for decoupling MIG from FE changes).

### Phase 4 implementation (done)

- **Layout:** `gs://<GCS_BUCKET>/frontend/<GITHUB_SHA>/` — full tree from `backend/src/main/resources/static/` after `pnpm bundle:all` (same files the jar embeds).
- **Script:** `infra/gcp/scripts/upload-frontend-static-to-gcs.sh` — env: `GCS_BUCKET`, `GITHUB_SHA`; optional `FRONTEND_STATIC_DIR`.
- **CI:** `.github/workflows/ci.yml` `Package-artifacts` runs GCP auth then the script after the prod `build` (and `bundle:all`).
- **Tests:** `scripts/test/upload-frontend-static-to-gcs.sh.test`.

**Not in this phase:** Prod LB/CDN routing (phase 5), cache headers, `latest` pointer object. **No** conditional skip for frontend upload—every successful `Package-artifacts` run uploads static for that `GITHUB_SHA`.

---

## Phase 5 — Prod wiring: static from GCS/CDN, API from backend

**Default (conventional):** One **browser-facing hostname** (the existing prod domain). An **HTTPS load balancer URL map** sends **SPA routes** (e.g. `/`, `/assets/*`, and fallbacks for client-side routing) to **GCS** via a **backend bucket** and optional **Cloud CDN**; **API** and other backend-only paths go to the **MIG**. The browser sees a **single origin**—no Vue router change for that reason, and cookies/sessions stay straightforward. GCS remains storage; users should not depend on raw `https://storage.googleapis.com/...` in the address bar or as the primary script origin (avoid ES-module CORS and versioning pain; use your domain in front of the bucket).

**Alternative:** Separate hostnames for static vs API (e.g. `cdn.` + app/API)—then document **CORS, cookies (`SameSite` / `Domain`), and OAuth redirect URIs** deliberately.

**Scope:** LB/backend-bucket/CDN wiring for the chosen layout; minimal Spring changes (e.g. stop serving the SPA from the jar in prod, or only non-SPA paths). Decide how the **active** tree under `frontend/<sha>/` is selected (URL map / rewrite to current SHA, `latest` pointer, etc.) and align **cache headers** with that story.

**One-time GCP setup:** This phase includes **platform work** (URL map and path rules, backend bucket, optional CDN, IAM, DNS, TLS, bucket CORS if ever needed). **Document it in `docs/gcp/`**—extend `prod_env.md` and/or add a short runbook: resources, naming, how the active frontend revision is chosen, rollback, and who can change it—so setup is **not** tribal knowledge.

**User/system value:** Backend MIG **not** required for frontend-only changes (once routing and the active-build pointer are in place).

### Phase 5 implementation (done)

- **Docs:** [docs/gcp/prod-frontend-static-lb.md](docs/gcp/prod-frontend-static-lb.md) — single-hostname LB + backend bucket/CDN, path rules (MIG vs GCS), choosing and rolling back **active** `frontend/<GITHUB_SHA>/`, IAM/CDN/cache notes, SPA deep-link options (staged default-to-MIG vs CDN 404→`index.html`), smoke checks. [docs/gcp/prod_env.md](docs/gcp/prod_env.md) §6 links to it.
- **GCP (applied):** Backend bucket `doughnut-frontend-backend-bucket` on `dough-01` with CDN; URL map `doughnut-app-service-map` path matcher `doughnut-paths` — `/`, `/index.html`, `/assets/*`, favicon paths → GCS prefix `frontend/<sha>/` via rewrites; default → MIG `doughnut-app-service`. Repo YAML: [`infra/gcp/url-maps/doughnut-app-service-map.yaml`](infra/gcp/url-maps/doughnut-app-service-map.yaml).
- **Code:** No Spring change in this phase; jar may still embed static until phase 7.

---

## Phase 6 — E2E mimics prod static + API shape

**Outcome:** Cypress SUT serves the **same built assets** as phase 4 from **outside the Spring jar** (local static server; no GCS in CI). **API** stays the existing Spring app. Topology should **match prod**: if prod uses **one hostname + path routing** (phase 5 default), prefer a **single browser origin** in E2E too (e.g. a small local reverse proxy that merges static + API); if prod uses **split origins**, use two origins (or two ports) and the same API base URL / cookie rules as prod.

**Research / design (this phase):**

- Vite **`base`** and **asset paths** in `index.html` must work for the chosen E2E layout (dev proxy differs from prod—E2E should mirror **prod**).
- Prefer **root-relative asset URLs** in built HTML (`/assets/...`) and serve the static tree under `/` on the static side so artifacts stay free of hardcoded cloud URLs unless prod truly requires them.
- If prod needs an absolute asset base, align **one build** or a minimal post-build step with **reproducible + simple**—prefer deciding after phase 5’s URL shape is fixed.
- **Cypress `baseUrl` / `start`:** e.g. `run-p` static server + backend + mountebank; exact wiring follows prod.

**User/system value:** E2E catches wrong API base URL, cookie scope, asset paths, and (when prod is split-origin) CORS issues that only appear when the UI is not embedded in the jar.

### Phase 6 implementation (done)

- **Proxy:** [`e2e_test/e2e-prod-topology-proxy.mjs`](../e2e_test/e2e-prod-topology-proxy.mjs) listens on **5173**, serves `backend/src/main/resources/static` (after `pnpm frontend:build` / `bundle:all`), proxies `/api`, `/attachments`, `/logout`, `/users/*`, `/install`, OAuth paths, `/robots.txt` to Spring **9081**; unknown GET paths fall back to `index.html` (SPA, like prod default-to-MIG for `/d/**`).
- **Cypress CI / `pnpm test`:** `baseUrl` and `E2E_BACKEND_BASE_URL` → `http://localhost:5173`. `pnpm exec run-p` runs `e2e:prod-topology-proxy` with `backend:sut` + mountebank; `cy:run-on-sut` waits on **9081** and **5173**. GitHub Action E2E job starts the same proxy and `wait-on` includes both healthcheck and `http://127.0.0.1:5173/`.
- **`E2E_SPRING_BACKEND_URL`** in [`e2e_test/config/constants.ts`](../e2e_test/config/constants.ts) documents the raw Spring port for tools that must bypass the browser origin.
- **Local `cy:focus` / `pnpm sut`:** same **5173** as CI, but **Vite** (HMR) instead of the proxy—do not run both Vite and `e2e:prod-topology-proxy` on one machine at once (`e2e_test/config/local.ts` unchanged).

---

## Phase 7 — Jar slimming (optional cleanup)

**Outcome:** Backend jar for prod **excludes** embedded SPA (or includes only a minimal stub). **Conditional deploy** stays **backend-only** (jar hash vs last successful deploy record). **Frontend static** keeps **always uploading** on green pipeline (phases 4–5); **frontend-only** commits still **skip MIG** when the jar hash is unchanged, without any separate conditional frontend deploy mechanism.

**User/system value:** Smaller backend deploy surface; frontend changes still publish to GCS every time.

---

## Testing strategy (per planning.mdc)

| Phase | Tests |
|-------|--------|
| 1 | `backend/scripts/boot-jar-reproducible.sh` (Gradle contract + real double `bootJar`). |
| 2 | Workflow-level: can use dry-run or a test bucket in a fork—prefer **observable** checks (record read/write, skip path) without mocking GCP in unit tests; keep one place that owns the behavior. |
| 3 | `scripts/test/deploy-backend-jar-to-gcp-mig.sh.test` (`test_force_full_deploy_when_record_matches_jar_hash`); docs `docs/gcp/conditional-backend-deploy.md`. |
| 4 | `scripts/test/upload-frontend-static-to-gcs.sh.test`; smoke: objects under `gs://…/frontend/<sha>/`. |
| 5 | Smoke after deploy; **docs** in `docs/gcp/` for one-time platform setup; optional scripted check that SPA and assets load through the prod URL shape. |
| 6 | **E2E** is the main proof: `e2e-prod-topology-proxy` + Cypress `baseUrl` :5173; full suite in CI. |
| 7 | E2E still green; backend conditional MIG skip still correct when jar unchanged; frontend upload remains unconditional. |

---

## Deploy gate

After phases that change prod behavior (especially 2, 5, 7), follow your usual **commit → CD → verify** before stacking risky follow-ups. For **phase 5**, treat **GCP runbook updates in `docs/gcp/`** as part of “done,” not a follow-up.

---

## Open questions (resolve before or during early phases)

- Where exactly to store **last successful deploy** metadata (GCS object, Firestore, etc.) and IAM for read vs write.
- Whether **rolling replace** must run when only **startup script / template** changes but jar hash is unchanged (commit-message force covers intentional cases; template changes might need path filters or always-replace policy).
- **Cookie/session:** straightforward with **single hostname + path-based routing** (phase 5 default); if using **separate static vs API hosts**, document `Domain` / `SameSite` and OAuth redirects.
