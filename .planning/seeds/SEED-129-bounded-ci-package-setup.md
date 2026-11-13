---
id: SEED-129
status: active
planted: 2026-11-13
scope: story
---

# Bounded CI package setup

<a id="bound-ci-system-package-installation"></a>

### Bound CI system package installation

**Identity:** SEED-129#bound-ci-system-package-installation

A Donut developer sees a prompt setup failure with enough detail to diagnose an
unreachable system package mirror. Jobs with working mirrors continue to run
their existing tests.

In [DD-485](../../DonutRetrospectiveFindings.md#dd-485), eleven jobs in each of
two GitHub Actions runs failed before testing, after about 50 minutes in the
package installation step. The same execution also exposed a gap in Open Dough’s
CI observation, tracked in the [shared findings](../../DearDough.md#dd-485).
This story owns Donut’s system package setup.

Choose a bounded wait and retry policy for the affected package setup, with
room for a slow successful install. Verify an unreachable mirror produces the
setup diagnosis and a failed job within that bound, without running tests for
that job. Verify a healthy mirror still installs the required packages and runs
the existing CI job. Reuse the existing setup actions for all their callers.
