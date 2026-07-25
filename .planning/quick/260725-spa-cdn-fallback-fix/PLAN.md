# Fix blank SPA deep links: CDN invalidation + rewrite-based fallback

## Problem

`/notebooks`, `/recall`, `/circles` return HTTP 200 with empty body on direct visit/refresh.
Other SPA routes and trailing-slash variants work. Root cause: Cloud CDN poisoned
custom-error-policy responses; durable fix is rewrite-based SPA fallback.

## Phases

### Phase 1 — Behavior: CDN invalidation (deferred to deploy)

gcloud reauth blocked in agent. Mitigation runs as part of Phase 3's
`invalidate-cdn-cache /*` on URL-map import after push.

### Phase 2 — Behavior: routeRules + pathTemplateRewrite catch-all (done locally)

Renderer emits `routeRules`; catch-all `pathTemplateMatch: /**` →
`pathTemplateRewrite: /frontend/<SHA>/index.html`; dropped
`defaultCustomErrorResponsePolicy`. Validator/tests updated.
`pnpm test:path-routing` + `validate:path-routing` green.

### Phase 3 — Structure: deploy-time cache hygiene (done locally)

- `upload-frontend-static-to-gcs.sh`: `Cache-Control:public,max-age=60` on index.html
- `apply-doughnut-app-service-url-map.sh`: post-import `invalidate-cdn-cache /* --async`
- Runbook updated
