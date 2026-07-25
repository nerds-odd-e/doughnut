# <Scope> test optimization

Status: in-progress

**Execution:** run via **execute-plan** (commit + push per phase).

## Profiling baseline (YYYY-MM-DD)

Command:

- **N tests**, suite wall ~**Xs**
- Eligible: **E** (E2E: profiled with `--env tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'`)
- Raw profile: `<local path>` — **do not commit**

### Top 10% slowest (n = ceil(E × 0.10))

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | | | |

### Grouping

- By file: **G_file** groups
- Batches of 3: **G_3** groups
- **Chosen:** … (fewer groups; tie → by file)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md`. Permanent skip (developer Jidoka only):
tag Scenario or Feature `@skipOptimizationDueToKnownNecessarySlowness`.

---

### Phase 1: <name>
Status: planned

**Tests:**
- `path/to/file` — "test name" (~Xms)

**Goals:**

**Verify:**

```bash
# focused command
```

---

### Phase N: Re-profile and close
Status: planned

| Metric | Before | After |
|--------|--------|-------|
| Test count | | |
| Suite wall | | |
| Top 10% total time | | |

**Candidates proposed this run:** (none / list)

**Commits:**
