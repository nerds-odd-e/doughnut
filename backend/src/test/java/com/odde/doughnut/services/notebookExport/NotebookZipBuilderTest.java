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

  private static final String README_FENCE = "---\ntype: Readme\n---\n";

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
  void writesNotebookReadmeAsReadmeMarkdownWithTypeWhenMissing() throws IOException {
    Map<String, String> entries =
        readZipEntries(buildZip("# Notebook readme", List.of(), List.of()));

    assertThat(entries.get("README.md"), equalTo(README_FENCE + "# Notebook readme"));
    assertThat(entries.containsKey("index.md"), equalTo(false));
  }

  @Test
  void writesNestedFolderReadmeAsReadmeMarkdownAndOmitsBlank() throws IOException {
    ExportFolderRow parent = new ExportFolderRow(10, null, "Parent Folder", "Parent readme");
    ExportFolderRow child = new ExportFolderRow(11, 10, "Child Folder", null);
    ExportNoteRow noteInChild = new ExportNoteRow(11, "Nested note", "Nested body");

    byte[] zipBytes = buildZip(null, List.of(parent, child), List.of(noteInChild));

    Map<String, String> entries = readZipEntries(zipBytes);

    assertThat(entries.get("Parent Folder/README.md"), equalTo(README_FENCE + "Parent readme"));
    assertThat(entries.containsKey("Parent Folder/Child Folder/README.md"), equalTo(false));
    assertThat(entries.get("Parent Folder/Child Folder/Nested note.md"), equalTo("Nested body"));
  }

  @Test
  void insertsReadmeTypeAsFirstKeyKeepingAuthorFence() throws IOException {
    String contentWithFrontmatter = "---\nwikidata_id: Q123\n---\n\nActual body text";

    Map<String, String> entries =
        readZipEntries(buildZip(contentWithFrontmatter, List.of(), List.of()));

    assertThat(
        entries.get("README.md"),
        equalTo("---\ntype: Readme\nwikidata_id: Q123\n---\n\nActual body text"));
  }

  @Test
  void canonicalizesReadmeTypeSpellingInPlace() throws IOException {
    String content = "---\ntype: readme\nwikidata_id: Q1\n---\nbody";

    Map<String, String> entries = readZipEntries(buildZip(content, List.of(), List.of()));

    assertThat(entries.get("README.md"), equalTo("---\ntype: Readme\nwikidata_id: Q1\n---\nbody"));
  }

  @Test
  void leavesOtherNonEmptyTypeUnchanged() throws IOException {
    String content = "---\ntype: Note\nwikidata_id: Q1\n---\nbody";

    Map<String, String> entries = readZipEntries(buildZip(content, List.of(), List.of()));

    assertThat(entries.get("README.md"), equalTo(content));
  }

  @Test
  void preservesAuthorFrontmatterWithoutStrippingProperties() throws IOException {
    String contentWithFrontmatter = "---\nwikidata_id: Q123\n---\n\nActual body text";
    ExportNoteRow note = new ExportNoteRow(null, "My Note", contentWithFrontmatter);

    byte[] zipBytes = buildZip(null, List.of(), List.of(note));

    Map<String, String> entries = readZipEntries(zipBytes);
    assertThat(
        entries.get("My Note.md"), equalTo("---\nwikidata_id: Q123\n---\n\nActual body text"));
  }

  @Test
  void writesNoteFileAsExactDisplayName() throws IOException {
    ExportNoteRow note = new ExportNoteRow(null, "Q&A: What/Why?", "body");

    Map<String, String> entries = readZipEntries(buildZip(null, List.of(), List.of(note)));

    assertThat(entries.get("Q&A: What/Why?.md"), equalTo("body"));
  }
}
