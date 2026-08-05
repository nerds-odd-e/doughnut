package com.odde.doughnut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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

  private byte[] buildZip(
      String notebookReadmeContent, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    return NotebookZipBuilder.build(notebookReadmeContent, folders, notes);
  }

  @Test
  void writesNotebookIndexAndRootNotesAsMarkdownFilesWithATitleHeading() throws IOException {
    byte[] zipBytes =
        buildZip(
            "# Notebook readme",
            List.of(),
            List.of(new ExportNoteRow(1, null, "First note", "First body")));

    Map<String, String> entries = readZipEntries(zipBytes);

    assertThat(entries.get("index.md"), equalTo("# Notebook readme"));
    assertThat(entries.get("First note.md"), equalTo("# First note\n\nFirst body"));
  }

  @Test
  void writesNestedFoldersWithTheirOwnIndexAndNotes() throws IOException {
    ExportFolderRow parent = new ExportFolderRow(10, null, "Parent Folder", "Parent readme");
    ExportFolderRow child = new ExportFolderRow(11, 10, "Child Folder", null);
    ExportNoteRow noteInChild = new ExportNoteRow(2, 11, "Nested note", "Nested body");

    byte[] zipBytes = buildZip(null, List.of(parent, child), List.of(noteInChild));

    Map<String, String> entries = readZipEntries(zipBytes);

    assertThat(entries.get("Parent Folder/index.md"), equalTo("Parent readme"));
    assertThat(entries.containsKey("Child Folder/index.md"), equalTo(false));
    assertThat(
        entries.get("Parent Folder/Child Folder/Nested note.md"),
        equalTo("# Nested note\n\nNested body"));
  }

  @Test
  void preservesAuthorFrontmatterWithoutStrippingProperties() throws IOException {
    String contentWithFrontmatter = "---\nwikidata_id: Q123\n---\n\nActual body text";
    ExportNoteRow note = new ExportNoteRow(3, null, "My Note", contentWithFrontmatter);

    byte[] zipBytes = buildZip(null, List.of(), List.of(note));

    Map<String, String> entries = readZipEntries(zipBytes);
    assertThat(
        entries.get("My Note.md"),
        equalTo("---\nwikidata_id: Q123\n---\n\n# My Note\n\nActual body text"));
  }

  @Test
  void leavesWikiLinksUnchangedInExportedNotes() throws IOException {
    ExportNoteRow source = new ExportNoteRow(1, null, "Source", "See [[Target Title]]");
    ExportNoteRow target = new ExportNoteRow(2, null, "Target Title", "Target body");

    Map<String, String> entries =
        readZipEntries(buildZip(null, List.of(), List.of(source, target)));

    assertThat(entries.get("Source.md"), containsString("[[Target Title]]"));
  }

  @Test
  void usesCollisionSafeFilenamesForDuplicateTitles() throws IOException {
    ExportNoteRow first = new ExportNoteRow(1, null, "Dup", "first");
    ExportNoteRow second = new ExportNoteRow(2, null, "Dup", "second");

    Map<String, String> entries = readZipEntries(buildZip(null, List.of(), List.of(first, second)));

    assertThat(entries.containsKey("Dup.md"), equalTo(true));
    assertThat(entries.containsKey("Dup (2).md"), equalTo(true));
  }
}
