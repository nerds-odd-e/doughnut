package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "recall_prompt")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({
  "id",
  "multipleChoicesQuestion",
  "notebook",
  "note",
  "questionGeneratedTime",
  "isContested",
  "answerTime",
  "predefinedQuestion",
  "answer"
})
public class RecallPrompt extends AnswerableQuestionInstance {
  @ManyToOne
  @JoinColumn(name = "memory_tracker_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private MemoryTracker memoryTracker;

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
    answerResult.answerDisplay = answer.getAnswerDisplay(this.getMultipleChoicesQuestion());
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
    if (getAnswer() != null) {
      questionDetails.put("userAnswer", getAnswer().getChoiceIndex());
      questionDetails.put(
          "userAnswerWasMarkedAs", getAnswer().getCorrect() ? "correct" : "incorrect");
    }
    return questionDetails.toPrettyString();
  }

  @JsonProperty
  public Timestamp getAnswerTime() {
    Answer answer = getAnswer();
    if (answer != null) {
      return answer.getCreatedAt();
    }
    return null;
  }

  @JsonProperty
  public Note getNote() {
    return super.getPredefinedQuestion().getNote();
  }

  @JsonProperty("predefinedQuestion")
  public PredefinedQuestion getPredefinedQuestionExposed() {
    return super.getPredefinedQuestion();
  }

  @JsonProperty("answer")
  public Answer getAnswerExposed() {
    return super.getAnswer();
  }

  @JsonProperty
  public Timestamp getQuestionGeneratedTime() {
    return super.getPredefinedQuestion().getCreatedAt();
  }

  @JsonProperty
  public Boolean getIsContested() {
    return super.getPredefinedQuestion().isContested();
  }
}
