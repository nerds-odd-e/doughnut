package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AudioTranscriptConversionConfig {
  private String additionalProcessingInstructions;
  private String previousNoteDetails;
  private String threadId;
  private String runId;
  private String toolCallId;
}
