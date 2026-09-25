package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookTree.PortableTreeAttachmentRow;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import com.odde.donut.services.notebookTree.PortableTreeNoteRow;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.TreeFormatter;
import org.junit.jupiter.api.Test;

/**
 * The full assembly (every projection row as an insertion over an empty base) and a web commit's
 * tree derived from a projection change.
 */
class NotebookGitTreeEncoderTest {

  private static final String README_FENCE = "---\ntype: Readme\n---\n";
  private static final String POINTER_OID =
      "4d7a214614ab2935c943f9e0ff69d22eadbb8f32b1258daaa5e2ca24d17e2393";

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

  @Test
  void initializationIncludesLfsAttributesExemptingMarkdownAndStructuralMarkers() {
    byte[] pointer = NotebookGitLfsPointer.format(POINTER_OID, 12345);

    NotebookGitTreeContent tree =
        NotebookGitTreeEncoder.fullTree(
            null,
            List.of(),
            List.of(new PortableTreeNoteRow(null, "Note", "---\ntype: Note\n---\nbody")),
            List.of(new PortableTreeAttachmentRow(null, "diagram.png", pointer)),
            NotebookGitAttributes.initialMetadata());

    assertThat(
        tree.blobs().get(tree.blobIds().get(".gitattributes")),
        equalTo(NotebookGitAttributes.INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8)));
    assertThat(tree.blobs().get(tree.blobIds().get("diagram.png")), equalTo(pointer));
    assertThat(
        NotebookGitAttributes.INITIAL_CONTENT.contains("*.md !filter !diff !merge text"), is(true));
    assertThat(
        NotebookGitAttributes.INITIAL_CONTENT.contains(".keep !filter !diff !merge text"),
        is(true));
  }

  @Test
  void preservesAcceptedAttributesWithoutRegeneratingDefaults() {
    PortableTreeEntry authored =
        PortableTreeEntry.ofText(".gitattributes", "* filter=lfs -text\ncustom !filter\n");

    NotebookGitTreeContent tree =
        NotebookGitTreeEncoder.fullTree(
            null,
            List.of(),
            List.of(),
            List.of(new PortableTreeAttachmentRow(null, "empty.bin", new byte[0])),
            List.of(authored));

    assertThat(tree.blobs().get(tree.blobIds().get(".gitattributes")), equalTo(authored.content()));
    assertFalse(
        NotebookGitAttributes.INITIAL_CONTENT.equals(
            new String(
                tree.blobs().get(tree.blobIds().get(".gitattributes")), StandardCharsets.UTF_8)));
  }

  @Test
  void pointerLookingAttachmentBytesRemainExactGitContent() {
    byte[] pointerLooking = NotebookGitLfsPointer.format(POINTER_OID, 99);

    NotebookGitTreeContent tree =
        NotebookGitTreeEncoder.fullTree(
            null,
            List.of(),
            List.of(),
            List.of(new PortableTreeAttachmentRow(null, "legacy.bin", pointerLooking)));

    assertThat(tree.blobs().get(tree.blobIds().get("legacy.bin")), equalTo(pointerLooking));
  }

  @Test
  void derivingAChangeThatInsertsAnAttachmentPutsItsAcceptedContentAtItsPath() throws Exception {
    Folder physics = new Folder();
    physics.setName(new DisplayName("physics"));
    NotebookAttachment moon = new NotebookAttachment();
    moon.setFolder(physics);
    moon.setFilename("moon.jpg");
    byte[] pointer = NotebookGitLfsPointer.format(POINTER_OID, 12345);
    moon.setAcceptedGitContent(pointer);
    NotebookProjectionChange change = new NotebookProjectionChange();
    change.inserted.add(moon);

    try (InMemoryRepository repository =
            new InMemoryRepository(new DfsRepositoryDescription("accepted"));
        ObjectInserter inserter = repository.newObjectInserter()) {
      ObjectId emptyRoot = inserter.insert(new TreeFormatter());
      inserter.flush();

      NotebookGitTreeContent tree =
          new NotebookGitTreeEncoder(null, null, null, null).derive(change, repository, emptyRoot);

      assertThat(tree.blobs().get(tree.blobIds().get("physics/moon.jpg")), equalTo(pointer));
    }
  }

  private static Map<String, ObjectId> blobIdsOf(PortableTreeEntry... entries) {
    return NotebookGitTreeContent.of(List.of(entries)).blobIds();
  }
}
