package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SRTProcessorTests {
  private SRTProcessor processor;
  private final String sampleSRT =
      "1\n00:00:00,000 --> 00:00:03,000\nFirst segment\n\n"
          + "2\n00:00:03,000 --> 00:00:06,000\nSecond segment\n\n"
          + "3\n00:00:06,000 --> 00:00:09,000\nLast segment";

  @BeforeEach
  void setUp() {
    processor = new SRTProcessor();
  }

  @Test
  void shouldNotModifySRTWhenNotIncomplete() {
    SRTProcessor.SRTProcessingResult result = processor.process(sampleSRT, false);
    assertThat(result.getProcessedSRT(), equalTo(sampleSRT));
    assertThat(result.getEndTimestamp(), equalTo("00:00:09,000"));
  }

  @Test
  void shouldRemoveLastSegmentWhenIncomplete() {
    SRTProcessor.SRTProcessingResult result = processor.process(sampleSRT, true);
    assertThat(result.getProcessedSRT(), not(containsString("Last segment")));
    assertThat(result.getProcessedSRT(), containsString("First segment"));
    assertThat(result.getProcessedSRT(), containsString("Second segment"));
    assertThat(result.getEndTimestamp(), equalTo("00:00:06,000"));
  }

  @Test
  void shouldHoldBackSingleSegmentWhenIncomplete() {
    String singleSegment = "1\n00:00:00,000 --> 00:00:03,000\nOnly segment";
    SRTProcessor.SRTProcessingResult result = processor.process(singleSegment, true);
    assertThat(result.getProcessedSRT(), equalTo(""));
    assertThat(result.getEndTimestamp(), equalTo("00:00:00,000"));
  }

  @Test
  void shouldRemoveRealLastSegmentWhenIncompleteSRTEndsWithBlankLines() {
    SRTProcessor.SRTProcessingResult result = processor.process(sampleSRT + "\n\n\n", true);
    assertThat(result.getProcessedSRT(), not(containsString("Last segment")));
    assertThat(result.getProcessedSRT(), containsString("Second segment"));
    assertThat(result.getEndTimestamp(), equalTo("00:00:06,000"));
  }

  @Test
  void shouldHoldBackSingleSegmentEndingWithBlankLinesWhenIncomplete() {
    String singleSegment = "1\n00:00:00,000 --> 00:00:03,000\nOnly segment\n\n\n";
    SRTProcessor.SRTProcessingResult result = processor.process(singleSegment, true);
    assertThat(result.getProcessedSRT(), equalTo(""));
    assertThat(result.getEndTimestamp(), equalTo("00:00:00,000"));
  }

  @Test
  void shouldHoldBackEmptySRTWhenIncomplete() {
    SRTProcessor.SRTProcessingResult result = processor.process("", true);
    assertThat(result.getProcessedSRT(), equalTo(""));
    assertThat(result.getEndTimestamp(), equalTo("00:00:00,000"));
  }

  @Test
  void textIsTheWrittenSegmentsJoinedByOneSpace() {
    assertThat(
        processor.process(sampleSRT, false).getText(),
        equalTo("First segment Second segment Last segment"));
    assertThat(
        processor.process(sampleSRT, true).getText(), equalTo("First segment Second segment"));
  }

  @Test
  void textIsTheLinesAfterTheTimestampLineWithoutAnIndexLine() {
    String srt =
        "00:00:00,000 --> 00:00:01,000\nits talk about\ndada struct day.\n\n"
            + "00:00:01,000 --> 00:00:02,000\nNext one.\n\n";
    assertThat(
        processor.process(srt, false).getText(),
        equalTo("its talk about dada struct day. Next one."));
  }

  @Test
  void shouldHandleInvalidSRTFormat() {
    String invalidSRT = "Invalid SRT format";
    SRTProcessor.SRTProcessingResult result = processor.process(invalidSRT, false);
    assertThat(result.getProcessedSRT(), equalTo(invalidSRT));
    assertThat(result.getEndTimestamp(), equalTo(""));
  }
}
