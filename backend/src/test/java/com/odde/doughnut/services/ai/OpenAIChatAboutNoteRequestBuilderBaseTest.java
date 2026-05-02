package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.junit.jupiter.api.Test;

class OpenAIChatAboutNoteRequestBuilderBaseTest {
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  String DETAILS = "details";

  @Test
  void messageShouldContainTitle() {
    Note note = makeMe.aNote().inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString(note.getTitle()));
  }

  @Test
  void messageShouldContainDetails() {
    Note note = makeMe.aNote().details("description").inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString("uri"));
    assertThat(content, containsString(DETAILS));
    assertThat(content, containsString(note.getDetails()));
  }

  @Test
  void relationshipNoteDescriptionContainsYamlTargetKey() {
    Note to = makeMe.aNote().inMemoryPlease();
    Note from = makeMe.aNote().inMemoryPlease();
    Note note = makeMe.aRelation().between(from, to).inMemoryPlease();
    String content = getNoteOfFocusDescription(note);
    assertThat(content, containsString("target"));
  }

  @Test
  void noteOverloadDelegatesToSameSystemContentAsStringOverload() {
    Note note = makeMe.aNote().details("body").title("T").inMemoryPlease();
    String fromNote =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder("gpt", note)
            .build()
            .messages()
            .getFirst()
            .system()
            .get()
            .content()
            .toString();
    String fromString =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder("gpt", note.getNoteDescription())
            .build()
            .messages()
            .getFirst()
            .system()
            .get()
            .content()
            .toString();
    assertThat(fromNote, equalTo(fromString));
  }

  private static String getNoteOfFocusDescription(Note note) {
    ChatCompletionCreateParams request =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder("gpt", note).build();
    return request.messages().get(0).system().get().content().toString();
  }
}
