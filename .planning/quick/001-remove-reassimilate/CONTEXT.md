# Remove re-assimilate — decisions (shipped)

See `PLAN.md` for phase log. Shipped 2026-08-12.

**Outcome:** Frequent-failure rule stays (≥5 wrong in 14 days) as an **alert** with live API counts. No re-assimilate confirm or tracker wipe on failure. Remember spelling later does not wipe trackers or affect assimilation queue. Property removal **hard-deletes** the property tracker. User-facing tracker soft-delete removed; note-delete cascade on `deletedAt` kept. Orphan soft-deleted trackers on live notes cleaned by migration. ADR 0001/0003 updated.
