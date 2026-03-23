# Review: conditional deploy / GCS frontend history

Review target: `ongoing/conditional-deploy-gcs-frontend.md`

## When the plan file was added

- Added in commit `c4489c6d8b707c8ad6fe15488d8304b989d2ef77` on `2026-03-22`.
- The implementation history for this plan then ran through the phase commits from `4a75bd169169965fc81933cf0a28c96ecdddad67` to `2de5826aeefea2f5618488695ffda77adf178298`.

## What I reviewed

- CI/deploy scripts for conditional backend deploy and GCS uploads.
- Current runtime wiring for local/proxy/backend behavior.
- Path-routing source-of-truth files and validator.
- Current docs and tests that claim the phases are done.

## Findings

### 1. Confirmed bug: non-prod login/root fallback still points to classpath `index.html`, but that file is gone

After phase 9 moved the SPA output to `frontend/dist` and phase 10 excluded `static/**` from the jar, the backend still returns `"/index.html"` for:

- `ApplicationController.home()`
- non-prod `ApplicationController.identify()`
- non-prod deep-link fallback

Evidence:

- `backend/src/main/java/com/odde/doughnut/controllers/ApplicationController.java`
- `backend/src/main/java/com/odde/doughnut/configs/MvcConfig.java`
- `backend/src/test/java/com/odde/doughnut/controllers/IndexControllerTests.java`
- there is no `backend/src/main/resources/index.html`
- there is no `backend/build/resources/main/index.html`
- runtime check with the existing `pnpm sut`:
  - `http://127.0.0.1:5173/users/identify` -> `404`
  - `http://127.0.0.1:9081/users/identify` -> `404`
  - `http://127.0.0.1:9081/` -> `404`

Impact:

- local/non-prod login entry is broken
- backend root is broken when reached directly
- docs still describe this path as working, which hides the regression

Why this slipped through:

- `IndexControllerTests` asserts the returned view name string, not observable HTTP behavior
- that structural test still passes even though the route is broken at runtime

Recommended simplification:

- stop using backend classpath `index.html` as a fallback at all
- make non-prod use the same app shell source as the real browser entry, or explicitly redirect to the proxy/static origin
- then delete the stale `MvcConfig` `/index.html` handler and replace the unit test with an observable controller/web test

### 2. Dead/broken duplicate path: local CLI serving still carries the old “copy into backend static” model

Phase 10 says the local fake LB serves `/doughnut-cli-latest/doughnut` from `cli/dist`, but the codebase still keeps the older backend-copy path around:

- `backend/build.gradle` task `copyCliBundle`
- `package.json` script `cli:bundle-and-copy`
- `e2e_test/config/cliE2eRepo.ts` function `bundleCliIntoBackendStatic()`
- `e2e_test/config/cliE2ePluginTasks.ts` tasks `bundleAndCopyCli*`
- E2E tag `@bundleAndCopyCliToBackendResources`
- `.cursor/rules/cli.mdc` still says “Build & copy to static”

This is not just naming debt. In the current local SUT:

- `http://127.0.0.1:5173/doughnut-cli-latest/doughnut` -> `200`
- `http://127.0.0.1:9081/doughnut-cli-latest/doughnut` -> `404`
- there is no `backend/build/resources/main/static/doughnut-cli-latest/doughnut`

So the proxy-served `cli/dist` path is the live one, while the backend-copy path is stale and misleading.

Recommended simplification:

- choose one local source of truth
- the simplest choice is: `cli/dist` + proxy only
- if that is the intended design, remove the backend copy task, rename the E2E helpers/tags, and update the CLI rule/doc text

### 3. Incomplete plan item: E2E still does not mirror prod for deep links

The plan goal says E2E should mirror prod routing, but the final plan text itself admits:

- “Deep links remain a separate prod vs local behavioral story.”

Current behavior confirms the mismatch:

- prod URL map default route sends unmatched paths to the MIG
- local proxy serves unmatched paths as SPA static
- runtime check:
  - `http://127.0.0.1:5173/d/some-note` -> `200`
  - `http://127.0.0.1:9081/d/some-note` -> `404`

That means CI/local E2E does not exercise the same deep-link path that prod uses.

Impact:

- a prod-only regression in deep-link routing can slip through even though phase 6/7/8 are marked done

Recommended next step:

- either move prod deep links fully to the static side
- or make the local fake LB mirror the prod MIG deep-link behavior exactly
- until then, the plan should not present the topology parity work as fully done

## Dead code / stale guidance

- `backend/src/main/java/com/odde/doughnut/configs/MvcConfig.java` caches `/index.html`, but current backend runtime no longer serves that file.
- `backend/src/test/java/com/odde/doughnut/controllers/IndexControllerTests.java` gives false confidence by asserting a string instead of a working route.
- `.cursor/rules/cli.mdc` still documents “Build & copy to static” and `backend/build/resources/main/static/...` as the E2E artifact.
- CLI E2E tags and task names still describe the pre-phase-10 model.
- `docs/gcp/prod-frontend-static-lb.md` still says non-prod forwards to classpath `index.html`; that is no longer true in a working sense.

## Places for simplification

- Remove the backend classpath SPA fallback and route local/non-prod through the same browser entry model as the proxy.
- Remove the duplicate CLI local-serving path and keep one representation of the install binary.
- Rename `cli:bundle-and-copy` to what it really does now, or delete the alias and call `pnpm cli:bundle` directly.

## Places for improvement

- Replace structural controller tests with observable route tests for `/`, `/users/identify`, and deep links.
- Extend the path-routing validation so it does not only prove “backend paths do not hit static”, but also proves the chosen deep-link story and other expected static paths.
- Keep the plan file honest about what is still intentionally divergent between prod and local.

## Further improvement opportunities

- Generate the prod URL-map YAML from a single higher-level source instead of hand-editing every `pathPrefixRewrite` to a new SHA.
- Move frontend static to a dedicated public/static bucket so the main mixed-use bucket does not need broad public-read exposure; the runbook already calls the current setup a stopgap.
- Consider automating active frontend revision promotion/rollback instead of manual YAML edits plus import.
