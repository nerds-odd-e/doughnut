package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.TrainingData;
import com.odde.doughnut.entities.json.TrainingDataMessage;
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
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class RestTrainingDataControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  RestTrainingDataController controller;
  private UserModel userModel;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestTrainingDataController(modelFactoryService, userModel);
  }

  @Nested
  class getGoodTrainingData {
    @Test
    void itShouldNotAllowNonMemberToSeeTrainingData() {
      controller = new RestTrainingDataController(modelFactoryService, makeMe.aNullUserModel());
      assertThrows(ResponseStatusException.class, () -> controller.getGoodTrainingData());
    }

    @Test
    void shouldReturnNoTrainingDataIfNoMarkedQuestion() {
      List<TrainingData> goodTrainingData = controller.getGoodTrainingData();
      assertTrue(goodTrainingData.isEmpty());
    }

    @Test
    void shouldReturnGoodTrainingDataIfHavingReadingAuth_whenCallGetGoodTrainingData() {
      Note note = makeMe.aNote().title("Test Topic").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).please();
      List<TrainingData> goodTrainingDataList = controller.getGoodTrainingData();
      assertEquals(1, goodTrainingDataList.size());
      List<TrainingDataMessage> goodTrainingData = goodTrainingDataList.get(0).getMessages();
      assertThat(goodTrainingData.get(0).getContent(), containsString("Test Topic"));
      assertThat(
          goodTrainingData.get(1).getContent(),
          containsString("assume the role of a Memory Assistant"));
    }

    @Test
    void shouldIncludeTheQuestion_whenCallGetGoodTrainingData() {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .withRawQuestion("This is the raw Json question")
          .please();
      List<TrainingData> goodTrainingDataList = controller.getGoodTrainingData();
      List<TrainingDataMessage> goodTrainingData = goodTrainingDataList.get(0).getMessages();
      assertThat(
          goodTrainingData.get(2).getContent(), containsString("This is the raw Json question"));
    }
  }

  @Test
  void shouldThrowExceptionIfUserDoesNotHaveReadingAuth_whenCallGetGoodTrainingData() {
    userModel = modelFactoryService.toUserModel(null);
    controller = new RestTrainingDataController(modelFactoryService, userModel);
    assertThrows(ResponseStatusException.class, () -> controller.getGoodTrainingData());
  }
}
