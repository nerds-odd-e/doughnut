---
id: SEED-099
---

# Deliberately retain Cucumber proof

<a id="deliberate-proof-recording"></a>

### Deliberate proof recording

**Identity:** SEED-099#deliberate-cucumber-proof
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/081-deliberate-cucumber-proof/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4fc14b4549bb8b163f7063132d003a63377b5498892ff59a9a01827a65055845","plan":"87cbb63a03298bd061be71ea180838e163394f6ae858e6f9d92c2b48ece8620f"}}
```

**Goal:** A contributor can deliberately refresh retained Cucumber proof while ordinary passing and failing test runs leave that evidence untouched.

**Scope:** Migrate every existing Cucumber producer to an explicit project-owned recording option using the existing Messages boundary. Maintain compact stable snapshots, read legacy proof with latest-first success/failure semantics, and configure an actionable generation instruction for every Cucumber Intent location. Preserve test selection, output, exits, retries, concurrency, and stack ownership. Update project commands and documentation, validate, land, follow installation and migration CI, and retire the owned workspace.

**Key examples:**

- Ordinary passing or failing runs leave existing proof bytes unchanged and create no proof directories, files, or locks.
- An explicit focused refresh records executed final passes, clears earlier proof for executed final unsuccessful identities, preserves unexecuted and newer observations, and survives retries and concurrent writers.
- Legacy proof remains readable; consolidation chooses the latest result before retaining only passes, with an unsuccessful result winning equal timestamps.
- Recording uses the same tests and normal diagnostics and exit meaning, writes no automatic commit, and records only features covered by configured Intent locations.

**Authorization:** Terry's delegated migration request includes implementation, ordinary validation/refactor, landing, and final applicable CI. No other framework adapter or deployment is requested.
