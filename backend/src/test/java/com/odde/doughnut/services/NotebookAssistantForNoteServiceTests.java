package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  private Note testNote;
  private OpenAiAssistant assistant;
  private NotebookAssistantForNoteService service;

  @BeforeEach
  void setup() {
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker = openAIAssistantMocker.mockThreadCreation(null);

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();

    // Initialize common services
    assistant = new OpenAiAssistant(new OpenAiApiHandler(openAiApi), "ass-id");
    service = new NotebookAssistantForNoteService(assistant, testNote);
  }

  @Nested
  class GenerateQuestion {
    @Test
    void shouldGenerateQuestionWithCorrectStem() throws Exception {
      // Mock AI response
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      mockSuccessfulQuestionGeneration(jsonQuestion);

      // Execute
      MCQWithAnswer generatedQuestion = service.generateQuestion();

      // Verify
      assertThat(
          generatedQuestion.getMultipleChoicesQuestion().getStem(),
          containsString("What is the first color in the rainbow?"));
    }

    @Test
    void shouldPassQuestionGenerationInstructionAsUserMessage() throws JsonProcessingException {
      // Mock AI response
      MCQWithAnswer mcqWithAnswer =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the capital of France?")
              .choices("Paris", "London", "Berlin")
              .correctChoiceIndex(0)
              .please();

      mockSuccessfulQuestionGeneration(mcqWithAnswer);

      // Execute
      service.generateQuestion();

      // Verify
      ArgumentCaptor<ThreadRequest> messagesCaptor = ArgumentCaptor.forClass(ThreadRequest.class);
      verify(openAiApi).createThread(messagesCaptor.capture());

      List<MessageRequest> messages = messagesCaptor.getValue().getMessages();
      assertThat(messages, hasSize(2));

      MessageRequest instructionMessage = messages.get(1);
      assertThat(instructionMessage.getRole(), is("user"));
      assertThat(
          instructionMessage.getContent().toString(),
          containsString("assume the role of a Memory Assistant"));
    }

    private void mockSuccessfulQuestionGeneration(MCQWithAnswer question) {
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(
              question, AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue())
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");
    }
  }
}