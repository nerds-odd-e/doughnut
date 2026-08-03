package com.odde.doughnut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

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

  private static final String NOTEBOOK = "My Notebook";
  private static final String ORIGIN = "http://localhost:9081";

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
    return NotebookZipBuilder.build(notebookReadmeContent, folders, notes, NOTEBOOK, ORIGIN);
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
    assertThat(
        entries.get("First note.md"),
        equalTo("---\ndoughnut_id: 1\n---\n\n# First note\n\nFirst body"));
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
        equalTo("---\ndoughnut_id: 2\n---\n\n# Nested note\n\nNested body"));
  }

  @Test
  void mergesDoughnutIdIntoAuthorFrontmatterWithoutStrippingProperties() throws IOException {
    String contentWithFrontmatter = "---\nwikidata_id: Q123\n---\n\nActual body text";
    ExportNoteRow note = new ExportNoteRow(3, null, "My Note", contentWithFrontmatter);

    byte[] zipBytes = buildZip(null, List.of(), List.of(note));

    Map<String, String> entries = readZipEntries(zipBytes);
    assertThat(
        entries.get("My Note.md"),
        equalTo("---\nwikidata_id: Q123\ndoughnut_id: 3\n---\n\n# My Note\n\nActual body text"));
  }

  @Test
  void rewritesResolvableWikiLinkToRelativeMarkdownAndAttachmentToAbsoluteUrl() throws IOException {
    ExportNoteRow source =
        new ExportNoteRow(
            1, null, "Source", "See [[Target Title]] and ![](/attachments/images/9/photo.png)");
    ExportNoteRow target = new ExportNoteRow(2, null, "Target Title", "Target body");

    byte[] zipBytes = buildZip(null, List.of(), List.of(source, target));

    Map<String, String> entries = readZipEntries(zipBytes);
    String sourceMd = entries.get("Source.md");

    assertThat(sourceMd, containsString("doughnut_id: 1"));
    assertThat(sourceMd, containsString("[Target Title](Target%20Title.md)"));
    assertThat(sourceMd, not(containsString("[[Target Title]]")));
    assertThat(sourceMd, containsString("http://localhost:9081/attachments/images/9/photo.png"));
    assertThat(
        entries.keySet().stream().anyMatch(k -> k.contains("attachments/images/")), equalTo(false));
  }

  @Test
  void leavesUnresolvedWikiAsDoubleBracketText() throws IOException {
    ExportNoteRow note =
        new ExportNoteRow(1, null, "Alone", "Missing [[Nowhere]] and [[Other Nb:Ghost]]");

    Map<String, String> entries = readZipEntries(buildZip(null, List.of(), List.of(note)));
    String md = entries.get("Alone.md");

    assertThat(md, containsString("[[Nowhere]]"));
    assertThat(md, containsString("[[Other Nb:Ghost]]"));
  }

  @Test
  void usesRelativePathFromNestedFolderToSibling() throws IOException {
    ExportFolderRow folderA = new ExportFolderRow(10, null, "FolderA", null);
    ExportFolderRow folderB = new ExportFolderRow(11, null, "FolderB", null);
    ExportNoteRow source = new ExportNoteRow(1, 10, "Source", "See [[Sibling]]");
    ExportNoteRow sibling = new ExportNoteRow(2, 11, "Sibling", "Hi");

    Map<String, String> entries =
        readZipEntries(buildZip(null, List.of(folderA, folderB), List.of(source, sibling)));
    String md = entries.get("FolderA/Source.md");

    assertThat(md, containsString("[Sibling](../FolderB/Sibling.md)"));
    assertThat(md, not(containsString("](/Folder")));
  }

  @Test
  void resolvesDuplicateTitlesViaLowestIdToCollisionSafeFilename() throws IOException {
    ExportNoteRow first = new ExportNoteRow(1, null, "Dup", "first");
    ExportNoteRow second = new ExportNoteRow(2, null, "Dup", "second");
    ExportNoteRow linker = new ExportNoteRow(3, null, "Linker", "See [[Dup]]");

    Map<String, String> entries =
        readZipEntries(buildZip(null, List.of(), List.of(first, second, linker)));

    assertThat(entries.containsKey("Dup.md"), equalTo(true));
    assertThat(entries.containsKey("Dup (2).md"), equalTo(true));
    assertThat(entries.get("Linker.md"), containsString("[Dup](Dup.md)"));
    assertThat(entries.get("Linker.md"), not(containsString("Dup%20(2)")));
  }
}
