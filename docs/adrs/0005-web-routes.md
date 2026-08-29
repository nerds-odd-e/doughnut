# 0005 — Web routes

**Status:** Proposed  
**Date:** 2026-08-29  
**Decision makers:** Terry Yin  
**Consulted:** None

## Context

Donut has three URL-shaped languages:

1. **SPA locations** — what the browser shows for a screen
2. **HTTP API** — JSON/auth/attachments the SPA and other clients call
3. **Wiki / path Markdown in notebook content** — portable note and
   property links
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
  their current URLs. That exception list lives only with the load balancer;
  do not invent it per page.
- Notebook content does not store SPA or API URLs as the form of an inter-note
  link. Portable tokens stay wiki or path Markdown (ADR 0004).

### SPA locations

- SPA path literals for screens live only in `routeMetadata` (plus the `/d/`
  leftover-path rewrite).
- In-app navigation (`push` / `replace` / `:to`) is a named location (or a
  helper that returns one). An HTML `href` is allowed only on rendered
  anchors, and is compiled from a named location against that table —
  never a second concatenated copy of a path.
- Unit tests: navigation assertions use named locations; rendered-href
  assertions use `noteShowHref` / `namedLocationHref`; path strings only in
  `routes.spec.ts` (matching / redirects) and inbound URL classifiers.
- Test routers that resolve named screen locations use production `routes`
  or `dummyRouteRecordsFromMetadata` (the `routeMetadata` table with dummy
  components; no page imports). Catch-all `/` or `/:pathMatch(.*)*` routers
  and `useRoute` stubs with `path: "/"` are not a second screen dialect.
- Nested layouts (notebook sidebar, settings) are assembled in `routes.ts`
  from named metadata entries.
- The URL identifies the **server-side note id**, not the portable path
  (ADR 0004). A **property** adds the **authored key** (no property
  surrogate id). Note-show URLs are compact. Nested property path stays
  under that note. Retired shapes redirect into the current table; do
  not keep a second tree of screens.
- Chrome on the current resource (conversation open) is **query on that
  named route**, not a new path.
- A **property** is a nested resource, not chrome: named route
  `noteProperty`, child of `noteShow`, last segment the authored key.
  Same note page with that property open. Later expansion keeps this
  path (or a child of it). Opening or closing the property **replaces**
  within the note family; inbound links **push**. Product surfaces that
  already know a property (next to assimilate, answered question, memory
  tracker) navigate to `noteProperty` — not a side channel on `noteShow`.
- E2E navigation goes through `e2e_test/start/router.ts` (`visitNamed` /
  named `push`). Compile hrefs with `namedLocationHref` / `noteShowHref`.
  Page objects and steps do not call `cy.visit` with SPA path strings
  (`scripts/check_e2e_spa_visit_gate.sh` in CI).

  | Intent | Mechanism |
  | --- | --- |
  | Unique trigger **is** in-app navigation | UI |
  | Given-shaped shortcut (including Gherkin `When I visit …` when the unique behavior is **on** that screen) | Named `router.push` after first load |
  | First SPA load, inbound URL, or **explicit remount** | `cy.visit` of href **compiled from the named table** |

- Recall: `visitRecallPage` is remount (`visitNamed('recall')`);
  `navigateToRecallPage` is sidebar UI. Gherkin `When I visit recall`
  stays remount — do not convert it to sidebar.

### Wiki links as web destinations

- A live **note** token navigates to `noteShow`. A live **property** token
  (`#prop:`, ADR 0004) navigates to `noteProperty`. The stored token is
  unchanged.
- A path-Markdown href with a leading `/` is **bundle-relative** (ADR 0004),
  not an SPA path. The two languages share a `/` prefix; they are not
  disjoint by string shape. Treat a token by **context**: notebook content
  vs a compiled location. Do not classify a leading `/` as a Vue path.
- The HTML `href` of a wiki or path-Markdown anchor is compiled from that
  named location. The concept path and `#prop:` key stay in the stored
  token — never as a navigable `href`.
- Paste or strip of a `noteShow` or `noteProperty` (or legacy) URL in note
  content becomes a wiki token (property URLs become `#prop:`). SPA
  addresses are not the stored form of a wiki link.
- Unresolved (dead / pending) tokens do not navigate.

## Consequences

- Bookmarks and in-app clicks share one web identity: note id, or note id
  plus property key.
- Exported trees stay portable: no Donut SPA URLs required in the markdown.
- Agents must not treat a concept path as a Vue location, or a note-show /
  note-property URL as portable identity. Do not put a concept path on an
  HTML `href` the browser can follow.
- Changing a path shape is a route-table (plus redirect) change; callers that
  used names keep working.

## Related

- [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md) (**Wiki link**, **Property**)
- [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  (token spelling, including `#prop:`, and tree identity — not web routing)
- [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
  (backend-owned path hints for the load balancer)
