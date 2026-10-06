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
  void shouldRemoveLastSegment() {
    SRTProcessor.SRTProcessingResult result = processor.process(sampleSRT);
    assertThat(result.getSegmentTexts(), contains("First segment", "Second segment"));
    assertThat(result.getEndTimestamp(), equalTo("00:00:06,000"));
  }

  @Test
  void shouldHoldBackSingleSegment() {
    String singleSegment = "1\n00:00:00,000 --> 00:00:03,000\nOnly segment";
    SRTProcessor.SRTProcessingResult result = processor.process(singleSegment);
    assertThat(result.getSegmentTexts(), empty());
    assertThat(result.getEndTimestamp(), equalTo("00:00:00,000"));
  }

  @Test
  void shouldRemoveRealLastSegmentWhenSRTEndsWithBlankLines() {
    SRTProcessor.SRTProcessingResult result = processor.process(sampleSRT + "\n\n\n");
    assertThat(result.getSegmentTexts(), contains("First segment", "Second segment"));
    assertThat(result.getEndTimestamp(), equalTo("00:00:06,000"));
  }

  @Test
  void shouldHoldBackSingleSegmentEndingWithBlankLines() {
    String singleSegment = "1\n00:00:00,000 --> 00:00:03,000\nOnly segment\n\n\n";
    SRTProcessor.SRTProcessingResult result = processor.process(singleSegment);
    assertThat(result.getSegmentTexts(), empty());
    assertThat(result.getEndTimestamp(), equalTo("00:00:00,000"));
  }

  @Test
  void shouldHoldBackEmptySRT() {
    SRTProcessor.SRTProcessingResult result = processor.process("");
    assertThat(result.getSegmentTexts(), empty());
    assertThat(result.getEndTimestamp(), equalTo("00:00:00,000"));
  }
}
