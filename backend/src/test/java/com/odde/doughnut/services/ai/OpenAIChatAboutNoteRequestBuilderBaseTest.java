package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import org.junit.jupiter.api.Test;

class OpenAIChatAboutNoteRequestBuilderBaseTest {
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  String DETAILS = "details";

  @Test
  void messageShouldContainTitle() {
    Note note = makeMe.aNote().inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString(note.getTopicConstructor()));
  }

  @Test
  void messageShouldContainDetails() {
    Note note = makeMe.aNote().details("description").inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString("uri"));
    assertThat(content, containsString(DETAILS));
    assertThat(content, containsString(note.getDetails()));
    assertThat(content, containsString("createdAt"));
  }

  @Test
  void messageShouldContainTarget() {
    Note to = makeMe.aNote().inMemoryPlease();
    Note from = makeMe.aNote().inMemoryPlease();
    Note note = makeMe.aReification().between(from, to).inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString("object"));
  }

  @Test
  void shouldNotAddNoteDescriptionWhenEmpty() {
    Note note = makeMe.aNote().details("").inMemoryPlease();
    ChatCompletionRequest request =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder("gpt", note).build();
    assertThat(request.getMessages().size(), equalTo(1)); // Only system instruction
    assertThat(
        request.getMessages().get(0).getTextContent(),
        equalTo(OpenAIChatRequestBuilder.systemInstruction));
  }

  @Test
  void shouldNotAddNoteDescriptionWhenNull() {
    Note note = makeMe.aNote().details(null).inMemoryPlease();
    ChatCompletionRequest request =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder("gpt", note).build();
    assertThat(request.getMessages().size(), equalTo(1)); // Only system instruction
    assertThat(
        request.getMessages().get(0).getTextContent(),
        equalTo(OpenAIChatRequestBuilder.systemInstruction));
  }

  private static String getNoteOfFocusDescription(Note note) {
    ChatCompletionRequest request =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder("gpt", note).build();
    return request.getMessages().get(1).getTextContent();
  }
}
