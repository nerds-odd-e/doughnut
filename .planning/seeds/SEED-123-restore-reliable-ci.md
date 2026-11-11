---
id: SEED-123
status: active
planted: 2026-11-11
scope: story
---

# Restore reliable CI

<a id="restore-reliable-ci"></a>

### Restore reliable CI

**Identity:** SEED-123#restore-reliable-ci
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** Resolve the reported failing CI and establish whether an intermittent test is responsible.

Scope: inspect current failing Actions runs, reproduce the observed failure, and make a bounded root-cause repair. If the failure is flaky, analyze its triggering conditions and remove the source of nondeterminism. Verify the affected tests and CI. Preserve unrelated work.

Expected: the affected CI check passes reliably for the intended behavior. Actual: the developer reports failing CI; the failing job, revision, cause, and intermittency remain to be established.

Execution: bug-fixing diagnosis followed by one bounded planless repair with a ten-minute implementation limit and no replanning. Larger or inconclusive work remains with this story and its evidence.
