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

For returning Donut users, a deploy, a period of inactivity, or closing the
browser should not routinely require another trip through GitHub OAuth before
they can access their notes and learning. Being signed out mid-recall or
mid-edit also costs work: a save that meets a signed-out session asks the user
to log in and lose the current changes.

GitHub OAuth remains the only way to establish a production login. The desired
change is continuity of an already established login on the user's browser.

## Diagnosis

Refinement on 2026-09-28 found that the reported repeat login is lost session
state, not GitHub re-checking authorization:

- Production keeps HTTP sessions only in each backend instance's memory; no
  shared session store is configured.
- Every production deploy replaces both backend instances, so each release signs
  out every browser user. Releases ran 15 times in the 8 days before refinement.
  Autohealing an instance has the same effect.
- Sessions expire after the default 30 minutes idle.
- The session cookie has no lifetime, so closing the browser drops it.
  Non-production profiles always remember the login, which is why development
  and E2E runs never show the problem.

## Alternatives and Decision

- A new browser credential or remember-me token (the original hypothesis) adds a
  second login mechanism beside the session and still loses session-held state
  on every deploy.
- Reusing user tokens (the CLI and MCP bearer tokens) was rejected: they never
  expire, are stored in plain text, and would tie browser login policy to
  unrelated token uses.
- **Decision (owner, 2026-09-28):** keep browser sessions in the existing MySQL
  database so they survive deploys and are shared by all backend instances, with
  a 30-day lifetime renewed by use and a session cookie that survives browser
  restarts. Because a long-lived cookie raises the cost of cross-site request
  forgery (production has CSRF protection disabled), the cookie must be `Secure`
  and `SameSite=Lax`.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
This seed captures one story.

<a id="story-1"></a>

### Return to Donut without repeating GitHub login while browser authorization remains valid

**Identity:** SEED-040#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-remember-browser-login/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"62091daaf2d4923bb868ee4adbf27c4e9234e727d4ea2b038bc6cb83bf3ef207","plan":"d2d9f7aab4f05fa1773746211df07ae992e37bf4fbaeb779f9b67fe31f99ba4c"}}
```

**Goal**

A signed-in Donut user stays signed in on their browser across deploys, idle
periods, and browser restarts until they log out or 30 days pass without using
Donut, so note-taking and learning are not interrupted by repeated GitHub logins
or lost edits.

**Scope**

- GitHub OAuth stays the only way to establish a production login.
- A browser session lasts until 30 days after its last use; each use renews it.
- The session survives backend deploys and restarts, and every backend instance
  recognizes it.
- The session survives closing and reopening the browser.
- Explicit logout ends that browser's session. A later sign-in goes through
  GitHub OAuth (GitHub may complete it without asking for credentials).
- A missing, unknown, or expired session is treated as signed out.
- **Constraint:** the session cookie is `Secure` and `SameSite=Lax`, because the
  longer lifetime makes cross-site request forgery more costly while production
  CSRF protection is disabled.
- CLI and MCP bearer-token authentication is unchanged.

**Deferred and excluded**

- New credential formats, remember-me tokens, and any reuse of user tokens.
- Session management UI: listing signed-in devices, signing out other devices,
  or a "remember me" choice (sessions are always remembered).
- Ending sessions when GitHub authorization is revoked.
- Changing the confirm-and-lose-changes behavior when a save meets a signed-out
  session.
- User-token hardening (hashing, expiry, last-use tracking).
- Enabling CSRF protection.

**Key examples**

- Signed in → Donut is redeployed (all backend instances replaced) → the next
  page load or save works and the user is still signed in.
- Signed in → browser closed and reopened the next day → still signed in, no
  GitHub redirect.
- Last used Donut 29 days ago → visit → still signed in, and the 30 days start
  again. Last used 31 days ago → visit → signed out; logging in goes through
  GitHub OAuth.
- Signed in → explicit logout → a later visit on that browser is signed out.
- A browser presents a session cookie Donut does not recognize → treated as
  signed out.
- The session cookie issued at sign-in is `Secure` and `SameSite=Lax`.

- **For / why:** Returning production users avoid repeated login interruptions
  and lost edits.
- **Evaluation:** After release, the owner stays signed in across the next
  production deploy and a browser restart; logout still signs out.
- **Value / learning:** Removes a daily interruption with a standard shared
  session store instead of a new login mechanism.
- **Effort hypothesis:** M, medium confidence. Moving sessions to MySQL requires
  session contents to be storable, which rules out today's session-scoped
  controllers.
- **Depends on:** Existing GitHub OAuth login and the production MySQL database.
- **Safe stopping point:** Sessions persisted, long-lived, and cookie-hardened
  together as one release.

## When to Surface

When selecting work to improve the returning-user login experience.

## Breadcrumbs

- Owner request on 2026-09-28 captured browser-retained authorization; the same
  day's refinement replaced that hypothesis with the decision above.
- [Earlier browser-history login recovery story](SEED-014-reliable-login-with-browser-history.md):
  related login reliability work with a different outcome.
- Ordering: sessions that survive deploys keep users on older frontend code
  longer, which raises the value of
  [the frontend update reminder](SEED-041-frontend-update-reminder.md).
