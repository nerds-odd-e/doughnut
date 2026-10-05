package com.odde.donut.services.ai;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextFromAudioWithCallInfo {
  private List<String> segmentTexts;

  private String rawSRT;

  private String endTimestamp;
}
