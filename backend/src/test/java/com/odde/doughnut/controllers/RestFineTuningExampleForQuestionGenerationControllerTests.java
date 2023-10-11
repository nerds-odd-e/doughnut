package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.FineTuningExampleForQuestionGeneration;
import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.controllers.json.SimplifiedOpenAIChatMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class RestFineTuningExampleForQuestionGenerationControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  RestFineTuningDataController controller;
  private UserModel userModel;

  @BeforeEach
  void setup() {
    userModel = makeMe.anAdmin().toModelPlease();
    controller = new RestFineTuningDataController(modelFactoryService, userModel);
  }

  @Nested
  class getGoodFineTuningExampleForQuestionGeneration {
    @Test
    void itShouldNotAllowNonMemberToSeeTrainingData() {
      controller = new RestFineTuningDataController(modelFactoryService, makeMe.aNullUserModel());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAllPositiveQuestionGenerationFineTuningExamples());
    }

    @Test
    void shouldReturnNoTrainingDataIfNoMarkedQuestion() throws UnexpectedNoAccessRightException {
      List<FineTuningExampleForQuestionGeneration> goodTrainingData =
          controller.getAllPositiveQuestionGenerationFineTuningExamples();
      assertTrue(goodTrainingData.isEmpty());
    }

    @Test
    void shouldReturnGoodTrainingDataIfHavingReadingAuth_whenCallGetGoodTrainingData()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().title("Test Topic").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).please();
      List<FineTuningExampleForQuestionGeneration> goodFineTuningExampleForQuestionGenerationList =
          controller.getAllPositiveQuestionGenerationFineTuningExamples();
      assertEquals(1, goodFineTuningExampleForQuestionGenerationList.size());
      List<SimplifiedOpenAIChatMessage> goodTrainingData =
          goodFineTuningExampleForQuestionGenerationList.get(0).getMessages();
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
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the raw Json question").please())
          .please();
      List<FineTuningExampleForQuestionGeneration> goodFineTuningExampleForQuestionGenerationList =
          controller.getAllPositiveQuestionGenerationFineTuningExamples();
      List<SimplifiedOpenAIChatMessage> goodTrainingData =
          goodFineTuningExampleForQuestionGenerationList.get(0).getMessages();
      assertThat(
          goodTrainingData.get(2).getContent(), containsString("This is the raw Json question"));
    }
  }

  @Nested
  class SuggestedQuestions {
    @Test
    void shouldThrowExceptionIfUserDoesNotHaveReadingAuth_whenCallGetGoodTrainingData() {
      controller = new RestFineTuningDataController(modelFactoryService, makeMe.aNullUserModel());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAllSuggestedQuestions());
    }

    @Test
    void shouldReturnAllSuggestedQuestions() throws UnexpectedNoAccessRightException {
      makeMe.aQuestionSuggestionForFineTunining().please();
      List<SuggestedQuestionForFineTuning> suggestions =
          controller.getAllSuggestedQuestions();
      assertEquals(1, suggestions.size());
    }

    @Test
    void shouldReturnAllPositiveSuggestedQuestions() throws UnexpectedNoAccessRightException {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().negative().please();
      List<SuggestedQuestionForFineTuning> suggestions =
          controller.getAllSuggestedQuestions();
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
      suggest = new QuestionSuggestionParams("new comment", makeMe.aMCQWithAnswer().please());
    }

    @Test
    void itShouldNotAllowNonAdmin() {
      controller =
          new RestFineTuningDataController(modelFactoryService, makeMe.aUser().toModelPlease());
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
  }
}
