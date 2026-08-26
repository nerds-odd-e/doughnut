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
  void shouldHandleSingleSegmentWhenIncomplete() {
    String singleSegment = "1\n00:00:00,000 --> 00:00:03,000\nOnly segment";
    SRTProcessor.SRTProcessingResult result = processor.process(singleSegment, true);
    assertThat(result.getProcessedSRT(), equalTo(singleSegment));
    assertThat(result.getEndTimestamp(), equalTo("00:00:03,000"));
  }

  @Test
  void shouldHandleEmptySRT() {
    SRTProcessor.SRTProcessingResult result = processor.process("", true);
    assertThat(result.getProcessedSRT(), equalTo(""));
    assertThat(result.getEndTimestamp(), equalTo(""));
  }

  @Test
  void shouldHandleInvalidSRTFormat() {
    String invalidSRT = "Invalid SRT format";
    SRTProcessor.SRTProcessingResult result = processor.process(invalidSRT, true);
    assertThat(result.getProcessedSRT(), equalTo(invalidSRT));
    assertThat(result.getEndTimestamp(), equalTo(""));
  }
}
