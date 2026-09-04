# Test Optimization Verification

Run focused tests for the current group first; widen only when shared helpers
changed.

```bash
# E2E
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/a.feature,e2e_test/features/b.feature

# Frontend
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/SomePage.spec.ts

# CLI
CURSOR_DEV=true nix develop -c pnpm cli:test

# Backend class
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.donut....ClassName"
```

E2E groups need at least three consecutive green runs on touched specs before
closing a slice.

Before committing, review the intended staged diff. The hook runs
`lint:changed` and never formats or mutates the Git index, so deliberate partial
staging remains intact.
