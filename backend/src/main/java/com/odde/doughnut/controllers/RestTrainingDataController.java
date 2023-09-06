package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.GoodTrainingData;
import com.odde.doughnut.entities.json.TrainingDataMessage;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/gettrainingdata")
class RestTrainingDataController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestTrainingDataController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/goodtrainingdata")
  public String getGoodTrainingData() {
    currentUser.assertLoggedIn();
    List<GoodTrainingData> goodTrainingDataList = new ArrayList<>();
    goodTrainingDataList.add(getTrainingData());
    goodTrainingDataList.add(getTrainingData());
    return createJSONLFromList(goodTrainingDataList);
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

  @NotNull
  private static TrainingDataMessage getTrainingDataMessage(String role, String content) {
    TrainingDataMessage tdMsg = new TrainingDataMessage();
    tdMsg.setRole(role);
    tdMsg.setContent(content);
    return tdMsg;
  }

  private static String createJSONLFromList(List<GoodTrainingData> goodTrainingDataList) {
    ObjectMapper mapper = new ObjectMapper();
    StringBuilder jsonStrBuilder = new StringBuilder();
    for (GoodTrainingData goodTrainingData : goodTrainingDataList) {
      try {
        jsonStrBuilder.append(mapper.writeValueAsString(goodTrainingData));
        jsonStrBuilder.append("\n");
      } catch (JsonProcessingException e) {
        return e.getMessage();
      }
    }
    return jsonStrBuilder.toString();
  }
}
