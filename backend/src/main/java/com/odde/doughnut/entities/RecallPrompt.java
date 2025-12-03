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
  "answer",
  "questionType"
})
public class RecallPrompt extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "memory_tracker_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private MemoryTracker memoryTracker;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "answerable_mcq_id", referencedColumnName = "id")
  @JsonIgnore
  private AnswerableMCQ answerableMCQ;

  @Column(name = "question_type")
  @Enumerated(EnumType.STRING)
  @NotNull
  private QuestionType questionType;

  public Notebook getNotebook() {
    return getPredefinedQuestion().getNote().getNotebook();
  }

  @JsonIgnore
  public AnsweredQuestion getAnsweredQuestion() {
    if (answerableMCQ == null || answerableMCQ.getAnswer() == null) {
      return null;
    }

    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answerableMCQ.getAnswer();
    answerResult.note = getPredefinedQuestion().getNote();
    answerResult.predefinedQuestion = getPredefinedQuestion();
    answerResult.answerDisplay =
        answerableMCQ.getAnswer().getAnswerDisplay(this.getMultipleChoicesQuestion());
    answerResult.recallPromptId = id;
    answerResult.memoryTrackerId = memoryTracker.getId();
    return answerResult;
  }

  @JsonIgnore
  public String getQuestionDetails() {
    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    ObjectNode questionDetails = mapper.createObjectNode();
    MCQWithAnswer mcqWithAnswer = getPredefinedQuestion().getMcqWithAnswer();
    questionDetails.set(
        "originalQuestionDefinition", mapper.convertValue(mcqWithAnswer, ObjectNode.class));
    if (answerableMCQ != null && answerableMCQ.getAnswer() != null) {
      questionDetails.put("userAnswer", answerableMCQ.getAnswer().getChoiceIndex());
      questionDetails.put(
          "userAnswerWasMarkedAs",
          answerableMCQ.getAnswer().getCorrect() ? "correct" : "incorrect");
    }
    return questionDetails.toPrettyString();
  }

  @JsonProperty
  public Timestamp getAnswerTime() {
    if (answerableMCQ != null && answerableMCQ.getAnswer() != null) {
      return answerableMCQ.getAnswer().getCreatedAt();
    }
    return null;
  }

  @JsonProperty
  public Note getNote() {
    return getPredefinedQuestion().getNote();
  }

  @JsonProperty("predefinedQuestion")
  public PredefinedQuestion getPredefinedQuestionExposed() {
    return getPredefinedQuestion();
  }

  @JsonIgnore
  public PredefinedQuestion getPredefinedQuestion() {
    if (answerableMCQ == null) {
      return null;
    }
    return answerableMCQ.getPredefinedQuestion();
  }

  @JsonProperty("answer")
  public Answer getAnswerExposed() {
    if (answerableMCQ == null) {
      return null;
    }
    return answerableMCQ.getAnswer();
  }

  @JsonIgnore
  public Answer getAnswer() {
    if (answerableMCQ == null) {
      return null;
    }
    return answerableMCQ.getAnswer();
  }

  @JsonProperty
  public Timestamp getQuestionGeneratedTime() {
    if (answerableMCQ == null) {
      return null;
    }
    return getPredefinedQuestion().getCreatedAt();
  }

  @JsonProperty
  public Boolean getIsContested() {
    if (answerableMCQ == null) {
      return null;
    }
    return getPredefinedQuestion().isContested();
  }

  @JsonProperty
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    if (answerableMCQ == null) {
      return null;
    }
    return answerableMCQ.getMultipleChoicesQuestion();
  }

  public enum QuestionType {
    MCQ
  }
}
