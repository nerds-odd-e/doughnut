package com.odde.doughnut.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SrtTest {

  @Test
  public void shouldReturnFalseCaseZero() {
    Srt srt = new Srt("");
    assertFalse(srt.isSRTFormat());

    srt = new Srt(null);
    assertFalse(srt.isSRTFormat());

    srt = new Srt("   ");
    assertFalse(srt.isSRTFormat());

    srt = new Srt("\n\n\t\n\n");
    assertFalse(srt.isSRTFormat());

    srt = new Srt("this is normal text\nthis is normal text line 2");
    assertFalse(srt.isSRTFormat());
  }

  @Test
  public void shouldReturnTextFormatCaseOne() {
    Srt srt = new Srt("1\n00:05:00,400 --> 00:05:15,300\nThis is an example of a subtitle.");
    assertTrue(srt.convertSrtToText().contains("This is an example of a subtitle."));
  }

  @Test
  public void shouldReturnTextFormatCaseMany() {
    Srt srt =
        new Srt(
            "1\n00:05:00,400 --> 00:05:15,300\nThis is an example of a subtitle.\n\n2\n00:06:00,400 --> 00:06:15,300\nThis is Second");
    assertTrue(srt.convertSrtToText().contains("This is an example of a subtitle."));
    assertTrue(srt.convertSrtToText().contains("This is Second"));
  }
}
