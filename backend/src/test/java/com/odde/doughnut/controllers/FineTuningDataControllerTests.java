package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.repositories.QuestionSuggestionForFineTuningRepository;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.openai.client.OpenAIClient;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.finetuning.jobs.FineTuningJob;
import com.openai.models.finetuning.jobs.JobCreateParams;
import com.openai.services.blocking.FileService;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class FineTuningDataControllerTests extends ControllerTestBase {
  @Autowired QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;
  @Autowired FineTuningDataController controller;

  @MockitoBean(name = "testableOpenAiApi")
  OpenAiApi openAiApi;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.anAdmin().please());
  }

  @Nested
  class getGoodOpenAIChatGPTFineTuningExample {
    @Test
    void authentication() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.uploadAndTriggerFineTuning());
    }

    @Test
    void shouldSuccessWhen10FeedbackAndUploadFileAndTriggerFineTune() {
      mockFeedback(11);
      FileService fileService = Mockito.mock(FileService.class);
      when(officialClient.files()).thenReturn(fileService);
      FileObject fakeFileResponse =
          FileObject.builder()
              .id("TestFileId")
              .bytes(0L)
              .createdAt(System.currentTimeMillis() / 1000)
              .filename("test.jsonl")
              .purpose(com.openai.models.files.FileObject.Purpose.FINE_TUNE)
              .status(com.openai.models.files.FileObject.Status.PROCESSED)
              .build();
      when(fileService.create(any(FileCreateParams.class))).thenReturn(fakeFileResponse);
      // Mock official SDK fine-tuning API
      var fineTuningService =
          Mockito.mock(
              com.openai.services.blocking.FineTuningService.class, Mockito.RETURNS_DEEP_STUBS);
      when(officialClient.fineTuning()).thenReturn(fineTuningService);
      FineTuningJob fakeFineTuningResponse =
          Mockito.mock(FineTuningJob.class, Mockito.RETURNS_DEEP_STUBS);
      when(fakeFineTuningResponse.status()).thenReturn(FineTuningJob.Status.SUCCEEDED);
      when(fakeFineTuningResponse.fineTunedModel())
          .thenReturn(java.util.Optional.of("ft:gpt-3.5-turbo-1106:test"));
      when(fineTuningService.jobs().create(any(JobCreateParams.class)))
          .thenReturn(fakeFineTuningResponse);
      assertDoesNotThrow(() -> controller.uploadAndTriggerFineTuning());
    }

    @Test
    void shouldFailWhenNoFeedbackAndTriggerFineTune() {
      var result =
          assertThrows(
              OpenAIServiceErrorException.class, () -> controller.uploadAndTriggerFineTuning());
      assertEquals(result.getMessage(), "Positive feedback cannot be less than 10.");
    }

    @Test
    void whenOpenAiServiceFailShouldGetFailMessageAndTriggerFineTune() {
      mockFeedback(10);
      FileService fileService = Mockito.mock(FileService.class);
      when(officialClient.files()).thenReturn(fileService);
      when(fileService.create(any(FileCreateParams.class))).thenThrow(new RuntimeException());
      var result =
          assertThrows(
              OpenAIServiceErrorException.class, () -> controller.uploadAndTriggerFineTuning());
      assertEquals(result.getMessage(), "Upload failed.");
    }

    private void mockFeedback(int count) {
      for (int i = 0; i < count; i++) {
        makeMe.aQuestionSuggestionForFineTunining().positive().please();
      }
    }
  }

  @Nested
  class SuggestedQuestions {
    @Test
    void shouldThrowExceptionIfUserDoesNotHaveReadingAuth_whenCallGetGoodTrainingData() {
      currentUser.setUser(null);
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getAllSuggestedQuestions());
    }

    @Test
    void shouldReturnAllSuggestedQuestions() throws UnexpectedNoAccessRightException {
      makeMe.aQuestionSuggestionForFineTunining().please();
      List<SuggestedQuestionForFineTuning> suggestions = controller.getAllSuggestedQuestions();
      assertEquals(1, suggestions.size());
    }

    @Test
    void shouldReturnAllPositiveSuggestedQuestions() throws UnexpectedNoAccessRightException {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().negative().please();
      List<SuggestedQuestionForFineTuning> suggestions = controller.getAllSuggestedQuestions();
      assertEquals(2, suggestions.size());
    }
  }

  @Nested
  class UpdateSuggestedQuestionForFineTuning {
    SuggestedQuestionForFineTuning suggested;
    private QuestionSuggestionParams suggest;

    @BeforeEach
    void setup() {
      suggested = makeMe.aQuestionSuggestionForFineTunining().please();
      suggest =
          new QuestionSuggestionParams(
              "new comment", makeMe.aMCQWithAnswer().please(), "note content", false, "0,1");
    }

    @Test
    void itShouldNotAllowNonAdmin() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateSuggestedQuestionForFineTuning(suggested, suggest));
    }

    @Test
    void onlyUpdate() throws UnexpectedNoAccessRightException {
      long oldCount = questionSuggestionForFineTuningRepository.count();
      controller.updateSuggestedQuestionForFineTuning(suggested, suggest);
      assertThat(questionSuggestionForFineTuningRepository.count(), equalTo(oldCount));
    }

    @Test
    void updateTheFields() throws UnexpectedNoAccessRightException {
      SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
          controller.updateSuggestedQuestionForFineTuning(suggested, suggest);
      assertThat(suggestedQuestionForFineTuning.getComment(), equalTo("new comment"));
      assertThat(suggestedQuestionForFineTuning.getRealCorrectAnswers(), equalTo("0,1"));
    }
  }

  @Nested
  class dupplicate {
    SuggestedQuestionForFineTuning suggested;

    @BeforeEach
    void setup() {
      suggested = makeMe.aQuestionSuggestionForFineTunining().please();
    }

    @Test
    void itShouldNotAllowNonAdmin() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.duplicate(suggested));
    }

    @Test
    void createANewIdenticalSuggestion() throws UnexpectedNoAccessRightException {
      var newSuggestion = controller.duplicate(suggested);
      assertThat(newSuggestion.getId(), notNullValue());
      assertThat(newSuggestion.getId(), not(equalTo(suggested.getId())));
    }
  }

  @Nested
  class delete {
    SuggestedQuestionForFineTuning suggested;

    @BeforeEach
    void setup() {
      suggested = makeMe.aQuestionSuggestionForFineTunining().please();
    }

    @Test
    void itShouldNotAllowNonAdmin() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.delete(suggested));
    }

    @Test
    void createANewIdenticalSuggestion() throws UnexpectedNoAccessRightException {
      var beforeCount = questionSuggestionForFineTuningRepository.count();
      controller.delete(suggested);
      assertThat(questionSuggestionForFineTuningRepository.count(), equalTo(beforeCount - 1));
    }
  }
}
