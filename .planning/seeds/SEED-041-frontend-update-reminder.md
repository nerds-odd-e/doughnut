---
id: SEED-041
status: dormant
planted: 2026-09-28
planted_during: owner request to capture frontend release awareness
trigger_when: improving how users receive updated frontend code
scope: small
---

# SEED-041: Help users receive the latest frontend code

## Why This Matters

Donut users often keep a browser tab open across releases, and the application
releases roughly daily. That tab keeps running the frontend it loaded until the
user happens to reload, so they miss frontend improvements without knowing.

The bottom line adds urgency: Donut does not promise that an older frontend keeps
working after a backend API change. A stale tab may therefore misbehave, and the
reminder is how users are told to get back onto a supported frontend.

Current behavior (inspected 2026-09-28): the production frontend is served from
a GCS bucket prefix per release commit through the load balancer with Cloud CDN;
`index.html` is cached for 60 seconds, each URL-map apply invalidates the CDN,
and there is no service worker. A plain reload therefore already obtains the
current frontend. Routes are statically imported, so a stale tab rarely requests
missing chunks. The frontend has no build identity and never checks for one; the
backend's health check reports a backend commit that can differ from the served
frontend's commit, so it is not the frontend's release identity.

## Alternatives and Decision

Doing nothing leaves users unaware of updates and on unsupported frontends after
API changes. Forcing or automatically reloading would interrupt users mid-task;
the owner chose an informative reminder that lets the user decide when to reload.

The frontend owns its own release identity, built into the bundle and published
beside the released frontend; no backend endpoint is involved. A reminder after
every application release is acceptable to the owner even when the frontend did
not change; keeping the identity stable across backend-only releases is a
preference, not a requirement.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.

<a id="story-1"></a>

### Know when the frontend is updated and reload to receive the latest code

**Identity:** SEED-041#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-frontend-update-reminder/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"c52851f465d2df50fc1f572fb44132277f68669bb07a59be674f1d636f08c98f","plan":"53d94dfb600718c580b754e90bfadd53ea0f08004a49f4ca4b52d5088f30c0f5"}}
```

**Goal**

A Donut user whose open tab runs an older frontend than the one currently
released learns that an update is available and reloads onto the current
frontend, receiving its improvements and staying on a frontend that matches the
backend API.

**Scope**

- Each frontend build carries a release identity, and the released frontend
  publishes the current identity where an open tab can read it.
- An open tab compares its own identity with the published one when the user
  returns to it (the tab becomes visible or focused again), without a periodic
  timer.
- When they differ, a non-blocking reminder says a newer version of Donut is
  available and offers a Reload action; the user can keep working.
- Reloading, by the action or by the browser, runs the current frontend and the
  reminder does not reappear for it.
- A dismissed reminder reappears only when a still newer frontend is released.

Deferred promises (not built or verified in this story):

- Keeping the identity unchanged across releases without frontend changes.
- Automatic or forced reload, and protection of unsaved work before reloading.
- Compatibility or messaging during a rollout while old and new backends serve
  together.
- Recovery from missing-asset or chunk-load errors in a stale tab.
- Version checks for the CLI or MCP clients, and semantic version numbering.
- Changes to caching; reload already obtains the current frontend.

**Key examples**

- A user has Donut open on the previous frontend → a new application release
  goes out → the user returns to the tab → a reminder says a newer version is
  available, with Reload.
- The user clicks Reload → Donut loads the current frontend → no reminder is
  shown when they later return to the tab.
- A user already runs the current frontend → returns to the tab → no reminder.
- A user dismisses the reminder and keeps working → returns to the tab before
  another release → no reminder; after a further release → the reminder shows
  again.

- **For / why:** Users get frontend improvements promptly and move off stale
  frontends that newer backend APIs no longer promise to support.
- **Evaluation:** A browser holding an older frontend identity shows the reminder
  on return; reloading obtains the current identity and clears it.
- **Value / learning:** Whether a return-to-tab check is timely enough, given
  roughly daily releases, before investing in anything more intrusive.
- **Effort hypothesis:** S–M; the reload path already works, so the work is the
  identity, its publication in the release, the check, and the reminder.
- **Depends on:** The existing frontend build and GCS/load-balancer release flow.
- **Safe stopping point:** Identity, check, and reminder with Reload working
  together in production.

## Ordering and Scope Reduction

The owner selected this as the second product backlog story, after browser login
continuity; the unsupported-stale-frontend bottom line confirms that priority.
If scope needs reducing, keep the return-to-tab reminder and drop dismissal
memory.

## Open Decisions

None. The [plan](../slice-plans/003-frontend-update-reminder/PLAN.md) uses the
served `index.html`'s hashed entry scripts as the published identity.

## When to Surface

Second queued story when improving general user experience and release
reliability.

## Breadcrumbs

- Owner request on 2026-09-28: version the frontend like the backend, make users
  aware of frontend updates, and remind them to reload.
- Owner refinement on 2026-09-28: purpose is receiving improvements; old
  frontends are not promised to work after backend API changes, which adds
  urgency; reminding on every release is acceptable; exclusions accepted;
  priority confirmed.
- Release infrastructure: `infra/gcp/scripts/upload-frontend-static-to-gcs.sh`,
  `infra/gcp/path-routing/doughnut-routing.json`,
  `docs/gcp/prod-frontend-static-lb.md`, `infra/gcp/scripts/publish-application.sh`.
