package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BareNoteTest {
  @Autowired private MakeMe makeMe;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldIncludeParentUriAndTitleWhenSerializedToJson() throws Exception {
    // Arrange
    Note parent = makeMe.aNote().titleConstructor("Parent Note").details("Parent Details").please();
    Note note = makeMe.aNote().under(parent).titleConstructor("Child Note").please();

    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(jsonNode.has("parentUriAndTitle"), is(true));
    assertThat(jsonNode.get("parentUriAndTitle").asText(), containsString("Parent Note"));
  }
}
