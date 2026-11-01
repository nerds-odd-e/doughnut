# Observe Cucumber behavior beside its intent

<a id="cucumber-proof"></a>

### Record ordinary Cypress/Cucumber runs as published proof

**Identity:** SEED-096#cucumber-proof
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** Readers of Donut's published Gherkin can see the latest observed
scenario outcome, when it ran, the run-start commit, and the tested feature
version through Open Dough Intent.

**Source:** Terry's 2026-11-02 implementation-and-landing handoff, following
[Open Dough 0.3.81's Cucumber proof contract](https://github.com/terryyin/open-dough/blob/v0.3.81/src/skills/dough-test-optimization/references/cucumber-proof.md)
(release commit `2561a018c1d57f66b3636abd911a8a875d6f0e46`).

**Scope:** Project-owned integration with Cypress and
`@badeball/cypress-cucumber-preprocessor`, plus the Gherkin location's proof
declaration in `.planning/open-dough.json`. Preserve composed event handlers,
runner ownership, output, diagnostics, exit meaning, retries, and concurrency.
Recording is silent and best effort; managed Open Dough files stay managed.

**Key examples:**

- Ordinary focused and full runs record only executed scenarios within the
  declaring locations, in version-1 proof files. Dry runs record nothing.
- A retried outline records its original Gherkin name, parsed example cells,
  final attempt outcome, all attempts' duration, and final completion time.
- Tested feature bytes and run-start Git HEAD identify the observation;
  later edits or commits do not relabel it.
- Each run owns a unique file, atomically rewritten after each final scenario;
  interruption preserves finished scenarios and concurrent runs do not collide.
- Meaningful ordinary tests generate real proof committed with the integration;
  the suite is not run solely to collect observations.

**Failure policy:** Carry [Open Dough's failing and flaky test policy](/Users/terryyin/git/open-dough/AGENTS.md#failing-and-flaky-tests)
as written into the plan, delegation briefs, and repair handoffs, together with
Donut's applicable guidance. Failures remain owned through applicable final CI.

**Delivery:** Validate the real runner and relevant project checks; explicitly
invoke Dough Land after settlement and publish to `origin` `refs/heads/main`.
