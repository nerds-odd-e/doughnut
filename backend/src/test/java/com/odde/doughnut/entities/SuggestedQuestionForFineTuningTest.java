package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.doughnut.controllers.json.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuggestedQuestionForFineTuningTest {
  MakeMeWithoutDB makeMe = new MakeMeWithoutDB();
  SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
      new SuggestedQuestionForFineTuning();

  @Test
  void test() {
    suggestedQuestionForFineTuning.setPreservedNoteContent("note content");
    MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
    suggestedQuestionForFineTuning.preserveQuestion(mcqWithAnswer);

    OpenAIChatGPTFineTuningExample questionEvaluationFineTuningData =
        suggestedQuestionForFineTuning.toQuestionEvaluationFineTuningData();
    List<ChatMessage> goodTrainingData = questionEvaluationFineTuningData.getMessages();
    assertThat(goodTrainingData.get(0).getContent(), containsString("note content"));
    assertThat(goodTrainingData.get(1).getContent(), containsString(mcqWithAnswer.stem));
  }
}
