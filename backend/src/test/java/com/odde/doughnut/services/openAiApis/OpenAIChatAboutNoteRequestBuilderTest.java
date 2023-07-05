package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.builders.NoteBuilder;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.AssertTrue;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIChatAboutNoteRequestBuilderTest {

  @Test
  void userInstructionToGenerateQuestion() {
    var builder = new OpenAIChatAboutNoteRequestBuilder("programmingLanguage");
    var note = MakeMe.makeMeWithoutFactoryService().aNote("programming language").inMemoryPlease();
    note.getNoteAccessories().setQuestionGenerationInstruction("only about java");

    var messages = builder.userInstructionToGenerateQuestion(note).build().getMessages();
    var lastMessage = messages.get(messages.size()-1);

    assertTrue(lastMessage.getContent().contains("only about java"));
  }
}
