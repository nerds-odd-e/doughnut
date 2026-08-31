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

### Usage

Letting a failure surface **loudly** is a legitimate production solution. An
uncaught failure becomes a Failure report so developers see that something
went wrong.

Handle an exception when a **business requirement** needs a specific
outcome, or when wrapping **improves the failure message**. Those handled
paths are the ones that need tests for the chosen outcome.

Code that is allowed to fail loudly needs no unit test of the failure. The
loud failure itself plays the same role as a unit test: it tells developers
something went wrong.

When a failure exists, prefer this order:

1. **Prevent** — make the invalid state unrepresentable.
2. **Propagate** — leave the failure visible in tests and production.
3. **Enrich** — add context when the raw failure would be unclear; keep the
   original cause.
4. **Catch** — recover, retry, compensate, contain a best-effort operation,
   or improve the message as above.

A catch always has one of those deliberate outcomes.

## Consequences

- A scheduled-job loop stays one Failure report and one GitHub issue while
  the failures stay similar and consecutive.
- Developers investigate in Donut; GitHub carries identity and count.
- HTTP and scheduled failures share one consecutive sequence.
- Loud production failures are an intended signal; handled exceptions are
  the tested ones.

## Pros

- One name already on the admin screen.
- Consecutive grouping matches a broken run of the same failure.
- Fail-loud is an explicit production strategy.

## Cons

- Similarity can still merge distinct bugs of the same type at the same
  site, or split unusual HTTP paths.
- Anyone who treated each throw as its own Failure report needs the glossary
  change (one report = one consecutive run).

## Related

- [ADR 0001](./0001-ubiquitous-language.md) — **Failure report**
