# Frontend update reminder

## Source

- Story: [Know when the frontend is updated and reload to receive the latest code](../../seeds/SEED-041-frontend-update-reminder.md#story-1)
- **Identity:** SEED-041#story-1

## Goal and scope

A user whose open tab runs an older frontend than the released one sees, on
returning to the tab, a non-blocking reminder with a Reload action; reloading
runs the current frontend. A dismissed reminder returns only for a still newer
release.

Excluded (deferred in the seed): stable identity across backend-only releases as
a promise, automatic or forced reload, unsaved-work protection, rollout-time
mixed backends, chunk-load recovery, CLI/MCP version checks, semantic versions,
and caching changes.

## Approach

The frontend's release identity is the module entry script list of its
`index.html`. A production build references the content-hashed entry
(`/assets/main-<hash>.js`), so the identity changes whenever frontend code
changes and the served `index.html` already publishes it for the active release.
No build define, uploaded version file, routing change, or backend endpoint is
added. When the tab becomes visible again, the app fetches `/` without using
the browser cache, reads the module entry script sources from the response, and
compares them with those of the loaded document. In development and E2E both
come from the same Vite `index.html`, so they match and no reminder appears.

The reminder is a small component rendered by `DonutApp.vue`; the check lives
with it. Reload calls `window.location.reload()`, which the catch-all routing
answers with the active release's `index.html`.

This makes the seed's optional preference (identity stable across backend-only
releases) naturally true without promising or verifying it.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| A production `index.html` names a content-hashed entry script | `grep -o '<script[^>]*>' frontend/dist/index.html` | `<script type="module" crossorigin src="/assets/main-DCMNySDI.js">` |
| `/` in production serves the active release's `index.html` with a short cache | Read `infra/gcp/path-routing/doughnut-routing.json:19-33` and `infra/gcp/scripts/upload-frontend-static-to-gcs.sh:35-36` | `/` and `/index.html` rewrite to `frontend/<active SHA>/index.html`; `Cache-Control: public,max-age=60`; URL-map apply invalidates the CDN |
| Reload already obtains the current frontend | No service worker or PWA plugin in `frontend/` (search for workbox, registerSW, vite-plugin-pwa, manifest) | None found |
| Nothing checks visibility at app level today, so the new listener adds no conflict | `grep -rn visibilitychange frontend/src` | Only `composables/useThinkingTimeTracker.ts` |
| App-level tests can mock the `/` fetch | `frontend/tests/setupVitest.ts:27-29` enables `vitest-fetch-mock` globally; `frontend/tests/DonutApp.loadingThinBar.spec.ts` renders `DonutApp` | Global fetch mock available; existing app specs never trigger `visibilitychange`, so they are unaffected |

The production journey (a real tab across a real release) is owner-held and is
observed after the next release, not as slice proof.

## Slices

### 1. Returning to a stale tab shows a reminder that reloads onto the current frontend
Type: Behavior
Status: done
Accepted proof: `pnpm frontend:test DonutApp.frontendUpdateReminder` (2 tests:
"offers a reload when the served frontend has a different entry", "shows no
reminder when the served frontend is the loaded one") and `vue-tsc --noEmit`
pass. The check lives in `components/commons/FrontendUpdateReminder.vue`;
Reload goes through `managedApi/window/browserLocation.ts`.
Planned proof: new `frontend/tests/DonutApp.frontendUpdateReminder.spec.ts` rendering
`DonutApp`, mocking `fetch("/")`, and dispatching `visibilitychange` while
visible; `pnpm frontend:test DonutApp.frontendUpdateReminder`.

Behavior: the app is loaded with entry script A →
- the tab becomes visible and `/` names entry B → a reminder says a newer
  version of Donut is available with a Reload button, and the rest of the app
  stays usable; clicking Reload calls `window.location.reload()`;
- the tab becomes visible and `/` names entry A → no reminder.

### 2. A dismissed reminder returns only for a newer release
Type: Behavior
Status: planned
Proof: extend the same spec; same command.

Behavior: the reminder for entry B is showing → the user dismisses it → the tab
becomes visible and `/` still names B → no reminder; the tab becomes visible and
`/` names C → the reminder shows again.

## Current decisions

- Identity is read from `index.html` entry scripts, not a build-time version
  string, so nothing is added to the build or release scripts.
- Checks run only on the tab becoming visible; no timer.
- Dismissal is remembered in memory for the tab's life; after a reload the tab
  runs the current frontend, so no persistence is needed.

## Learnings

- A fetch-driven check does not settle within one `flushPromises()` under
  vitest-fetch-mock. The spec waits on its `DOMParser.prototype.parseFromString`
  spy (`returnToTabWhileServing`) so absence assertions cannot pass before the
  check runs; slice 2 reuses that helper.
