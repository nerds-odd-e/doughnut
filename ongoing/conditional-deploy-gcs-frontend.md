# Conditional deploy, reproducible jar, GCS frontend, E2E parity

Informal plan; delete or archive when done.

## Goals

1. **Skip GCP deploy** when the deployable backend jar is **byte-identical** to the last **successful** production deploy, using a **recorded hash** (not only “GCS object exists”).
2. **Reproducible builds** so the comparison is trustworthy (same sources → same jar hash for a given build environment).
3. **Guardrail (implemented):** If the **`main`** commit CI deploys (`GITHUB_SHA`) has a message containing **`force-deployment: true`** (subject or body; optional spaces around `:`), always run the full deploy path when the job runs, **even when** the jar hash matches the last successful deploy record—subject to existing job success/`needs`. See `docs/gcp/conditional-backend-deploy.md`.
4. **Later:** Ship the **frontend static assets from GCS** (or CDN in front of it) so **frontend-only** changes do not require a **backend MIG** rollout in prod.
5. **E2E:** After (4), tests should **mimic CDN-only static hosting** (browser loads UI from a static origin separate from the API), without depending on Google infrastructure in CI.

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

**Not in this phase:** Prod LB/CDN routing (phase 5), cache headers, `latest` pointer object, conditional skip when static unchanged.

---

## Phase 5 — Prod wiring: static from GCS/CDN, API from backend

**Outcome:** Production users load the SPA from the static hosting path and call the API on the existing backend host (or separate host), with **CORS/cookies/same-site** and **OAuth redirect URIs** still correct.

**Scope:** GCP load balancer URL map, backend bucket, or Cloud CDN—whatever matches your infra; minimal Spring changes (e.g. stop serving static from the jar for prod, or redirect `/` to the static origin—pick one coherent story).

**User/system value:** Backend MIG **not** required for frontend-only changes.

---

## Phase 6 — E2E mimics CDN-only hosting

**Outcome:** Cypress SUT uses **two origins** (or two ports): **API** from the existing Spring app, **static UI** from a **local** static server that serves the **same built assets** as phase 4 (no GCS in CI). The browser behavior matches prod: **HTML/JS/CSS not served from the jar**.

**Research / design (this phase):**

- Vite (or your build) **`base`** and **asset paths** in `index.html` must work when the app is opened from the static origin while API calls go to the backend origin (proxy in dev is different from prod—E2E should mirror **prod** shape).
- Prefer **root-relative asset URLs** in built HTML (`/assets/...`) and serve the static tree under `/` on a local server so no hardcoded Google URLs appear in the artifact.
- If prod HTML must contain an absolute CDN base, consider **one build** with `base` from env at build time: CI would set **local static base** for E2E and **CDN base** for prod, or use a tiny post-build substitution—**investigate which option matches “reproducible + simple”** before coding.
- **Cypress `baseUrl` / `start`:** likely `run-p` static server + backend + mountebank; health checks wait on both if needed.

**User/system value:** E2E catches CORS, wrong API base URL, cookie scope, and asset path bugs that only appear when UI is not embedded in the jar.

---

## Phase 7 — Jar slimming and deploy skip granularity (optional cleanup)

**Outcome:** Backend jar for prod **excludes** embedded SPA (or includes only a minimal stub), **conditional deploy** compares **backend-only** artifact hash where applicable, and **frontend-only** commits **skip MIG** while still publishing static (phases 4–5).

**Note:** Hash comparison might be **one hash for backend jar** plus optional **separate record for static bundle** if you still want “no-op” static uploads—product decision.

**User/system value:** Fastest feedback loop for frontend changes; smallest backend deploy surface.

---

## Testing strategy (per planning.mdc)

| Phase | Tests |
|-------|--------|
| 1 | `backend/scripts/boot-jar-reproducible.sh` (Gradle contract + real double `bootJar`). |
| 2 | Workflow-level: can use dry-run or a test bucket in a fork—prefer **observable** checks (record read/write, skip path) without mocking GCP in unit tests; keep one place that owns the behavior. |
| 3 | `scripts/test/deploy-backend-jar-to-gcp-mig.sh.test` (`test_force_full_deploy_when_record_matches_jar_hash`); docs `docs/gcp/conditional-backend-deploy.md`. |
| 4 | `scripts/test/upload-frontend-static-to-gcs.sh.test`; smoke: objects under `gs://…/frontend/<sha>/`. |
| 5 | Smoke after deploy; optional scripted check that `index.html` and assets load from the new origin. |
| 6 | **E2E** is the main proof: existing features still pass with static+API split; add or extend one scenario that fails if assets load from the wrong origin or API base is wrong. |
| 7 | E2E still green; deploy skip behavior validated for FE-only vs BE-only commits. |

---

## Deploy gate

After phases that change prod behavior (especially 2, 5, 7), follow your usual **commit → CD → verify** before stacking risky follow-ups.

---

## Open questions (resolve before or during early phases)

- Where exactly to store **last successful deploy** metadata (GCS object, Firestore, etc.) and IAM for read vs write.
- Whether **rolling replace** must run when only **startup script / template** changes but jar hash is unchanged (commit-message force covers intentional cases; template changes might need path filters or always-replace policy).
- **Cookie/session** domain when static and API hosts differ (subdomain vs path-based).
