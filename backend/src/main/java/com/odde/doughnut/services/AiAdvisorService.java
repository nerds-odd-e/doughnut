package com.odde.doughnut.services;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;

public class AiAdvisorService {

  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public OtherAiServices getOtherAiServices() {
    return new OtherAiServices(openAiApiHandler);
  }

  public AssistantService getContentCompletionService(SettingAccessor settingAccessor) {
    return new AssistantService(
        openAiApiHandler,
        settingAccessor,
        "Note details completion",
        List.of(
            AiTool.build(
                COMPLETE_NOTE_DETAILS,
                "Text completion for the details of the note of focus",
                NoteDetailsCompletion.class,
                (noteDetailsCompletion) -> {
                  AiCompletionRequiredAction result = new AiCompletionRequiredAction();
                  result.setContentToAppend(noteDetailsCompletion.completion);
                  return result;
                }),
            AiTool.build(
                askClarificationQuestion,
                "Ask question to get more context",
                ClarifyingQuestion.class,
                (clarifyingQuestion) -> {
                  AiCompletionRequiredAction result = new AiCompletionRequiredAction();
                  result.setClarifyingQuestion(clarifyingQuestion);
                  return result;
                })));
  }

  public AssistantService getChatService(
      GlobalSettingsService.GlobalSettingsKeyValue settingAccessor) {
    return new AssistantService(openAiApiHandler, settingAccessor, "chat assistant", List.of());
  }
}
