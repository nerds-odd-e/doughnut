package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.ChatMessageForFineTuning;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMeWithoutDB;
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
    ChatMessageForFineTuning firstMessage = getFirstMessage();
    assertThat(firstMessage.getRole(), equalTo("system"));
    assertThat(firstMessage.getContent(), equalTo("note content"));

    ChatMessageForFineTuning secondMessage = getSecondMessage();
    assertThat(secondMessage.getRole(), equalTo("user"));
    assertThat(
        secondMessage.getContent(),
        containsString("You are an AI assistant evaluating a memory recall question"));
    assertThat(secondMessage.getContent(), containsString("a default question stem"));

    ChatMessageForFineTuning thirdMessage = getThirdMessage();
    assertThat(thirdMessage.getRole(), equalTo("assistant"));
    assertThat(thirdMessage.getContent(), containsString("correctChoices"));
    assertThat(thirdMessage.getContent(), containsString("feasibleQuestion"));
    assertThat(thirdMessage.getContent(), containsString("a comment"));
  }

  @Test
  void testFunctionCall() {
    QuestionEvaluation questionEvaluation = getQuestionEvaluation();
    assertThat(questionEvaluation.improvementAdvices, equalTo("a comment"));
    assertThat(questionEvaluation.feasibleQuestion, equalTo(false));
    assertThat(questionEvaluation.correctChoices, equalTo(new int[] {1, 2}));
  }

  @Test
  void testFunctionCallOfPositiveFeedback() {
    suggestedQuestionForFineTuning.setPositiveFeedback(true);
    QuestionEvaluation questionEvaluation = getQuestionEvaluation();
    assertThat(questionEvaluation.correctChoices, equalTo(new int[] {0}));
  }

  private QuestionEvaluation getQuestionEvaluation() {
    ChatMessageForFineTuning message = getThirdMessage();
    try {
      // First parse the JSON string to JsonNode, then convert to QuestionEvaluation
      return new com.odde.doughnut.configs.ObjectMapperConfig()
          .objectMapper()
          .treeToValue(
              new com.odde.doughnut.configs.ObjectMapperConfig()
                  .objectMapper()
                  .readTree(message.getContent()),
              QuestionEvaluation.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private ChatMessageForFineTuning getFirstMessage() {
    List<ChatMessageForFineTuning> messages =
        suggestedQuestionForFineTuning.toQuestionEvaluationFineTuningData().getMessages();
    return messages.get(0);
  }

  private ChatMessageForFineTuning getSecondMessage() {
    List<ChatMessageForFineTuning> messages =
        suggestedQuestionForFineTuning.toQuestionEvaluationFineTuningData().getMessages();
    return messages.get(1);
  }

  private ChatMessageForFineTuning getThirdMessage() {
    List<ChatMessageForFineTuning> messages =
        suggestedQuestionForFineTuning.toQuestionEvaluationFineTuningData().getMessages();
    return messages.get(2);
  }
}
