# Show the accepted note title in the sidebar immediately

**Identity:** SEED-073#reviewed-title-sidebar-refresh

## Source

[Show the accepted note title in the sidebar immediately](../../seeds/SEED-073-learning-refinement-and-retrieval.md#reviewed-title-sidebar-refresh).
The seed holds the goal, scope, key examples, the dated observation, and the
refinement reading.

## Goal and scope

When an author accepts a reviewed title correction, the sidebar shows the
accepted title at once and keeps showing it during later in-app navigation.
This holds for both reference-handling choices and for corrections accepted
from note conversations and recall conversations.

Excluded: refreshing the sidebar for title changes made outside this browser
session. Heading refresh, reference-text handling, title validation, and
correction receipts already work, and their existing tests stay unchanged.

## Outside-in proof

| Key example | Observable signal | Owner |
| --- | --- | --- |
| 1, 2: accept a title correction with either reference choice; the sidebar shows the accepted title | Accepting a title correction from both a note conversation and a recall conversation triggers the sidebar listing refresh signal, `sidebarStructuralRefreshKey`. This is the signal ordinary title edits use, asserted the same way as in `frontend/tests/store/noteStore.spec.ts` | Slice 1: new case in `frontend/tests/components/conversation/ConversationInner.acceptTitleCorrections.spec.ts` |
| 1, 2: the signal makes the sidebar reload its rows | A refresh request makes the mounted sidebar fetch its folder listings again | Retained: `frontend/tests/notes/sidebar/SidebarFolderListingReload.spec.ts` |
| 3: navigate away and back; the sidebar still shows the accepted title | The refresh clears the listing cache, so later listing loads fetch fresh data | Same retained sidebar reload coverage; no new test |
| 1, 2: heading, reference handling, validation, receipt | Unchanged behavior | Retained: existing cases in `ConversationInner.acceptTitleCorrections.spec.ts` and the backend correction tests |

Local gate: run the two frontend spec files named above with
`CURSOR_DEV=true nix develop -c pnpm frontend:test <spec>`, with `NODE_ENV`
unset. No backend change is planned, so no broader local check is needed.

## Decisive premises

- **Accepting a correction does not trigger the sidebar refresh at
  `d2365e495b` (this workspace).** The accept handler in
  `ConversationNoteCorrectionReview.vue` emits `decided` and reloads the note
  through `noteStore.loadNoteRealm`. That reload does not call
  `refreshSidebarStructuralListings`, which ordinary title edits call from
  `noteTextEditing.ts`. Observation: a scratch spec in
  `frontend/tests/components/conversation/` accepted "Reviewed title" from a
  note conversation and from a recall conversation and asserted that
  `sidebarStructuralRefreshKey` increased. Command:
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation/ScratchSidebarRefresh.spec.ts`.
  Result: 2 of 2 failed (`expected 0 to be greater than 0`). This reproduces
  the symptom's cause.
- **The planned check distinguishes fixed from unfixed code.** With a scratch
  edit that calls `refreshSidebarStructuralListings()` after an `ACCEPTED`
  decision, using a static import, the same command passed 2 of 2. With a
  dynamic `import()` instead, the counter did not increase. Use a static
  import. Both scratch edits were reverted before this plan was written.
- **The refresh signal reloads the rendered sidebar.** Settled by existing
  coverage: in `SidebarFolderListingReload.spec.ts`, calling
  `refreshSidebarStructuralListings()` makes the mounted sidebar request its
  folder listing again.

## Slices

### 1. Accepted title corrections refresh the sidebar

Type: Behavior
Status: done
Proof: the new case in `ConversationInner.acceptTitleCorrections.spec.ts`,
run with both `noteConversation` and `recallConversation`, fails before the
change and passes after it. Existing cases in that file and
`SidebarFolderListingReload.spec.ts` stay green.

Behavior: an author reviews a title correction in a conversation → accepts it
→ the sidebar listing refresh is triggered, so the sidebar shows the accepted
title now and on later navigation.

Accepted proof: the case "refreshes the sidebar listings after accepting a
title correction" in `ConversationInner.acceptTitleCorrections.spec.ts`, for
`noteConversation` and `recallConversation`, failed before the change (2 of 2,
`expected +0 to be 1`) and passes after. Its setup (pending title proposal,
accepted receipt, typing, and the Accept click) is shared with the existing
explicit-acceptance case through the spec's `acceptReviewedTitle` helper. The
conversation specs (64 of 64), `SidebarFolderListingReload.spec.ts` (4 of 4),
and `vue-tsc --noEmit` pass.

## Current decisions

- Refresh the sidebar only when the accepted correction is a `TITLE`
  correction. This matches the ordinary edit path, which refreshes only for
  title edits. Content corrections do not change what the sidebar shows.
- Keep the rule "a confirmed title change refreshes the sidebar" next to the
  ordinary title edit if the post-change refactor finds a clean shared home in
  the note store. Do not build a general change-notification mechanism.
- Learning: no shared note-store home fits. The rule already lives in
  `refreshSidebarStructuralListings()` (`sidebarStructuralRefresh.ts`), and
  each title-change path calls it in one line. Moving it into `loadNoteRealm`
  would refresh the sidebar on every note load, so the call stays in
  `ConversationNoteCorrectionReview.vue`.
