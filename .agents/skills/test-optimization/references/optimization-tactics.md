# Test Optimization Tactics

Work only in the current group and use the first applicable tactic.

For all layers, delete or merge redundant tests, hoist shared setup, narrow
broad fixtures, and parameterize copy-pasted cases.

For unit/component tests:

- Frontend: prefer `data-testid`, `getByText`, or `querySelector` over role
  queries; replace polling with `flushPromises`, `nextTick`, or fake timers.
- CLI: share Ink helpers, wait on observable frames, and use `test.each`.
- Backend: slim `makeMe` fixtures and multipart/OpenAPI setup, merge redundant
  methods, and use controller slices instead of full-stack tests when possible.

For Cypress/Cucumber E2E tests, prefer testability injection, API setup, direct
routes, intercept waits, removal of redundant steps, cached expensive prep, and
`invoke('val')` plus `input` for long markdown.

Never commit `@focus`/`@only`, and never add
`@skipOptimizationDueToKnownNecessarySlowness` without developer Jidoka.
