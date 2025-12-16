package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
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
class GraphRAGResultTest {
  @Autowired private MakeMe makeMe;
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
  private static final String PARENT = "parent";

  @Test
  void shouldIncludeParentUriAndTitleWhenSerializedToJson() throws Exception {
    // Arrange
    Note parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
    Note note = makeMe.aNote().under(parent).title("Child Note").please();

    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(jsonNode.has(PARENT), is(true));
    JsonNode parentJson = jsonNode.get(PARENT);
    assertThat(parentJson.isObject(), is(true));
    assertThat(parentJson.get("title").asText(), containsString("Parent Note"));
    assertThat(parentJson.has("uri"), is(true));
  }

  @Nested
  class RelatedNoteTest {
    private Note parent;
    private Note targetNote;
    private Note note;
    private JsonNode jsonNode;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().title("Parent Note").please();
      targetNote = makeMe.aNote().title("Target Note").details("Target Details").please();
      note = makeMe.aRelation().between(parent, targetNote).please();

      BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);
      jsonNode = objectMapper.valueToTree(bareNote);
    }

    @Test
    void shouldIncludeSubjectButNotParentWhenNoteIsRelated() {
      // Assert
      assertThat(jsonNode.has(PARENT), is(false));
      assertThat(jsonNode.has("subject"), is(true));
      JsonNode subject = jsonNode.get("subject");
      assertThat(subject.isObject(), is(true));
      assertThat(subject.get("title").asText(), containsString("Parent Note"));
    }

    @Test
    void shouldHaveRelationTypeFieldInsteadOfTitleWhenNoteIsRelated() {
      assertThat(jsonNode.has("title"), is(false));
      assertThat(jsonNode.has("relation_type"), is(true));
      assertThat(jsonNode.get("relation_type").asText(), is("a specialization of"));
    }

    @Test
    void shouldHaveCorrectPropertyOrderForRelatedNote() throws Exception {
      // Act
      String jsonString = objectMapper.writeValueAsString(jsonNode);

      // Assert property order
      assertThat(jsonString, startsWith("{\"uri\":"));
      assertThat(
          jsonString.indexOf("\"subject\":"), is(greaterThan(jsonString.indexOf("\"uri\":"))));
      assertThat(
          jsonString.indexOf("\"relation_type\":"),
          is(greaterThan(jsonString.indexOf("\"subject\":"))));
      assertThat(
          jsonString.indexOf("\"target\":"),
          is(greaterThan(jsonString.indexOf("\"relation_type\":"))));
    }
  }

  @Test
  void shouldHaveExpectedFieldNamesForNoteWithoutParent() throws Exception {
    // Arrange
    Note note = makeMe.aNote().title("Root Note").details("Some details").please();
    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(
        jsonNode::fieldNames,
        containsInAnyOrder("uri", "title", "details", "relationToFocusNote", "createdAt"));
    // detailsTruncated should not be present when details are not truncated
    assertThat(jsonNode.has("detailsTruncated"), is(false));
  }

  @Test
  void shouldIncludeCreatedAtWhenSerializedToJson() throws Exception {
    // Arrange
    Timestamp createdAt = new Timestamp(System.currentTimeMillis() - 86400000); // 1 day ago
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

    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(jsonNode.has("createdAt"), is(true));
    assertThat(jsonNode.has("updatedAt"), is(false));
    // Timestamps are serialized as ISO strings when WRITE_DATES_AS_TIMESTAMPS is disabled
    assertThat(jsonNode.get("createdAt").asText(), is(not(emptyString())));
  }

  @Nested
  class FocusNoteTest {
    @Test
    void shouldIncludeParentUriAndTitleWhenSerializedToJson() throws Exception {
      // Arrange
      Note parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
      Note note = makeMe.aNote().under(parent).title("Child Note").please();

      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);
      String jsonString = objectMapper.writeValueAsString(focusNote);

      // Assert
      assertThat(jsonNode.has(PARENT), is(true));
      JsonNode parentJson = jsonNode.get(PARENT);
      assertThat(parentJson.isObject(), is(true));
      assertThat(parentJson.get("title").asText(), containsString("Parent Note"));

      // Assert property order
      assertThat(jsonString, startsWith("{\"uri\":"));
      assertThat(jsonString.indexOf("\"title\":"), is(greaterThan(jsonString.indexOf("\"uri\":"))));
      assertThat(
          jsonString.indexOf("\"details\":"), is(greaterThan(jsonString.indexOf("\"title\":"))));
    }

    @Test
    void shouldIncludeCreatedAtWhenSerializedToJson() throws Exception {
      // Arrange
      Timestamp createdAt = new Timestamp(System.currentTimeMillis() - 86400000); // 1 day ago
      Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
      Note note =
          makeMe
              .aNote()
              .title("Focus Note")
              .details("Focus Details")
              .createdAt(createdAt)
              .updatedAt(updatedAt)
              .please();
      makeMe.refresh(note);

      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);

      // Assert
      assertThat(jsonNode.has("createdAt"), is(true));
      assertThat(jsonNode.has("updatedAt"), is(false));
      // Timestamps are serialized as ISO strings when WRITE_DATES_AS_TIMESTAMPS is disabled
      assertThat(jsonNode.get("createdAt").asText(), is(not(emptyString())));
    }
  }

  @Nested
  class DetailsTruncationTest {
    @Test
    void shouldNotIncludeDetailsTruncatedWhenDetailsAreNotTruncated() throws Exception {
      // Arrange - details shorter than truncation limit
      Note note = makeMe.aNote().title("Short Note").details("Short details").please();
      BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(bareNote);

      // Assert
      assertThat(jsonNode.has("detailsTruncated"), is(false));
      assertThat(jsonNode.get("details").asText(), is("Short details"));
    }

    @Test
    void shouldIncludeDetailsTruncatedAsTrueWhenDetailsAreTruncated() throws Exception {
      // Arrange - create details longer than truncation limit (250 bytes)
      String longDetails = "x".repeat(300); // 300 characters should exceed 250 bytes
      Note note = makeMe.aNote().title("Long Note").details(longDetails).please();
      BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(bareNote);

      // Assert
      assertThat(jsonNode.has("detailsTruncated"), is(true));
      assertThat(jsonNode.get("detailsTruncated").asBoolean(), is(true));
      assertThat(jsonNode.get("details").asText(), endsWith("..."));
    }

    @Test
    void shouldNotIncludeDetailsTruncatedForFocusNote() throws Exception {
      // Arrange
      Note note = makeMe.aNote().title("Focus Note").details("Some details").please();
      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);

      // Assert - FocusNote uses fromNoteWithoutTruncate internally, so detailsTruncated should be
      // null
      assertThat(jsonNode.has("detailsTruncated"), is(false));
    }
  }
}
