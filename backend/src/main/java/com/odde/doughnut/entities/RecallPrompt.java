package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.SpellingQuestion;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recall_prompt")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class RecallPrompt extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "memory_tracker_id", referencedColumnName = "id")
  @NotNull
  private MemoryTracker memoryTracker;

  @ManyToOne
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  private PredefinedQuestion predefinedQuestion;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "quiz_answer_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Answer answer;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_type")
  @NotNull
  private QuestionType questionType;

  @Column(name = "created_at")
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  public MemoryTracker requireMemoryTracker() {
    return Objects.requireNonNull(memoryTracker, "recall prompt requires a memory tracker");
  }

  public String getPropertyKey() {
    if (memoryTracker == null) {
      return null;
    }
    String key = memoryTracker.getPropertyKey();
    return key == null || key.isEmpty() ? null : key;
  }

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

  public Timestamp getAnswerTime() {
    if (getAnswer() != null) {
      return getAnswer().getCreatedAt();
    }
    return null;
  }

  public Note getNote() {
    if (getPredefinedQuestion() != null) {
      return getPredefinedQuestion().getNote();
    }
    if (getMemoryTracker() != null) {
      return getMemoryTracker().getNote();
    }
    return null;
  }

  public Notebook getNotebook() {
    return getMemoryTracker().getNote().getNotebook();
  }

  public Boolean getIsContested() {
    if (getPredefinedQuestion() == null) {
      return null;
    }
    return getPredefinedQuestion().isContested();
  }

  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    if (getPredefinedQuestion() == null) {
      return null;
    }
    return getPredefinedQuestion().getMultipleChoicesQuestion();
  }

  public SpellingQuestion getSpellingQuestion() {
    if (questionType != QuestionType.SPELLING) {
      return null;
    }
    Note note = memoryTracker.getNote();
    String stem = note.createMaskedContentForRecall().maskedContentAsMarkdown();
    return new SpellingQuestion(stem, getNotebook());
  }
}
