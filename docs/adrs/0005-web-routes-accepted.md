# 0005 — Web routes

**Status:** Accepted  
**Date:** 2026-09-01  
**Decision makers:** Terry Yin  
**Consulted:** None

## Context

Donut has three URL-shaped languages:

1. **SPA locations** — what the browser shows for a screen
2. **HTTP API** — JSON/auth/attachments the SPA and other clients call
3. **Notebook content links** — wiki links whose destinations are **Portable
   paths**, and Markdown links whose destinations are URLs
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

### Notebook links as web destinations

- A resolved Portable path to a **note** navigates to `noteShow`. A resolved
  Portable path with a **property** selector (`#prop:`, ADR 0004) navigates to
  `noteProperty`. The stored Portable path is unchanged.
- The rendered HTML `href` of a resolved wiki link is compiled from the named
  location. The Portable path, including any `#prop:` selector, stays in the
  stored wiki destination.
- A Markdown link retains its authored href through save, paste, render, and
  export. Donut recognizes it as a semantic note reference when the href is
  either a root-relative canonical note URL (`/n1234`) or an absolute HTTP(S)
  URL on a recognized Donut deployment origin
  (`https://doughnut.odd-e.com/n1234`). The href's note ID is authoritative;
  anchor text is display only. Absolute Donut note URLs are preferred for links
  intended to work outside their authoring host. Root-relative note URLs have
  their normal host-relative portability limitation.
- Unresolved (dead / pending) Portable paths do not navigate.

## Consequences

- Bookmarks and in-app clicks share one web identity: note id, or note id
  plus property key.
- Changing a path shape is a route-table (plus redirect) change; callers that
  used names keep working.

## Related

- [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md)
  (**Portable notebook tree**, **Portable path**, **Wiki link**, **Property**,
  **Property panel**)
- [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  (wiki Portable paths and Markdown URL semantics)
- [`doughnut-routing.json`](../../infra/gcp/path-routing/doughnut-routing.json)
  (backend-owned path hints for the load balancer)
