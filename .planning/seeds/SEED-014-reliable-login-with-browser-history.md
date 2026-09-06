---
id: SEED-014
status: dormant
planted: 2026-09-07
planted_during: production iPad Safari login failure investigation
trigger_when: next product backlog story is selected
scope: medium
---

# SEED-014: Keep accumulated browser history from blocking login

## Why This Matters

For a returning Donut learner, accumulated browser data should not turn a
GitHub login into an unexplained HTTP 400 page. Prevent unnecessary cookie
growth and provide an actionable recovery path when a request is rejected.

The developer reported repeated production login failures in iPad Safari.
Clearing website data restored access; Safari reported about 200 MB of total
site data. That total is not the size of the cookies sent with requests.
Diagnostic requests with synthetic cookies produced a normal login-error
redirect at 4 KB and the same bare 400 page at 9 KB. Oversized request headers
are therefore a supported hypothesis, not a confirmed measurement of the
original iPad request. Production logs were unavailable because GCP credentials
needed reauthentication.

## Alternatives and Decision

- **Defer:** leaves a demonstrated login interruption and unexplained failure.
- **Smaller change:** moving search history alone reduces unnecessary request
  data but leaves users stranded if another oversized request occurs.
- **Manual workaround:** clearing website data restores access, but requires
  outside guidance and discards more browser state than necessary.
- **Selected direction:** the developer chose search-history migration and a
  useful recovery page together as one story. Increasing request-header limits
  and reducing general browser cache usage are excluded.

## Story Decomposition

<a id="story-1"></a>

### 1. Return to Donut without browser history blocking login or an unexplained error

**Status:** Refined and [slice-planned](../quick/047-reliable-login-with-browser-history/PLAN.md);
the plan is refined for direct execution; not implemented.

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

Conservative refinement assumption: if local storage cannot be written,
ordinary search and login remain usable without persistent search history;
do not fall back to growing a cookie. Do not discard valid legacy history
before it has been saved successfully. Automatic recovery from browser storage
restrictions is excluded.

**Key examples**

1. A returning learner has valid cookie history with `beta` more recent than
   `alpha` → the app loads and migration succeeds → the same recent searches
   remain available in that order, including after reload, and the legacy
   history cookie is gone.
2. History has migrated → the learner searches for `alpha` again → it appears
   once at the front and persists after reload without a search-history cookie.
   Adding a 101st distinct entry drops the oldest entry.
3. A migration runs again after newer local searches exist → the learner
   revisits the app → newer local history is not replaced by stale cookie data.
   An unreadable legacy cookie does not prevent ordinary use.
4. The browser cannot save local history → the learner searches or logs in →
   those actions remain usable, without creating a replacement history cookie
   or claiming that history was saved.
5. An already-blocked browser sends an oversized request to the login callback
   → the server rejects it before the app handles it → the response itself
   displays recovery instructions and a homepage link, even if further asset
   requests would also fail.
6. The learner follows the affected-site cleanup instructions → opens the
   homepage and starts a new login → ordinary login can proceed. The page does
   not invite them to retry the old OAuth callback URL or automatically clear
   any browser data.

**UI — proposed copy and presentation**

A simple page readable on iPad Safari, with recovery text visible immediately:

> **We couldn't process this request**
>
> Return to the Donut homepage and start again. If this keeps happening, try a
> new Private tab. Saved site cookies may be causing the problem.
>
> If a Private tab works, remove the affected site's cookies or website data
> in Safari settings, then return to the homepage and log in again. Removing
> site data may sign you out and clear locally saved history and preferences.
>
> **Return to Donut**

Include concise Safari instructions for finding the affected site's website
data. Exact copy is a proposal, not a separately approved design. Keep the
heading neutral for other browser 400 errors. Do not display OAuth parameters,
cookie contents, stack traces, or implementation details in the page.

**Evaluation:** Demonstrate history preservation and bounded future use without
the cookie, plus recovery content in the actual oversized-request response.
An ordinary application error-page test alone does not prove the early
rejection case. These are complementary acceptance signals within the single
developer-selected story.

**Effort hypothesis:** M (about 1–2 hours), medium confidence; assumes the
current deployment can serve recovery content for early request rejection.
Verify that boundary during slice planning. **Depends on:** No other product
story.

## Ordering and Scope Reduction

One story, explicitly grouped by the developer, queued first as an urgent
production usability exception to the current Git synchronization direction.
Both prevention and recovery are required for completion. No later story is
needed to make the outcome useful; do not add cache management, larger header
limits, automatic deletion of unrelated browser data, or broader login changes.

## Open Decisions

No unanswered questions currently require a developer decision. The unavailable
storage behavior and page copy above are conservative refinement proposals,
open to revision. The precise original cookie contents are unknown; do not
claim that total website storage caused the rejection. Confirm recovery-page
delivery at the actual server rejection boundary during planning and
verification; do not silently narrow it to errors handled inside the app.

## When to Surface

Next backlog selection. This seed captures scope and priority only; it does
not authorize implementation or deployment.

## Breadcrumbs

- Developer-approved options: migrate search history to `localStorage` and
  replace the bare 400 with actionable recovery guidance, grouped as one story.
- Prior investigation: `frontend/src/utils/searchKeyHistoryCookie.ts` stores
  search history in a cookie sent on all paths.
- Prior investigation: `backend/src/main/resources/application-prod.yml` and
  the production callback diagnostic described above.
- [Tomcat error report configuration](https://tomcat.apache.org/tomcat-11.0-doc/config/valve.html#Error_Report_Valve).
