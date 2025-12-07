package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.Test;

class UriAndTitleTest {

  @Test
  void shouldGetUriFromNote() {
    Note note = mock(Note.class);
    when(note.getUri()).thenReturn("/n123");

    UriAndTitle uriAndTitle = UriAndTitle.fromNote(note);
    assertThat(uriAndTitle.getUri(), equalTo("/n123"));
  }

  @Test
  void shouldGetTitleFromNote() {
    Note note = mock(Note.class);
    when(note.getTitleConstructor()).thenReturn("Test Note");

    UriAndTitle uriAndTitle = UriAndTitle.fromNote(note);
    assertThat(uriAndTitle.getTitle(), equalTo("Test Note"));
  }

  @Test
  void shouldSerializeToObject() throws Exception {
    Note note = mock(Note.class);
    when(note.getTitleConstructor()).thenReturn("Test Note");
    when(note.getUri()).thenReturn("/n123");

    UriAndTitle uriAndTitle = UriAndTitle.fromNote(note);
    var mapper = new ObjectMapperConfig().objectMapper();

    JsonNode jsonNode = mapper.valueToTree(uriAndTitle);
    assertThat(jsonNode.has("uri"), is(true));
    assertThat(jsonNode.has("title"), is(true));
    assertThat(jsonNode.get("uri").asText(), equalTo("/n123"));
    assertThat(jsonNode.get("title").asText(), equalTo("Test Note"));
  }
}
