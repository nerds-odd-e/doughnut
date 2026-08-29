# 0005 — Web routes

**Status:** Proposed  
**Date:** 2026-08-29  
**Decision makers:** Terry Yin  
**Consulted:** None

## Context

Donut has three URL-shaped languages:

1. **SPA locations** — what the browser shows for a screen
2. **HTTP API** — JSON/auth/attachments the SPA and other clients call
3. **Inter-note links in notebook content** — portable wiki / path Markdown
   ([ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md))

They look similar (all start with `/`) and are easy to mix. Mixing them
breaks portability, bookmarks, or in-app navigation. This ADR is the routing
policy. Path literals live in the SPA route table, OpenAPI, and
[`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
— not here.

Wiki **spelling and resolution in the tree** stay in ADR 0004. This ADR only
covers how those links relate to **web** destinations.

## Decision

### Namespaces

- User screens are a Vue Router SPA (HTML5 history, site root). The load
  balancer rewrites unknown paths to the SPA shell. The backend does not
  serve or whitelist client routes. A new screen is a route-table change.
- JSON and most backend traffic stay under the API prefix. Other
  backend-owned paths (auth, attachments, install, and the like) stay at
  their current URLs. That exception list lives only in
  [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
  (`backendPathHints`); do not duplicate it here or invent it per page.
- Notebook content does not store SPA or API URLs as the form of an inter-note
  link. Portable tokens stay wiki or path Markdown (ADR 0004).

### SPA route convention

- SPA path literals for screens live only in `routeMetadata` (plus the `/d/`
  leftover-path rewrite).
- In-app `push` / `replace` / `:to` is always a named location (or a helper
  that returns one).
- An HTML `href` is allowed only on rendered anchors, and is **compiled** from
  a named location against that table — never a second concatenated copy of the
  compact note-show path.
- Unit tests: navigation assertions use named locations; rendered-href
  assertions use `noteShowHref`; path strings only in `routes.spec.ts`
  (matching / redirects) and inbound URL classifiers.
- Test routers use production `routes` or stub records from `routeMetadata`,
  not a hand-copied path dialect.
- Nested layouts (notebook sidebar, settings) are assembled in `routes.ts`
  from named metadata entries.
- The URL identifies the **server-side id**, not the portable path (ADR 0004:
  path is identity in the tree; note id is server-side). Note-show URLs are
  compact. Retired shapes (including a former site-wide prefix) redirect into
  the current table; do not keep a second tree of screens.
- Chrome on the same resource (for example conversation open) is **query on
  that named route**, not a new path.

### Using the convention

- Add a screen: named metadata entry, wire the page (nested layouts in
  `routes.ts` from those entries), push/link by **name**.
- Do not concatenate SPA path literals in components or stored markdown.
- Do not add client routes to the backend or to `backendPathHints` in
  `doughnut-routing.json`.
- The non-production login screen is a named SPA route, production continue is a
  backend path hint, and a `prod` backend must not present the password form.
- E2E prefers UI for the trigger; a direct location change is a Given
  shortcut. When it uses a route, first load may `visit` an href from
  the named table; later jumps use named `router.push`.

### Wiki links as web destinations

- A **live** wiki link in the web app navigates to the **note-show** named
  route (id). The stored token is unchanged.
- A path-Markdown href with a leading `/` is **bundle-relative** (ADR 0004),
  not an SPA path. Classify hrefs before routing: note-show SPA vs concept
  path vs external. Concept paths must not be fed to the router as locations.
- Paste or strip of a note-show (or legacy) URL in note content becomes a
  wiki token. SPA addresses are not the stored form of a wiki link.
- Unresolved (dead / pending) wiki links do not navigate. `http(s)` opens a
  new tab. Other in-app hrefs go through the router.

## Consequences

- Bookmarks and in-app wiki clicks share one web identity: note id.
- Exported trees stay portable: no Donut SPA URLs required in the markdown.
- Agents must not treat a concept path as a Vue location, or a note-show URL
  as portable identity.
- Changing a path shape is a route-table (plus redirect) change; callers that
  used names keep working.

## Related

- [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md) (**Wiki link**)
- [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  (token spelling and tree identity — not web routing)
- [ADR 0002 — Git-native notebooks](./0002-git-native-notebooks-backed-by-mysql.md)
  (lineage vs server-side id)
- [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
  (backend-owned path hints for the load balancer)
