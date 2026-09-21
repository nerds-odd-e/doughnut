package com.odde.donut.services.notebookTree;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;
import org.junit.jupiter.api.Test;

class PortableTreeSnapshotTest {

  private static final String README_FENCE = "---\ntype: Readme\n---\n";

  @Test
  void keepsCompleteFilenamesAndExactBytesOfRootAttachmentsSortedAfterNotes() {
    PortableTreeFolderRow folder = new PortableTreeFolderRow(10, null, "Recipes", null);
    PortableTreeNoteRow note = new PortableTreeNoteRow(null, "My Note", "body");
    byte[] invalidUtf8 = {(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0x80};

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(
            "# Notebook readme",
            List.of(folder),
            List.of(note, new PortableTreeNoteRow(10, "Pasta", "Boil water")),
            List.of(
                new PortableTreeAttachmentRow("diagram.png", invalidUtf8),
                new PortableTreeAttachmentRow("empty.bin", new byte[0]),
                new PortableTreeAttachmentRow("agents.json", "{\"a\": 1}".getBytes(UTF_8))));

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
    PortableTreeFolderRow parent =
        new PortableTreeFolderRow(10, null, "Parent Folder", "Parent readme");
    PortableTreeFolderRow child = new PortableTreeFolderRow(11, 10, "Child Folder", null);
    PortableTreeNoteRow noteInChild = new PortableTreeNoteRow(11, "Nested note", "Nested body");

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
    PortableTreeNoteRow note = new PortableTreeNoteRow(null, "My Note", "body");

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build("# Notebook readme", List.of(), List.of(note), List.of());

    assertThat(
        entries,
        contains(
            ofText("README.md", README_FENCE + "# Notebook readme"), ofText("My Note.md", "body")));
  }

  @Test
  void representsOnlyOtherwiseEmptyNonRootLeavesWithKeepFiles() {
    PortableTreeFolderRow empty = new PortableTreeFolderRow(10, null, "Empty", null);
    PortableTreeFolderRow blankReadme = new PortableTreeFolderRow(11, null, "Blank", " \n");
    PortableTreeFolderRow readmeOnly = new PortableTreeFolderRow(12, null, "Documented", "About");
    PortableTreeFolderRow noteBearing = new PortableTreeFolderRow(13, null, "Notes", null);
    PortableTreeFolderRow nestedParent = new PortableTreeFolderRow(14, null, "Parent", null);
    PortableTreeFolderRow nestedLeaf = new PortableTreeFolderRow(15, 14, "Leaf", null);

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(
            null,
            List.of(empty, blankReadme, readmeOnly, noteBearing, nestedParent, nestedLeaf),
            List.of(new PortableTreeNoteRow(13, "Existing", "body")),
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
