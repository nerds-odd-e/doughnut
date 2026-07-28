package com.odde.doughnut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class NotebookExportFilenamesTest {

  @Test
  void sanitizeReplacesFilesystemInvalidCharactersWithSpaces() {
    String result = NotebookExportFilenames.sanitize("Q&A: What/Why?");

    assertThat(result, equalTo("Q&A What Why"));
  }

  @Test
  void sanitizeFallsBackToUntitledWhenNameIsBlankAfterCleaning() {
    String result = NotebookExportFilenames.sanitize("   ///:::   ");

    assertThat(result, equalTo("Untitled"));
  }
}
