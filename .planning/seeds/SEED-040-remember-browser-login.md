---
id: SEED-040
status: dormant
planted: 2026-09-28
planted_during: owner request to capture the returning-user login experience
trigger_when: improving login continuity for returning production users
scope: medium
---

# SEED-040: Let returning users stay signed in on their browser

## Why This Matters

For returning Donut users, a period of inactivity should not routinely require
another trip through GitHub OAuth before they can access their notes and learning.
The owner reports that production login currently checks OAuth repeatedly and
frequently asks users to log in again after a while away. This is a reported
experience, not an investigation of the current authentication implementation.

GitHub OAuth remains the only way to establish a production login. The desired
change is continuity of an already authorized login on the user's browser.

## Alternatives and Decision

Doing nothing, or asking users to repeat the existing GitHub login whenever their
session ends, retains the reported interruption. Extending the existing session
lifetime may be a simpler way to achieve the outcome and should be considered
during refinement; whether it suffices has not been established.

The owner's proposed direction is a temporary credential or private authorization
retained by the browser and validated by Donut. A valid credential lets the user
return without going back to GitHub. This is a solution hypothesis, not a selected
token format, storage mechanism, or authentication design.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
This seed captures one story and authorizes no implementation or executable plan.

<a id="story-1"></a>

### Return to Donut without repeating GitHub login while browser authorization remains valid

**Identity:** SEED-040#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

Returning production users can resume using Donut after a period of inactivity
without repeating GitHub OAuth while their browser's retained authorization is
still valid, reducing interruption to note-taking and learning.

**Scope**

- Keep GitHub OAuth as the sole way to establish a production login.
- Retain temporary authorization on the user's browser after a successful login,
  so Donut can validate it when the user returns and restore authenticated access
  without another GitHub OAuth round trip.
- Explicit logout ends that browser's retained authorization. Logging in again
  requires GitHub OAuth.
- If retained authorization is missing, no longer matches, or otherwise fails
  validation, require GitHub OAuth again before granting authenticated access.
- Consider sharing appropriate infrastructure with the user-token feature,
  potentially through a temporary token. The owner raised this as speculation;
  reuse and token design are not requirements or prerequisites for this story.

**Key examples**

- Successful GitHub login → user leaves Donut inactive for a while, then returns
  with valid retained browser authorization → authenticated access resumes
  without another trip to GitHub.
- Signed-in user → explicit logout, then a later visit requiring login → the old
  browser authorization does not sign them back in; GitHub OAuth is required.
- Previously signed-in browser → retained authorization is missing or fails
  validation on return → authenticated access requires GitHub OAuth again.

- **For / why:** Returning production users avoid frequent login interruptions.
- **Evaluation:** A returning user can access their authenticated Donut experience
  without repeating GitHub OAuth while retained authorization is valid; logout
  and invalid authorization still require a fresh GitHub login.
- **Value / learning:** Establish whether temporary browser authorization can
  provide useful login continuity, and whether existing session or user-token
  infrastructure can support that outcome simply.
- **Effort hypothesis:** M, low confidence pending inspection of existing session
  behavior and agreement on credential lifetime and invalidation.
- **Depends on:** Existing GitHub OAuth login; no new login provider or separate
  user-token infrastructure story is required by this capture.
- **Safe stopping point:** Login continuity works as one usable end-to-end
  behavior, with explicit logout and validation failure handled together.

## Ordering and Scope Reduction

Queue this single story under the current user-experience and hardening direction.
Keep token infrastructure reuse optional; it must not expand this story into a
general token redesign. No additional stories or implementation slices are selected.

## Open Decisions

- How long should temporary browser authorization remain valid, and should
  inactivity or continued use affect that lifetime? No duration or indefinite
  login promise has been selected.
- What existing session behavior causes the reported repeat login, and could a
  smaller session-policy change deliver the desired continuity?
- What events beyond explicit logout invalidate retained authorization?
- Can the user-token feature share suitable infrastructure without coupling
  browser login policy to unrelated token uses?

## When to Surface

When selecting work to improve the returning-user login experience.

## Breadcrumbs

- Owner request on 2026-09-28: capture browser-retained temporary authorization,
  preserve GitHub OAuth, require it again after logout or validation mismatch,
  and record possible infrastructure sharing with the user-token feature as an
  idea to investigate.
- Owner instruction: write directly on main and leave the changes uncommitted.
- [Earlier browser-history login recovery story](SEED-014-reliable-login-with-browser-history.md):
  related login reliability work with a different outcome.
