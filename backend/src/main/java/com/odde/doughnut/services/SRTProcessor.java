package com.odde.doughnut.services;

import lombok.Getter;
import lombok.Setter;

public class SRTProcessor {
  @Getter
  @Setter
  public static class SRTProcessingResult {
    private String processedSRT;
    private String endTimestamp;
  }

  public SRTProcessingResult process(String rawSRT, boolean incomplete) {
    if (!incomplete) {
      SRTProcessingResult result = new SRTProcessingResult();
      result.setProcessedSRT(rawSRT);
      result.setEndTimestamp(extractLastTimestamp(rawSRT));
      return result;
    }

    String[] segments = rawSRT.split("\n\n");
    if (segments.length <= 1) {
      SRTProcessingResult result = new SRTProcessingResult();
      result.setProcessedSRT(rawSRT);
      result.setEndTimestamp(extractLastTimestamp(rawSRT));
      return result;
    }

    // Remove the last segment and join the rest
    StringBuilder processedSRT = new StringBuilder();
    for (int i = 0; i < segments.length - 1; i++) {
      if (i > 0) {
        processedSRT.append("\n\n");
      }
      processedSRT.append(segments[i]);
    }

    SRTProcessingResult result = new SRTProcessingResult();
    result.setProcessedSRT(processedSRT.toString());
    result.setEndTimestamp(extractTimestampFromSegment(segments[segments.length - 2]));
    return result;
  }

  private String extractLastTimestamp(String srt) {
    String[] segments = srt.split("\n\n");
    if (segments.length == 0) {
      return "";
    }
    return extractTimestampFromSegment(segments[segments.length - 1]);
  }

  private String extractTimestampFromSegment(String segment) {
    String[] lines = segment.split("\n");
    if (lines.length < 2) {
      return "";
    }
    String timestampLine = lines[1];
    String[] timestamps = timestampLine.split(" --> ");
    if (timestamps.length < 2) {
      return "";
    }
    return timestamps[1].trim();
  }
}
