# Review of `ongoing/conditional-deploy-gcs-frontend.md`

Informal review note. Delete when the follow-up work is done.

## Added in history

- The plan file was added in commit `c4489c6d8b707c8ad6fe15488d8304b989d2ef77` on `2026-03-22`.

## Evolution summary

- `4a75bd169169965fc81933cf0a28c96ecdddad67`: phase 1 reproducible jar settings landed in `backend/build.gradle`.
- `a571e4f8f1a430e8923780226852c0f32557cfe8` + `80125442712b439909b584b7b1c7ba057041ffc1`: conditional backend deploy script and the reproducibility script landed.
- `e761a326e955b362ec6ba173d23f21299b80d0c1`: force deploy by commit message and the first script test landed.
- `9c3225c25eaab3d34ef5e0f1dd0a7487629352aa`: frontend static upload to GCS landed.
- `086270118d29ef4dd8011dc3cca6ca318258d3b2` through `aa698d7d91ea5a2ec9c494102f99a152a3f2fe8c`: fake LB / prod-topology proxy and the Vite-port split landed.
- `a3933c57734fb448278637fc8792ce872e7399f7`: shared backend path hints and the URL-map validator landed.
- `b27b9361b592161c83f07d74ae74c0e045a6721c`: SPA build output moved to `frontend/dist`.
- `2de5826aeefea2f5618488695ffda77adf178298`: CLI binary moved out of the deploy jar and into GCS/LB flow.

## Bugs and reliability risks

### 1. Phase 8 does not fully deliver the claimed drift protection

- `scripts/validate-url-map-static-vs-backend-hints.mjs` only checks one direction: "backend paths must not be swallowed by static rules".
- It does **not** check the reverse: "every static path used by the built frontend must be routed to GCS in prod".
- `e2e_test/e2e-prod-topology-proxy.mjs` serves any file that exists under `frontend/dist`, but `infra/gcp/url-maps/doughnut-app-service-map.yaml` hardcodes only `/`, `/index.html`, `/assets/*`, `odd-e.ico`, `odd-e.png`, and `/doughnut-cli-latest/*`.
- Result: adding a new root-level static file can work locally and in CI while silently failing in prod. Example shape: `/site.webmanifest`, another icon, or any future root asset.
- This is the biggest gap between the phase-8 claim and the current implementation.

### 2. Frontend release is still manual after CI upload

- CI uploads `frontend/<GITHUB_SHA>/`, but the active revision is still chosen by manually editing `infra/gcp/url-maps/doughnut-app-service-map.yaml` and re-importing it.
- So a green pipeline does **not** by itself publish the new frontend to users.
- That means the phase-4/5/10 story is only half automated: artifacts are published automatically, but promotion is still manual.
- This is also why the hardcoded SHA in the URL map is a second source of truth.

### 3. Documented current GCS setup is too broad

- `docs/gcp/prod-frontend-static-lb.md` explicitly documents that the same bucket currently holds frontend assets, backend jars, deploy records, and other objects, and that `allUsers` object viewer is used as a stopgap.
- That makes the bucket layout operationally convenient, but it is not cohesive and it widens accidental exposure.
- The document already points to the better shape: a frontend-only bucket.
- This is more than a "nice to have"; it is a real reliability and security boundary problem.

## Dead code and obsolete behavior

### 1. `cli:bundle-and-copy` is now a misleading compatibility alias

- `package.json` now defines `cli:bundle-and-copy` as just `pnpm cli:bundle`.
- The name still suggests that it copies something, but phase 10 moved the real copy behavior into Gradle `copyCliBundle` for `bootRun`.
- Keeping both names increases mental overhead without adding behavior.

### 2. Local backend SPA fallback is an obsolete leftover

- `e2e_test/e2e-prod-topology-proxy.mjs` has a special case for `/users/identify` because the non-prod Spring behavior still assumes classpath `index.html`, but phase 9 moved the SPA to `frontend/dist`.
- `backend/src/main/resources/static/` is now empty, so the old "Spring serves the SPA shell locally" model is gone.
- The proxy workaround is practical, but it is also a sign that some pre-phase-9 backend behavior is now stale.

## Incomplete tasks in the plan

### 1. "One source of truth" is only partially complete

- Phase 8 says fake LB routing and prod routing should not drift silently.
- The backend-path hints are a good start, but only backend-bound routes are canonicalized.
- Static-path coverage, active SHA selection, and rewrite generation are still outside that source of truth.

### 2. Frontend promotion is not integrated into the delivery path

- The plan goal says frontend-only changes should not require a backend MIG rollout.
- That part is true.
- But frontend-only changes still require a manual URL-map promotion step, so the delivery path is not yet simple or fully reliable.

### 3. Deep-link production behavior is under-tested

- The current prod deep-link strategy depends on `ApplicationController` fetching the shell from the public origin when `/d/**` or `/n**` hits the MIG.
- I did not find focused tests around this behavior.
- That leaves an important cross-system path unprotected: backend -> public origin -> active frontend shell.

## Places for simplification

### 1. Remove the `cli:bundle-and-copy` alias

- Call `pnpm cli:bundle` directly from `bundle:all`.
- Keep the actual local-copy concern in one place: Gradle `copyCliBundle`.

### 2. Stop special-casing `/users/identify` in the fake LB

- The special case exists because the backend's non-prod behavior still points at a classpath SPA that no longer exists.
- A simpler shape is: local browser-facing routes always come from Vite/static, backend routes always come from Spring, with no one-off escape hatch.

### 3. Reduce duplicated active-SHA rewrites

- The same frontend SHA appears multiple times in `infra/gcp/url-maps/doughnut-app-service-map.yaml`.
- That file should be generated from a smaller source, or templated by one variable, instead of being hand-edited in several places.

## Places for improvement

### 1. Make static routing validation bidirectional

- Keep the current backend-path check.
- Add a second check that reads the built frontend output and verifies that every root-level asset that must be public is routed to the backend bucket in prod.
- This is the missing half of phase 8.

### 2. Move frontend promotion into one explicit mechanism

- The current system has three separate concerns:
  - artifact upload
  - active revision selection
  - URL-map application
- That should become one reviewed, explicit flow, ideally with one input: the active frontend SHA.

### 3. Add a dedicated frontend bucket

- It improves cohesion.
- It removes the need to mix public static hosting concerns with backend deploy storage concerns.
- It makes the documentation and IAM shape easier for humans and AI to understand.

### 4. Add focused tests for prod deep-link shell serving

- Test that prod `/d/...` and `/n...` return the public shell path, not classpath static.
- Test the failure mode too, or at least make it explicit.

## Further improvement opportunities

- Generate `infra/gcp/url-maps/doughnut-app-service-map.yaml` from committed routing inputs instead of hand-editing it.
- Consider making the frontend "active revision" a tiny committed config file or deploy parameter rather than repeated literal SHA rewrites.
- Consider validating that the active frontend SHA in the URL map actually exists in GCS before promotion.
- Consider making the fake LB use the same generated static-path list as prod, not just the backend-path hints.

## Overall assessment

- The work moved the system in a good direction: reproducible backend deploys, conditional MIG rollouts, frontend artifacts outside the jar, and a clearer local/prod topology.
- The main remaining weakness is that the "single source of truth" promise is only half implemented. Backend paths are centralized; static routing and active frontend revision are not.
- The next best step is to finish that cohesion: one active frontend SHA, one routing source, one validation story.
