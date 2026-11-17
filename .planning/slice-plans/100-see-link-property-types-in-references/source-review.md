# Source review: See link property types in references

**Launch and input:** On 2026-11-17, the execution coordinator launched a fresh
general-purpose reviewer with no conversation inheritance. Its only input was
the selected story's Goal, Scope (required behavior, no new restriction, scope
limits), Key examples and UI text from
`.planning/seeds/SEED-131-reference-details.md` (section "See link property
types in references") at revision 6b93cb40b2. It received no plan, product,
proof or slice scope. It was told to read no project files and change nothing.

**Unnumbered context the reviewer returned:** the goal's reasoning; why a
reversed wording would defeat the goal; the deferred limits (only the first
link in a multi-link value is labeled, so Target shows no label in example 6;
keys the index leaves out; the Markdown focus context; retrieval path steps and
outgoing links; attachment page references; no grouping, filtering or
sorting); a later excerpt story adds to the same card.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Each entry in the web References section (note page and note context) shows the property types of the referring note's properties whose value links to this note (Scope bullet 1; Goal; example 1) | slice 2 | observed: property_wiki_link.feature "References show the property types the referring note links through": note page Source card labelled "via its prerequisite"; note context renders the same NoteReferences with realm.references (NoteContextReader.vue:19), NoteReferences.spec shows the label |
| 2a | In MCP `get_note_graph`, each `inboundReferences` entry carries the same property types (Scope bullet 2; example 1) | slice 3 | |
| 2b | In `GET /notes/{id}/graph`, each `inboundReferences` entry carries the same property types (Scope bullet 2) | slice 3 | |
| 3 | Each `inboundReferences` entry keeps today's link string unchanged so a consumer can still follow it (Scope bullet 2; example 1) | slice 3 | |
| 4 | A property type is the key without its numeric suffix; each distinct type appears once (Scope bullet 3; example 3) | slice 1 | observed: NoteRealmServiceTest.ReferencePropertyTypes.lists_each_distinct_type_once_in_authored_order: prerequisite + prerequisite 2 + related give one entry with contains(related, prerequisite) |
| 5 | Types appear in the order the referring note authors them (Scope bullet 3; example 3) | slice 1 | observed: NoteRealmServiceTest.ReferencePropertyTypes.lists_each_distinct_type_once_in_authored_order: authored related before prerequisite gives exactly that order |
| 6 | On the web the label reads as the referring note's own property, never inverse wording (Scope bullet 4; UI) | slice 2 | observed: Card.vue label "via its <types>" on its own line inside the referring note card; NoteReferences.spec "shows the referring note property types together as secondary text on its card" |
| 7 | In the structured result the field name and the tool description state the type is the referring note's property (Scope bullet 4) | slice 3 | |
| 8 | Labels come only from the property index and cached authored references; no referring note's content is loaded or parsed for a label (Scope bullet 5) | slice 1 | observed: AuthoredNoteReferenceRowRepository.findPropertyKeysByReferenceRowIds (one JPQL query on NotePropertyIndex by surviving row ids) read in AuthoredNoteReferenceInboundFacade.distinctInboundReferencesForViewer; labels_only_the_link_the_property_index_points_at fails if content were parsed |
| 9 | After an accepted content change the label follows the source property: renamed key shows the new name; link moved to body loses its label (Scope bullet 5; example 5) | slice 1 | observed: NoteControllerShowTests.referencePropertyTypesFollowAcceptedContentChanges: updateNoteContent prerequisite -> follows -> body-only gives prerequisite, follows, null on Target realm |
| 10 | A reference with no linking property shows no label and is otherwise unchanged on both surfaces; a body link alongside a property link adds nothing, source appears once with only the property type (Scope bullet 6; examples 2, 4) | slice 3 | |
| 11 | Which references appear, their order, caps and sampling stay the same; a label appears only on a referring note the viewer already sees and can read (No new restriction) | slice 3 | |
| 12 | When a property value holds several links, its first link gets the label: Other shows Source with `see also` (example 6; Scope limits bullet 1) | slice 1 | observed: NoteRealmServiceTest.ReferencePropertyTypes.labels_only_the_link_the_property_index_points_at: Other realm contains(see also); Target realm one entry with null types |
| 13 | On the card the property types appear as secondary text smaller than the note title (UI) | slice 2 | observed: NoteReferences.spec: label classes text-sm (smaller than daisy-card-title) and dimmed text-base-content/70 |
| 14 | Several types sit together on the one reference card (UI) | slice 2 | observed: NoteReferences.spec: Source card label text "via its prerequisite, related" |
| 15 | A long key is shortened on screen and does not break the card (UI) | slice 2 | observed: NoteReferences.spec: label has truncate class and full-text title tooltip; Cards.vue grid item min-w-0 keeps the card from widening |
| 16 | The collapsed count of references is unchanged (UI) | slice 2 | observed: NoteReferences.spec "keeps the collapsed count of references": collapsed count reads 2 |
