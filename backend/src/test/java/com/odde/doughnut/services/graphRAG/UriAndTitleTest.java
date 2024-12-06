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
  void shouldCreateMarkdownLinkFromTitleAndUri() {
    UriAndTitle uriAndTitle = new UriAndTitle("Test Title", "/test/uri");
    assertThat(uriAndTitle.toString(), equalTo("[Test Title](/test/uri)"));
  }

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
    UriAndTitle uriAndTitle = new UriAndTitle("Test Title", "/test/uri");
    ObjectMapper mapper = new ObjectMapper();

    String json = mapper.writeValueAsString(uriAndTitle);
    assertThat(json, equalTo("\"[Test Title](/test/uri)\""));
  }

  @Test
  void shouldTreatSameMarkdownLinkAsEqual() {
    UriAndTitle link1 = new UriAndTitle("Test", "/uri");
    UriAndTitle link2 = new UriAndTitle("Test", "/uri");
    UriAndTitle link3 = new UriAndTitle("Different", "/uri");

    assertThat(link1, equalTo(link2));
    assertThat(link1.hashCode(), equalTo(link2.hashCode()));
    assertThat(link1.equals(link3), equalTo(false));
  }
}
