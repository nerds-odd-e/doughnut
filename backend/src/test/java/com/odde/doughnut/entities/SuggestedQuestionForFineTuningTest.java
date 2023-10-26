package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuggestedQuestionForFineTuningTest {
  MakeMeWithoutDB makeMe = new MakeMeWithoutDB();
  SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
      new SuggestedQuestionForFineTuning();
  MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();

  @BeforeEach
  void setup() {
    suggestedQuestionForFineTuning.setPreservedNoteContent("note content");
    suggestedQuestionForFineTuning.preserveQuestion(mcqWithAnswer);
    suggestedQuestionForFineTuning.setComment("a comment");
    suggestedQuestionForFineTuning.setRealCorrectAnswers("1,2");
  }

  @Test
  void testFirstPart() {
    OpenAIChatGPTFineTuningExample questionEvaluationFineTuningData =
        suggestedQuestionForFineTuning.toQuestionEvaluationFineTuningData();
    List<ChatMessage> goodTrainingData = questionEvaluationFineTuningData.getMessages();
    assertThat(goodTrainingData.get(0).getContent(), containsString("note content"));
    assertThat(goodTrainingData.get(1).getContent(), containsString(mcqWithAnswer.stem));
    assertThat(goodTrainingData.get(2).getContent(), nullValue());
    assertThat(goodTrainingData.get(2).getFunctionCall().getName(), equalTo("evaluate_question"));
  }

  @Test
  void testFunctionCall() throws JsonProcessingException {
    OpenAIChatGPTFineTuningExample questionEvaluationFineTuningData =
        suggestedQuestionForFineTuning.toQuestionEvaluationFineTuningData();
    List<ChatMessage> goodTrainingData = questionEvaluationFineTuningData.getMessages();
    QuestionEvaluation questionEvaluation =
        new ObjectMapper()
            .treeToValue(
                goodTrainingData.get(2).getFunctionCall().getArguments(), QuestionEvaluation.class);
    assertThat(questionEvaluation.comment, equalTo("a comment"));
    assertThat(questionEvaluation.feasibleQuestion, equalTo(false));
    assertThat(questionEvaluation.correctChoices, equalTo(new int[] {1, 2}));
  }

  @Test
  void testFunctionCallOfPositiveFeedback() throws JsonProcessingException {
    suggestedQuestionForFineTuning.setPositiveFeedback(true);
    OpenAIChatGPTFineTuningExample questionEvaluationFineTuningData =
        suggestedQuestionForFineTuning.toQuestionEvaluationFineTuningData();
    List<ChatMessage> goodTrainingData = questionEvaluationFineTuningData.getMessages();
    QuestionEvaluation questionEvaluation =
        new ObjectMapper()
            .treeToValue(
                goodTrainingData.get(2).getFunctionCall().getArguments(), QuestionEvaluation.class);
    assertThat(questionEvaluation.comment, equalTo("a comment"));
    assertThat(questionEvaluation.feasibleQuestion, equalTo(true));
    assertThat(
        questionEvaluation.correctChoices, equalTo(new int[] {mcqWithAnswer.correctChoiceIndex}));
  }
}
