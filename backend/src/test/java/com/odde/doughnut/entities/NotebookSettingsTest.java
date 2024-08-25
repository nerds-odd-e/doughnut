package com.odde.doughnut.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Period;
import org.junit.jupiter.api.Test;

class NotebookSettingsTest {
  @Test
  void testValueOfDefaultExpiry() {
    NotebookSettings notebookSettings = new NotebookSettings();
    assertEquals(Period.ofYears(1), notebookSettings.getCertificateExpiry());
  }
}
