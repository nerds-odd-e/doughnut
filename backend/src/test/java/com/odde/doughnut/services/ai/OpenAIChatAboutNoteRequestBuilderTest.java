package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import org.junit.jupiter.api.Test;

class OpenAIChatAboutNoteRequestBuilderTest {
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();

  @Test
  void messageShouldContainTopic() {
    Note note = makeMe.aNote().inMemoryPlease();
    ChatCompletionRequest request =
        new OpenAIChatAboutNoteRequestBuilder("").detailsOfNoteOfCurrentFocus(note).build();
    assertThat(request.getMessages().get(1).getContent(), containsString(note.getTopic()));
  }

  @Test
  void messageShouldContainDescription() {
    Note note = makeMe.aNote().description("description").inMemoryPlease();
    ChatCompletionRequest request =
        new OpenAIChatAboutNoteRequestBuilder("").detailsOfNoteOfCurrentFocus(note).build();
    assertThat(request.getMessages().get(1).getContent(), containsString("Description"));
    assertThat(request.getMessages().get(1).getContent(), containsString(note.getDescription()));
  }

  @Test
  void messageShouldNotContainDescriptionIfEmpty() {
    Note note = makeMe.aNote().withNoDescription().inMemoryPlease();
    ChatCompletionRequest request =
        new OpenAIChatAboutNoteRequestBuilder("").detailsOfNoteOfCurrentFocus(note).build();
    assertThat(request.getMessages().get(1).getContent(), not(containsString("Description")));
  }
}
