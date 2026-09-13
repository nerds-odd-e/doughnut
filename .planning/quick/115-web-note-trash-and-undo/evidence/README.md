# Derived trash membership query proof

Planning-only experiment, 2026-09-13. Uses local MySQL at 127.0.0.1:3309 and the
repository's resolved Hibernate runtime; no application rows are read or changed.
The Java program creates a process-unique disposable schema (CREATE must succeed,
so it cannot adopt an existing schema) and removes it in `finally`.

Run from repository root:

```sh
CURSOR_DEV=true nix develop -c bash .planning/quick/115-web-note-trash-and-undo/evidence/run-query-proof.sh
```

Observed MySQL 8.4.11; Hibernate ORM 7.4.5.Final.

- Direct recursive CTE inside `@Formula` failed: Hibernate prefixed the CTE name
  with the entity alias, producing invalid `with recursive pn1_0.trash_folders`.
- A read-only view containing that CTE, referenced by a simple formula, passed.
- Passed root/case/descendant membership, ordinary nested `_trash` and `Trash`,
  root notes, legacy-deleted exclusion, multiple roots, pagination/counts, direct
  reads, native/Hibernate agreement, move-out refresh, subtree movement, root
  renaming, and no trash roots.
- Formula fields reflect loaded state until refreshed. The plan therefore uses
  current ancestry for mutable entity accessors rather than trusting cached
  formula state after a move. Query predicates use the derived view.

Terminal observations:

```text
PASS: Hibernate formula, root/case/nested distinction, legacy exclusion, direct access, pagination/count, move refresh
PASS: native SQL agrees; subtree move, root rename, and zero roots recompute without stored membership
CLEANUP: removed isolated schema doughnut_trash_plan_probe_9664
```

This is not a performance benchmark or proof of the feature's product behavior.
It establishes the query/mapping mechanism and its freshness limitation. The
runner resolves classpath into an external temporary directory; it does not
compile or modify product code. Keep this evidence with the active plan until
ordinary story wrap-up decides its disposition.
