package com.odde.doughnut.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NotebookSettingsTest {
  @Test
  void defaults() {
    NotebookSettings notebookSettings = new NotebookSettings();
    assertEquals(false, notebookSettings.getSkipMemoryTrackingEntirely());
  }
}
