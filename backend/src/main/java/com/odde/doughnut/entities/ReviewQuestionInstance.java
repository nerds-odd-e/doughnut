package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "review_question_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "bareQuestion", "notebook"})
public class ReviewQuestionInstance extends AnswerableQuestionInstance {
  public Notebook getNotebook() {
    return getPredefinedQuestion().getNote().getNotebook();
  }

  @JsonIgnore
  public AnsweredQuestion getAnsweredQuestion() {
    if (answer == null) {
      return null;
    }

    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answer;
    answerResult.note = getPredefinedQuestion().getNote();
    answerResult.predefinedQuestion = getPredefinedQuestion();
    answerResult.answerDisplay = answer.getAnswerDisplay(this.getBareQuestion());
    answerResult.reviewQuestionInstanceId = id;
    return answerResult;
  }

  @JsonIgnore
  public String getQuestionDetails() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode questionDetails = mapper.createObjectNode();
    questionDetails.put("question", getBareQuestion().toString());
    questionDetails.put("correctAnswerIndex", getPredefinedQuestion().getCorrectAnswerIndex());

    if (getAnswer() != null) {
      questionDetails.put("userAnswer", getAnswer().getChoiceIndex());
      questionDetails.put("isCorrect", getAnswer().getCorrect());
    }
    return questionDetails.toPrettyString();
  }
}
