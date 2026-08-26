package com.odde.donut.services.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextFromAudioWithCallInfo {
  private NoteContentCompletion completionFromAudio;

  private String rawSRT;

  private String endTimestamp;
}
