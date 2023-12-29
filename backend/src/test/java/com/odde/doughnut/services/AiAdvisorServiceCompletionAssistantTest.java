package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.assistants.AssistantRequest;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class AiAdvisorServiceCompletionAssistantTest {

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiApi openAiApi;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Nested
  class CreateNoteCompletionAssistant {
    @Test
    void getAiSuggestion_givenAString_returnsAiSuggestionObject() {
      when(openAiApi.createAssistant(ArgumentMatchers.any()))
          .thenReturn(Single.just(new Assistant()));
      aiAdvisorService.createNoteCompletionAssistant("gpt");
      ArgumentCaptor<AssistantRequest> captor = ArgumentCaptor.forClass(AssistantRequest.class);
      verify(openAiApi).createAssistant(captor.capture());
      AssistantRequest assistantRequest = captor.getValue();
      assertThat(assistantRequest.getName(), is("Note details completion"));
      assertThat(assistantRequest.getTools(), hasSize(2));
    }
  }
}
