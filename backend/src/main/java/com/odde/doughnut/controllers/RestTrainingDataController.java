package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.GoodTrainingData;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
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
  public List<GoodTrainingData> getGoodTrainingData() {
    currentUser.assertLoggedIn();
    List<GoodTrainingData> goodTrainingDataList = new ArrayList<>();
    modelFactoryService
        .markedQuestionRepository
        .findAll()
        .forEach(
            markedQuestion -> {
              goodTrainingDataList.add(new GoodTrainingData());
            });
    return goodTrainingDataList;
  }
}
