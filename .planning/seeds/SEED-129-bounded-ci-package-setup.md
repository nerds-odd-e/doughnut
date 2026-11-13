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
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/097-bounded-ci-system-packages/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"37617313a7f3db71a30cf787b9da1d594e2f0569a186a7bfa182d1550afde79c","plan":"5957e2753c795225447026b34e340bae3f5f0da64af061f3740ce5214f32dd1a"}}
```

**Goal**

A Donut developer gets a timely, diagnosable CI setup failure when a system
package mirror is unreachable, while jobs with working mirrors install their
required packages and continue to their existing checks.

In [DD-485](../../DonutRetrospectiveFindings.md#dd-485), eleven jobs in each of
two GitHub Actions runs failed before testing, after about 50 minutes in the
package installation step. The same execution also exposed a gap in Open Dough’s
CI observation, tracked in the [shared findings](../../DearDough.md#dd-485).
This story owns Donut’s system package setup.

**Scope**

- Bound each system-package phase in the existing Node/Cypress and MySQL binary
  setup actions to ten minutes from phase start, including acquisition retries,
  with at most ten additional seconds to terminate outstanding work. This cuts
  the observed fifty-minute wait while leaving room for a slow successful
  install. The limit applies to the package phase, not the whole CI job.
- Allow at most two retries of transient package downloads within that shared
  deadline. Exhaustion or deadline expiry fails setup and its job, preserving
  the underlying package-manager/download output and identifying the failing
  phase and its time budget. Tests for that failed job do not start: running
  them after incomplete dependency setup would give misleading feedback.
- Include APT metadata refresh and installation for the canvas build packages,
  and the compatibility `.deb` downloads and APT installation for MySQL's
  system libraries. Reuse the existing actions for all their callers, including
  lint, frontend, other tests, E2E, application packaging, and CLI release setup.
- Keep the required packages, supported runner behavior, and downstream checks.
  Slow successful acquisition and recovery from a transient download failure
  within the deadline remain successful setup.

Scope limits: this story does not change Open Dough's CI observation, select
new mirrors, add cross-job retry coordination, or change Node/pnpm/Cypress,
MySQL archive, Docker, Java, or Cloud VM setup policies. Existing downstream
release work is preserved; proving setup does not require publishing a release.

**Key examples**

1. A Linux job's package mirror does not resolve → its shared setup action runs
   → setup fails within ten minutes plus the ten-second termination allowance;
   the log identifies the package phase and budget and retains the failing
   mirror's diagnostic; the job's test step is skipped.
2. A download stalls despite retries → the package phase reaches its deadline
   → outstanding package work terminates within the allowance and the job
   reports setup failure with its original output.
3. A mirror is slow but the required packages finish inside ten minutes → setup
   completes → the existing job continues to its checks, including tests where
   that job has them.
4. A package download has one transient failure and then succeeds within the
   retry allowance and deadline → setup completes → downstream checks run.
