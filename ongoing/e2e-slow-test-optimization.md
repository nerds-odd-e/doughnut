# E2E slow test optimization

Status: in-progress

## Profiling baseline (2026-06-03)

Command: `CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json` (SUT already up)

- **192 scenarios**, suite wall ~8:06
- Raw JSON capture: `ongoing/e2e-profile-results.json` (local only, do not commit)

### Top 10% slowest (20 scenarios)

| # | s | feature | scenario (abbrev) |
|---|-----|---------|-------------------|
| 1 | 6.57 | book_reading/book_browsing.feature | See book layout and beginning of PDF |
| 2 | 5.68 | cli/cli_install_and_run.feature | Update from 0.2.0 to 0.3.0 |
| 3 | 5.47 | note_creation_and_update/note_edit.feature | YAML round-trip markdown/rich |
| 4 | 5.30 | recall/re_assimilate.feature | 5 wrong answers → assimilation |
| 5 | 5.22 | cli/cli_recall.feature | Complete all due, decline load more |
| 6 | 5.21 | messages/message_for_note.feature | Message about note shared to bazaar |
| 7 | 5.08 | book_reading/book_browsing.feature | Scrolling PDF updates block |
| 8 | 4.97 | messages/message_center_with_unread_message_count.feature | Receiver 1 unread |
| 9 | 4.93 | message_center… | Reply increases sender unread |
| 10 | 4.83 | book_reading/book_browsing.feature | Same-page scroll |
| 11 | 4.67 | messages/message_for_note.feature | Message in circle |
| 12 | 4.59 | message_center… | Circle read marks all read |
| 13 | 4.43 | book_reading/book_browsing.feature | Book block jumps PDF |
| 14 | 4.28 | user_admin/manage_bazaar.feature | Remove notebook from Bazaar |
| 15 | 4.23 | circles/creating_circles.feature | New user via invitation |
| 16 | 3.84 | note_creation_and_update/predefined_questions_management.feature | Manually add question |
| 17 | 3.81 | note_topology/link.feature | Dead wiki link |
| 18 | 3.69 | circles/notebooks_in_circles.feature | Note in circle |
| 19 | 3.68 | recall/browse_answer_and_notes_while_recalling.feature | Revive memory tracker |
| 20 | 3.60 | cli/cli_recall.feature | MCQ wrong choice |

### Grouping

Top 20 span **13 files**. Per-file groups = **13**; per-2-test groups = **10**. Use **pairs** (smaller group count).

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits (`sleep`, arbitrary `cy.wait(ms)` without assertion).
3. Flaky = failure; tests must be deterministic.

---

### Phase 1: book_browsing (layout) + cli_install (update)
Status: done

**Specs:** `e2e_test/features/book_reading/book_browsing.feature` (scenario: See book layout…), `e2e_test/features/cli/cli_install_and_run.feature` (Update 0.2.0→0.3.0)

**Goals:** Cut wall time; remove redundant setup; replace fixed waits with assertions; no new flakes.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/book_reading/book_browsing.feature,e2e_test/features/cli/cli_install_and_run.feature`

---

### Phase 2: note_edit (YAML) + re_assimilate
Status: done

**Specs:** `note_creation_and_update/note_edit.feature`, `recall/re_assimilate.feature`

**Verify:** `--spec` both feature paths.

---

### Phase 3: cli_recall (complete session) + message_for_note (bazaar)
Status: done

**Specs:** `cli/cli_recall.feature`, `messages/message_for_note.feature` (bazaar scenario only if file has multiple — run full file if cheaper).

---

### Phase 4: book_browsing (scroll) + message_center (receiver unread)
Status: done

**Specs:** `book_reading/book_browsing.feature`, `messages/message_center_with_unread_message_count.feature`

---

### Phase 5: message_center (reply unread) + book_browsing (same-page scroll)
Status: done

---

### Phase 6: message_for_note (circle) + message_center (circle read)
Status: done

---

### Phase 7: book_browsing (block jump) + manage_bazaar
Status: done

**Specs:** `book_reading/book_browsing.feature`, `user_admin/manage_bazaar.feature`

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/book_reading/book_browsing.feature,e2e_test/features/user_admin/manage_bazaar.feature`

---

### Phase 8: creating_circles + predefined_questions_management
Status: planned

---

### Phase 9: link (dead wiki) + notebooks_in_circles
Status: planned

---

### Phase 10: browse_answer_while_recalling + cli_recall (MCQ)
Status: planned

---

### Phase 11: Re-profile and close
Status: planned

Re-run full `cy:run-on-sut`, compare top 10% and total wall time.
