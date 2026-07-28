package com.odde.doughnut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class NotebookZipBuilderTest {

  private Map<String, String> readZipEntries(byte[] zipBytes) throws IOException {
    Map<String, String> entries = new LinkedHashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        entries.put(entry.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    return entries;
  }

  @Test
  void writesNotebookReadmeAndRootNotesAsMarkdownFilesWithTitleHeading() throws IOException {
    byte[] zipBytes =
        NotebookZipBuilder.build(
            "# Notebook readme",
            List.of(),
            List.of(new ExportNoteRow(1, null, "First note", "First body")));

    Map<String, String> entries = readZipEntries(zipBytes);

    assertThat(entries.get("README.md"), equalTo("# Notebook readme"));
    assertThat(entries.get("First note.md"), equalTo("# First note\n\nFirst body"));
  }
}
