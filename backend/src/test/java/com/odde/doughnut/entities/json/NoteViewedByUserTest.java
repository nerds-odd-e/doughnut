package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NoteViewedByUserTest {

    MakeMe makeMe = new MakeMe();

    @Nested
    class JsonTest {
        Note note1;
        NoteViewedByUser value;

        @BeforeEach
        void thereAreTwoNotesWithALinkInBetween() {
            Note top = makeMe.aNote().inMemoryPlease();
            note1 = makeMe.aNote().under(top).description("note1description").inMemoryPlease();
            value = new NoteViewedByUser(){{
                setNoteItself(note1.jsonObjectViewedBy1(null));
                setNoteBreadcrumbViewedByUser(note1.jsonBreadcrumbViewedBy(null));
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
            final Object deNotebook = deserialized.get("noteBreadcrumbViewedByUser");
            assertThat(deNotebook.toString(), not(containsString("headNote")));
        }

        private Map<String, Object> getJsonString(NoteViewedByUser value) throws JsonProcessingException {
            return new ObjectMapper().readerForMapOf(Object.class).readValue(new ObjectMapper().writeValueAsString(value));
        }
    }

}
