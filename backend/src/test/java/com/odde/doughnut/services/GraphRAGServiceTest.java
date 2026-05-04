package com.odde.doughnut.services;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GraphRAGServiceTest {
  @Autowired private MakeMe makeMe;
  @Autowired private GraphRAGService graphRAGService;
  @Autowired private NoteService noteService;
  @Autowired private WikiTitleCacheService wikiTitleCacheService;

  // Helper methods for common test operations
  private List<BareNote> getNotesWithRelationship(
      GraphRAGResult result, RelationshipToFocusNote relationship) {
    return result.getRelatedNotes().stream()
        .filter(n -> n.getRelationToFocusNote() == relationship)
        .collect(Collectors.toList());
  }

  private void assertRelatedNotesContain(
      GraphRAGResult result, RelationshipToFocusNote relationship, Note... expectedNotes) {
    List<BareNote> notes = getNotesWithRelationship(result, relationship);
    assertThat(describeRelatedNotes(result), notes, hasSize(expectedNotes.length));
    assertThat(notes, containsInAnyOrder((Object[]) expectedNotes));
  }

  private void assertRelatedNotesIncludeNotes(GraphRAGResult result, Note... expectedNotes) {
    for (Note note : expectedNotes) {
      assertThat(
          describeRelatedNotes(result),
          result.getRelatedNotes().stream().anyMatch(b -> b.equals(note)),
          is(true));
    }
  }

  private String describeRelatedNotes(GraphRAGResult result) {
    return result.getRelatedNotes().stream()
        .map(
            n ->
                (n.getRelationToFocusNote() != null
                        ? String.valueOf(n.getRelationToFocusNote())
                        : "null")
                    + ":"
                    + n.getTitle())
        .collect(Collectors.joining(", "));
  }

  private void refreshWikiCache(Note note) {
    wikiTitleCacheService.refreshForNote(note, note.getCreator());
  }

  @Nested
  class SimpleNoteWithNoParentOrChild {
    @Test
    void zeroBudgetKeepsFullFocusDetailsAndLeavesRelatedNotesEmpty() {
      Note shortNote = makeMe.aNote().title("Short").details("Test Details").please();
      GraphRAGResult shortResult = graphRAGService.retrieve(shortNote, 0, shortNote.getCreator());
      assertThat(shortResult.getFocusNote().getDetails(), equalTo("Test Details"));
      assertThat(shortResult.getRelatedNotes(), empty());

      String longDetails = "a".repeat(2000);
      Note longNote = makeMe.aNote().title("Long").details(longDetails).please();
      GraphRAGResult longResult = graphRAGService.retrieve(longNote, 0, longNote.getCreator());
      assertThat(longResult.getFocusNote().getDetails().length(), equalTo(2000));
      assertThat(longResult.getRelatedNotes(), empty());
    }
  }

  @Nested
  class WhenNoteHasParent {
    private Note parent;
    private Note note;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("parent-child-peers").please();
      parent = makeMe.theNote(parent).folder(peerFolder).please();
      note = makeMe.aNote().folder(peerFolder).please();
    }

    @Test
    void folderCrumbOnFocusWithoutLegacyParentExpansionAcrossBudgets() {
      GraphRAGResult full = graphRAGService.retrieve(note, 1000, note.getCreator());
      assertThat(full.getFocusNote().getContextualPath(), containsString("parent-child-peers"));

      GraphRAGResult zero = graphRAGService.retrieve(note, 0, note.getCreator());
      assertThat(zero.getFocusNote().getContextualPath(), containsString("parent-child-peers"));
      assertThat(zero.getRelatedNotes(), empty());
    }
  }

  @Nested
  class WhenNoteHasYoungerSiblings {
    private Note focusNote;
    private Note youngerSibling1;
    private Note youngerSibling2;

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().title("Parent Note").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder =
          makeMe.aFolder().notebook(notebook).name("younger-sibling-peers").please();
      focusNote =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Focus Note")
              .please();
      youngerSibling1 =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Younger One")
              .details("Sibling 1 Details")
              .please();
      youngerSibling2 =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Younger Two")
              .details("Sibling 2 Details")
              .please();
    }

    @Test
    void youngerSiblingsOnFocusAndRelatedNotesAlign() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          result.getFocusNote().getYoungerSiblings(),
          contains(
              GraphNoteWikiUri.of(youngerSibling1, false),
              GraphNoteWikiUri.of(youngerSibling2, false)));
      assertRelatedNotesContain(
          result, RelationshipToFocusNote.YoungerSibling, youngerSibling1, youngerSibling2);
    }
  }

  @Nested
  class WhenNoteHasContextualPath {
    private Note grandParent;
    private Note parent;
    private Note focusNote;

    @BeforeEach
    void setup() {
      grandParent = makeMe.aNote().title("Grand Parent").details("GP Details").please();
      Notebook nb = grandParent.getNotebook();
      Folder outer = makeMe.aFolder().notebook(nb).name("CtxOuter").please();
      Folder inner = makeMe.aFolder().notebook(nb).parentFolder(outer).name("CtxInner").please();
      grandParent = makeMe.theNote(grandParent).folder(inner).please();
      parent =
          makeMe.aNote().underSameNotebookAs(grandParent).folder(inner).title("Parent").please();
      focusNote = makeMe.aNote().underSameNotebookAs(parent).folder(inner).title("Focus").please();
    }

    @Test
    void folderCrumbOnFocusWithoutLegacyContextAncestorRelatedNotes() {
      GraphRAGResult zero = graphRAGService.retrieve(focusNote, 0, focusNote.getCreator());
      assertThat(zero.getFocusNote().getContextualPath(), equalTo("CtxOuter / CtxInner"));
    }
  }

  @Nested
  class WhenNoteHasOlderSiblings {
    private Note olderSibling1;
    private Note olderSibling2;
    private Note focusNote;

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().title("Parent Note").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("older-sibling-peers").please();
      olderSibling1 =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Prior One")
              .details("Sibling 1 Details")
              .please();
      olderSibling2 =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Prior Two")
              .details("Sibling 2 Details")
              .please();
      focusNote =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Focus Note")
              .please();
    }

    @Test
    void olderSiblingsOnFocusAndRelatedNotesAlign() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          result.getFocusNote().getOlderSiblings(),
          contains(
              GraphNoteWikiUri.of(olderSibling1, false),
              GraphNoteWikiUri.of(olderSibling2, false)));

      List<BareNote> siblingNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.OlderSibling)
              .toList();
      assertThat(siblingNotes, hasSize(2));
      assertThat(
          siblingNotes.stream().map(BareNote::getUriAndTitle).collect(Collectors.toList()),
          containsInAnyOrder(olderSibling1, olderSibling2));
    }
  }

  @Nested
  class WhenNoteHasReferenceByNotes {
    private Note focusNote;
    private Note inboundReferenceParent1;
    private Note inboundReferenceNote1;
    private Note inboundReferenceParent2;
    private Note inboundReferenceNote2;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus Note").details("Focus Details").please();
      Notebook notebook = focusNote.getNotebook();

      // Create first inbound reference note
      inboundReferenceParent1 =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(focusNote.getCreator())
              .title("Inbound Reference Parent 1")
              .please();
      inboundReferenceNote1 =
          makeMe.aRelation().between(inboundReferenceParent1, focusNote).please();
      refreshWikiCache(inboundReferenceNote1);

      // Create second inbound reference note
      inboundReferenceParent2 =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(focusNote.getCreator())
              .title("Inbound Reference Parent 2")
              .please();
      inboundReferenceNote2 =
          makeMe.aRelation().between(inboundReferenceParent2, focusNote).please();
      refreshWikiCache(inboundReferenceNote2);
    }

    @Test
    void inboundReferenceNotesAppearInRelatedAndFocusInboundRefs() {
      GraphRAGResult full = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          full.getFocusNote().getInboundReferences(),
          containsInAnyOrder(
              GraphNoteWikiUri.of(inboundReferenceNote1, false),
              GraphNoteWikiUri.of(inboundReferenceNote2, false)));
      assertRelatedNotesIncludeNotes(full, inboundReferenceNote1, inboundReferenceNote2);

      GraphRAGResult limited = graphRAGService.retrieve(focusNote, 3, focusNote.getCreator());

      assertRelatedNotesIncludeNotes(limited, inboundReferenceNote1, inboundReferenceNote2);
    }
  }

  @Nested
  class WhenNoteHasCacheOnlyReference {
    @Test
    void shouldIncludeReferenceFromWikiTitleCacheWhenNoLegacyRelationshipFieldExists() {
      Note anchor = makeMe.aNote().title("Root").please();
      User viewer = anchor.getCreator();
      Notebook notebook = anchor.getNotebook();
      Folder cacheFolder =
          makeMe.aFolder().notebook(notebook).name("cache-only-ref-folder").please();
      anchor = makeMe.theNote(anchor).folder(cacheFolder).please();
      Note focus =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(anchor)
              .folder(cacheFolder)
              .title("Cache Focus")
              .please();
      Note referrer =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(anchor)
              .folder(cacheFolder)
              .title("Plain Referrer")
              .details("Mentions [[Cache Focus]].")
              .please();
      refreshWikiCache(referrer);

      GraphRAGResult result = graphRAGService.retrieve(focus, 1000, viewer);

      assertThat(
          result.getFocusNote().getInboundReferences(),
          contains(GraphNoteWikiUri.of(referrer, false)));
      assertRelatedNotesIncludeNotes(result, referrer);
    }
  }

  @Nested
  class WhenStructuralPeersAreFolderScoped {
    @Test
    void youngerAndOlderSiblingsExcludeNotesOutsideFolder() {
      Note parent = makeMe.aNote().title("Notebook anchor").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("graph-folder-peers").please();
      Note folderOlder =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Folder older")
              .please();
      Note focus =
          makeMe.aNote().underSameNotebookAs(parent).folder(peerFolder).title("Focus").please();
      Note folderYounger =
          makeMe
              .aNote()
              .underSameNotebookAs(parent)
              .folder(peerFolder)
              .title("Folder younger")
              .please();
      makeMe.aNote().underSameNotebookAs(parent).title("Tree only not in folder").please();

      GraphRAGResult result = graphRAGService.retrieve(focus, 1000, focus.getCreator());

      assertThat(
          result.getFocusNote().getOlderSiblings(),
          contains(GraphNoteWikiUri.of(folderOlder, false)));
      assertThat(
          result.getFocusNote().getYoungerSiblings(),
          contains(GraphNoteWikiUri.of(folderYounger, false)));
      assertRelatedNotesContain(result, RelationshipToFocusNote.YoungerSibling, folderYounger);
      assertRelatedNotesContain(result, RelationshipToFocusNote.OlderSibling, folderOlder);
    }
  }

  @Nested
  class WhenStructuralPeersAreNotebookRootScope {
    @Test
    void youngerAndOlderSiblingsMatchNotebookRootFolderScopedOrdering() {
      Note a = makeMe.aNote().title("Root A").please();
      Notebook notebook = a.getNotebook();
      var creator = a.getCreator();
      Note b = makeMe.aNote().title("Root B").inNotebook(notebook).creator(creator).please();
      Note c = makeMe.aNote().title("Root C").inNotebook(notebook).creator(creator).please();

      List<Note> peersB = noteService.findStructuralPeerNotesInOrder(b);
      int idx = peersB.indexOf(b);
      List<String> expectedYounger =
          peersB.subList(idx + 1, peersB.size()).stream()
              .map(n -> GraphNoteWikiUri.of(n, false))
              .toList();
      GraphRAGResult resultMiddle = graphRAGService.retrieve(b, 1000, b.getCreator());
      assertThat(resultMiddle.getFocusNote().getYoungerSiblings(), equalTo(expectedYounger));

      List<Note> peersC = noteService.findStructuralPeerNotesInOrder(c);
      int cIdx = peersC.indexOf(c);
      List<String> expectedOlder =
          peersC.subList(0, cIdx).stream().map(n -> GraphNoteWikiUri.of(n, false)).toList();
      GraphRAGResult resultLast = graphRAGService.retrieve(c, 1000, c.getCreator());
      assertThat(resultLast.getFocusNote().getOlderSiblings(), equalTo(expectedOlder));
    }
  }

  /**
   * Wiki-link graph contract on serialized GraphRAG: folder crumb string on the focus note, {@code
   * inboundReferences}, and folder-scoped peers in related notes.
   */
  @Nested
  class WikiLinkGraphContract {

    private List<String> jsonTextArrayElements(JsonNode arrayNode) {
      List<String> out = new ArrayList<>();
      if (arrayNode != null && arrayNode.isArray()) {
        arrayNode.forEach(n -> out.add(n.asText()));
      }
      return out;
    }

    @Test
    void serializedFocusNote_exposesInboundRefsAndFolderCrumbString() {
      Note anchor = makeMe.aNote().title("713 Folder Anchor").please();
      Notebook notebook = anchor.getNotebook();
      User viewer = anchor.getCreator();
      Folder outer = makeMe.aFolder().notebook(notebook).name("OuterCrumb").please();
      Folder inner =
          makeMe.aFolder().notebook(notebook).parentFolder(outer).name("InnerCrumb").please();
      anchor = makeMe.theNote(anchor).folder(inner).please();

      Note outgoing =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(anchor)
              .folder(inner)
              .title("713 Outgoing")
              .please();

      Note focus =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(anchor)
              .folder(inner)
              .title("713 Focus")
              .details("See [[713 Outgoing]].")
              .please();

      Note referrer =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(anchor)
              .folder(inner)
              .title("713 Referrer")
              .details("Ref [[713 Focus]].")
              .please();

      refreshWikiCache(focus);
      refreshWikiCache(referrer);

      GraphRAGResult result = graphRAGService.retrieve(focus, 2500, viewer);
      var mapper = new ObjectMapperConfig().objectMapper();
      JsonNode treeJson = mapper.valueToTree(result);
      JsonNode focusJson = treeJson.get("focusNote");

      assertThat(treeJson.get("relatedNotes").isArray(), is(true));
      assertThat(treeJson.get("relatedNotes").size(), greaterThanOrEqualTo(1));
      assertThat(treeJson.get("relatedNotes").get(0).has("relationToFocusNote"), is(true));

      // Folder-name crumbs from folder ancestry (not legacy note URIs).
      assertThat(focusJson.get("contextualPath").isTextual(), is(true));

      String crumbs = focusJson.get("contextualPath").asText();
      assertThat(crumbs, allOf(containsString("OuterCrumb"), containsString("InnerCrumb")));

      assertThat(focusJson.has("links"), is(false));

      assertThat(focusJson.get("inboundReferences").isArray(), is(true));
      assertThat(
          jsonTextArrayElements(focusJson.get("inboundReferences")),
          hasItem(GraphNoteWikiUri.of(referrer, false)));

      BareNote outboundRelated =
          result.getRelatedNotes().stream()
              .filter(b -> b.equals(outgoing))
              .findFirst()
              .orElseThrow(() -> new AssertionError(describeRelatedNotes(result)));
      assertThat(
          outboundRelated.getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.OutboundWikiLink));
    }

    @Test
    void outboundWikiLinkOnly_includesTargetWithOutboundWikiLinkRelation() {
      Note target = makeMe.aNote().title("814 Out Target").details("Target body").please();
      User viewer = target.getCreator();
      Note focus =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(target)
              .title("814 Out Focus")
              .details("See [[814 Out Target]].")
              .please();
      refreshWikiCache(focus);

      GraphRAGResult result = graphRAGService.retrieve(focus, 2500, viewer);

      BareNote related =
          result.getRelatedNotes().stream()
              .filter(b -> b.equals(target))
              .findFirst()
              .orElseThrow(() -> new AssertionError(describeRelatedNotes(result)));
      assertThat(
          related.getRelationToFocusNote(), equalTo(RelationshipToFocusNote.OutboundWikiLink));
      assertThat(related.getDetails(), containsString("Target body"));
    }
  }

  @Nested
  class WhenTargetOfRelationshipHasReferenceBy {
    private Note focusNote;
    private Note relatedChild;
    private Note targetNote;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus Note").please();
      Notebook notebook = focusNote.getNotebook();

      targetNote =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(focusNote.getCreator())
              .title("Target Note")
              .details("Target Details")
              .please();

      relatedChild = makeMe.aRelation().between(focusNote, targetNote).please();
      refreshWikiCache(relatedChild);

      makeMe.refresh(targetNote);
    }

    @Test
    void shouldNotIncludeReferencedTargetWhenTargetNotReachedViaStructuralChildWalk() {
      Note referenceParent1 =
          makeMe
              .aNote()
              .inNotebook(focusNote.getNotebook())
              .creator(focusNote.getCreator())
              .title("Reference Parent 1")
              .please();
      refreshWikiCache(makeMe.aRelation().between(referenceParent1, targetNote).please());
      Note referenceParent2 =
          makeMe
              .aNote()
              .inNotebook(focusNote.getNotebook())
              .creator(focusNote.getCreator())
              .title("Reference Parent 2")
              .please();
      refreshWikiCache(makeMe.aRelation().between(referenceParent2, targetNote).please());
      makeMe.refresh(targetNote);

      GraphRAGResult resultHighBudget =
          graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());
      GraphRAGResult resultLowBudget =
          graphRAGService.retrieve(focusNote, 3, focusNote.getCreator());

      for (GraphRAGResult result : List.of(resultHighBudget, resultLowBudget)) {
        for (Note refParent : List.of(referenceParent1, referenceParent2)) {
          result.getRelatedNotes().stream()
              .filter(b -> b.equals(refParent))
              .findFirst()
              .ifPresent(
                  b ->
                      assertThat(
                          describeRelatedNotes(result),
                          b.getRelationToFocusNote(),
                          anyOf(
                              equalTo(RelationshipToFocusNote.YoungerSibling),
                              equalTo(RelationshipToFocusNote.OlderSibling))));
        }
      }
    }
  }

  @Nested
  class TruncateDetailsTests {
    @Test
    void shouldTruncateASCIICharactersCorrectly() {
      String longDetails = "a".repeat(2000);
      Note treeParent = makeMe.aNote().title("Truncate ASCII Tree").please();
      Notebook notebook = treeParent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("truncate-peers").please();
      treeParent = makeMe.theNote(treeParent).folder(peerFolder).please();
      Note olderLong =
          makeMe
              .aNote()
              .underSameNotebookAs(treeParent)
              .folder(peerFolder)
              .title("Older Long")
              .details(longDetails)
              .please();
      Note focus =
          makeMe
              .aNote()
              .underSameNotebookAs(treeParent)
              .folder(peerFolder)
              .title("Focus Note")
              .please();

      GraphRAGResult result = graphRAGService.retrieve(focus, 1000, focus.getCreator());

      BareNote bn =
          result.getRelatedNotes().stream()
              .filter(b -> b.equals(olderLong))
              .findFirst()
              .orElseThrow();
      assertThat(bn.getRelationToFocusNote(), equalTo(RelationshipToFocusNote.OlderSibling));
      assertThat(
          bn.getDetails(), equalTo("a".repeat(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "..."));
    }

    @Test
    void shouldTruncateCJKCharactersCorrectly() {
      String cjkText = "你好世界".repeat(500);
      Note treeParent = makeMe.aNote().title("Truncate CJK Tree").please();
      Notebook notebook = treeParent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("truncate-cjk-peers").please();
      treeParent = makeMe.theNote(treeParent).folder(peerFolder).please();
      Note olderLong =
          makeMe
              .aNote()
              .underSameNotebookAs(treeParent)
              .folder(peerFolder)
              .title("Older Long CJK")
              .details(cjkText)
              .please();
      Note focus =
          makeMe
              .aNote()
              .underSameNotebookAs(treeParent)
              .folder(peerFolder)
              .title("Focus Note CJK")
              .please();

      GraphRAGResult result = graphRAGService.retrieve(focus, 1000, focus.getCreator());

      BareNote bn =
          result.getRelatedNotes().stream()
              .filter(b -> b.equals(olderLong))
              .findFirst()
              .orElseThrow();
      assertThat(bn.getRelationToFocusNote(), equalTo(RelationshipToFocusNote.OlderSibling));
      String truncated = bn.getDetails();

      assertThat(truncated, endsWith("..."));

      byte[] truncatedBytes =
          truncated.substring(0, truncated.length() - 3).getBytes(StandardCharsets.UTF_8);
      assertThat(truncatedBytes.length, lessThanOrEqualTo(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH));

      String withoutEllipsis = truncated.substring(0, truncated.length() - 3);
      assertThat(
          withoutEllipsis.chars().filter(ch -> ch >= 0x4E00 && ch <= 0x9FFF).count() * 3
              + withoutEllipsis.chars().filter(ch -> ch < 0x80).count(),
          lessThanOrEqualTo((long) RELATED_NOTE_DETAILS_TRUNCATE_LENGTH));
    }
  }

  @Nested
  class GetGraphRAGDescriptionTests {
    @Test
    void shouldNotContainNewlinesInJson() {
      Note anchor = makeMe.aNote().title("Test Note").details("Test Details").please();
      Notebook notebook = anchor.getNotebook();
      Folder peerFolder =
          makeMe.aFolder().notebook(notebook).name("graph-rag-json-newlines").please();
      anchor = makeMe.theNote(anchor).folder(peerFolder).please();
      User viewer = anchor.getCreator();
      Note focusPeer =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(anchor)
              .folder(peerFolder)
              .title("Focus for JSON")
              .please();

      String description = graphRAGService.getGraphRAGDescription(focusPeer);

      // Extract the JSON part (from first { to last })
      int jsonStart = description.indexOf("{");
      int jsonEnd = description.lastIndexOf("}") + 1;
      String jsonPart = description.substring(jsonStart, jsonEnd);
      // Verify no newlines in the JSON content itself
      assertThat(jsonPart, not(containsString("\n")));
      assertThat(jsonPart, not(containsString("\r")));
      // Verify it's valid JSON by parsing it
      try {
        JsonNode jsonNode = new ObjectMapperConfig().objectMapper().readTree(jsonPart);
        assertThat(jsonNode.has("focusNote"), is(true));
      } catch (Exception e) {
        throw new RuntimeException("Generated JSON is invalid", e);
      }
    }
  }
}
