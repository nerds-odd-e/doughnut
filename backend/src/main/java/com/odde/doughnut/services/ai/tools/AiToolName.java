package com.odde.doughnut.services.ai.tools;

public enum AiToolName {
  COMPLETE_NOTE_DETAILS("complete_note_details"),
  SUGGEST_TOPIC_TITLE("suggest_note_topic_title");

  private final String value;

  AiToolName(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
