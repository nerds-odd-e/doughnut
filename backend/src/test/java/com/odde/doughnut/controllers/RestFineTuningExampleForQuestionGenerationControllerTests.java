package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.json.FineTuningExampleForQuestionGeneration;
import com.odde.doughnut.controllers.json.SimplifiedOpenAIChatMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

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
          () -> controller.getAllQuestionGenerationFineTuningExamples());
    }

    @Test
    void shouldReturnNoTrainingDataIfNoMarkedQuestion() throws UnexpectedNoAccessRightException {
      List<FineTuningExampleForQuestionGeneration> goodTrainingData =
          controller.getAllQuestionGenerationFineTuningExamples();
      assertTrue(goodTrainingData.isEmpty());
    }

    @Test
    void shouldReturnGoodTrainingDataIfHavingReadingAuth_whenCallGetGoodTrainingData()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().title("Test Topic").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).please();
      List<FineTuningExampleForQuestionGeneration> goodFineTuningExampleForQuestionGenerationList =
          controller.getAllQuestionGenerationFineTuningExamples();
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
          controller.getAllQuestionGenerationFineTuningExamples();
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
          UnexpectedNoAccessRightException.class, () -> controller.getAllSuggestedQuestions());
    }

    @Test
    void shouldReturnAllSuggestedQuestions() throws UnexpectedNoAccessRightException {
      makeMe.aQuestionSuggestionForFineTunining().please();
      List<SuggestedQuestionForFineTuning> suggestions = controller.getAllSuggestedQuestions();
      assertEquals(1, suggestions.size());
    }
  }
}
