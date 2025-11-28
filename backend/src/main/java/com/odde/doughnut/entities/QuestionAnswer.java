package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.exceptions.QuestionAnswerException;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "recall_prompt")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "notebook"})
public class QuestionAnswer extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private PredefinedQuestion predefinedQuestion;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "quiz_answer_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private Answer answer;

  public Notebook getNotebook() {
    return getPredefinedQuestion().getNote().getNotebook();
  }

  public Integer getPredefinedQuestionId() {
    return getPredefinedQuestion() != null ? getPredefinedQuestion().getId() : null;
  }

  @JsonIgnore
  public AnsweredQuestion getAnsweredQuestion() {
    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answer;
    answerResult.note = getPredefinedQuestion().getNote();
    answerResult.predefinedQuestion = getPredefinedQuestion();
    answerResult.answerDisplay =
        answer.getAnswerDisplay(getPredefinedQuestion().getMultipleChoicesQuestion());
    answerResult.recallPromptId = id;
    return answerResult;
  }

  @JsonIgnore
  public String getQuestionDetails() {
    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    ObjectNode questionDetails = mapper.createObjectNode();
    MCQWithAnswer mcqWithAnswer = getPredefinedQuestion().getMcqWithAnswer();
    questionDetails.set(
        "originalQuestionDefinition", mapper.convertValue(mcqWithAnswer, ObjectNode.class));
    questionDetails.put("userAnswer", getAnswer().getChoiceIndex());
    questionDetails.put(
        "userAnswerWasMarkedAs", getAnswer().getCorrect() ? "correct" : "incorrect");
    return questionDetails.toPrettyString();
  }

  public Answer buildAnswer(AnswerDTO answerDTO) {
    if (getAnswer() != null) {
      throw new QuestionAnswerException("The question is already answered");
    }
    this.answer = new Answer();
    this.answer.choiceIndex = answerDTO.getChoiceIndex();
    this.answer.setCorrect(getPredefinedQuestion().checkAnswer(answerDTO));
    return answer;
  }
}
