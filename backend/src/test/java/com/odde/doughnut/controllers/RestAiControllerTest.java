package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.ToolCallResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.AiAdvisorWithStorageService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestAiControllerTest {
  RestAiController controller;
  UserModel currentUser;
  AssessmentQuestionInstance assessmentQuestionInstance;

  Note note;
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  TestabilitySettings testabilitySettings = new TestabilitySettings();
  AiAdvisorService aiAdvisorService;
  AiAdvisorWithStorageService aiAdvisorWithStorageService;

  @BeforeEach
  void Setup() {
    aiAdvisorService = new AiAdvisorService(openAiApi);
    aiAdvisorWithStorageService =
        new AiAdvisorWithStorageService(aiAdvisorService, makeMe.modelFactoryService);
    currentUser = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().please();
    controller =
        new RestAiController(aiAdvisorWithStorageService, currentUser, testabilitySettings);
  }

  @Nested
  class GenerateImage {
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
                      aiAdvisorWithStorageService,
                      makeMe.aNullUserModelPlease(),
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

    private Single<ImageResult> buildImageResult(String s) {
      ImageResult result = new ImageResult();
      Image image = new Image();
      image.setB64Json(s);
      result.setData(List.of(image));
      return Single.just(result);
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
  class SubmitToolCallResult {
    @Test
    void shouldSubmitToolOutputSuccessfully() throws JsonProcessingException {
      String threadId = "thread-123";
      String runId = "run-123";
      String toolCallId = "call-456";

      when(openAiApi.submitToolOutputs(
              eq(threadId),
              eq(runId),
              argThat(
                  request -> {
                    assertEquals(1, request.getToolOutputs().size());
                    assertEquals(toolCallId, request.getToolOutputs().get(0).getToolCallId());
                    return true;
                  })))
          .thenReturn(Single.just(new Run()));

      ToolCallResult result = new ToolCallResult();
      result.status = "accepted";

      controller.submitToolCallResult(threadId, runId, toolCallId, result);

      verify(openAiApi).submitToolOutputs(eq(threadId), eq(runId), any());
    }
  }

  @Nested
  class CancelRun {
    @Test
    void shouldCancelRunSuccessfully() {
      String threadId = "thread-123";
      String runId = "run-123";

      when(openAiApi.cancelRun(threadId, runId)).thenReturn(Single.just(new Run()));

      controller.cancelRun(threadId, runId);

      verify(openAiApi).cancelRun(threadId, runId);
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      controller =
          new RestAiController(
              aiAdvisorWithStorageService, makeMe.aNullUserModelPlease(), testabilitySettings);

      assertThrows(
          ResponseStatusException.class, () -> controller.cancelRun("thread-123", "run-123"));
    }
  }
}
