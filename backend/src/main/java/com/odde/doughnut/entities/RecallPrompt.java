package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.SpellingQuestion;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recall_prompt")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({
  "id",
  "questionType",
  "multipleChoicesQuestion",
  "notebook",
  "note",
  "questionGeneratedTime",
  "isContested",
  "answerTime",
  "predefinedQuestion",
  "answer",
  "spellingQuestion"
})
public class RecallPrompt extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "memory_tracker_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private MemoryTracker memoryTracker;

  @ManyToOne
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  @JsonIgnore
  private PredefinedQuestion predefinedQuestion;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "quiz_answer_id", referencedColumnName = "id")
  @Getter
  @Setter
  @JsonIgnore
  Answer answer;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_type")
  @NotNull
  @JsonProperty
  private QuestionType questionType;

  @Column(name = "created_at")
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  public Notebook getNotebook() {
    if (getPredefinedQuestion() == null) {
      return null;
    }
    return getPredefinedQuestion().getNote().getNotebook();
  }

  @JsonIgnore
  public AnsweredQuestion getAnsweredQuestion() {
    if (getPredefinedQuestion() == null || getAnswer() == null) {
      return null;
    }

    Answer answer = getAnswer();
    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answer;
    answerResult.note = getPredefinedQuestion().getNote();
    answerResult.recallPrompt = this;
    answerResult.memoryTrackerId = memoryTracker.getId();
    return answerResult;
  }

  @JsonIgnore
  public String getQuestionDetails() {
    if (getPredefinedQuestion() == null) {
      return "{}";
    }
    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    ObjectNode questionDetails = mapper.createObjectNode();
    MCQWithAnswer mcqWithAnswer = getPredefinedQuestion().getMcqWithAnswer();
    questionDetails.set(
        "originalQuestionDefinition", mapper.convertValue(mcqWithAnswer, ObjectNode.class));
    if (getAnswer() != null) {
      Answer answer = getAnswer();
      questionDetails.put("userAnswer", answer.getChoiceIndex());
      questionDetails.put("userAnswerWasMarkedAs", answer.getCorrect() ? "correct" : "incorrect");
    }
    return questionDetails.toPrettyString();
  }

  @JsonProperty
  public Timestamp getAnswerTime() {
    if (getAnswer() != null) {
      return getAnswer().getCreatedAt();
    }
    return null;
  }

  @JsonProperty
  public Note getNote() {
    if (getPredefinedQuestion() == null) {
      return null;
    }
    return getPredefinedQuestion().getNote();
  }

  @JsonProperty("predefinedQuestion")
  public PredefinedQuestion getPredefinedQuestionExposed() {
    return getPredefinedQuestion();
  }

  @JsonProperty("answer")
  public Answer getAnswerExposed() {
    return getAnswer();
  }

  @JsonProperty
  public Timestamp getQuestionGeneratedTime() {
    return getCreatedAt();
  }

  @JsonProperty
  public Boolean getIsContested() {
    if (getPredefinedQuestion() == null) {
      return null;
    }
    return getPredefinedQuestion().isContested();
  }

  @JsonProperty
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    if (getPredefinedQuestion() == null) {
      return null;
    }
    return getPredefinedQuestion().getMultipleChoicesQuestion();
  }

  @JsonProperty
  public SpellingQuestion getSpellingQuestion() {
    if (questionType != QuestionType.SPELLING) {
      return null;
    }
    Note note = memoryTracker.getNote();
    String stem = note.getClozeDescription().clozeDetails();
    Notebook notebook = note.getNotebook();
    return new SpellingQuestion(stem, notebook);
  }
}
