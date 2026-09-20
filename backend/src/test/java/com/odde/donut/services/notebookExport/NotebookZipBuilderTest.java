package com.odde.donut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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

  private Map<String, byte[]> readZipEntryBytes(byte[] zipBytes) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        entries.put(entry.getName(), zis.readAllBytes());
      }
    }
    return entries;
  }

  private Map<String, String> readZipEntryTexts(byte[] zipBytes) throws IOException {
    Map<String, String> texts = new LinkedHashMap<>();
    readZipEntryBytes(zipBytes)
        .forEach((path, content) -> texts.put(path, new String(content, StandardCharsets.UTF_8)));
    return texts;
  }

  private byte[] buildZip(
      String notebookReadmeContent, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    return NotebookZipBuilder.build(notebookReadmeContent, folders, notes, List.of());
  }

  @Test
  void writesRootAttachmentBytesUnchangedAlongsideNotes() throws IOException {
    byte[] invalidUtf8 = {(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0x80};

    byte[] zipBytes =
        NotebookZipBuilder.build(
            null,
            List.of(),
            List.of(new ExportNoteRow(null, "My Note", "body")),
            List.of(
                new ExportAttachmentRow("diagram.png", invalidUtf8),
                new ExportAttachmentRow("empty.bin", new byte[0])));

    Map<String, byte[]> entries = readZipEntryBytes(zipBytes);

    assertThat(entries.keySet(), contains("My Note.md", "diagram.png", "empty.bin"));
    assertThat(entries.get("diagram.png"), equalTo(invalidUtf8));
    assertThat(entries.get("empty.bin"), equalTo(new byte[0]));
    assertThat(entries.get("My Note.md"), equalTo("body".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void writesNotebookReadmeAsReadmeMarkdownWithTypeWhenMissing() throws IOException {
    Map<String, String> entries =
        readZipEntryTexts(buildZip("# Notebook readme", List.of(), List.of()));

    assertThat(entries.get("README.md"), equalTo(README_FENCE + "# Notebook readme"));
    assertThat(entries.containsKey("index.md"), equalTo(false));
  }

  @Test
  void writesNestedFolderReadmeAsReadmeMarkdownAndOmitsBlank() throws IOException {
    ExportFolderRow parent = new ExportFolderRow(10, null, "Parent Folder", "Parent readme");
    ExportFolderRow child = new ExportFolderRow(11, 10, "Child Folder", null);
    ExportNoteRow noteInChild = new ExportNoteRow(11, "Nested note", "Nested body");

    byte[] zipBytes = buildZip(null, List.of(parent, child), List.of(noteInChild));

    Map<String, String> entries = readZipEntryTexts(zipBytes);

    assertThat(entries.get("Parent Folder/README.md"), equalTo(README_FENCE + "Parent readme"));
    assertThat(entries.containsKey("Parent Folder/Child Folder/README.md"), equalTo(false));
    assertThat(entries.get("Parent Folder/Child Folder/Nested note.md"), equalTo("Nested body"));
  }

  @Test
  void insertsReadmeTypeAsFirstKeyKeepingAuthorFence() throws IOException {
    String contentWithFrontmatter = "---\nwikidata_id: Q123\n---\n\nActual body text";

    Map<String, String> entries =
        readZipEntryTexts(buildZip(contentWithFrontmatter, List.of(), List.of()));

    assertThat(
        entries.get("README.md"),
        equalTo("---\ntype: Readme\nwikidata_id: Q123\n---\n\nActual body text"));
  }

  @Test
  void canonicalizesReadmeTypeSpellingInPlace() throws IOException {
    String content = "---\ntype: readme\nwikidata_id: Q1\n---\nbody";

    Map<String, String> entries = readZipEntryTexts(buildZip(content, List.of(), List.of()));

    assertThat(entries.get("README.md"), equalTo("---\ntype: Readme\nwikidata_id: Q1\n---\nbody"));
  }

  @Test
  void leavesOtherNonEmptyTypeUnchanged() throws IOException {
    String content = "---\ntype: Note\nwikidata_id: Q1\n---\nbody";

    Map<String, String> entries = readZipEntryTexts(buildZip(content, List.of(), List.of()));

    assertThat(entries.get("README.md"), equalTo(content));
  }

  @Test
  void preservesAuthorFrontmatterWithoutStrippingProperties() throws IOException {
    String contentWithFrontmatter = "---\nwikidata_id: Q123\n---\n\nActual body text";
    ExportNoteRow note = new ExportNoteRow(null, "My Note", contentWithFrontmatter);

    byte[] zipBytes = buildZip(null, List.of(), List.of(note));

    Map<String, String> entries = readZipEntryTexts(zipBytes);
    assertThat(
        entries.get("My Note.md"), equalTo("---\nwikidata_id: Q123\n---\n\nActual body text"));
  }

  @Test
  void writesNoteFileAsExactDisplayName() throws IOException {
    ExportNoteRow note = new ExportNoteRow(null, "Q&A What Why", "body");

    Map<String, String> entries = readZipEntryTexts(buildZip(null, List.of(), List.of(note)));

    assertThat(entries.get("Q&A What Why.md"), equalTo("body"));
  }
}
