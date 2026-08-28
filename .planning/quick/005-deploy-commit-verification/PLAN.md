# Deployed commit verification

**Status:** in progress — slice 1 done; next is slice 2.
**Type:** ad-hoc plan (`.planning/quick/`)

## Goal

Make it possible to know, in one HTTP call, exactly which commit is running in
production — and make the deploy pipeline itself refuse to report success when
what it just rolled out doesn't match what it meant to deploy.

## Origin

Found while investigating why `GET /api/user/recall-split-half-reliability`
405'd in production. Root cause:
`infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` hardcoded
`ARTIFACT="doughnut"` independently of `deploy.yml`'s `ARTIFACT="donut"` (the
two never synced after the `com.odde.doughnut` → `com.odde.donut` package
rename, `a3aafb83eb`, 2026-08-26). Every deploy since then silently launched a
~2-day-stale jar from the old GCS path while CI, the GCS upload, and the
deploy script's own hash record all reported success — there was no way, from
outside the box, to tell the running app's commit apart from the intended one
without SSHing in and diffing jar contents by hand (which is how this was
actually found, over many round trips). The `ARTIFACT` mismatch itself is
already fixed directly on `main` (`d24eff0871`); this plan is the follow-up so
the *next* drift of this kind is caught automatically instead of taking a
multi-hour investigation.

## Key design decisions

- **No `spring-boot-starter-actuator`.** Confirmed absent from
  `backend/build.gradle` and no `git.properties`/build-info in the jar today.
  Rather than adding actuator for a single `/actuator/info` call, extend the
  existing `permitAll` `/api/healthcheck`
  (`backend/src/main/java/com/odde/donut/controllers/HealthCheckController.java`)
  to report the commit too. One less dependency, and it's already the
  endpoint `app-instance-healthcheck.sh` polls after every rollout.
- **`BuildProperties` needs no actuator.** Its auto-configuration lives in
  `spring-boot-autoconfigure` and only requires `META-INF/build-info.properties`
  on the classpath — actuator is only needed to auto-expose it at
  `/actuator/info`, which we're not using.
- **Commit comes from `git rev-parse HEAD` at build time**, not a CI-only env
  var, so local `bootJar` builds also carry a real (if locally-dirty) commit
  value instead of a blank/placeholder — keeps CI and local builds on one code
  path.
- **Sequencing removes the bootstrap problem.** Slice 3's mismatch check reads
  the commit that slice 2 makes `/api/healthcheck` report. Because each slice
  is committed, pushed, and deployed before the next slice starts (per the
  deploy-gate rule), slice 3's own first deploy is verifying a build that
  already has slice 2 live — no chicken-and-egg case to special-case.
- **Dropping the ARTIFACT-name lint idea.** Considered and rejected in favor
  of slice 3: a lint that just checks `ARTIFACT` strings match between
  `deploy.yml` and the startup script only catches *this* bug's exact shape.
  The commit-mismatch check in slice 3 is strictly more general — it catches
  this bug, a future rename of the same kind, a stuck GCS cache, a bad
  rollout, or any other reason the wrong build ends up running — for the same
  implementation cost.

## Out of scope

- Rotating/fixing prod secrets visible via `ps aux` on the app instance —
  tracked separately, dormant, at
  `.planning/seeds/SEED-008-prod-secrets-visible-in-process-args.md`.
- Re-verifying `recall-split-half-reliability` against prod once today's
  `ARTIFACT` fix actually deploys — a one-off follow-up, not a planning slice.

## Slices

### 1. Build carries its own commit SHA (Structure)
**Status:** done

`backend/build.gradle` `springBoot.buildInfo` embeds `git rev-parse HEAD` as
`build.commit`. `time` is excluded so two `bootJar` runs of the same tree still
hash the same (`boot-jar-reproducible.sh`). No `HealthCheckController` test
class exists; healthcheck response is unchanged.

Verified: `bootJar` jar contains `META-INF/build-info.properties` with
`build.commit` matching `git rev-parse HEAD`.

### 2. Healthcheck reports the deployed commit (Behavior)

**Pre-condition:** app built with slice 1's build-info wiring.
**Trigger:** `GET /api/healthcheck`.
**Post-condition:** response includes the commit SHA from `BuildProperties`
(e.g. `OK. Active Profile: prod. Commit: <sha>`), alongside the existing
active-profile text — keep the existing `"OK"` substring so
`app-instance-healthcheck.sh`'s current `[[ "$last_body" == *"OK"* ]]` match
keeps working unchanged.

Test: extend the existing `HealthCheckController` test to assert the response
contains a commit value sourced from the injected `BuildProperties` bean
(inject a test double/fixture value rather than asserting a specific real SHA).

This alone already delivers the core value: anyone can `curl` prod and know
in one call what commit is actually running, instead of the multi-round-trip
SSH/jar-diffing this plan's Origin required.

### 3. Deploy pipeline fails loudly on commit mismatch (Behavior)

**Pre-condition:** MIG rollout has completed and
`app-instance-healthcheck.sh` already confirms the app responds with `"OK"`
(slice 2 live, so the response carries a real commit).
**Trigger:** the deploy workflow runs after a rollout, as it already does.
**Post-condition:** if the commit reported by `/api/healthcheck` does not
equal `$GITHUB_SHA` (the commit the workflow meant to deploy), the deploy
script exits non-zero — failing the `Deploy` job in `deploy.yml`, which
already triggers the existing `Notify-on-failure` Slack step with no new
notification plumbing needed. On a match, behavior is unchanged (deploy
reports success as today).

Implementation: extend
`infra/gcp/scripts/app-instance-healthcheck.sh` (already curls
`HEALTHCHECK_URL` and retries) to also extract the commit from the response
body and compare it against `$GITHUB_SHA`, passed through from
`deploy-backend-jar-to-gcp-mig.sh`.

No shell-script test harness exists in this repo for `infra/gcp/scripts`
(only `.mjs` tests for URL-map rendering, unrelated) — verify by code review
and a manual dry run of the comparison logic against a captured healthcheck
response, plus watching the next real deploy actually pass the check.
