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

- SPA path literals for screens live only in `routeMetadata`. Legacy rewrites
  are explicit exceptions to the canonical table.
- In-app navigation (`push` / `replace` / `:to`) is a named location (or a
  helper that returns one). An HTML `href` is allowed only on rendered
  anchors, and is compiled from a named location against that table —
  never a second concatenated copy of a path.
- Shared layouts are assembled from named metadata entries rather than
  duplicating paths.
- The URL identifies the **server-side note id**, not the portable path
  (ADR 0004). A **property** adds the **authored key** (no property
  surrogate id). Named route helpers receive the exact decoded key and
  Vue Router serializes it as one path parameter; callers do not pre-encode
  it. Note-show URLs are compact. Nested property path stays under that
  note. Retired shapes redirect into the current table; do not keep a
  second tree of screens.
- Chrome on the current resource (conversation open) is **query on that
  named route**, not a new path.
- A **property** is a nested resource, not chrome: named route
  `noteProperty`, nested under the note URL family, last segment the
  authored key. It uses the same note page and shared notebook-layout parent
  as `noteShow`. Opening or closing the **property panel** **replaces** within
  the note family; inbound links **push**. A surface that already identifies a
  property navigates directly to `noteProperty` — not a side channel on
  `noteShow`.
- `noteShow` and `noteProperty` are one **note route family** for notebook
  chrome, sidebar state, active navigation, and conversation query. A
  route to a readable note with a missing property keeps the note visible
  but shows an explicit unresolved-property state; it must not silently
  look like `noteShow`.
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
  content becomes a wiki token only after the note id is resolved to its
  portable target (and cross-notebook qualification when needed).
  Property URLs add the encoded `#prop:` component. Anchor text is display
  text, never portable identity. SPA addresses are not the stored form of
  a wiki link.
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

- [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md) (**Wiki link**, **Property**, **Property panel**)
- [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  (token spelling, including `#prop:`, and tree identity — not web routing)
- [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
  (backend-owned path hints for the load balancer)
