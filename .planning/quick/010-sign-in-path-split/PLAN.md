# Sign-in: SPA identify vs backend continue

**Status:** in progress (slices 1–2 done; slice 3 gated on 1–2 deployed)
**Type:** ad-hoc plan (`.planning/quick/`)
**Related:** proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md)

## Goal

`/users/identify` is only a Vue screen (non-prod password form). Production
OAuth uses a backend-only continue URL. The local-LB override
`spaShellInsteadOfBackendExactPaths` goes away. A production **backend**
must not present that password form (typed URL / old bookmark).

## Key design decisions

- **SPA keeps `/users/identify`** (named route `nonproductionOnlyLogin`).
  E2E `cy.visit('/users/identify')` and AGENTS.md stay on that path.
- **Backend bounce is `/login/continue?from=`** — same job as today’s
  `ApplicationController` identify mapping (`authenticated()` then redirect
  to `from`, default `/`). GitHub OAuth callback paths stay unchanged.
  `/login` (Spring default login page) and `/login/oauth2/` stay as they are;
  add **exact** `/login/continue` to `backendPathHints` (it does not match
  exact `/login`).
- **Do not use `import.meta.env.PROD` to choose the href or hide the form.**
  CI and `pnpm test` serve **`frontend/dist`** (Vite production build) against
  Spring profile `e2e` / `test`. That is the same class of bundle as GCS.
  Discriminator is the **backend** `prod` profile (public
  `GET /api/healthcheck` already returns `Active Profile: …`). Match a
  comma-separated profile token equal to `prod`, not a substring.
- **Login / 401 / circle-join** still call `loginOrRegisterAndHaltThisThread`.
  After a healthcheck read: `prod` → `/login/continue?from=`; otherwise →
  `/users/identify?from=`. Keep today’s `from=` concatenation (no extra
  encoding change).
- **Item 5:** on `/users/identify`, if the backend is `prod`, do **not**
  render username/password; navigate to `/login/continue?from=` (query `from`
  or `/`). Covers typed URLs after `/users/` is dropped from the LB hints.
- **Deploy order:** do not drop `/users/` from hints until the trampoline
  and continue URL are already on `main` (slices 1–2). Slice 3 is the LB
  cutover; it must not ship before 1–2 have been deployed.
- **ADR 0005:** keep path literals out of the ADR. Add a one-liner that the
  non-prod login screen is a named SPA route, production continue is a
  backend hint, and a `prod` backend must not show the password form.

## Slice sizing and commit contract

Each numbered slice is one commit-and-push boundary and targets about five
minutes of implementation plus focused tests. If a slice crosses the
5-minute scrutiny point because the change is larger—not because a single
required test is slow—stop and refine that slice again before continuing.

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Slices

### 1. Unauthenticated GET `/login/continue` identifies the user — Behavior `[x]`

Done: `ApplicationController` dual-maps `{/users/identify, /login/continue}`;
`CommonConfiguration` requires `authenticated()` on both; `doughnut-routing.json`
`exactPaths` includes `/login/continue`. MockMvc + path-routing tests cover it.

**Learning:** `/login` is already an exact hint; `/login/continue` must be listed
exactly (it does not inherit). Keep the dual-map until slice 3.

**Enables slice 2.**

### 2. Production backend does not show the password form; sign-in goes to continue — Behavior `[x]`

Done: `signInRedirect.ts` parses healthcheck `Active Profile:` (comma-separated
token equal to `prod`, not substring). Helper and identify page send the
browser to `/login/continue?from=…` on `prod` and keep `/users/identify` plus
the password form otherwise. No new JSON API.

**Learning:** ping parse + href live in one module; identify page must not
import the halt-thread helper. `production` as a token must not match.

**Enables slice 3** once slices 1–2 are **deployed** (not only on `main`).

### 3. `/users/identify` is SPA-owned; local LB needs no identify override — Behavior `[ ]`

**Pre-condition:** slices 1–2 are on `main` / deployed. **Trigger:** routing
and Spring stop claiming `/users/identify`. **Post-condition:**
`pathGoesToBackend('/users/identify')` is false; `/login/continue` still
true; `/users/settings` is not a backend prefix. Local proxy has no
`spaShellInsteadOfBackendExactPaths`. Spring no longer maps
`/users/identify`. E2E sign-in page still works via SPA.

- Remove `/users/` from `backendPathHints`; delete `localProxy` identify
  exception (keep local-lb’s empty-list support if the key is absent).
- Remove identify from `ApplicationController`, `CommonConfiguration`, and
  `NonProductConfiguration` permitAll.
- Path-routing tests: identify hits the SPA catch-all; continue stays MIG.
- Targeted E2E: `e2e_test/features/users/new_user.feature` (and circle
  invite new-user if still using the form).
- Update `docs/gcp/prod-frontend-static-lb.md` table (continue, not
  identify). Proposed ADR 0005 one-liner only — no path list.

## Out of scope

- Moving attachments, install, logout, or OAuth callback URLs under `/api`.
- Changing GitHub OAuth app redirect URIs.
- Open-redirect hardening of `from` (existing identify behavior).
- Removing local-lb’s generic `localProxy` mechanism unless it has no
  remaining JSON uses after this plan.

## Verify

- Slice 1: backend tests that cover `ApplicationController` / MockMvc;
  `pnpm test:path-routing`.
- Slice 2: `pnpm frontend:test` on the helper and identify page specs.
- Slice 3: `pnpm test:path-routing`; `pnpm cypress run --spec`
  `e2e_test/features/users/new_user.feature`.
