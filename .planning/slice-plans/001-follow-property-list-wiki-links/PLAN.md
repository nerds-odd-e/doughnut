# Follow wiki links in property lists

**Identity:** SEED-065#story-1
**Source:** [Refined story](../../seeds/SEED-065-clickable-wiki-links-in-property-lists.md#story-1)

## Goal and scope

Note readers can follow each wiki-link list element from the property row,
both before and after opening its chevron panel. Mixed lists retain their
plain-text elements and order. Existing wiki-link labels, destination semantics,
pending/dead status, and recovery forwarding apply where already provided.

Clickable links in the pencil value dialog, new wiki syntax, new resolution
workflows, and a property-editing redesign are deferred. This plan introduces
no new rejection rules or property-key allowlist.

This is planning input only; implementation is not authorized by this request.
Preparation continues in `/Users/terryyin/git/doughnut/.worktrees/follow-wiki-links-in-property-lists-from-both-pr`,
branch `codex/follow-wiki-links-in-property-lists-from-both-pr`, under Airi-chan's
existing assignment. Starting preparation revision: `e242d1f94afba0f9e2613a2f078bfffac6c17e6e`.
Recorded publication target: `origin/main`; integration checkout:
`/Users/terryyin/git/doughnut`. The draft stays here pending a keep decision.

## Existing solution and constraints

PFE result: change the existing list-value renderer and reuse `WikiLinkToken`.
`RichFrontmatterListPropertyValue.vue` already renders wiki tokens for
`overlaps`, but ordinary lists use compact text. Its consumers are
`RichFrontmatterScalarPropertyValue.vue` and
`RichFrontmatterReadOnlyPropertyValue.vue`; both receive the note's link data.
The chevron reveals property actions in `RichFrontmatterPropertyRow.vue`
without replacing the list-value renderer. One per-item wiki-link rule therefore
covers both panel states and the existing read-only consumer. Preserve the
existing external-link behavior for URL elements alongside that rule.

Reuse the existing authored-token parsing and resolved-location helpers; do
not create a parallel parser, resolver, route construction, or expanded-view
renderer. Backend extraction already includes one-level frontmatter list
strings, so the expected change is presentation, not an API or storage change.

- [ADR 0004 — OKF-compatible notebook Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments)
  applies wiki-link semantics to frontmatter scalars and one-level list items.
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md)
  requires named note/property destinations and compiled hrefs. Unresolved
  links do not navigate; opening the source property panel must not alter a
  clicked link's destination.
- `.planning/NORTH-STAR.md` was reviewed. Its tree, format, and attachment
  direction needs no change for this local presentation extension.

## Outside-in proof ownership

All promises belong to slice 1. Extend the mounted `RichMarkdownEditor`
boundary and the existing `property_wiki_link.feature`; do not test a new
internal helper as a substitute for navigation.

| Promise | Owning observation |
| --- | --- |
| A list such as `topics: ["[[Grammar]]", "practice", "[[Syntax]]"]` lets the reader open each target independently | Mounted editor with real router: activate each rendered link from a fresh source state and assert its named destination; assert plain text and order once |
| Links remain navigable with the chevron panel expanded | Open the source panel through its UI toggle, assert that it is open, then activate a list link and assert the destination |
| Saved list links resolve and open a real target note in both panel states | Browser scenarios with notes created through existing Given setup: visit source, optionally open its chevron panel, follow the list link, and observe the target note |
| Existing display labels and property-target routing still apply | Mounted editor: a list item with a display label and `#prop:` target shows that label and navigates to the named target property |
| Ordinary text, URL elements, read-only rendering, and existing pending/dead handling survive the shared renderer change | Existing list/property-link tests plus focused mixed-list/read-only deltas; retain status and event-forwarding assertions without adding a new recovery workflow |
| Pointer, touch, and keyboard users have normal link activation | Render semantic anchors using the existing link component; one mounted browser keyboard activation observes navigation without introducing custom activation handling |

The mounted fixtures supply API-shaped resolved link data, so they prove
rendering and navigation, not backend resolution. The browser scenarios close
that gap by loading saved list content through the real application. Keep the
body free of duplicate wiki links so the observed click belongs to the list.

## Ordered slices

### 1. Follow each wiki link from the shared property-list row

Type: Behavior
Status: planned
Proof: the observations above pass through the mounted editor and real browser.

Behavior: given a saved list containing wiki links and ordinary values, view
its property row with the panel closed or open, then activate one link → its
referenced note or property opens using the existing navigation semantics.
Other list elements keep their presentation and remain independently usable.

Extend the existing list rendering rule to recognize wiki-link elements
independently of the property's name. Preserve the URL-specific presentation
of non-wiki URL elements and ordinary plain-text lists. Reuse the shared
component for both panel states; necessary cleanup belongs in this slice.
Add only the mounted and browser examples needed for the mapped promises.
Reuse existing property-panel opening and wiki-navigation steps/page objects;
any missing interaction belongs in a small domain-named page-object method.
Pair loading actions with `waitUntilAppIsNotBusy()` before dependent observations.
Record the resulting list-navigation behavior in the rich-property section of
`docs/note-content-saving.md`.

Local verification commands, run from the execution checkout:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts tests/components/form/RichMarkdownEditor.listProperties.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/property_wiki_link.feature
```

The frontend skill requires the typecheck as well as behavioral proof.
The focused browser feature is required here because frontend fixtures supply
resolution rather than proving it. Full backend or E2E suites are not local
gates for this presentation change. Regenerate API/component declarations only
if their actual triggers change.

Sizing: target about 5 minutes of implementation, focused proof, and cleanup;
confidence medium. The single shared renderer and existing harnesses keep this
one cohesive outcome. Focused browser stack startup may exceed the target or
10-minute limit; this is a stated focused-test wait exception, not permission
for implementation thrash. At an implementation overrun, record the failed
sizing premise and finer-decompose under repository guidance before continuing.

Safe stopping point: deliver only when the complete slice and its proof are
green; do not publish an unfinished browser scenario or a partially supported
panel state. After delivery, readers retain the complete story's navigation.

Delivery follows the required repository sequence: Jidoka, fresh
`dough-post-change-refactor` agent, generation if triggered, coordinator
`./scripts/run.sh pnpm format:changed` once, plan evidence update, commit with
the check-only lint hook, then authorized publication under dough-execute-plan.
Implementers/refactorers do not run formatting or standalone `lint:changed`.

## Observed premises

Observations were made before implementation on the preparation revision above;
only this story's draft prose was modified.

| Premise and consuming operation | Literal observation | Result |
| --- | --- | --- |
| One list renderer serves both panel states; slice 1 can change it once | `cat frontend/src/components/form/RichFrontmatterListPropertyValue.vue frontend/src/components/form/RichFrontmatterReadOnlyPropertyValue.vue frontend/src/components/form/RichFrontmatterScalarPropertyValue.vue frontend/src/components/form/RichFrontmatterPropertyRow.vue frontend/src/components/notes/WikiLinkToken.vue` | Ordinary lists take the text branch; wiki rendering is gated on `overlaps`. Opening the panel adds actions without replacing the row value. Token rendering already handles labels, status, and named destinations. |
| List-item references already enter the authored-reference pipeline consumed by note rendering | `cat backend/src/main/java/com/odde/donut/algorithms/AuthoredNoteReferences.java`; `sed -n '70,110p' backend/src/main/java/com/odde/donut/algorithms/Frontmatter.java`; `cat frontend/src/components/notes/NoteShow.vue` | Extraction iterates supported scalar/list-item strings; list strings are included explicitly. NoteShow passes API wikiLinks into its content renderer. New browser proof will verify the complete saved-list journey. |
| Existing frontend harness can observe routing and preserve list behavior | `cat frontend/tests/components/form/richMarkdownEditorTestHarness.ts frontend/tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts frontend/tests/components/form/RichMarkdownEditor.listProperties.spec.ts`; `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts tests/components/form/RichMarkdownEditor.listProperties.spec.ts` | Explicit route options create a real router; the existing scalar-link test activates an anchor and observes its property destination. Both files passed: 17 tests. The overlaps test's default router stub is presentation proof only; new navigation assertions must use the real-router path. |
| Browser support reaches property wiki navigation; slice 1 can extend the existing feature | `cat e2e_test/features/note_topology/property_wiki_link.feature e2e_test/step_definitions/wiki_link.ts e2e_test/step_definitions/note_property.ts e2e_test/start/pageObjects/noteWikiLinkMethods.ts`; `rg -n 'property_wiki_link' scripts/isolated-cypress-active-specs.mjs` | Existing scenarios cover scalar/body property links; steps follow rendered anchors and open chevron panels. The feature is accepted by the isolated runner. No claim that existing scenarios prove ordinary list navigation. |

## Current decisions and concerns

- The owner's expanded view means the chevron panel; the pencil dialog is an
  editing surface and does not need clickable links.
- Keep one common per-item link rule in the existing renderer. No preparatory
  Structure slice or architecture topic is warranted.
- No slice-boundary, cumulative-design, or proof-ownership concern was found
  in this review. Existing baseline proof does not establish the new behavior;
  slice 1 owns that verification.
