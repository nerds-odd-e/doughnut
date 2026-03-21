# Conditional deploy, reproducible jar, GCS frontend, E2E parity

Informal plan; delete or archive when done.

## Goals

1. **Skip GCP deploy** when the deployable backend jar is **byte-identical** to the last **successful** production deploy, using a **recorded hash** (not only “GCS object exists”).
2. **Reproducible builds** so the comparison is trustworthy (same sources → same jar hash on CI).
3. **Guardrail:** If a file **`force_deployment`** exists at the **repository root**, always run the full deploy path (upload + MIG rolling replace) when the workflow would otherwise deploy—subject to existing job success/`needs` (same as today).
4. **Later:** Ship the **frontend static assets from GCS** (or CDN in front of it) so **frontend-only** changes do not require a **backend MIG** rollout in prod.
5. **E2E:** After (4), tests should **mimic CDN-only static hosting** (browser loads UI from a static origin separate from the API), without depending on Google infrastructure in CI.

---

## Phase 1 — Reproducible fat jar

**Outcome:** Building the same commit twice in CI (or locally in the nix env) yields the **same SHA-256** for `doughnut-0.0.1-SNAPSHOT.jar` (or the agreed deploy artifact name).

**Scope:** Gradle/Spring Boot packaging only (the jar that includes embedded static assets today). Document any intentional non-reproducible inputs (timestamps, property files) and eliminate or normalize them.

**Check:** Add a minimal **observable** check in CI for this phase—e.g. a job step that builds twice and asserts equal `sha256sum` of the jar, or a dedicated script invoked from the workflow. Prefer one stable entry point (Gradle task or script) so the rule does not rot.

**User/system value:** Trustworthy artifact hashing for all later phases.

---

## Phase 2 — Last successful deploy record + conditional deploy

**Outcome:** On `main` green pipeline, **if** the new jar’s hash **equals** the hash in the **last successful deploy record**, **skip** uploading the jar to GCS **and skip** MIG rolling replace. Otherwise perform upload + replace as today, then **update the record** only after deploy steps succeed.

**Record shape (intent, not prescription):** Store at least `sha256` and ideally `git_sha` + timestamp in a small object in GCS (e.g. next to the jar or a dedicated `deploy/` prefix), or another single source of truth the workflow can read/write with the same credentials. The record represents **“what we intend prod to run after a good deploy”**, not “hash of whatever is in the bucket” alone—so a failed replace does not advance the record.

**Edge case (document in implementation):** If the bucket already has jar A but a previous **replace never finished**, comparing only to the record (updated only on success) avoids falsely “skipping” when you still need a replace. Optionally add a manual “bump record without upload” playbook for rare recovery.

**User/system value:** Fewer unnecessary rollouts and less blast radius when nothing deployable changed.

---

## Phase 3 — `force_deployment` guardrail

**Outcome:** If **`force_deployment`** exists at the **repo root** on the commit being built, the workflow **always** performs the full deploy path (upload + rolling replace) when that job runs, **even when** the jar hash matches the last successful deploy record.

**Removal:** Deleting the file in a follow-up commit returns to normal hash-based skipping.

**User/system value:** Escape hatch for “must roll VMs” without a code change to the jar (or to recover from drift).

---

## Phase 4 — Frontend publish to GCS (versioned or content-addressed)

**Outcome:** CI produces a **frontend build** and uploads it to GCS under a **predictable layout** (e.g. version or content hash prefix) suitable for a CDN or load balancer to serve. **Prod** traffic for static assets hits that location; **API** still hits the backend.

**Dependencies:** Decide URL layout (`base` / asset paths), cache headers, and whether HTML is immutable per release or updated in place. Align with how the browser resolves `index.html` vs chunk URLs.

**User/system value:** Frontend releases no longer require baking the same bundle into the jar for prod (prep for decoupling MIG from FE changes).

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
| 1 | CI (or script): reproducibility assertion on the jar. |
| 2 | Workflow-level: can use dry-run or a test bucket in a fork—prefer **observable** checks (record read/write, skip path) without mocking GCP in unit tests; keep one place that owns the behavior. |
| 3 | Minimal: document + one manual run with file present/absent, or a workflow test in a branch. |
| 4–5 | Smoke after deploy; optional scripted check that `index.html` and assets load from the new origin. |
| 6 | **E2E** is the main proof: existing features still pass with static+API split; add or extend one scenario that fails if assets load from the wrong origin or API base is wrong. |
| 7 | E2E still green; deploy skip behavior validated for FE-only vs BE-only commits. |

---

## Deploy gate

After phases that change prod behavior (especially 2, 5, 7), follow your usual **commit → CD → verify** before stacking risky follow-ups.

---

## Open questions (resolve before or during early phases)

- Where exactly to store **last successful deploy** metadata (GCS object, Firestore, etc.) and IAM for read vs write.
- Whether **rolling replace** must run when only **startup script / template** changes but jar hash is unchanged (`force_deployment` covers manual cases; template changes might need path filters or always-replace policy).
- **Cookie/session** domain when static and API hosts differ (subdomain vs path-based).
