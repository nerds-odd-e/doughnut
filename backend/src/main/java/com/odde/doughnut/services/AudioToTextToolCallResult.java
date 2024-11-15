package com.odde.doughnut.services;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AudioToTextToolCallResult {
  private final String instruction;
  private final String newTranscription;
  private final String tailOfPreviousCompletedNoteDetails;
}
