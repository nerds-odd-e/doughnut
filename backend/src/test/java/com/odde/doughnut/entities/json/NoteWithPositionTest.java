package com.odde.doughnut.entities.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.testability.MakeMe;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class NoteWithPositionTest {

  MakeMe makeMe = new MakeMe();

  @Nested
  class JsonTest {
    Note note1;
    NoteWithPosition value;

    @BeforeEach
    void thereAreTwoNotesWithALinkInBetween() {
      Note top = makeMe.aNote().inMemoryPlease();
      note1 = makeMe.aNote().under(top).description("note1description").inMemoryPlease();
      value =
          new NoteWithPosition() {
            {
              setNote(new NoteViewer(null, note1).toJsonObject());
              setNotePosition(new NoteViewer(null, note1).jsonNotePosition(note1));
            }
          };
    }

    @Test
    public void ownershipInfo() throws JsonProcessingException {
      Map<String, Object> deserialized = getJsonString(value);
      final Object deNote = deserialized.get("note");
      assertThat(deNote.toString(), not(containsString("ownership")));
    }

    private Map<String, Object> getJsonString(NoteWithPosition value)
        throws JsonProcessingException {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new Jdk8Module());
      return objectMapper
          .readerForMapOf(Object.class)
          .readValue(objectMapper.writeValueAsString(value));
    }
  }
}
