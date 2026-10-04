package com.odde.donut.services.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextFromAudioWithCallInfo {
  private String dictatedText;

  private String rawSRT;

  private String endTimestamp;
}
