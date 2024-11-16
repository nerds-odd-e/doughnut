package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.*;

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
    assertEquals(sampleSRT, result.getProcessedSRT());
    assertEquals("00:00:09,000", result.getEndTimestamp());
  }

  @Test
  void shouldRemoveLastSegmentWhenIncomplete() {
    SRTProcessor.SRTProcessingResult result = processor.process(sampleSRT, true);
    assertFalse(result.getProcessedSRT().contains("Last segment"));
    assertTrue(result.getProcessedSRT().contains("First segment"));
    assertTrue(result.getProcessedSRT().contains("Second segment"));
    assertEquals("00:00:06,000", result.getEndTimestamp());
  }

  @Test
  void shouldHandleSingleSegmentWhenIncomplete() {
    String singleSegment = "1\n00:00:00,000 --> 00:00:03,000\nOnly segment";
    SRTProcessor.SRTProcessingResult result = processor.process(singleSegment, true);
    assertEquals(singleSegment, result.getProcessedSRT());
    assertEquals("00:00:03,000", result.getEndTimestamp());
  }

  @Test
  void shouldHandleEmptySRT() {
    SRTProcessor.SRTProcessingResult result = processor.process("", true);
    assertEquals("", result.getProcessedSRT());
    assertEquals("", result.getEndTimestamp());
  }

  @Test
  void shouldHandleInvalidSRTFormat() {
    String invalidSRT = "Invalid SRT format";
    SRTProcessor.SRTProcessingResult result = processor.process(invalidSRT, true);
    assertEquals(invalidSRT, result.getProcessedSRT());
    assertEquals("", result.getEndTimestamp());
  }
}
