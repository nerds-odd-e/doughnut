package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ConversationSubject {
  @ManyToOne
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  private Note note;

  @ManyToOne
  @JoinColumn(name = "recall_prompt_id", referencedColumnName = "id")
  @JsonIgnore
  private RecallPrompt recallPrompt;

  @JsonProperty("recallPrompt")
  public AnsweredQuestion getRecallPromptExposed() {
    if (recallPrompt == null) {
      return null;
    }
    return AnsweredQuestion.from(recallPrompt);
  }

  @JsonIgnore
  public boolean isEmpty() {
    return note == null && recallPrompt == null;
  }
}
