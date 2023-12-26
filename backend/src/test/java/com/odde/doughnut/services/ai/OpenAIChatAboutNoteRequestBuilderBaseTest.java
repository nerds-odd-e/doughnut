package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import org.junit.jupiter.api.Test;

class OpenAIChatAboutNoteRequestBuilderBaseTest {
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  String DETAILS = "Details";

  @Test
  void messageShouldContainTopic() {
    Note note = makeMe.aNote().inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString(note.getTopic()));
  }

  @Test
  void messageShouldContainDetails() {
    Note note = makeMe.aNote().details("description").inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString(DETAILS));
    assertThat(content, containsString(note.getDetails()));
  }

  @Test
  void messageShouldNotContainDetailsIfEmpty() {
    Note note = makeMe.aNote().withNoDescription().inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, not(containsString(DETAILS)));
  }

  private static String getNoteOfFocusDescription(Note note) {
    ChatCompletionRequest request = new OpenAIChatAboutNoteRequestBuilder("gpt", note).build();
    return request.getMessages().get(1).getContent();
  }
}
