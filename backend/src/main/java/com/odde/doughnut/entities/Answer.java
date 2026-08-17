package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.exceptions.QuestionAnswerException;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Getter
@Entity
@Table(name = "answer")
public class Answer extends EntityIdentifiedByIdOnly {
  @Column(name = "choice_index")
  Integer choiceIndex;

  @Column(name = "created_at")
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Transient @Setter private Boolean correct;

  @OneToMany(mappedBy = "answer")
  @Fetch(FetchMode.SUBSELECT)
  @JsonIgnore
  @Getter
  private List<RecallLog> recallLogs = new ArrayList<>();

  @Column(name = "thinking_time_ms")
  @Setter
  private Integer thinkingTimeMs;

  @Column(name = "spelling_answer")
  @Setter
  private String spellingAnswer;

  @Transient @Getter @Setter private Long matchedNoteId;

  @Column(name = "outcome")
  @Enumerated(EnumType.STRING)
  @Setter
  private AnswerOutcome outcome;

  @JsonProperty
  public Boolean getCorrect() {
    Boolean fromOutcome = correctFrom(outcome, null);
    if (fromOutcome != null) {
      return fromOutcome;
    }
    if (correct != null) {
      return correct;
    }
    for (RecallLog log : recallLogs) {
      Boolean fromLog = correctFrom(null, log.getProductOutcome());
      if (fromLog != null) {
        return fromLog;
      }
    }
    return null;
  }

  public void addRecallLog(RecallLog recallLog) {
    recallLogs.add(recallLog);
  }

  public static Boolean correctFrom(AnswerOutcome outcome, ProductOutcome productOutcome) {
    if (outcome == AnswerOutcome.OVERLAP) {
      return true;
    }
    if (productOutcome == ProductOutcome.GOOD) {
      return true;
    }
    if (productOutcome == ProductOutcome.AGAIN) {
      return false;
    }
    return null;
  }

  public static Answer buildAnswer(AnswerDTO answerDTO, Mcq mcq, Answer existingAnswer) {
    if (existingAnswer != null) {
      throw new QuestionAnswerException("The question is already answered");
    }
    Answer answer = new Answer();
    answer.choiceIndex = answerDTO.getChoiceIndex();
    answer.setCorrect(mcq.checkAnswer(answerDTO));
    answer.setThinkingTimeMs(answerDTO.getThinkingTimeMs());
    return answer;
  }
}
