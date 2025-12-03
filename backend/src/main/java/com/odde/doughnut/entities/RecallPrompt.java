package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
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
public class RecallPrompt extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "memory_tracker_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private MemoryTracker memoryTracker;

  @ManyToOne
  @JoinColumn(name = "answerable_mcq_id", referencedColumnName = "id")
  @JsonIgnore
  private AnswerableMCQ answerableMCQ;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_type")
  @NotNull
  @JsonIgnore
  private QuestionType questionType;

  public Notebook getNotebook() {
    if (getAnswerableMCQ() == null) {
      return null;
    }
    return getAnswerableMCQ().getPredefinedQuestion().getNote().getNotebook();
  }

  @JsonIgnore
  public AnsweredQuestion getAnsweredQuestion() {
    if (getAnswerableMCQ() == null || getAnswerableMCQ().getAnswer() == null) {
      return null;
    }

    Answer answer = getAnswerableMCQ().getAnswer();
    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answer;
    answerResult.note = getAnswerableMCQ().getPredefinedQuestion().getNote();
    answerResult.predefinedQuestion = getAnswerableMCQ().getPredefinedQuestion();
    answerResult.answerDisplay = answer.getAnswerDisplay(this.getMultipleChoicesQuestion());
    answerResult.recallPromptId = id;
    answerResult.memoryTrackerId = memoryTracker.getId();
    return answerResult;
  }

  @JsonIgnore
  public String getQuestionDetails() {
    if (getAnswerableMCQ() == null) {
      return "{}";
    }
    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    ObjectNode questionDetails = mapper.createObjectNode();
    MCQWithAnswer mcqWithAnswer = getAnswerableMCQ().getPredefinedQuestion().getMcqWithAnswer();
    questionDetails.set(
        "originalQuestionDefinition", mapper.convertValue(mcqWithAnswer, ObjectNode.class));
    if (getAnswerableMCQ().getAnswer() != null) {
      Answer answer = getAnswerableMCQ().getAnswer();
      questionDetails.put("userAnswer", answer.getChoiceIndex());
      questionDetails.put("userAnswerWasMarkedAs", answer.getCorrect() ? "correct" : "incorrect");
    }
    return questionDetails.toPrettyString();
  }

  @JsonProperty
  public Timestamp getAnswerTime() {
    if (getAnswerableMCQ() != null && getAnswerableMCQ().getAnswer() != null) {
      return getAnswerableMCQ().getAnswer().getCreatedAt();
    }
    return null;
  }

  @JsonProperty
  public Note getNote() {
    if (getAnswerableMCQ() == null) {
      return null;
    }
    return getAnswerableMCQ().getPredefinedQuestion().getNote();
  }

  @JsonProperty("predefinedQuestion")
  public PredefinedQuestion getPredefinedQuestionExposed() {
    if (getAnswerableMCQ() == null) {
      return null;
    }
    return getAnswerableMCQ().getPredefinedQuestion();
  }

  @JsonProperty("answer")
  public Answer getAnswerExposed() {
    return getAnswerableMCQ() != null ? getAnswerableMCQ().getAnswer() : null;
  }

  @JsonProperty
  public Timestamp getQuestionGeneratedTime() {
    if (getAnswerableMCQ() == null) {
      return null;
    }
    return getAnswerableMCQ().getPredefinedQuestion().getCreatedAt();
  }

  @JsonProperty
  public Boolean getIsContested() {
    if (getAnswerableMCQ() == null) {
      return null;
    }
    return getAnswerableMCQ().getPredefinedQuestion().isContested();
  }

  @JsonProperty
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    if (getAnswerableMCQ() == null) {
      return null;
    }
    return getAnswerableMCQ().getMultipleChoicesQuestion();
  }

  @JsonIgnore
  public PredefinedQuestion getPredefinedQuestion() {
    return getAnswerableMCQ() != null ? getAnswerableMCQ().getPredefinedQuestion() : null;
  }
}
