package com.odde.donut.services;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class SRTProcessor {
  @Getter
  @AllArgsConstructor
  public static class SRTProcessingResult {
    private String processedSRT;
    private String text;
    private String endTimestamp;
  }

  public SRTProcessingResult process(String rawSRT, boolean midSpeech) {
    List<String> segments = Arrays.asList(rawSRT.strip().split("\n\n"));
    if (!midSpeech) {
      return new SRTProcessingResult(
          rawSRT, textOf(segments), extractTimestampFromSegment(segments.getLast()));
    }

    if (segments.size() <= 1) {
      return new SRTProcessingResult("", "", "00:00:00,000");
    }

    List<String> written = segments.subList(0, segments.size() - 1);
    return new SRTProcessingResult(
        String.join("\n\n", written),
        textOf(written),
        extractTimestampFromSegment(written.getLast()));
  }

  private static String textOf(List<String> segments) {
    return segments.stream().map(SRTProcessor::segmentText).collect(Collectors.joining(" "));
  }

  private static String segmentText(String segment) {
    return segment.replaceFirst("(?s)\\A.*?-->[^\\n]*", "").strip().replace('\n', ' ');
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
