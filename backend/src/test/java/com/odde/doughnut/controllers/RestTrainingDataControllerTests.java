package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.GoodTrainingData;
import com.odde.doughnut.entities.json.TrainingDataMessage;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
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
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    UserModel userModel = makeMe.aUser().toModelPlease();

    controller =
        new RestTrainingDataController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class getGoodTrainingData {
    @Test
    void itShouldNotAllowNonMemberToSeeTrainingData() {
      controller =
          new RestTrainingDataController(
              modelFactoryService, makeMe.aNullUserModel(), testabilitySettings);
      assertThrows(ResponseStatusException.class, () -> controller.getGoodTrainingData());
    }

    @Test
    void shouldReturnNoTrainingDataIfNoMarkedQuestion() {
      List<GoodTrainingData> goodTrainingData = controller.getGoodTrainingData();
      assertTrue(goodTrainingData.isEmpty());
    }

    @Test
    void shouldReturnTrainingDataIfHavingReadingAuth() {
      Note note = makeMe.aNote().please();
      note.setTopic("Test Topic");
      MarkedQuestion markedQuestion = makeMe.aMarkedQuestion().ofNote(note).please();
      modelFactoryService.markedQuestionRepository.save(markedQuestion);

      List<GoodTrainingData> goodTrainingDataList = controller.getGoodTrainingData();
      assertEquals(1, goodTrainingDataList.size());
      GoodTrainingData GoodTrainingData = goodTrainingDataList.get(0);
      assertTrue(GoodTrainingData.getMessages().get(0).getContent().contains(note.getTopic()));
      assertTrue(
          GoodTrainingData.getMessages()
              .get(1)
              .getContent()
              .contains(
                  " assume the role of a Memory Assistant, which involves helping me review"));
    }

    private static GoodTrainingData getTrainingData() {
      GoodTrainingData goodTrainingData = new GoodTrainingData();
      goodTrainingData.addTrainingDataMessage(getTrainingDataMessage("system", "System Content"));
      goodTrainingData.addTrainingDataMessage(
          getTrainingDataMessage("user", "Please assume the role of a Memory Assistant."));
      goodTrainingData.addTrainingDataMessage(
          getTrainingDataMessage("assistant", "Test question and answers."));
      return goodTrainingData;
    }

    private static TrainingDataMessage getTrainingDataMessage(String role, String content) {
      TrainingDataMessage tdMsg = new TrainingDataMessage(role, content);
      tdMsg.setRole(role);
      tdMsg.setContent(content);
      return tdMsg;
    }
  }
}
