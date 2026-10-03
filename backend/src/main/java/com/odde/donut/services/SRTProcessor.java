package com.odde.donut.services;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class SRTProcessor {
  @Getter
  @AllArgsConstructor
  public static class SRTProcessingResult {
    private String processedSRT;
    private String endTimestamp;
  }

  public SRTProcessingResult process(String rawSRT, boolean midSpeech) {
    if (!midSpeech) {
      return new SRTProcessingResult(rawSRT, extractLastTimestamp(rawSRT));
    }

    String[] segments = rawSRT.strip().split("\n\n");
    if (segments.length <= 1) {
      return new SRTProcessingResult("", "00:00:00,000");
    }

    // Remove the last segment and join the rest
    StringBuilder processedSRT = new StringBuilder();
    for (int i = 0; i < segments.length - 1; i++) {
      if (i > 0) {
        processedSRT.append("\n\n");
      }
      processedSRT.append(segments[i]);
    }

    return new SRTProcessingResult(
        processedSRT.toString(), extractTimestampFromSegment(segments[segments.length - 2]));
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
