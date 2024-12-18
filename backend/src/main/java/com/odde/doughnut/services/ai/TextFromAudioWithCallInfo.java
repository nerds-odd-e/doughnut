package com.odde.doughnut.services.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextFromAudioWithCallInfo {
  private NoteDetailsCompletion completionFromAudio;

  private String rawSRT;

  private String endTimestamp;
}
