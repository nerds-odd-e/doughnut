---
id: SEED-014
status: completed
planted: 2026-09-07
completed: 2026-09-07
---

# SEED-014: Keep accumulated browser history from blocking login

<a id="story-1"></a>

### 1. Return to Donut without browser history blocking login or an unexplained error

**Status:** Completed on 2026-09-07. Recovery response, tablet presentation,
local search persistence, and pre-login legacy migration are implemented and
covered by real HTTP and mounted browser tests.

**Goal**

Returning learners can retain their recent searches without that history
consuming the login request's header budget. If the server still rejects a
browser request, they receive guidance to regain access instead of an
unexplained error. This supports reliable access to their notes and learning.
It does not promise to eliminate every cause of login failure.

**Scope**

The developer selected prevention and recovery together as one story:

- Store search history in bounded `localStorage` instead of a cookie. Preserve
  its existing user behavior: newest first, duplicate searches moved to the
  front, at most 100 entries, and the existing 512-character per-query bound.
  Keep the existing search-history interaction; no history-management UI or
  cross-device synchronization is added.
- On an app visit where migration can run, preserve valid existing cookie
  history in local storage and expire the legacy search-history cookie after
  successful persistence. Later visits and searches use local storage without
  recreating that cookie. Repeated migration must not overwrite newer local
  history. Invalid legacy history must not obstruct search or login.
- Keep authentication in its existing session cookies. Do not automatically
  clear authentication cookies, unrelated cookies, cached content, or other
  browser data as part of migration or error-page display.
- Replace the bare server-generated browser 400 page with readable recovery
  guidance and a homepage link. Cover the observed oversized-header rejection
  at the login callback, including rejection before the normal application
  handles the request. The recovery content must remain readable without the
  app or additional assets loading successfully. Keep the error response an
  error; do not turn it into a successful response or an automatic retry loop.
- An already-blocked browser is not guaranteed to run migration. Its recovery
  page must explain how to start a fresh login, try a Private tab, and remove
  only the affected site's cookies/website data if necessary. Explain that
  manual cleanup can sign the user out and remove local preferences/history.
  Do not imply that all 400 responses mean oversized cookies or that the
  reported 200 MB of website data was sent to the server.

If local storage cannot be written,
ordinary search and login remain usable without persistent search history;
do not fall back to growing a cookie. Do not discard valid legacy history
before it has been saved successfully. Automatic recovery from browser storage
restrictions is excluded.

Larger request-header limits, general cache cleanup, OAuth configuration changes,
and recovery for errors generated upstream of Tomcat are excluded.
