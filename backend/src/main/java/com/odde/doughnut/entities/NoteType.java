package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NoteType {
  CONCEPT(
      "concept",
      """
      **Special Instruction for Concept Note**: Test meaning, correct interpretation, or correct application. Prefer scenario/implication questions. Avoid trivia, dates, or verbatim wording recall unless ambiguity is the point.
      """),
  SOURCE(
      "source",
      """
      **Special Instruction for Source Note**: Test the source's key idea/argument (or what it supports/claims), not bibliographic trivia. Use distractors that plausibly misstate the argument. Do not treat the source as unquestionable truth.
      """),
  PERSON(
      "person",
      """
      **Special Instruction for Person Note**: Test attribution: which ideas/roles/stances/actions are associated with the person. Use distractors from nearby thinkers or similar concepts. Avoid pure biographical trivia unless it matters.
      """),
  EXPERIENCE(
      "experience",
      """
      **Special Instruction for Experience Note**: Test the lesson, observation, or takeaway from the experience (what was learned and why). Avoid pure event recall (date/location/attendees) unless essential to the lesson.
      """),
  INITIATIVE(
      "initiative",
      """
      **Special Instruction for Initiative Note**: Test purpose, problem framing, guiding constraints, strategy, or trade-offs. Use distractors as plausible alternative goals/framings. Avoid outcome/success questions unless stated.
      """),
  QUEST(
      "quest",
      """
      **Special Instruction for Quest Note**: Do NOT ask for 'the answer'. Test the inquiry's shape: what is being asked, why it matters, key assumptions, what evidence would move it forward, or what would invalidate it. Avoid premature closure.
      """);

  @JsonValue public final String label;
  public final String questionGenerationInstruction;

  NoteType(String label, String questionGenerationInstruction) {
    this.label = label;
    this.questionGenerationInstruction = questionGenerationInstruction;
  }

  public String getQuestionGenerationInstruction() {
    return questionGenerationInstruction;
  }

  @JsonCreator
  public static NoteType fromLabel(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    for (NoteType type : NoteType.values()) {
      if (type.label.equalsIgnoreCase(text)) {
        return type;
      }
    }
    return CONCEPT; // Default if not found
  }
}
