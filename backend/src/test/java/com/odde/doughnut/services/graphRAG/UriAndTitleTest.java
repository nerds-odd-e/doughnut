package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.configs.ObjectMapperConfig;
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
    var mapper = new ObjectMapperConfig().objectMapper();

    String json = mapper.writeValueAsString(uriAndTitle);
    assertThat(json, equalTo("\"[Test Note](/n123)\""));
  }
}
