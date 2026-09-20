package com.odde.donut.services.notebookExport;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;
import org.junit.jupiter.api.Test;

class PortableTreeSnapshotTest {

  private static final String README_FENCE = "---\ntype: Readme\n---\n";

  @Test
  void keepsCompleteFilenamesAndExactBytesOfRootAttachmentsSortedAfterNotes() {
    ExportFolderRow folder = new ExportFolderRow(10, null, "Recipes", null);
    ExportNoteRow note = new ExportNoteRow(null, "My Note", "body");
    byte[] invalidUtf8 = {(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0x80};

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(
            "# Notebook readme",
            List.of(folder),
            List.of(note, new ExportNoteRow(10, "Pasta", "Boil water")),
            List.of(
                new ExportAttachmentRow("diagram.png", invalidUtf8),
                new ExportAttachmentRow("empty.bin", new byte[0]),
                new ExportAttachmentRow("agents.json", "{\"a\": 1}".getBytes(UTF_8))));

    assertThat(
        entries,
        contains(
            ofText("README.md", README_FENCE + "# Notebook readme"),
            ofText("My Note.md", "body"),
            new PortableTreeEntry("agents.json", "{\"a\": 1}".getBytes(UTF_8)),
            new PortableTreeEntry("diagram.png", invalidUtf8),
            new PortableTreeEntry("empty.bin", new byte[0]),
            ofText("Recipes/Pasta.md", "Boil water")));
  }

  @Test
  void buildsOrderedEntriesMatchingZipOutputForNestedFoldersAndNotes() {
    ExportFolderRow parent = new ExportFolderRow(10, null, "Parent Folder", "Parent readme");
    ExportFolderRow child = new ExportFolderRow(11, 10, "Child Folder", null);
    ExportNoteRow noteInChild = new ExportNoteRow(11, "Nested note", "Nested body");

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(null, List.of(parent, child), List.of(noteInChild), List.of());

    assertThat(
        entries,
        contains(
            ofText("Parent Folder/README.md", README_FENCE + "Parent readme"),
            ofText("Parent Folder/Child Folder/Nested note.md", "Nested body")));
  }

  @Test
  void buildsRootReadmeAndNoteEntriesInOrder() {
    ExportNoteRow note = new ExportNoteRow(null, "My Note", "body");

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build("# Notebook readme", List.of(), List.of(note), List.of());

    assertThat(
        entries,
        contains(
            ofText("README.md", README_FENCE + "# Notebook readme"), ofText("My Note.md", "body")));
  }

  @Test
  void representsOnlyOtherwiseEmptyNonRootLeavesWithKeepFiles() {
    ExportFolderRow empty = new ExportFolderRow(10, null, "Empty", null);
    ExportFolderRow blankReadme = new ExportFolderRow(11, null, "Blank", " \n");
    ExportFolderRow readmeOnly = new ExportFolderRow(12, null, "Documented", "About");
    ExportFolderRow noteBearing = new ExportFolderRow(13, null, "Notes", null);
    ExportFolderRow nestedParent = new ExportFolderRow(14, null, "Parent", null);
    ExportFolderRow nestedLeaf = new ExportFolderRow(15, 14, "Leaf", null);

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(
            null,
            List.of(empty, blankReadme, readmeOnly, noteBearing, nestedParent, nestedLeaf),
            List.of(new ExportNoteRow(13, "Existing", "body")),
            List.of());

    assertThat(
        entries,
        contains(
            ofText("Empty/.keep", ""),
            ofText("Blank/.keep", ""),
            ofText("Documented/README.md", README_FENCE + "About"),
            ofText("Notes/Existing.md", "body"),
            ofText("Parent/Leaf/.keep", "")));
  }
}
