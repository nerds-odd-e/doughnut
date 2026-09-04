package com.odde.donut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;
import org.junit.jupiter.api.Test;

class PortableTreeSnapshotTest {

  private static final String README_FENCE = "---\ntype: Readme\n---\n";

  @Test
  void buildsOrderedEntriesMatchingZipOutputForNestedFoldersAndNotes() {
    ExportFolderRow parent = new ExportFolderRow(10, null, "Parent Folder", "Parent readme");
    ExportFolderRow child = new ExportFolderRow(11, 10, "Child Folder", null);
    ExportNoteRow noteInChild = new ExportNoteRow(11, "Nested note", "Nested body");

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(null, List.of(parent, child), List.of(noteInChild));

    assertThat(
        entries,
        contains(
            new PortableTreeEntry("Parent Folder/README.md", README_FENCE + "Parent readme"),
            new PortableTreeEntry("Parent Folder/Child Folder/Nested note.md", "Nested body")));
  }

  @Test
  void buildsRootReadmeAndNoteEntriesInOrder() {
    ExportNoteRow note = new ExportNoteRow(null, "My Note", "body");

    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build("# Notebook readme", List.of(), List.of(note));

    assertThat(
        entries,
        contains(
            new PortableTreeEntry("README.md", README_FENCE + "# Notebook readme"),
            new PortableTreeEntry("My Note.md", "body")));
  }
}
