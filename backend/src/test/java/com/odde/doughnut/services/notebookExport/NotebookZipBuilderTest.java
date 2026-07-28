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

  @Test
  void writesNestedFoldersWithTheirOwnReadmeAndNotes() throws IOException {
    ExportFolderRow parent = new ExportFolderRow(10, null, "Parent Folder", "Parent readme");
    ExportFolderRow child = new ExportFolderRow(11, 10, "Child Folder", null);
    ExportNoteRow noteInChild = new ExportNoteRow(2, 11, "Nested note", "Nested body");

    byte[] zipBytes = NotebookZipBuilder.build(null, List.of(parent, child), List.of(noteInChild));

    Map<String, String> entries = readZipEntries(zipBytes);

    assertThat(entries.get("Parent Folder/README.md"), equalTo("Parent readme"));
    assertThat(entries.containsKey("Child Folder/README.md"), equalTo(false));
    assertThat(
        entries.get("Parent Folder/Child Folder/Nested note.md"),
        equalTo("# Nested note\n\nNested body"));
  }
}
