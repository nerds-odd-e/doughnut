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

  @Test
  void uniqueFileNamesKeepsCleanNameForFirstOccurrenceAndSuffixesLaterDuplicates() {
    java.util.Map<Integer, String> result =
        NotebookExportFilenames.uniqueFileNames(
            java.util.List.of(
                java.util.Map.entry(1, "Recipe"),
                java.util.Map.entry(2, "Recipe"),
                java.util.Map.entry(3, "Other")),
            ".md");

    assertThat(result.get(1), equalTo("Recipe.md"));
    assertThat(result.get(2), equalTo("Recipe (2).md"));
    assertThat(result.get(3), equalTo("Other.md"));
  }
}
