package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.theokanning.openai.client.OpenAiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteAutomationServiceTests {
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  OpenAIAssistantMocker openAIAssistantMocker;
  OpenAIAssistantThreadMocker openAIAssistantThreadMocker;
  private Note testNote;
  private OpenAiAssistant assistant;
  private NoteAutomationService service;

  @BeforeEach
  void setup() {
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker = openAIAssistantMocker.mockThreadCreation("thread-id");

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();

    // Initialize common services
    assistant = new OpenAiAssistant(new OpenAiApiHandler(openAiApi), "ass-id");
    service =
        new NoteAutomationService(
            new NotebookAssistantForNoteService(assistant, testNote, getTestObjectMapper()));
  }

  @Test
  void shouldHandleCompletedRunWhenSuggestingTitle() throws JsonProcessingException {
    openAIAssistantThreadMocker
        .mockCreateRunInProcess("my-run-id")
        .aCompletedRun()
        .mockRetrieveRun();

    String result = service.suggestTitle();

    assertThat(result, is(nullValue()));
  }

  private com.fasterxml.jackson.databind.ObjectMapper getTestObjectMapper() {
    return new ObjectMapperConfig().objectMapper();
  }
}
