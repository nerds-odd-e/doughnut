package com.odde.doughnut.entities.json;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class GoodTrainingData {
  @Setter @Getter List<TrainingDataMessage> messages;

  public void addTrainingDataMessage(TrainingDataMessage message) {
    if (messages == null) messages = new ArrayList<>();
    messages.add(message);
  }

  public void addTrainingDataMessages(List<TrainingDataMessage> trainingDataMessageLIst) {
    if (messages == null) messages = new ArrayList<>();
    messages.addAll(trainingDataMessageLIst);
  }
}
