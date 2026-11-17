# Source review

Launch: fresh general-purpose agent (no conversation inheritance, fork or resume), 2026-11-17, by the
coordinator of SEED-131#label-every-property-link-in-references. Input: only the selected story's
Goal, Scope and Key examples from `.planning/seeds/SEED-131-reference-details.md`, with the
source-only promise instructions; no plan, product, proof or slice scope. Instructed to read no
project files and change nothing.

Unnumbered context returned: goal reasons; the unchanged reference set (which references appear,
order, caps, sampling); why relationship notes get no label; the scope limits (Markdown focus
context, retrieval path steps, outgoing links, attachment page references, grouping/filtering/sorting
by property type); body-text evidence belongs to "See surrounding text in references".

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Any key, including keys the property index leaves out, is named — Scope "This holds for any key"; example 2 `overlaps: ["[[Farther]]"]` → Farther's References show Further "via its overlaps" | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.labels_any_key (overlaps) |
| 2 | Scalar values and list items of any length, including over 255 characters — Scope "for a scalar or a list item of any length"; example 3 long `notes` item → "via its notes" | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.labels_a_list_item_of_any_length (300+ char notes item); scalar: lists_the_property_type_holding_the_link |
| 3 | Every link in a value holding several labels each target — Scope "for any link in a value that holds several"; example 1 `see also: "[[Other]] and [[Target]]"` → Other's and Target's References show "via its see also" | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.labels_every_link_a_value_holds (Other and Target both see also) |
| 4 | Several properties holding the link are all named — Scope "every property"; example 7 → "via its prerequisite, related" | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.lists_each_distinct_type_once_in_authored_order (related, prerequisite) |
| 5 | Numeric suffix dropped and duplicates merged — Unchanged "Property types stay distinct … with the numeric suffix dropped"; example 7 | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.lists_each_distinct_type_once_in_authored_order (prerequisite 2 merged) |
| 6 | Authored key order — Unchanged "in the referring note's authored key order"; example 7 | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.lists_each_distinct_type_once_in_authored_order (authored order) |
| 7 | Example-of family shown in canonical form — Unchanged "the example-of family canonical" | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.labels_the_example_of_family_in_canonical_form |
| 8 | Card line wording stays "via its …" — Unchanged | slice 1 | observed: e2e_test/features/note_topology/property_wiki_link.feature 'References show the property types the referring note links through' (12/12 pass); Card.vue unchanged |
| 9 | A property that links elsewhere is not named on this target's reference — example 6: Target shows Source with no label; Other's show "via its prerequisite" | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.omits_a_property_that_links_elsewhere |
| 10 | A body-only link shows no label — Unchanged; example 6 | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.omits_types_for_a_body_link; omits_a_property_that_links_elsewhere (Target null) |
| 11 | Web: no label for a relationship note's `source` and `target` — example 4: Greek's and Lithos's References show it with no label | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.omits_types_for_a_relationship_notes_endpoints (both ends null) |
| 12 | Retrieval: no `referrerPropertyTypes` for a relationship note's `source` and `target` — example 4 | slice 1 | observed: backend/src/test/java/com/odde/donut/controllers/NoteControllerGraphTests.inboundReferencesCarryTheReferringNotesPropertyTypes (relationship entry null); backend/src/test/java/com/odde/donut/mcp/GraphNavigationMcpEndpointTest.inboundReferenceCarriesTheReferringNotesPropertyTypesBesideItsLink (key absent) |
| 13 | The same keys on an ordinary note are labelled — example 5 Article `source: "[[Book]]"` → "via its source" | slice 1 | observed: backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes.labels_relationship_keys_on_an_ordinary_note (source) |
| 14 | Web References section on the note page shows the types | slice 1 | observed: NoteRealmService.build tests (backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes); backend/src/test/java/com/odde/donut/controllers/NoteControllerShowTests.referencePropertyTypesFollowAcceptedContentChanges; e2e property_wiki_link.feature |
| 15 | Web References section in the note context shows the same types | slice 1 | observed: NoteRealmService.build serves note page and note context realm (backend/src/test/java/com/odde/donut/services/NoteRealmServiceTest.java ReferencePropertyTypes); e2e property_wiki_link.feature |
| 16 | `get_note_graph` focus-note `inboundReferences` carries the types as `referrerPropertyTypes` — example 1 `["see also"]` | slice 1 | observed: backend/src/test/java/com/odde/donut/mcp/GraphNavigationMcpEndpointTest.inboundReferenceCarriesTheReferringNotesPropertyTypesBesideItsLink (referrerPropertyTypes [see also]) |
| 17 | `GET /notes/{id}/graph` focus-note `inboundReferences` carries the same types — example 1 | slice 1 | observed: backend/src/test/java/com/odde/donut/controllers/NoteControllerGraphTests.inboundReferencesCarryTheReferringNotesPropertyTypes (contains see also) |
