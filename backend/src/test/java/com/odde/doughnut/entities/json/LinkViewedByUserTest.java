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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class LinkViewedByUserTest {

    MakeMe makeMe = new MakeMe();

    @Nested
    class JsonTest {
        Note note1;
        Note note2;
        Link link;
        final LinkViewed linkViewedByUser = new LinkViewed();
        Map<Link.LinkType, LinkViewed> links = new HashMap<>(){{
            put(Link.LinkType.SPECIALIZE, linkViewedByUser);
        }};
        NoteViewedByUser value;

        @BeforeEach
        void thereAreTwoNotesWithALinkInBetween() {
            Note top = makeMe.aNote().inMemoryPlease();
            note1 = makeMe.aNote().under(top).description("note1description").inMemoryPlease();
            note2 = makeMe.aNote().under(top).description("note2description").inMemoryPlease();
            link = makeMe.aLink().between(note1, note2).inMemoryPlease();
            value = new NoteViewedByUser(){{
                setNote(note1);
                setLinks(links);
            }};

        }

        @Test
        public void directLink() throws JsonProcessingException {
            linkViewedByUser.setDirect(Collections.singletonList(link));
            Map<String, Object> deserialized = getJsonString(value);
            final Object o = deserialized.get("links");
            assertThat(o.toString(), containsString(note2.getTitle()));
            assertThat(o.toString(), not(containsString("noteContent")));
            assertThat(o.toString(), containsString("targetNote"));
            assertThat(o.toString(), not(containsString("sourceNote")));
        }

        @Test
        public void reverseLink() throws JsonProcessingException {
            linkViewedByUser.setReverse(Collections.singletonList(link));
            Map<String, Object> deserialized = getJsonString(value);
            final Object o = deserialized.get("links");
            assertThat(o.toString(), containsString(note1.getTitle()));
            assertThat(o.toString(), not(containsString("noteContent")));
            assertThat(o.toString(), containsString("sourceNote"));
            assertThat(o.toString(), not(containsString("targetNote")));
        }

        private Map<String, Object> getJsonString(NoteViewedByUser value) throws JsonProcessingException {
            return new ObjectMapper().readerForMapOf(Object.class).readValue(new ObjectMapper().writeValueAsString(value));
        }
    }

}
