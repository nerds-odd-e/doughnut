package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.services.notebookTree.PortableTreeAttachmentRow;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import com.odde.donut.services.notebookTree.PortableTreeNoteRow;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

/** The full assembly: every projection row as an insertion over an empty base. */
class NotebookGitTreeEncoderTest {

  private static final String README_FENCE = "---\ntype: Readme\n---\n";

  @Test
  void placesReadmesNotesAndAttachmentsWithExactBytesUnderTheirFolderPaths() {
    PortableTreeFolderRow parent = new PortableTreeFolderRow(10, null, "Parent", "Parent readme");
    PortableTreeFolderRow child = new PortableTreeFolderRow(11, 10, "Child", null);
    byte[] invalidUtf8 = {(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0x80};

    NotebookGitTreeContent tree =
        NotebookGitTreeEncoder.fullTree(
            "# Notebook readme",
            List.of(parent, child),
            List.of(
                new PortableTreeNoteRow(null, "My Note", "body"),
                new PortableTreeNoteRow(11, "Nested note", null)),
            List.of(
                new PortableTreeAttachmentRow(null, "diagram.png", invalidUtf8),
                new PortableTreeAttachmentRow(10, "empty.bin", new byte[0])));

    assertThat(
        tree.blobIds(),
        equalTo(
            blobIdsOf(
                ofText("README.md", README_FENCE + "# Notebook readme"),
                ofText("My Note.md", "body"),
                new PortableTreeEntry("diagram.png", invalidUtf8),
                ofText("Parent/README.md", README_FENCE + "Parent readme"),
                new PortableTreeEntry("Parent/empty.bin", new byte[0]),
                ofText("Parent/Child/Nested note.md", ""))));
    assertThat(tree.blobs().get(tree.blobIds().get("diagram.png")), equalTo(invalidUtf8));
  }

  @Test
  void representsOnlyOtherwiseEmptyNonRootLeavesWithKeepFiles() {
    PortableTreeFolderRow empty = new PortableTreeFolderRow(10, null, "Empty", null);
    PortableTreeFolderRow blankReadme = new PortableTreeFolderRow(11, null, "Blank", " \n");
    PortableTreeFolderRow readmeOnly = new PortableTreeFolderRow(12, null, "Documented", "About");
    PortableTreeFolderRow noteBearing = new PortableTreeFolderRow(13, null, "Notes", null);
    PortableTreeFolderRow nestedParent = new PortableTreeFolderRow(14, null, "Parent", null);
    PortableTreeFolderRow nestedLeaf = new PortableTreeFolderRow(15, 14, "Leaf", null);

    NotebookGitTreeContent tree =
        NotebookGitTreeEncoder.fullTree(
            null,
            List.of(empty, blankReadme, readmeOnly, noteBearing, nestedParent, nestedLeaf),
            List.of(new PortableTreeNoteRow(13, "Existing", "body")),
            List.of());

    assertThat(
        tree.blobIds(),
        equalTo(
            blobIdsOf(
                ofText("Empty/.keep", ""),
                ofText("Blank/.keep", ""),
                ofText("Documented/README.md", README_FENCE + "About"),
                ofText("Notes/Existing.md", "body"),
                ofText("Parent/Leaf/.keep", ""))));
  }

  private static Map<String, ObjectId> blobIdsOf(PortableTreeEntry... entries) {
    return NotebookGitTreeContent.of(List.of(entries)).blobIds();
  }
}
