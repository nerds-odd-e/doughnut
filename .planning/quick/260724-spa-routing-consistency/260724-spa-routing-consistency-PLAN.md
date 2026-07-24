# Plan: One consistent SPA routing solution (LB fallback; backend stops serving frontend)

## Problem

New SPA routes (`/settings`, `/settings/recall-stats`) 404 in production when visited
directly: the GCP URL map defaults every non-static path to the backend MIG, and the
SPA shell for deep links is served only for a hardcoded whitelist in
`ApplicationController.spaDeepLink` — which was never updated.

## Diagnosis (from investigation, 2026-07-24)

Three overlapping mechanisms existed before this plan:

1. **Local dev/e2e** — `scripts/local-lb.mjs`: backend paths from
   `infra/gcp/path-routing/doughnut-routing.json` `backendPathHints`; everything else
   falls back to `index.html`. Correct model; new routes just work.
2. **Prod** — LB static path rules → GCS; default → MIG; Spring whitelist
   (`spaDeepLink`) re-fetches the public origin's shell (`fetchSpaShellFromPublicOrigin`)
   to avoid jar/GCS chunk desync. Required a Java edit + backend deploy per new route.
   Whitelist was already stale (`/recent`, `/generate-token` moved under `/settings/`).
3. **Dead code** — `home()` and non-prod `spaDeepLink` forwarded to
   `classpath:/static/index.html` which does not exist (jar embeds no static tree);
   `MvcConfig` resource handler pointed at it; non-prod `/users/identify` branch was
   unreachable (local LB intercepts via `localProxy.spaShellInsteadOfBackendExactPaths`);
   `IndexControllerTests` pinned the dead forward.

**Target (achieved):** prod mirrors local-lb — backend paths → MIG, everything else →
GCS with SPA fallback via URL-map custom error response policy (404 from bucket →
`/frontend/<SHA>/index.html`, override 200). Backend lost all frontend awareness.

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

## Results (done, 2026-07-24)

All 4 phases shipped; the backend no longer has any frontend-serving code — the LB is
the single, consistent SPA routing mechanism for both local dev and prod.

- **Phase 1** (behavior, interim, `fa18718aa8`): added `/settings`, `/settings/**` to
  the `spaDeepLink` whitelist so deep links worked immediately while the infra phases
  below were still in progress.
- **Phase 2** (structure, `eb38c3706f`): migrated the prod LB (backend service
  `doughnut-app-service`, forwarding rule `doughnut-app-https-content-rule`) from
  classic (`EXTERNAL`) to global external ALB (`EXTERNAL_MANAGED`) via GCP's staged
  migration-state flow, zero observable downtime, to unlock `customErrorResponsePolicy`.
  Learning: `forwarding-rules update --external-managed-backend-bucket-migration-state`
  is the only state machine for a forwarding rule's own `EXTERNAL`→`EXTERNAL_MANAGED`
  readiness (name is misleading, not bucket-specific); no separate PREPARE/TEST cycle
  is needed once it reaches `TEST_ALL_TRAFFIC`.
- **Phase 3** (behavior, `a65e04c64b`, `f6e14eb32d`): inverted the URL map —
  `backendPathHints` now generate explicit MIG `pathRules`; a `/*` catch-all routes
  everything else to the backend bucket with the active-SHA rewrite;
  `defaultCustomErrorResponsePolicy` turns the bucket's 404 into `index.html` with
  `overrideResponseCode: 200`. Any unknown path now resolves to the SPA shell via GCS,
  zero backend involvement. Learning: prod smoke checks right after a URL-map deploy
  can show transient 404s on previously-uncached paths for up to ~30s while the change
  propagates across Google Front Ends — retry before treating one 404 as a failure.
- **Phase 4** (structure): deleted `ApplicationController.home()`/`spaDeepLink()`/
  `fetchSpaShellFromPublicOrigin()` and the `spaPublicBaseUrl`/`RestTemplate`
  constructor wiring; simplified `/users/identify` to always `redirect:` (kept
  `/robots.txt`). Deleted `MvcConfig`, `doughnut.spa-public-base-url` config,
  `IndexControllerTests`, `ApplicationControllerProdDeepLinkTests`, and the Phase 1
  interim `SettingsDeepLinkTests`. Kept `RestTemplateConfig` (used by
  `DoughnutTaskRunner`). Rewrote `docs/gcp/prod-frontend-static-lb.md` to describe the
  LB catch-all + error-response-policy fallback as the only mechanism.
  `backend:test_only` and the `new_user.feature` login/identify e2e spec green; final
  prod smoke (`/`, `/api/healthcheck`, `/settings`, `/settings/recall-stats`,
  `/notebooks`, `/robots.txt`) all 200 after the MIG rolling-update deploy.
