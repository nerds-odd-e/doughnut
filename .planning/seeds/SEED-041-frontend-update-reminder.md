---
id: SEED-041
status: dormant
planted: 2026-09-28
planted_during: owner request to capture frontend release awareness
trigger_when: improving how users receive updated frontend code
scope: medium
---

# SEED-041: Help users receive the latest frontend code

## Why This Matters

For Donut users with an existing browser session, a frontend release should become
visible so they know to reload and receive the updated code. The owner wants a
way to version the frontend, analogous to backend versioning, and to remind users
when their loaded frontend is older than the released version.

This records the requested experience; the current release and browser caching
behavior has not been investigated.

## Alternatives and Decision

Doing nothing or relying on users to reload without a reminder leaves them unaware
of updates. A version tied to every application release may be a simpler starting
point than detecting frontend-only changes. The owner prefers the frontend version
to change only when frontend code has changed since the previous release, but
explicitly treats this as optional because releases are infrequent.

Capture version identification, update awareness, and reload as one user-visible
story. Build and release support belong within that outcome; no versioning scheme
or detection mechanism has been selected.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
This seed authorizes no implementation or executable plan.

<a id="story-1"></a>

### Know when the frontend is updated and reload to receive the latest code

**Identity:** SEED-041#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

Donut users running an older frontend know that an update is available and can
reload to receive the latest released frontend code.

**Scope**

- Give the released frontend a version or equivalent release identity, analogous
  in purpose to backend versioning, through the build and release process.
- Establish whether the frontend loaded in the browser is older than the
  currently released frontend.
- Make users aware of the update and remind them that a reload is needed.
- Reloading in response to the reminder must load the latest released frontend,
  rather than leave the user on the same stale code.
- Prefer retaining the frontend version across releases with no frontend code
  changes. This is a desirable optimization, not a hard acceptance requirement;
  versioning each application release is acceptable if simpler.

**Key examples**

- A user has Donut open on the previous frontend → an updated frontend is
  released → the user sees an update reminder explaining the need to reload.
- A user sees that reminder → reloads → Donut runs the latest released frontend
  and no longer reports that loaded version as out of date.
- A user already runs the latest frontend → version comparison occurs → no
  outdated-frontend reminder is shown.

- **For / why:** Users receive current frontend improvements instead of
  unknowingly continuing with older browser code.
- **Evaluation:** A browser running an older release shows the reminder; a reload
  obtains the current release and clears the outdated-version condition.
- **Value / learning:** Determine whether frontend release identity and update
  detection can reliably guide users onto current code through the existing
  deployment and caching behavior.
- **Effort hypothesis:** M, low confidence until build, release, and caching
  behavior are inspected.
- **Depends on:** Existing frontend build and application release flow; no
  dependency on the preceding login-continuity story is established.
- **Safe stopping point:** Version detection, reminder, and effective reload work
  together as one usable outcome. Frontend-only version-change detection may be
  deferred without losing that value.

## Ordering and Scope Reduction

The owner selected this as the second product backlog story, after browser login
continuity. If scope needs reducing, drop frontend-only version-change detection
first and retain the update reminder and reliable reload outcome.

## Open Decisions

- What frontend release identity and build/release integration are simplest?
- When should the browser check for updates, and how should the reminder appear?
- How should the reload interaction handle unsaved user work?
- Is detecting frontend-only changes worthwhile, and what changes count toward
  that identity? The owner's preference does not block the core outcome.

## When to Surface

Select as the second queued story when improving general user experience and
release reliability.

## Breadcrumbs

- Owner request on 2026-09-28: version the frontend like the backend, make users
  aware of frontend updates, and remind them to reload. Prefer version changes
  only when frontend code changes, but this is optional given infrequent releases.
- Owner-selected priority: second story in the product backlog.
- Continuing session instruction: write directly on main, without committing.
