# Test optimization blacklist

**Candidates** are proposals from optimization runs (hard-to-improve after a
serious attempt). Permanent exclusion from profiling is done by tagging the
Scenario or Feature with `@skipOptimizationDueToKnownNecessarySlowness` — that
is a developer decision after review, not an automatic move from this list.

Profile E2E with:

```bash
--env tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'
```

## Candidates

<!-- file path — test/scenario name — duration — why hard — proposed YYYY-MM-DD -->

_(none)_

## Resolved (tagged `@skipOptimizationDueToKnownNecessarySlowness` 2026-07-23)

Reviewed as genuine multi-step user journeys whose cost is inherent to the product
behavior under test (unit tests cannot reproduce the external user-value clarity):

- `e2e_test/features/wikidata/associate_wikidata.feature` — Associate note to wikipedia via wikidata using real service — developer chose to keep the live-integration canary; the real Wikidata contract check is worth the network cost, and mocked scenarios cover the rest of the UI.
- `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` — Browse notes while recalling and come back — pauseable recall session ("Resume" menu item) comes from the frontend UI assimilation flow, not replicable via API setup.
- `e2e_test/features/cli/cli_access_token.feature` — Set access token — PTY/Ink startup floor (~1s) is inherent to the interactive CLI happy path; steps already minimal.
- `e2e_test/features/bazaar/add_to_learning.feature` — subscribe to a note and browse — genuine subscribe→browse→unsubscribe navigations; SPA-nav shortcut regresses the `@mockBrowserTime` recall sibling.
- `e2e_test/features/note_creation_and_update/predefined_questions_management.feature` — Manually add a question to the note successfully — genuine multi-panel add-question UI flow; shared helpers already lean.
- `e2e_test/features/circles/creating_circles.feature` — New user via circle invitation — genuine new-user invitation journey needing two full page loads (logged-out invitation + post-join circle render).
- `e2e_test/features/book_reading/ai_reorganize_layout.feature` — AI reorganize opens preview dialog and applies on confirm — dominant cost is book-page full load + PDF canvas render, inherent to opening the book.
- `e2e_test/features/wikidata/associate_wikidata_person_entries.feature` — Create a note for a person with wikidata should auto fill the content — cost is notebook full load + (mocked) wikidata fetch + auto-fill, inherent to the person-auto-fill behavior.
