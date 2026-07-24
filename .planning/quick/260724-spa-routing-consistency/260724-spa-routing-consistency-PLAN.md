# Plan: One consistent SPA routing solution (LB fallback; backend stops serving frontend)

## Problem

New SPA routes (`/settings`, `/settings/recall-stats`) 404 in production when visited
directly: the GCP URL map defaults every non-static path to the backend MIG, and the
SPA shell for deep links is served only for a hardcoded whitelist in
`ApplicationController.spaDeepLink` — which was never updated.

## Diagnosis (from investigation, 2026-07-24)

Three overlapping mechanisms today:

1. **Local dev/e2e** — `scripts/local-lb.mjs`: backend paths from
   `infra/gcp/path-routing/doughnut-routing.json` `backendPathHints`; everything else
   falls back to `index.html`. Correct model; new routes just work.
2. **Prod** — LB static path rules → GCS; default → MIG; Spring whitelist
   (`spaDeepLink`) re-fetches the public origin's shell (`fetchSpaShellFromPublicOrigin`)
   to avoid jar/GCS chunk desync. Requires a Java edit + backend deploy per new route.
   Whitelist already stale (`/recent`, `/generate-token` moved under `/settings/`).
3. **Dead code** — `home()` and non-prod `spaDeepLink` forward to
   `classpath:/static/index.html` which does not exist (jar embeds no static tree);
   `MvcConfig` resource handler points at it; non-prod `/users/identify` branch is
   unreachable (local LB intercepts via `localProxy.spaShellInsteadOfBackendExactPaths`);
   `IndexControllerTests` pins the dead forward.

**Target:** prod mirrors local-lb — backend paths → MIG, everything else → GCS with SPA
fallback via URL-map custom error response policy (404 from bucket →
`/frontend/<SHA>/index.html`, override 200). Backend loses all frontend awareness.
Runbook already names this as the alternative (`docs/gcp/prod-frontend-static-lb.md`,
"SPA deep links" section).

## Pre-flight gate (Jidoka — before Phase 3) — RESOLVED 2026-07-24

Ran against project `carbon-syntax-298809` (gcloud reauth required, then succeeded):

1. **LB scheme is classic (`EXTERNAL`)**, confirmed on both the forwarding rule
   (`doughnut-app-https-content-rule` → `doughnut-app-service-map-target-proxy-2`) and
   the backend service (`doughnut-app-service`). `customErrorResponsePolicy` needs
   `EXTERNAL_MANAGED` → **blocked**. Developer decision: migrate the LB first as its
   own Structure phase (new Phase 2 below), then proceed with the original Phase 2
   (renumbered Phase 3).
2. MIG backend-service health check: legacy `doughnut-app-health-check`
   (`httpHealthChecks`), `requestPath: /api/healthcheck`, port 8081 — **not** `/`.
   Confirmed safe to delete `home()` in the final structure phase. Legacy HTTP health
   checks remain supported after migrating to `EXTERNAL_MANAGED` (GCP docs: supported
   when backends are instance groups serving HTTP/HTTPS — true here), so no health
   check change is needed as part of the LB migration.
3. `doughnut-app-http-content-rule` (port 80) uses url map `doughnut-app-web-map-http`,
   which is a pure `defaultUrlRedirect` (HTTP→HTTPS, no backend reference) — it does
   not touch `doughnut-app-service` or the frontend bucket and does **not** need
   migrating.

## Phases

### Phase 1 — Behavior (interim): `/settings` deep links load the app in prod (done)

- Test-first: backend test asserting `GET /settings` and `GET /settings/recall-stats`
  are handled by `spaDeepLink` (non-prod forward to `/index.html`); confirm red.
- Add `"/settings"`, `"/settings/**"` to the `spaDeepLink` mapping; test green.
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`; commit, push (CI deploys).
- **Interim behavior:** the whole whitelist mechanism is deleted in Phase 4. Justified:
  ships the user-facing fix now, while Phases 2–3 carry infra risk (LB migration +
  URL-map rework).
- Stop-safe: prod bug fixed even if nothing else proceeds.

**Done:** added `SettingsDeepLinkTests` (MockMvc, `test` profile, asserts
`forwardedUrl("/index.html")` for `GET /settings` and `GET /settings/recall-stats`;
confirmed red/404 before the mapping change). `backend:test_only` green.

### Phase 2 — Structure: migrate prod LB from classic to global external ALB (done)

No external behavior change (staged, near-zero-downtime, GCP-native migration path);
enables `customErrorResponsePolicy` for Phase 3. Resources in play (project
`carbon-syntax-298809`): backend service `doughnut-app-service`, backend bucket
`doughnut-frontend-backend-bucket`, forwarding rule `doughnut-app-https-content-rule`
(target proxy `doughnut-app-service-map-target-proxy-2`). `doughnut-app-http-content-rule`
(HTTP→HTTPS redirect only) is untouched.

**Time-budget exception (planning.mdc):** GCP mandates ≥6 min stabilization between
each migration-state change; this phase's wall-clock will exceed the 10-minute hard
trigger by design (waiting on infra, not thrashing) — proceed through the checkpoints
below rather than reverting for time alone. Smoke-check after every state change;
stop and roll back the *last* state change (see rollback column) on any failure —
do not proceed to the next stage.

Order: backend service → backend bucket (via forwarding rule flag) → forwarding rule
(must fully migrate the backend service before starting the bucket/forwarding rule,
per GCP requirement).

**Simplified 2026-07-24 (developer decision, low prod traffic):** skip percentage-based
traffic-shifting steps (10%/50%) for the remaining forwarding-rule stage — go straight
`PREPARE` → `TEST_ALL_TRAFFIC` → finalize, keeping only the mandatory ~6 min
stabilization wait between each state (that wait is about GCP infra settling, not
about traffic percentage). Backend service and the first half of the bucket migration
had already gone through 10%/50% before this decision.

| Step | Command | Wait | Smoke check | Rollback |
|---|---|---|---|---|
| 2a | `gcloud compute backend-services update doughnut-app-service --external-managed-migration-state=PREPARE --global` | ≥6 min | `curl -sf $BASE/api/healthcheck` | re-run with `--external-managed-migration-state=PREPARE` is idempotent; no traffic shifted yet |
| 2b | `--external-managed-migration-state=TEST_BY_PERCENTAGE --external-managed-migration-testing-percentage=10` | ≥6 min | repeat curl + check error rate/logs for `doughnut-app-service` | drop back to `PREPARE` |
| 2c | same with `--external-managed-migration-testing-percentage=50` | ≥6 min | same | drop back to `PREPARE` or lower percentage |
| 2d | `--external-managed-migration-state=TEST_ALL_TRAFFIC` | ≥6 min | same | drop back to a lower `TEST_BY_PERCENTAGE` |
| 2e | `--load-balancing-scheme=EXTERNAL_MANAGED --global` (finalize backend service) | ≥6 min | `curl -sf $BASE/api/healthcheck`; confirm `loadBalancingScheme: EXTERNAL_MANAGED` in describe | GCP-documented rollback window after finalize (time-boxed); otherwise reverse migration steps |
| 2f | `gcloud compute forwarding-rules update doughnut-app-https-content-rule --external-managed-backend-bucket-migration-state=PREPARE --global` | ≥6 min | `curl -sI $BASE/` (200 from bucket) | re-run PREPARE; no shift yet |
| 2g | same with `TEST_BY_PERCENTAGE --external-managed-backend-bucket-migration-testing-percentage=10`, then `50` | ≥6 min each | `curl -sI $BASE/` and a real `/assets/*` URL | drop back a stage |
| 2h | `--external-managed-backend-bucket-migration-state=TEST_ALL_TRAFFIC` | ≥6 min | same | drop back |
| 2i | `gcloud compute forwarding-rules update doughnut-app-https-content-rule --load-balancing-scheme=EXTERNAL_MANAGED --global` (finalize forwarding rule — **correction**: `--external-managed-backend-bucket-migration-state` is the *only* state machine for this forwarding rule's own EXTERNAL→EXTERNAL_MANAGED readiness despite its name; steps 2f–2h already drove it to `TEST_ALL_TRAFFIC`, so no separate PREPARE/TEST cycle is needed here) | ≥6 min | full smoke: `/`, `/api/healthcheck`, a real `/assets/*` URL, `/settings` (still via Phase 1 whitelist at this point) | GCP rollback window after finalize |

- Verify final state: `gcloud compute forwarding-rules describe doughnut-app-https-content-rule --global --format='value(loadBalancingScheme)'` → `EXTERNAL_MANAGED`; same for `doughnut compute backend-services describe doughnut-app-service --global`.
- Stop-safe: identical external behavior throughout and after; unblocks Phase 3.
  If migration is abandoned partway, everything still works (classic or in-test
  states both serve traffic); nothing here is user-visible.

**Done (2026-07-24):** Both `doughnut-app-service` and `doughnut-app-https-content-rule`
confirmed `EXTERNAL_MANAGED`. Ran 2a–2e (backend service, with 10%/50% percentage
testing) and 2f–2i (bucket + forwarding rule scheme, simplified per developer
decision to skip percentage-testing given low prod traffic — went `PREPARE` →
`TEST_ALL_TRAFFIC` → finalize, keeping only the mandatory ~6 min stabilization waits).
Smoke-checked after every state change; zero failures throughout. Final comprehensive
check: `/`, `/api/healthcheck`, a real `/assets/*.js`, `/settings`, `/settings/recall-stats`,
`/d/1`, `/notebooks` all 200. `doughnut-app-http-content-rule` (HTTP→HTTPS redirect,
no backend) left untouched as planned.
**Learning:** `forwarding-rules update --external-managed-backend-bucket-migration-state`
is the *only* state machine governing that forwarding rule's own EXTERNAL→EXTERNAL_MANAGED
readiness (its name is misleading — it is not bucket-specific). There is no separate
`--external-managed-migration-state` flag for forwarding rules (that flag only exists
for `backend-services update`). Once this state reaches `TEST_ALL_TRAFFIC`, finalize
directly with `--load-balancing-scheme=EXTERNAL_MANAGED` — no separate PREPARE/TEST
cycle needed for "the forwarding rule itself" as Phase 2's original table implied.

### Phase 3 — Behavior: LB serves the SPA shell for any non-backend path (planned)

One observable behavior: a deep link unknown to the backend (e.g.
`/settings/recall-stats`, or any future route) returns the active SHA's `index.html`
with 200 from GCS, with zero backend involvement.

Slices (~5 min each, test-first via `pnpm test:path-routing` / `pnpm validate:path-routing`):

- **3a (red→green):** extend URL-map renderer unit tests, then
  `renderDoughnutAppServiceUrlMapYamlFromRouting`:
  - MIG pathRules generated from `backendPathHints` (exact → `path`; prefix `p/` →
    `p` + `p/*`; allow-bare likewise).
  - Catch-all `/*` pathRule → backend bucket with `pathPrefixRewrite`
    `/frontend/<SHA>/` (keep explicit `/`, `/index.html`, `/doughnut-cli-latest/*`
    rules so common hits skip the 404 round-trip).
  - `pathMatchers[].defaultCustomErrorResponsePolicy`: `matchResponseCodes: [404]`,
    `path: /frontend/<SHA>/index.html`, `overrideResponseCode: 200`,
    `errorService:` the backend bucket.
- **3b:** update `validate-url-map-static-vs-backend-hints.mjs` for the inverted shape
  (backend hints must hit MIG; probes + a deep-link sample like `/settings/recall-stats`
  must resolve to bucket/fallback). Local LB needs no change — same `backendPathHints`.
- **3c:** merge → CI `apply-doughnut-app-service-url-map.sh` imports the map. Prod
  smoke: `curl -sI $BASE/settings/recall-stats` → 200 `text/html`;
  `curl -sf $BASE/api/healthcheck` → MIG; a hashed `/assets/*` URL still 200.
- Legacy `/d/**` bookmarks keep working (fallback shell + frontend `/d/:pathMatch`
  strip-prefix route).
- Stop-safe: consistent solution live; Java whitelist becomes unreachable overlap,
  removed next phase.

### Phase 4 — Structure: backend stops serving frontend (planned)

No external behavior change through the public origin; removes the overlapped/dead
mechanisms.

- `ApplicationController`: delete `home()`, `spaDeepLink()`,
  `fetchSpaShellFromPublicOrigin()`, `spaPublicBaseUrl` + `RestTemplate` constructor
  wiring; simplify `/users/identify` to always `redirect:` (non-prod branch was
  unreachable/broken). Keep `/robots.txt`.
- Delete `MvcConfig`; remove `doughnut.spa-public-base-url` from `application.yml`.
- Delete `IndexControllerTests`, `ApplicationControllerProdDeepLinkTests`, and the
  Phase 1 interim test. Keep `RestTemplateConfig` (used by `DoughnutTaskRunner`).
- Docs: rewrite `docs/gcp/prod-frontend-static-lb.md` "SPA deep links" + MIG path
  table — LB fallback is *the* mechanism; drop whitelist maintenance instructions.
- Verify: `backend:test_only` green; targeted e2e spec covering login/identify flow
  green; prod smoke from Phase 3 still holds after deploy.

## Key decisions

- **LB-side fallback, not backend whitelist** — single source of truth stays
  `doughnut-routing.json` for prod and local; new frontend routes touch only
  `frontend/src/routes/`.
- **Custom error response policy** chosen over per-route bucket rules (no enumeration)
  and over serving shell from the jar (chunk-desync problem disappears since fallback
  always serves the active SHA's shell).
- **Catch-all pathRule `/*` + rewrite** instead of `defaultRouteAction` — more specific
  rules (MIG paths, CLI) win by longest-match; default service stays MIG-independent.
- `/users/identify` and `/robots.txt` remain backend endpoints (auth entry / metadata),
  not frontend source.

## Risks

- LB confirmed classic → Phase 2 (LB migration) required before Phase 3; Phase 1
  already shipped the user-facing fix so nothing is blocked from the user's view.
- LB migration (Phase 2) touches live prod traffic in stages; each stage is
  independently reversible per GCP's staged rollout (see rollback column). Increased
  backend memory/connections possible post-migration (GCP docs) — monitor after 2e/2i.
- URL-map import failure mid-deploy (Phase 3): rollback = re-import previous SHA's map
  (existing runbook recovery commands unchanged).
