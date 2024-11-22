package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.theokanning.openai.client.OpenAiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookAssistantForNoteServiceTests {
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  OpenAIAssistantMocker openAIAssistantMocker;
  OpenAIAssistantThreadMocker openAIAssistantThreadMocker;

  @BeforeEach
  void setup() {
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker = openAIAssistantMocker.mockThreadCreation(null);
  }

  @Nested
  class GenerateQuestion {
    @Test
    void shouldGenerateQuestionWithCorrectStem() throws Exception {
      // Prepare test data
      Note note = makeMe.aNote().details("description long enough.").please();
      makeMe.aNote().under(note).please(); // Additional note needed for question generation

      // Mock AI response
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(
              jsonQuestion, AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue())
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      // Execute
      OpenAiAssistant assistant = new OpenAiAssistant(new OpenAiApiHandler(openAiApi), "ass-id");
      NotebookAssistantForNoteService service =
          new NotebookAssistantForNoteService(assistant, note);
      MCQWithAnswer generatedQuestion = service.generateQuestion();

      // Verify
      assertThat(
          generatedQuestion.getMultipleChoicesQuestion().getStem(),
          containsString("What is the first color in the rainbow?"));
    }
  }
}
