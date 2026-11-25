# 0006 — Failure handling

**Status:** Accepted  
**Date:** 2026-08-31  
**Decision makers:** Terry Yin
**Consulted:** Team

## Context

The product needs one Failure report (and one GitHub issue) for a consecutive
run of similar failures, an occurrence count, and a throttled count update on
that issue. Investigation detail stays in Donut.

## Decision

### Names

- **Failure report** — A consecutive run of **similar** failures. One admin
  list entry. One GitHub issue.
- **Occurrence count** — How many times this Failure report’s similar
  failure has fired in this run (including the first).
- **Fingerprint** — Internal similarity key for a failure.

### Similar

Two failures are **similar** when they are the same kind of failure from the
same origin. Kind is the exception type together with the application site.
Origin is the scheduled (or other background) source, or the HTTP request.
Similarity is independent of user, query string, identifier values in the
path, exception message, and stack line numbers.

### Consecutive run

A Failure report is a consecutive run of similar failures. Similar failures
with a dissimilar failure between them are two Failure reports.

### GitHub

Each Failure report has one GitHub issue. Further occurrences of that report
update the issue with the latest count. Count updates are debounced at six
hours. Investigation detail stays in Donut.

### Reporting and tests

Donut records unexpected failures that reach its reporting boundary as
Failure reports.

Test the outcome when Donut handles a failure or enriches its message. A path
that relies on Donut’s existing Failure report mechanism needs no unit test
solely to prove propagation.

## Consequences

- A scheduled-job loop stays one Failure report and one GitHub issue while
  the failures stay similar and consecutive.
- Developers investigate in Donut; GitHub carries identity and count.
- HTTP and scheduled failures share one consecutive sequence.

## Pros

- One name already on the admin screen.
- Consecutive grouping matches a broken run of the same failure.

## Cons

- Similarity can still merge distinct bugs of the same type at the same
  site, or split unusual HTTP paths.
- Anyone who treated each throw as its own Failure report needs the glossary
  change (one report = one consecutive run).

## Related

- [ADR 0001](./0001-ubiquitous-language.md) — **Failure report**
