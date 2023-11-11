package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.file.File;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import io.reactivex.Single;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class RestOpenAIChatGPTFineTuningExampleControllerTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  RestFineTuningDataController controller;
  @Mock private OpenAiApi openAiApi;

  @BeforeEach
  void setup() {
    controller =
        new RestFineTuningDataController(
            modelFactoryService, makeMe.anAdmin().toModelPlease(), openAiApi);
  }

  @Nested
  class getGoodOpenAIChatGPTFineTuningExample {

    @Test
    void shouldSuccessWhen10FeedbackAndUploadFileAndTriggerFineTune() {
      mockFeedback(11);
      File fakeResponse = new File();
      fakeResponse.setId("TestFileId");
      FineTuningJob fakeFineTuningResponse = new FineTuningJob();
      fakeFineTuningResponse.setStatus("success");
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenReturn(Single.just(fakeResponse));
      when(openAiApi.createFineTuningJob(any())).thenReturn(Single.just(fakeFineTuningResponse));
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
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenThrow(new RuntimeException());
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
      controller =
          new RestFineTuningDataController(modelFactoryService, makeMe.aNullUserModel(), openAiApi);
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
      controller =
          new RestFineTuningDataController(
              modelFactoryService, makeMe.aUser().toModelPlease(), openAiApi);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateSuggestedQuestionForFineTuning(suggested, suggest));
    }

    @Test
    void onlyUpdate() throws UnexpectedNoAccessRightException {
      long oldCount = modelFactoryService.questionSuggestionForFineTuningRepository.count();
      controller.updateSuggestedQuestionForFineTuning(suggested, suggest);
      assertThat(
          modelFactoryService.questionSuggestionForFineTuningRepository.count(), equalTo(oldCount));
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
      controller =
          new RestFineTuningDataController(
              modelFactoryService, makeMe.aUser().toModelPlease(), openAiApi);
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
      controller =
          new RestFineTuningDataController(
              modelFactoryService, makeMe.aUser().toModelPlease(), openAiApi);
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.delete(suggested));
    }

    @Test
    void createANewIdenticalSuggestion() throws UnexpectedNoAccessRightException {
      var beforeCount = modelFactoryService.questionSuggestionForFineTuningRepository.count();
      controller.delete(suggested);
      assertThat(
          modelFactoryService.questionSuggestionForFineTuningRepository.count(),
          equalTo(beforeCount - 1));
    }
  }
}
