package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.Test;

class UriAndTitleTest {

  @Test
  void shouldCreateMarkdownLinkFromNote() {
    Note note = mock(Note.class);
    when(note.getTopicConstructor()).thenReturn("Test Note");
    when(note.getId()).thenReturn(123);

    UriAndTitle uriAndTitle = UriAndTitle.fromNote(note);
    assertThat(uriAndTitle.toString(), equalTo("[Test Note](/n123)"));
  }

  @Test
  void shouldSerializeToMarkdownLinkString() throws Exception {
    Note note = mock(Note.class);
    when(note.getTopicConstructor()).thenReturn("Test Note");
    when(note.getId()).thenReturn(123);

    UriAndTitle uriAndTitle = UriAndTitle.fromNote(note);
    ObjectMapper mapper = new ObjectMapper();

    String json = mapper.writeValueAsString(uriAndTitle);
    assertThat(json, equalTo("\"[Test Note](/n123)\""));
  }

  @Test
  void shouldTreatSameNoteAsEqual() {
    Note note = mock(Note.class);
    UriAndTitle link1 = UriAndTitle.fromNote(note);
    UriAndTitle link2 = UriAndTitle.fromNote(note);
    Note differentNote = mock(Note.class);
    UriAndTitle link3 = UriAndTitle.fromNote(differentNote);

    assertThat(link1, equalTo(link2));
    assertThat(link1.hashCode(), equalTo(link2.hashCode()));
    assertThat(link1.equals(link3), equalTo(false));
  }
}
