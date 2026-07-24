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

## Pre-flight gate (Jidoka — before Phase 2)

Needs gcloud access on project `carbon-syntax-298809`; stop for developer if unavailable:

1. `customErrorResponsePolicy` requires the **global external ALB (EXTERNAL_MANAGED)**,
   not classic. Check: `gcloud compute forwarding-rules list --global --format='table(name,loadBalancingScheme,target)'`.
   If classic → **stop**; LB migration is a developer decision and its own pre-phase.
2. Confirm MIG backend-service health check hits `/api/healthcheck` (not `/`), so
   deleting `home()` in Phase 3 is safe.

## Phases

### Phase 1 — Behavior (interim): `/settings` deep links load the app in prod (done)

- Test-first: backend test asserting `GET /settings` and `GET /settings/recall-stats`
  are handled by `spaDeepLink` (non-prod forward to `/index.html`); confirm red.
- Add `"/settings"`, `"/settings/**"` to the `spaDeepLink` mapping; test green.
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`; commit, push (CI deploys).
- **Interim behavior:** the whole whitelist mechanism is deleted in Phase 3. Justified:
  ships the user-facing fix now, while Phase 2 carries infra risk (LB scheme gate).
- Stop-safe: prod bug fixed even if nothing else proceeds.

**Done:** added `SettingsDeepLinkTests` (MockMvc, `test` profile, asserts
`forwardedUrl("/index.html")` for `GET /settings` and `GET /settings/recall-stats`;
confirmed red/404 before the mapping change). `backend:test_only` green.

### Phase 2 — Behavior: LB serves the SPA shell for any non-backend path (planned)

One observable behavior: a deep link unknown to the backend (e.g.
`/settings/recall-stats`, or any future route) returns the active SHA's `index.html`
with 200 from GCS, with zero backend involvement.

Slices (~5 min each, test-first via `pnpm test:path-routing` / `pnpm validate:path-routing`):

- **2a (red→green):** extend URL-map renderer unit tests, then
  `renderDoughnutAppServiceUrlMapYamlFromRouting`:
  - MIG pathRules generated from `backendPathHints` (exact → `path`; prefix `p/` →
    `p` + `p/*`; allow-bare likewise).
  - Catch-all `/*` pathRule → backend bucket with `pathPrefixRewrite`
    `/frontend/<SHA>/` (keep explicit `/`, `/index.html`, `/doughnut-cli-latest/*`
    rules so common hits skip the 404 round-trip).
  - `pathMatchers[].defaultCustomErrorResponsePolicy`: `matchResponseCodes: [404]`,
    `path: /frontend/<SHA>/index.html`, `overrideResponseCode: 200`,
    `errorService:` the backend bucket.
- **2b:** update `validate-url-map-static-vs-backend-hints.mjs` for the inverted shape
  (backend hints must hit MIG; probes + a deep-link sample like `/settings/recall-stats`
  must resolve to bucket/fallback). Local LB needs no change — same `backendPathHints`.
- **2c:** merge → CI `apply-doughnut-app-service-url-map.sh` imports the map. Prod
  smoke: `curl -sI $BASE/settings/recall-stats` → 200 `text/html`;
  `curl -sf $BASE/api/healthcheck` → MIG; a hashed `/assets/*` URL still 200.
- Legacy `/d/**` bookmarks keep working (fallback shell + frontend `/d/:pathMatch`
  strip-prefix route).
- Stop-safe: consistent solution live; Java whitelist becomes unreachable overlap,
  removed next phase.

### Phase 3 — Structure: backend stops serving frontend (planned)

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
  green; prod smoke from Phase 2 still holds after deploy.

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

- LB may be classic (pre-flight gate) → Phase 2 blocked pending migration decision;
  Phase 1 has already shipped the user-facing fix.
- URL-map import failure mid-deploy: rollback = re-import previous SHA's map (existing
  runbook recovery commands unchanged).
