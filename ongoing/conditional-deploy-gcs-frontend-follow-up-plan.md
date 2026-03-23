# Follow-up plan for conditional deploy + GCS frontend

Informal phased plan based on `ongoing/conditional-deploy-gcs-frontend-history-review.md`.

Do not execute this plan all at once. Complete one phase, test it, deploy it, then update this note before starting the next phase.

## Main intent

Finish the incomplete "single source of truth" story so that:

1. prod routing cannot silently drift from local/CI behavior,
2. frontend promotion is reliable and understandable,
3. deploy storage and public static hosting are separated cleanly,
4. obsolete compatibility behavior is removed after safer replacements exist.

## Architecture direction

- Keep **backend deploy skip** based on the backend jar hash.
- Keep **frontend upload** unconditional for green packaging runs.
- Add one explicit concept for frontend release state: the **active frontend SHA**.
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

---

## Phase 3 — Frontend promotion becomes one explicit operation

**Outcome:** Promoting a frontend build is one reviewed, explicit operation with one input: the active frontend SHA.

**User-visible value:** Frontend-only changes can be published reliably without a backend rollout and without hand-editing the same SHA in several places.

**Scope:**

- Introduce one promotion mechanism, likely a script or generated config, that:
  - takes the active frontend SHA,
  - verifies the uploaded GCS prefix exists,
  - updates the prod URL-map artifact from that one value,
  - leaves a reviewable result in the repo or command output.
- Do not couple this to backend jar deployment.
- Keep rollback simple: promote an older SHA using the same path.

**Tests:**

- Script tests for promotion input validation and generated output.
- Validation that the produced URL-map artifact still passes path-routing checks.

**Done when:**

- The active frontend SHA is set in one place, not repeated manually in several rewrites.
- The documented promotion flow is shorter and less error-prone than today.

---

## Phase 4 — Make local/prod routing share one committed source

**Outcome:** Local fake LB behavior and prod URL-map behavior are derived from the same committed routing inputs, not only loosely checked against each other.

**User-visible value:** Dev, CI, and prod behave the same more often, so bugs are found before release.

**Scope:**

- Expand the current backend-path hints approach into a fuller routing source, or add a second committed source for static/public routing if that keeps things simpler.
- Generate or derive:
  - proxy routing behavior,
  - prod URL-map fragments or full YAML,
  - validation probes.
- Avoid a big generic framework. Keep the representation small and specific to the current routing needs.

**Tests:**

- Existing validator extended to prove generated/prod routing and proxy expectations match.
- Minimal tests around generation or derivation behavior.

**Done when:**

- Changing routing rules happens in one committed source, with downstream artifacts regenerated or validated from it.

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

**Outcome:** The release story is small enough to explain in a short runbook: package artifacts, promote frontend SHA, deploy backend when jar changes.

**User-visible value:** Faster, safer operations and less tribal knowledge.

**Scope:**

- Trim docs that describe replaced interim behavior.
- Remove obsolete comments and compatibility wording.
- Make the final docs reflect only the surviving model.

**Tests / verification:**

- Re-run the focused tests and validation commands touched by earlier phases.
- Manual checklist in docs for frontend promotion and backend deploy.

**Done when:**

- A human or AI can understand the whole deployment story from a small number of files without cross-referencing historical leftovers.

---

## Suggested execution order

1. Phase 1: protect existing deep-link behavior.
2. Phase 2: close the prod-only static routing gap.
3. Phase 3: make frontend promotion explicit and reliable.
4. Phase 4: unify routing sources after the promotion flow is clearer.
5. Phase 5: split the frontend bucket from deploy storage.
6. Phase 6: remove stale compatibility behavior.
7. Phase 7: final cleanup and doc compression.

## Notes for future implementation

- Prefer test-first for each phase.
- Keep each phase deployable on its own.
- Do not mix bucket migration and routing-source refactors into the same phase unless tests show that the change is genuinely small.
- Update this file as soon as any phase changes the recommended order or removes an interim behavior.
