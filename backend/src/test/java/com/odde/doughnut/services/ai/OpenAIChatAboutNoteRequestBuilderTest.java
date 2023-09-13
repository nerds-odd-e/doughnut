package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

  @ParameterizedTest
  @CsvSource({
    "<p>abc<br>def</p>,                       abc\\s*\\Rdef",
    "<a href=\"https://abc.com\">a link</a>,  \\[a link\\]\\(https://abc.com\\)",
  })
  void messageShouldConvertHTMLToMarkdown(String original, String converted) {
    Note note = makeMe.aNote().details(original).inMemoryPlease();
    ChatCompletionRequest request =
        new OpenAIChatAboutNoteRequestBuilder().contentOfNoteOfCurrentFocus(note).build();
    assertThat(
        request.getMessages().get(0).getContent(),
        matchesPattern("(?s).*%s.*".formatted(converted)));
  }
}
