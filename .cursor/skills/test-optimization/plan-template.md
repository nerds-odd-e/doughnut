# <Scope> test optimization

Status: in-progress

## Profiling baseline (YYYY-MM-DD)

Command:

- **N tests**, suite wall ~**Xs**
- Raw profile: `<local path>` — **do not commit**

### Top 10% slowest (n = ceil(N × 0.10))

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | | | |

### Grouping

- By file: **G1** groups
- Pairs of 2: **G2** groups
- Batches of 10: **G3** groups
- **Chosen:** … (smallest count)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

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

**Commits:**
