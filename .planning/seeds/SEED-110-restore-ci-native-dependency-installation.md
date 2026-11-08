---
id: SEED-110
status: active
planted: 2026-11-08
scope: story
---

# Restore CI native dependency installation

<a id="restore-ci-native-dependency-installation"></a>

### Restore CI native dependency installation

**Identity:** SEED-110#restore-ci-native-dependency-installation
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** Donut contributors get CI results when the `canvas` installer needs to build from source on Ubuntu.

**Scope:** Repair the locked dependency installation in the shared CI Node setup. In [run 42471440326](https://github.com/nerds-odd-e/doughnut/actions/runs/42471440326), three Cypress shards stopped before running tests because the prebuilt binary was unavailable and the native build could not find `pixman-1`.

**Proof:** Reproduce the missing dependency on Ubuntu 24.04, build and load `canvas` from source with the repaired prerequisites, and verify the complete CI workflow.
