# 0005 — Web routes

**Status:** Proposed  
**Date:** 2026-08-29  
**Decision makers:** Terry Yin  
**Consulted:** None

## Context

Donut has three URL-shaped languages:

1. **SPA locations** — what the browser shows for a screen
2. **HTTP API** — JSON/auth/attachments the SPA and other clients call
3. **Wiki / path Markdown in notebook content** — note and property links
   whose destinations are **Portable paths**
   ([ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md))

The three languages share a leading `/`; this ADR defines their relationships
at the web boundary.

## Decision

### Namespaces

- User screens use Vue Router with HTML5 history at the site root.
  `routeMetadata` is the canonical registry of screen names and absolute
  paths; route assembly groups those entries under shared layout parents.
  The load balancer serves unmatched browser paths with the SPA shell.
- The HTTP API uses `/api` and is described by OpenAPI.
  [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
  defines the load balancer's backend-owned route set.
- ADR 0004 defines **Portable path** spelling and resolution. Notebook content
  stores inter-note links as wiki or path Markdown.

### SPA locations

- In-app navigation (`push` / `replace` / `:to`) is a named location (or a
  helper that returns one). An HTML `href` is allowed only on rendered
  anchors, and is compiled from a named location against that table —
  never a second concatenated copy of a path.
- The URL identifies the **server-side note id**, not the **Portable path**
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

- A resolved Portable path to a **note** navigates to `noteShow`. A resolved
  Portable path with a **property** selector (`#prop:`, ADR 0004) navigates to
  `noteProperty`. The stored Portable path is unchanged.
- A path-Markdown link destination with a leading `/` is **bundle-relative**
  (ADR 0004), not an SPA path. The two languages share a `/` prefix; they are
  not disjoint by string shape. Treat a value by **context**: notebook content
  vs a compiled location. Do not classify a leading `/` as a Vue path.
- The HTML `href` of a wiki or path-Markdown anchor is compiled from that
  named location. The Portable path, including any `#prop:` selector, stays in
  the stored link destination — never as a navigable `href`.
- Paste or strip of a `noteShow` or `noteProperty` (or legacy) URL in note
  content becomes a wiki link only after the note id is resolved to its
  Portable path (including cross-notebook qualification when needed).
  Property URLs add the encoded `#prop:` component. Anchor text is display
  text, never part of the Portable path. SPA addresses are not the stored form
  of a wiki link.
- Unresolved (dead / pending) Portable paths do not navigate.

## Consequences

- Bookmarks and in-app clicks share one web identity: note id, or note id
  plus property key.
- A **Portable notebook tree** requires no Donut SPA URLs for note or property
  links; Donut-authored notebook links store Portable paths.
- Agents must not treat a Portable path as a Vue location, or a note-show /
  note-property URL as a Portable path. Do not put a Portable path directly on
  an HTML `href` the browser can follow.
- Changing a path shape is a route-table (plus redirect) change; callers that
  used names keep working.

## Related

- [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md)
  (**Portable notebook tree**, **Portable path**, **Wiki link**, **Property**,
  **Property panel**)
- [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  (Portable path spelling and resolution, including `#prop:` — not web routing)
- [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
  (backend-owned path hints for the load balancer)
