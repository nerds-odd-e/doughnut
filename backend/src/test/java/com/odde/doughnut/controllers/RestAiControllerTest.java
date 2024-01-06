package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.AiCompletionResponse;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.messages.Message;
import com.theokanning.openai.messages.MessageRequest;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.runs.RunCreateRequest;
import com.theokanning.openai.threads.Thread;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestAiControllerTest {
  RestAiController controller;
  UserModel currentUser;

  Note note;
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  AiCompletionParams params = new AiCompletionParams();

  @BeforeEach
  void Setup() {
    currentUser = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().please();
    controller =
        new RestAiController(
            openAiApi, makeMe.modelFactoryService, currentUser, testabilitySettings);
  }

  @Nested
  class AutoCompleteNoteDetails {
    ArgumentCaptor<ChatCompletionRequest> captor =
        ArgumentCaptor.forClass(ChatCompletionRequest.class);
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      Note cosmos = makeMe.aNote("cosmos").please();
      Note solar = makeMe.aNote("solar system").under(cosmos).please();
      note = makeMe.aNote("Earth").under(solar).please();
      openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(
          new NoteDetailsCompletion("blue planet"), "");
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () ->
              new RestAiController(
                      openAiApi,
                      makeMe.modelFactoryService,
                      makeMe.aNullUserModel(),
                      testabilitySettings)
                  .getCompletion(note, params));
    }

    @Nested
    class StartACompletionThread {
      @BeforeEach
      void setup() {
        Thread thread = new Thread();
        thread.setId("this-thread");
        when(openAiApi.createThread(ArgumentMatchers.any())).thenReturn(Single.just(thread));
        when(openAiApi.createMessage(eq("this-thread"), ArgumentMatchers.any()))
            .thenReturn(Single.just(new Message()));
      }

      @Test
      void useTheCorrectAssistant() {
        new GlobalSettingsService(makeMe.modelFactoryService)
            .getNoteCompletionAssistantId()
            .setKeyValue(makeMe.aTimestamp().please(), "my-assistant-id");
        controller.getCompletion(note, params);
        ArgumentCaptor<RunCreateRequest> runRequest =
            ArgumentCaptor.forClass(RunCreateRequest.class);
        verify(openAiApi).createRun(any(), runRequest.capture());
        assertEquals("my-assistant-id", runRequest.getValue().getAssistantId());
      }

      @Test
      void mustCreateANewThreadIfNoThreadIDGiven() {
        AiCompletionResponse aiCompletionResponse = controller.getCompletion(note, params);
        assertEquals("this-thread", aiCompletionResponse.getThreadId());
      }

      @Test
      void mustCreateMessageToRequestCompletion() {
        ArgumentCaptor<MessageRequest> captor = ArgumentCaptor.forClass(MessageRequest.class);
        controller.getCompletion(note, params);
        verify(openAiApi, times(1)).createMessage(any(), captor.capture());
        assertThat(captor.getAllValues().get(0).getContent()).contains("cosmos › solar system");
        assertThat(captor.getAllValues().get(0).getContent())
            .contains(" \"details_to_complete\" : \"\"");
      }
    }

    @Nested
    class AnswerClarifyingQuestion {
      @BeforeEach
      void setup() {
        params.setThreadId("any-thread-id");
      }

      @Test
      void askSuggestionWithRightPrompt() {
        controller.answerCompletionClarifyingQuestion(note, params);
        verify(openAiApi).createChatCompletion(captor.capture());
        assertThat(captor.getValue().getMaxTokens()).isLessThan(200);
        assertThat(captor.getValue().getMessages()).hasSize(3);
        assertThat(captor.getValue().getMessages().get(2).getContent())
            .contains(" \"details_to_complete\" : \"\"");
        assertThat(captor.getValue().getMessages().get(1).getContent())
            .contains("cosmos › solar system");
      }

      @Test
      void askSuggestionWithRightModel() {
        new GlobalSettingsService(makeMe.modelFactoryService)
            .getGlobalSettingOthers()
            .setKeyValue(makeMe.aTimestamp().please(), "gpt-future");
        controller.answerCompletionClarifyingQuestion(note, params);
        verify(openAiApi).createChatCompletion(captor.capture());
        assertEquals("gpt-future", captor.getValue().getModel());
      }

      @Test
      void askSuggestionWithIncompleteAssistantMessage() {
        params.setDetailsToComplete("What goes up,");
        controller.answerCompletionClarifyingQuestion(note, params);
        verify(openAiApi).createChatCompletion(captor.capture());
        assertThat(captor.getValue().getMessages().get(2).getContent())
            .contains(" \"details_to_complete\" : \"What goes up,\"");
      }

      @Test
      void askCompletionAndUseStopResponse() {
        AiCompletionResponse aiCompletionResponse =
            controller.answerCompletionClarifyingQuestion(note, params);
        assertEquals("blue planet", aiCompletionResponse.getMoreCompleteContent());
        assertEquals("stop", aiCompletionResponse.getFinishReason());
      }

      @Test
      void itMustPassTheThreadIdBack() {
        AiCompletionResponse aiCompletionResponse =
            controller.answerCompletionClarifyingQuestion(note, params);
        assertEquals("any-thread-id", aiCompletionResponse.getThreadId());
      }
    }
  }

  @Nested
  class AskEngagingStory {
    Note aNote;

    @BeforeEach
    void setup() {
      aNote = makeMe.aNote("sanskrit").creatorAndOwner(currentUser).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () ->
              new RestAiController(
                      openAiApi,
                      makeMe.modelFactoryService,
                      makeMe.aNullUserModel(),
                      testabilitySettings)
                  .generateImage("create an image"));
    }

    @Test
    void askEngagingStoryWithRightPrompt() {
      when(openAiApi.createImage(
              argThat(
                  request -> {
                    assertEquals("create an image", request.getPrompt());
                    return true;
                  })))
          .thenReturn(buildImageResult("This is an engaging story."));
      controller.generateImage("create an image");
    }

    @Test
    void generateImage() {
      when(openAiApi.createImage(Mockito.any()))
          .thenReturn(buildImageResult("this is supposed to be a base64 image"));
      final String aiImage = controller.generateImage("create an image").b64encoded();
      assertEquals("this is supposed to be a base64 image", aiImage);
    }
  }

  @Nested
  class GetModelVersions {

    @Test
    void shouldGetModelVersionsCorrectly() {
      List<Model> fakeModels = new ArrayList<>();
      Model model1 = new Model();
      model1.setId("gpt-4");
      fakeModels.add(model1);
      Model model2 = new Model();
      model2.setId("any-model");
      fakeModels.add(model2);
      OpenAiResponse<Model> fakeResponse = new OpenAiResponse<>();
      fakeResponse.setData(fakeModels);

      when(openAiApi.listModels()).thenReturn(Single.just(fakeResponse));
      assertThat(controller.getAvailableGptModels()).contains("gpt-4");
    }
  }

  @Nested
  class recreateAllAssistants {
    @Test
    void authentication() {
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.recreateAllAssistants());
    }

    @Nested
    class asAdmin {
      @BeforeEach
      void setup() {
        currentUser = makeMe.anAdmin().toModelPlease();
        controller =
            new RestAiController(
                openAiApi, makeMe.modelFactoryService, currentUser, testabilitySettings);
        Assistant assistantToReturn = new Assistant();
        assistantToReturn.setId("1234");
        when(openAiApi.createAssistant(ArgumentMatchers.any()))
            .thenReturn(Single.just(assistantToReturn));
      }

      @Test
      void callingTheApi() throws UnexpectedNoAccessRightException {
        Map<String, String> result = controller.recreateAllAssistants();
        assertThat(result.get("note details completion")).isEqualTo("1234");
      }

      @Test
      void resultMustBePersisted() throws UnexpectedNoAccessRightException {
        controller.recreateAllAssistants();
        GlobalSettingsService globalSettingsService =
            new GlobalSettingsService(makeMe.modelFactoryService);
        assertThat(globalSettingsService.getNoteCompletionAssistantId().getValue())
            .isEqualTo("1234");
      }
    }
  }

  private Single<ImageResult> buildImageResult(String s) {
    ImageResult result = new ImageResult();
    Image image = new Image();
    image.setB64Json(s);
    result.setData(List.of(image));
    return Single.just(result);
  }
}
