package com.odde.donut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription("Text dictated in the audio transcription, formatted in Markdown.")
@NoArgsConstructor
@AllArgsConstructor
public class DictatedText {
  @JsonPropertyDescription("Only the new dictated text, including any leading whitespace it needs.")
  @JsonProperty(required = true)
  public String dictatedText;
}
