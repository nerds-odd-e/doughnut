package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.json.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.file.File;
import io.reactivex.Single;
import java.io.IOException;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
  private UserModel userModel;
  @Mock private OpenAiApi openAiApi;

  @BeforeEach
  void setup() {
    userModel = makeMe.anAdmin().toModelPlease();
    controller = new RestFineTuningDataController(modelFactoryService, userModel, openAiApi);
  }

  @Nested
  class getGoodOpenAIChatGPTFineTuningExample {
    @Test
    void itShouldNotAllowNonMemberToSeeTrainingData() {
      controller =
          new RestFineTuningDataController(modelFactoryService, makeMe.aNullUserModel(), openAiApi);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAllPositiveFeedbackQuestionGenerationFineTuningExamples());
    }

    @Test
    void shouldReturnNoTrainingDataIfNoMarkedQuestion() throws UnexpectedNoAccessRightException {
      List<OpenAIChatGPTFineTuningExample> goodTrainingData =
          controller.getAllPositiveFeedbackQuestionGenerationFineTuningExamples();
      assertTrue(goodTrainingData.isEmpty());
    }

    @Test
    void shouldSuccessWhen10FeedbackAndUploadFile() throws IOException {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      File fakeResponse = Mockito.mock(File.class);
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenReturn(Single.just(fakeResponse));
      var result = controller.uploadFineTuningExamples();
      assertEquals(true, result.isSuccess());
    }

    @Test
    void shouldFailWhenNoFeedback() throws IOException {
      var result = controller.uploadFineTuningExamples();
      assertEquals(false, result.isSuccess());
      assertEquals("Positive feedback cannot be less than 10.", result.getMessage());
    }

    @Test
    void shouldReturnGoodTrainingDataIfHavingReadingAuth_whenCallGetGoodTrainingData()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().title("Test Topic").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).positive().please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          controller.getAllPositiveFeedbackQuestionGenerationFineTuningExamples();
      assertEquals(1, goodOpenAIChatGPTFineTuningExampleList.size());
      List<ChatMessage> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(goodTrainingData.get(0).getContent(), containsString("Test Topic"));
      assertThat(
          goodTrainingData.get(1).getContent(),
          containsString("assume the role of a Memory Assistant"));
    }

    @Test
    void shouldIncludeTheQuestion_whenCallGetGoodTrainingData()
        throws UnexpectedNoAccessRightException {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .positive()
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the raw Json question").please())
          .please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          controller.getAllPositiveFeedbackQuestionGenerationFineTuningExamples();
      List<ChatMessage> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(
          goodTrainingData.get(2).getContent(), containsString("This is the raw Json question"));
    }

    @Test
    void shouldIncludeOnlyPositiveQuestion_whenCallGetGoodTrainingData()
        throws UnexpectedNoAccessRightException {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .negative()
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the negative raw Json question").please())
          .please();

      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          controller.getAllPositiveFeedbackQuestionGenerationFineTuningExamples();

      assertEquals(0, goodOpenAIChatGPTFineTuningExampleList.size());
    }
  }

  @Nested
  class getAllOpenAIChatGPTFineTuningExample {
    @Test
    void shouldIncludeAllFeedbackData_whenCallGetGoodTrainingData()
        throws UnexpectedNoAccessRightException {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().negative().please();
      assertThat(controller.getAllEvaluationExamples(), hasSize(2));
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
