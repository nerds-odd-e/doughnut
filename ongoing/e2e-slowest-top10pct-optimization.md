# E2E slowest top 10% optimization

Profiled full suite on 2026-06-03 (`pnpm cypress run`, JSON reporter to `/tmp/e2e-run.log`).
192 scenarios; top 10% = 20 slowest. Grouping: **pairs of 2** (10 groups; fewer than 15 file-based groups).

Raw data: `e2e_test/reports/slowest-top10pct.json`

## Optimization rules (all groups)

1. **Remove or simplify redundant tests first** — merge overlapping coverage, drop duplicate setup paths.
2. **No fixed-time waits** — no `cy.wait(ms)`, no `sleep` in steps/page objects; use assertions and `cy.intercept` aliases.
3. **Flaky is failure** — re-run touched specs until stable; fix root cause, do not retry blindly.

## Groups

### Group 1: book browsing + note content completion
Status: done

Specs:
- `e2e_test/features/book_reading/book_browsing.feature` — "See book layout and beginning of PDF in the browser" (6.08s)
- `e2e_test/features/ai_generated_content/note_content_completion.feature` — "OpenAI Service Unavailability" (4.84s)

### Group 2: note edit + message center unread
Status: done

- `note_edit.feature` — "Note YAML properties round-trip through markdown and rich editing" (4.80s)
- `message_center_with_unread_message_count.feature` — "The receiver's reply should increase the unread count of the sender" (4.76s)

### Group 3: re-assimilate + MCP services
Status: done

- `re_assimilate.feature` — "Note returns to assimilation after 5 wrong answers" (4.56s)
- `mcp_services.feature` — "AI agent learns from Doughnut via MCP client (example #1)" (4.14s)

### Group 4: circles notebooks + link dead wiki
Status: done

- `notebooks_in_circles.feature` — "Creating note that belongs to the circle" (4.00s)
- `link.feature` — "A dead wiki link is shown and can create the missing note" (3.96s)

### Group 5: message for note + predefined questions
Status: done

- `message_for_note.feature` — "User send message about a note shared to a bazaar" (3.83s)
- `predefined_questions_management.feature` — "Manually add a question to the note successfully" (3.71s)

### Group 6: relationship edit + creating circles
Status: done

- `relationship_edit_and_remove.feature` — "change relation type keeps user-authored content suffix" (3.64s)
- `creating_circles.feature` — "New user via circle invitation" (3.61s)

### Group 7: browse while recalling + note edit markdown
Status: done

- `browse_answer_and_notes_while_recalling.feature` — "View last answered question when the quiz answer was correct" (3.51s)
- `note_edit.feature` — "Edit a note's content as markdown" (3.48s)

### Group 8: browse remove recall + spelling quiz
Status: done

- `browse_answer_and_notes_while_recalling.feature` — "I can remove a note from further recalls" (3.36s)
- `recall_quiz_spelling_question.feature` — "Spelling quiz - incorrect answer" (3.31s)

### Group 9: wikidata associate + link rename update
Status: planned

- `associate_wikidata.feature` — "Associate note to wikipedia via wikidata using real service" (3.29s)
- `link.feature` — "Renaming a referenced note while updating visible reference text" (3.28s)

### Group 10: message read + link rename keep text
Status: planned

- `message_center_with_unread_message_count.feature` — "The message is read by the receiver" (3.28s)
- `link.feature` — "Renaming a referenced note while keeping visible reference text" (3.23s)
