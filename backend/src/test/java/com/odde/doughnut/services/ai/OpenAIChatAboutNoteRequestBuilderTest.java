package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import org.junit.jupiter.api.Test;

class OpenAIChatAboutNoteRequestBuilderTest {
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  String DETAILS = "Details";

  @Test
  void messageShouldContainTopic() {
    Note note = makeMe.aNote().inMemoryPlease();
    ChatCompletionRequest request =
        new OpenAIChatAboutNoteRequestBuilder().contentOfNoteOfCurrentFocus(note).build();
    assertThat(request.getMessages().get(0).getContent(), containsString(note.getTopic()));
  }

  @Test
  void messageShouldContainDetails() {
    Note note = makeMe.aNote().details("description").inMemoryPlease();
    ChatCompletionRequest request =
        new OpenAIChatAboutNoteRequestBuilder().contentOfNoteOfCurrentFocus(note).build();
    assertThat(request.getMessages().get(0).getContent(), containsString(DETAILS));
    assertThat(request.getMessages().get(0).getContent(), containsString(note.getDetails()));
  }

  @Test
  void messageShouldNotContainDetailsIfEmpty() {
    Note note = makeMe.aNote().withNoDescription().inMemoryPlease();
    ChatCompletionRequest request =
        new OpenAIChatAboutNoteRequestBuilder().contentOfNoteOfCurrentFocus(note).build();
    assertThat(request.getMessages().get(0).getContent(), not(containsString(DETAILS)));
  }
}
