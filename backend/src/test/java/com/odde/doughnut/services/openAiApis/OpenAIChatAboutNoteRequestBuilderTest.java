package com.odde.doughnut.services.openAiApis;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;

class OpenAIChatAboutNoteRequestBuilderTest {

  @Test
  void userInstructionToGenerateQuestion() {
    var builder = new OpenAIChatAboutNoteRequestBuilder("programmingLanguage");
    var note = MakeMe.makeMeWithoutFactoryService().aNote("programming language").inMemoryPlease();
    note.getNoteAccessories().setQuestionGenerationInstruction("only about java");

    var messages = builder.userInstructionToGenerateQuestion(note,null).build().getMessages();
    var lastMessage = messages.get(messages.size() - 1);

    assertTrue(lastMessage.getContent().contains("only about java"));
  }
}
