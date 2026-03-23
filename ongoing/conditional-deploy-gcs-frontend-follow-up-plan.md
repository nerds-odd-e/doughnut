# Follow-up plan for conditional deploy + GCS frontend

Informal phased plan based on `ongoing/conditional-deploy-gcs-frontend-history-review.md`.

Do not execute this plan all at once. Complete one phase, test it, deploy it, then update this note before starting the next phase.

## Main intent

Finish the incomplete "single source of truth" story so that:

1. prod routing cannot silently drift from local/CI behavior,
2. every successful `main` CI run makes the matching frontend live automatically,
3. deploy storage and public static hosting are separated cleanly,
4. obsolete compatibility behavior is removed after safer replacements exist.

## Architecture direction

- Keep **backend deploy skip** based on the backend jar hash.
- Keep **frontend upload** unconditional for green packaging runs.
- Make the normal release path **fully automatic**:
  - CI uploads frontend static to `gs://.../frontend/${GITHUB_SHA}/`
  - the green deploy flow updates the prod URL map to serve that same SHA
  - no separate human "promote frontend" step is needed for normal releases
- Prefer a **committed routing source or template** that CI can render for `${GITHUB_SHA}` at deploy time, rather than a hand-maintained concrete prod URL map with repeated SHAs.
- Treat `/doughnut-cli-latest/doughnut` as a **static-only** path:
  - served by GCS/LB in prod
  - served by the fake LB from `cli/dist` locally
  - never served by Spring on `9081`
- Make routing validation cover both sides:
  - backend paths must not be routed to static
  - required static paths must be routed to static
- Prefer one committed routing source that both local proxy behavior and prod URL-map generation/validation can derive from.

---

## Phase 1 — Regression proof for prod deep-link shell

**Outcome:** Prod deep links (`/d/...`, `/n...`) are protected by focused automated tests, so future routing and promotion work cannot silently break the shell returned by the MIG.

**Why first:** The review found this is important prod behavior with weak test protection. Per the planning rule, existing behavior without strong tests deserves its own phase.

**User-visible value:** Users opening deep links in production keep getting the correct shell that matches the active frontend revision.

**Scope:**

- Add focused tests around `ApplicationController` prod deep-link behavior.
- Cover the main success path: prod deep link returns HTML fetched from the configured public SPA origin.
- Cover at least one failure path or make the current failure behavior explicit in test expectations and docs.

**Tests:**

- Backend controller/integration tests driven through HTTP behavior, not internal helper methods.
- Run the relevant backend test file only.

**Done when:**

- Deep-link shell behavior is explicit, tested, and documented enough for later routing changes.

**Status:** Implemented — `ApplicationControllerProdDeepLinkTests` (MockMvc + `MockRestServiceServer`): prod-style deep links under `/d/**` and `/n**` GET the SPA shell from `doughnut.spa-public-base-url/`; upstream error is covered as 500 after `RestTemplate` throws (test-only `@ControllerAdvice` maps `RestClientException` to 500, matching unhandled failure behavior).

---

## Phase 2 — Catch prod-only static routing drift in CI

**Outcome:** CI fails when the built frontend requires a root-level static asset path that prod would not route to GCS.

**Why now:** This is the highest-value reliability gap from the review and the clearest missing half of phase 8.

**User-visible value:** A new favicon, manifest, or similar root asset no longer works locally/CI but breaks only after release.

**Scope:**

- Extend the current path-routing validation so it remains backend-safe and also checks required static paths.
- Derive required static paths from the built frontend output and/or committed frontend public assets, not from an ad hoc hardcoded list in multiple places.
- Keep the validation observable and black-box: "given this build and this prod route config, would the browser get the file?"

**Tests:**

- Unit-style tests for the validator using small fixture URL maps and fixture static trees.
- Run the validator through its public script entry point.

**Done when:**

- Adding a new root-level public asset without corresponding prod routing causes CI failure.

**Status:** Implemented — `infra/gcp/path-routing/validateUrlMapPathRouting.mjs` runs backend + static checks; required paths come from `requiredStaticPathsFromFrontend.mjs` (source `index.html`, `public/`, optional `dist/index.html`) plus `mandatoryStaticBucketProbes()` for `/`, `/index.html`, and `/assets/*`. `pnpm test:path-routing` (in `lint:all`) covers fixtures; `pnpm validate:path-routing` is the repo entry point.

**Later change (Phase 4):** Default validation input is now the URL map YAML **generated** from `doughnut-routing.json` (same as deploy); `--url-map` still accepts a rendered file for pre-import checks.

---

## Phase 3 — Green CI automatically activates the matching frontend build

**Outcome:** After a successful `main` CI run, users get the frontend built from that same commit on the prod hostname without any developer-operated promotion step.

**User-visible value:** Frontend-only changes go live as soon as CI is green, matching the commit that just passed tests.

**Scope:**

- Extend the existing deploy path so that, after the frontend upload for `${GITHUB_SHA}` and after all green gates, CI updates the prod URL map to serve `frontend/${GITHUB_SHA}/`.
- Keep this in the same production deployment flow that already handles backend rollout logic, so "green CI" means "latest frontend is live" even when the backend jar is unchanged.
- Prefer one small render/apply path for the URL map:
  - CI provides `${GITHUB_SHA}`,
  - committed routing inputs define the stable path split,
  - deploy applies the rendered URL map to GCP.
- Do not require a committed "active frontend SHA" file or any manual repo edit as part of the normal release path.
- Keep rollback possible, but treat it as a separate recovery path, not the everyday deployment mechanism.

**Tests:**

- Script or unit tests for the URL-map render step given a commit SHA.
- Validation that the generated URL map for `${GITHUB_SHA}` still passes the existing path-routing checks before CI applies it.
- One focused deploy-path test or dry-run command proving the CI entry point wires upload output and URL-map generation together correctly.

**Done when:**

- A successful `main` CI run makes the frontend for that commit live on the prod hostname without manual intervention.
- Frontend-only changes no longer wait for a separate promotion step.

**Status:** Implemented — `renderDoughnutAppServiceUrlMap.mjs` + `doughnut-routing.json` (Phase 4); `apply-doughnut-app-service-url-map.sh` renders, runs `validate-url-map-static-vs-backend-hints.mjs --url-map`, then `gcloud compute url-maps import`. `deploy-backend-jar-to-gcp-mig.sh` invokes apply **before** the jar skip gate so frontend-only commits still activate. `pnpm validate:path-routing` validates generated YAML (dummy SHA). Tests: `renderDoughnutAppServiceUrlMap.test.mjs`, `apply-doughnut-app-service-url-map-wiring.test`, extended `deploy-backend-jar-to-gcp-mig.sh.test`.

---

## Phase 4 — Make local/prod routing share one committed source

**Outcome:** Local fake LB behavior, CI validation, and deploy-time prod URL-map generation are derived from the same committed routing inputs, not only loosely checked against each other.

**User-visible value:** Dev, CI, and prod behave the same more often, so bugs are found before release.

**Scope:**

- Expand the current backend-path hints approach into a fuller routing source, or add a second committed source for static/public routing if that keeps things simpler.
- Generate or derive:
  - proxy routing behavior,
  - deploy-time prod URL-map fragments or full YAML,
  - validation probes.
- Move Phase 2 validation off the hand-maintained concrete prod URL-map file and onto the same generated URL-map output CI will apply in Phase 3.
- Avoid a big generic framework. Keep the representation small and specific to the current routing needs.

**Tests:**

- Existing validator extended to prove generated deploy-time routing and proxy expectations match.
- Minimal tests around generation or derivation behavior.

**Done when:**

- Changing routing rules happens in one committed source, with downstream artifacts regenerated or validated from it.

**Status:** Implemented — Single source [`infra/gcp/path-routing/doughnut-routing.json`](infra/gcp/path-routing/doughnut-routing.json): `backendPathHints` (proxy + validation), `gcpUrlMap` (deploy-time YAML via `renderDoughnutAppServiceUrlMapYamlFromRouting`), `mandatoryStaticBucketProbes`, `localProxy` (e.g. `/users/identify` SPA shell locally). `pnpm validate:path-routing` without `--url-map` validates that generated output. Removed `backend-path-hints.json` and `doughnut-app-service-map.template.yaml`.

---

## Phase 5 — Separate public frontend hosting from deploy storage

**Outcome:** Frontend static files live in a frontend-only bucket, separate from backend jars, deploy records, and other operational objects.

**User-visible value:** Safer and more reliable production hosting, with clearer ownership and lower accidental exposure.

**Scope:**

- Create or document the dedicated frontend bucket and backend-bucket wiring.
- Update CI/frontend upload configuration to publish static there.
- Keep backend deploy record and jar storage where they belong.
- Remove the need for broad public read on the mixed-purpose bucket.

**Tests / verification:**

- Script test updates for the upload target if env/config changes.
- Manual smoke checks in the runbook for prod routing after cutover.

**Done when:**

- Public frontend hosting no longer depends on a mixed-purpose bucket.

---

## Phase 6 — Simplify local topology after the safer model is in place

**Outcome:** Remove stale compatibility behavior that survived the earlier migration.

**User-visible value:** Easier local reasoning for developers and AI agents; fewer one-off exceptions.

**Scope:**

- Remove the misleading `cli:bundle-and-copy` alias and use the real behavior names directly.
- Remove `copyCliBundle` entirely from `backend/build.gradle`, including all `dependsOn` wiring tied to `bootRun`.
- Remove any remaining assumption in code, tests, scripts, or docs that Spring on `9081` serves `/doughnut-cli-latest/doughnut`.
- Keep the CLI install story cohesive:
  - the install script still points users at `/doughnut-cli-latest/doughnut`
  - the fake LB serves that path locally from `cli/dist`
  - prod serves that path from GCS through the LB
- Revisit the `/users/identify` fake-LB special case and remove it if the local/prod routing model makes it unnecessary.
- Keep the dev browser entry model simple:
  - browser-facing app routes come from Vite or built static,
  - backend routes come from Spring,
  - no leftover assumption that Spring serves the SPA locally from classpath static.

**Tests:**

- Relevant script test updates.
- Targeted E2E or local topology checks for the affected flows.

**Done when:**

- The local topology has fewer exceptions and names reflect actual behavior.
- No build or runtime path still relies on Spring resources for the CLI binary.

---

## Phase 7 — Optional final cohesion pass

**Outcome:** The release story is small enough to explain in a short runbook: package artifacts, green CI makes that frontend live, backend rolls only when the jar changes.

**User-visible value:** Faster, safer operations and less tribal knowledge.

**Scope:**

- Trim docs that describe replaced interim behavior.
- Remove obsolete comments and compatibility wording.
- Make the final docs reflect only the surviving model.

**Tests / verification:**

- Re-run the focused tests and validation commands touched by earlier phases.
- Manual checklist in docs for automatic frontend activation, backend deploy, and rollback/recovery.

**Done when:**

- A human or AI can understand the whole deployment story from a small number of files without cross-referencing historical leftovers.

---

## Suggested execution order

1. Phase 1: protect existing deep-link behavior.
2. Phase 2: close the prod-only static routing gap.
3. Phase 3: make successful CI automatically activate that commit's frontend.
4. Phase 4: unify routing sources and move validation onto the generated deploy-time URL map.
5. Phase 5: split the frontend bucket from deploy storage.
6. Phase 6: remove stale compatibility behavior.
7. Phase 7: final cleanup and doc compression.

## Notes for future implementation

- Prefer test-first for each phase.
- Keep each phase deployable on its own.
- Do not mix bucket migration and routing-source refactors into the same phase unless tests show that the change is genuinely small.
- Update this file as soon as any phase changes the recommended order or removes an interim behavior.
