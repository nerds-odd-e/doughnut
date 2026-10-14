package com.odde.donut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    """
    Replace the entire `content` field of the current focus note with the new content.
    The content should be in markdown format.

    IMPORTANT: Keep all parts of the existing content that the user did not ask to change.
    Copy those unchanged parts exactly as they appear in the original content, without any
    modifications. Only modify or add the parts that the user specifically requested to change.
    """)
@NoArgsConstructor
public class NoteContentCompletion {

  @JsonPropertyDescription(
      "The complete new content text that will replace the current note's content.")
  @JsonProperty(required = true)
  public String content;

  @JsonPropertyDescription("Why this correction helps the user, grounded in the conversation.")
  public String reason;

  public NoteContentCompletion(String content) {
    this.content = content;
  }
}
