---
id: SEED-128
status: active
planted: 2026-11-13
scope: story
---

# Faster end-to-end test feedback

<a id="speed-up-the-slowest-e2e-scenarios"></a>

### Speed up the slowest E2E scenarios

**Identity:** SEED-128#speed-up-the-slowest-e2e-scenarios
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** A developer waits less for the slowest fifth of the E2E scenarios,
with the same behavioral protection.

**Evidence:** CI run 43259460645 at `a13ab0cb63` (2026-11-12): 381 scenarios
sum to 814s; the slowest 77 sum to 353s. The leads with removable cost are the
large-notebook publication scenario (44s; 24s in run 43254480442), seeded with
11,000 notes to push the accepted Git listing above 1 MiB, and the four EPUB
reopen scenarios (6.4–7.1s each), which sleep 3.5s in each leave-and-return.

**Scope:** Test-only experiments on those leads, each kept only with a
measured gain: reach the large listing through longer paths with fewer notes,
and wait on observable reading-position saves instead of fixed sleeps.
Remaining slow scenarios stay as live candidates in
`.planning/TEST-OPTIMIZATION-CANDIDATES.md`.

**Preserved promises:** The installed CLI publishes small commits on a notebook
whose full Git listing exceeds 1 MiB, and a fresh clone receives them; an EPUB
reopens at the saved place, at its block start when the exact place no longer
resolves, and at the same paragraph after a width change.
