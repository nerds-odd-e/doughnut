package com.odde.doughnut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;
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
    Map<Integer, String> result =
        NotebookExportFilenames.uniqueFileNames(
            List.of(Map.entry(10, "Recipe"), Map.entry(99, "Recipe"), Map.entry(3, "Other")),
            ".md");

    assertThat(result.get(10), equalTo("Recipe.md"));
    assertThat(result.get(99), equalTo("Recipe (2).md"));
    assertThat(result.get(3), equalTo("Other.md"));
  }

  @Test
  void uniqueFileNamesSkipsHumanSuffixAlreadyTakenByAnotherBasename() {
    Map<Integer, String> result =
        NotebookExportFilenames.uniqueFileNames(
            List.of(Map.entry(10, "Recipe"), Map.entry(20, "Recipe (2)"), Map.entry(99, "Recipe*")),
            ".md");

    assertThat(result.get(99), equalTo("Recipe (3).md"));
  }
}
