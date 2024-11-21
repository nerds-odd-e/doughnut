package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AiToolName {
  COMPLETE_NOTE_DETAILS("complete_note_details"),
  SUGGEST_NOTE_TOPIC_TITLE("suggest_note_topic_title"),
  ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION("ask_single_answer_multiple_choice_question");

  private final String value;

  AiToolName(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
