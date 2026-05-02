package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GraphRAGResultTest {
  @Autowired private MakeMe makeMe;
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @Test
  void structuralChildBareNoteOmitsRemovedGraphFields() throws Exception {
    Note parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
    Note note = makeMe.aNote().under(parent).title("Child Note").please();
    JsonNode json =
        objectMapper.valueToTree(BareNote.fromNote(note, RelationshipToFocusNote.Child));

    assertThat(json.has("parent"), is(false));
    assertThat(json.has("subject"), is(false));
    assertThat(json.has("target"), is(false));
    assertThat(json.has("relation_type"), is(false));
    assertThat(json.get("title").asText(), containsString("Child Note"));
    assertThat(json.has("uri"), is(true));
  }

  @Test
  void relationshipBareNoteOmitsRemovedGraphFields() throws Exception {
    Note parent = makeMe.aNote().title("Parent Note").please();
    Note targetNote = makeMe.aNote().title("Target Note").details("Target Details").please();
    Note note = makeMe.aRelation().between(parent, targetNote).please();
    JsonNode json =
        objectMapper.valueToTree(BareNote.fromNote(note, RelationshipToFocusNote.Child));

    assertThat(json.has("parent"), is(false));
    assertThat(json.has("subject"), is(false));
    assertThat(json.has("target"), is(false));
    assertThat(json.has("relation_type"), is(false));
    assertThat(json.get("title").asText(), equalTo(note.getTitle()));
    assertThat(json.get("details").asText(), containsString("Parent Note"));
  }

  @Test
  void rootBareNoteHasExpectedJsonShape() throws Exception {
    Note note = makeMe.aNote().title("Root Note").details("Some details").please();
    JsonNode json =
        objectMapper.valueToTree(BareNote.fromNote(note, RelationshipToFocusNote.Child));

    assertThat(
        json::fieldNames,
        containsInAnyOrder(
            "uri",
            "title",
            "details",
            "relationToFocusNote",
            "linkFromFocus",
            "linkHop2",
            "createdAt"));
    assertThat(json.has("detailsTruncated"), is(false));
    assertThat(json.get("details").asText(), is("Some details"));
  }

  @Test
  void createdAtSerializedWithoutUpdatedAt() throws Exception {
    Timestamp createdAt = new Timestamp(System.currentTimeMillis() - 86400000);
    Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
    Note note =
        makeMe
            .aNote()
            .title("Test Note")
            .details("Test Details")
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .please();
    makeMe.refresh(note);

    JsonNode json =
        objectMapper.valueToTree(BareNote.fromNote(note, RelationshipToFocusNote.Child));

    assertThat(json.has("createdAt"), is(true));
    assertThat(json.has("updatedAt"), is(false));
    assertThat(json.get("createdAt").asText(), is(not(emptyString())));
  }

  @Nested
  class FocusNoteTest {
    @Test
    void usesFolderCrumbContextualPathInsteadOfParentField() throws Exception {
      Note parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
      Folder peerFolder =
          makeMe.aFolder().notebook(parent.getNotebook()).name("focus-peer-folder").please();
      parent = makeMe.theNote(parent).folder(peerFolder).please();
      Note note = makeMe.aNote().under(parent).folder(peerFolder).title("Child Note").please();
      JsonNode json = objectMapper.valueToTree(FocusNote.fromNote(note));

      assertThat(json.has("parent"), is(false));
      assertThat(json.has("contextualPath"), is(true));
      assertThat(json.get("contextualPath").asText(), containsString("focus-peer-folder"));
    }
  }

  @Nested
  class DetailsTruncationTest {
    @Test
    void truncatesLongDetailsAndSetsTruncatedFlag() throws Exception {
      String longDetails = "x".repeat(600);
      Note note = makeMe.aNote().title("Long Note").details(longDetails).please();
      JsonNode json =
          objectMapper.valueToTree(BareNote.fromNote(note, RelationshipToFocusNote.Child));

      assertThat(json.has("detailsTruncated"), is(true));
      assertThat(json.get("detailsTruncated").asBoolean(), is(true));
      assertThat(json.get("details").asText(), endsWith("..."));
    }

    @Test
    void focusNoteDoesNotExposeDetailsTruncated() throws Exception {
      Note note = makeMe.aNote().title("Focus Note").details("Some details").please();
      JsonNode json = objectMapper.valueToTree(FocusNote.fromNote(note));

      assertThat(json.has("detailsTruncated"), is(false));
    }
  }
}
