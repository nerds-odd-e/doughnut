package com.odde.doughnut.services.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextFromAudioWithCallInfo {
  private String completionMarkdownFromAudio;

  private String rawSRT;

  private ToolCallInfo toolCallInfo;
}
