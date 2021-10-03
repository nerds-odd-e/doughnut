package com.odde.doughnut.entities.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;

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
            value = new NoteWithPosition(){{
                setNote(note1.jsonObjectViewedBy1(null));
                setNotePosition(note1.jsonNotePosition(null));
            }};
        }

        @Test
        public void ownershipInfo() throws JsonProcessingException {
            Map<String, Object> deserialized = getJsonString(value);
            final Object deNote = deserialized.get("noteItself");
            assertThat(deNote.toString(), not(containsString("ownership")));
        }

        @Test
        public void notebookInfo() throws JsonProcessingException {
            Map<String, Object> deserialized = getJsonString(value);
            final Object deNotebook = deserialized.get("notePosition");
            assertThat(deNotebook.toString(), not(containsString("headNote")));
        }

        private Map<String, Object> getJsonString(NoteWithPosition value) throws JsonProcessingException {
            return new ObjectMapper().readerForMapOf(Object.class).readValue(new ObjectMapper().writeValueAsString(value));
        }
    }

}
