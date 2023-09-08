package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.MarkedQuestion;
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
    void shouldReturnGoodTrainingDataIfHavingReadingAuth() {
      Note note = makeMe.aNote().please();
      note.setTopic("Test Topic");
      MarkedQuestion markedQuestion = makeMe.aMarkedQuestion().ofNote(note).please();
      markedQuestion.setIsGood(true);
      modelFactoryService.markedQuestionRepository.save(markedQuestion);
      List<TrainingData> goodTrainingDataList = controller.getGoodTrainingData();
      assertEquals(1, goodTrainingDataList.size());
      List<TrainingDataMessage> GoodTrainingData = goodTrainingDataList.get(0).getMessages();
      assertTrue(GoodTrainingData.get(0).getContent().contains(note.getTopic()));
      assertTrue(
          GoodTrainingData.get(1)
              .getContent()
              .contains(
                  " assume the role of a Memory Assistant, which involves helping me review"));
    }

    @Test
    void shouldNotReturnBadTrainingDataIfHavingReadingAuth() {
      Note note = makeMe.aNote().please();
      note.setTopic("Test Topic");
      MarkedQuestion markedQuestion = makeMe.aMarkedQuestion().ofNote(note).please();
      markedQuestion.setIsGood(false);
      markedQuestion.setComment("This is a bad comment!");
      modelFactoryService.markedQuestionRepository.save(markedQuestion);
      List<TrainingData> goodTrainingDataList = controller.getGoodTrainingData();
      assertEquals(0, goodTrainingDataList.size());
    }
  }

  @Test
  void shouldThrowExceptionIfUserDoesNotHaveReadingAuth() {
    userModel = modelFactoryService.toUserModel(null);
    controller = new RestTrainingDataController(modelFactoryService, userModel);
    assertThrows(ResponseStatusException.class, () -> controller.getGoodTrainingData());
  }
}
