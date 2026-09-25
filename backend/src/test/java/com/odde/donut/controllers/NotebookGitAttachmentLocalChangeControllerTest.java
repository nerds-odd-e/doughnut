package com.odde.donut.controllers;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedTip;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitAttachmentLocalChangeControllerTest extends NotebookGitControllerTestBase {

  private static final String NOTE_MARKDOWN = "---\ntype: Note\n---\naccepted content";
  private static final String REFERENCE_JSON = "{\"schema\": \"donut\"}\n";
  private static final String CHANGED_REFERENCE_JSON = "{\"schema\": \"changed\"}\n";
  // Deliberately not valid UTF-8, so nothing may decode these bytes on the way in or out.
  private static final byte[] DIAGRAM_BYTES = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

  private static final PortableTreeEntry NOTE = ofText("Root Note.md", NOTE_MARKDOWN);
  private static final PortableTreeEntry REFERENCE = ofText("reference.json", REFERENCE_JSON);
  private static final PortableTreeEntry DIAGRAM =
      new PortableTreeEntry("Diagram.png", DIAGRAM_BYTES);
  // Identical bytes under a second name: a no-op-byte guard must not conflate the two.
  private static final PortableTreeEntry TWIN = new PortableTreeEntry("copy.png", DIAGRAM_BYTES);

  /** Git tree order is byte-wise by path, so capitals sort before lowercase. */
  private static final List<PortableTreeEntry> BASELINE = List.of(DIAGRAM, NOTE, TWIN, REFERENCE);

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired FolderRepository folderRepository;

  private static Stream<FileOnlyChange> fileOnlyChanges() {
    return Stream.of(
        new FileOnlyChange(
            "edits one file's bytes",
            List.of(DIAGRAM, NOTE, TWIN, ofText("reference.json", CHANGED_REFERENCE_JSON))),
        new FileOnlyChange(
            "renames one file, keeping its bytes",
            List.of(DIAGRAM, NOTE, ofText("catalog.json", REFERENCE_JSON), TWIN)),
        new FileOnlyChange("removes one file", List.of(DIAGRAM, NOTE, TWIN)),
        new FileOnlyChange(
            "renames one of two identical-byte files",
            List.of(new PortableTreeEntry("Figure.png", DIAGRAM_BYTES), NOTE, TWIN, REFERENCE)));
  }

  @ParameterizedTest
  @MethodSource("fileOnlyChanges")
  void aFileOnlyProposalBecomesExactlyItsFinalRootFileSet(FileOnlyChange change) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = publishBaseline(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(accepted, asProposed(notebook, change.finalTree())));

    assertFinalRootFileSet(notebook, acceptedTip(notebook), change.finalTree());
  }

  @Test
  void aMultiCommitRenameAndEditRangeLandsAsItsFinalSetOnItsOriginalAncestry() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = publishBaseline(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    List<PortableTreeEntry> finalTree =
        List.of(DIAGRAM, NOTE, ofText("catalog.json", CHANGED_REFERENCE_JSON));
    ObjectId firstRename;
    ObjectId edit;
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      firstRename =
          localCommitOnTopOf(
              repository,
              acceptedHead,
              asProposed(
                  notebook, List.of(DIAGRAM, NOTE, TWIN, ofText("interim.json", REFERENCE_JSON))),
              "Rename the reference file");
      edit =
          localCommitOnTopOf(
              repository,
              firstRename,
              asProposed(
                  notebook,
                  List.of(DIAGRAM, NOTE, TWIN, ofText("interim.json", CHANGED_REFERENCE_JSON))),
              "Edit the renamed file");
      tip =
          localCommitOnTopOf(
              repository,
              edit,
              asProposed(notebook, finalTree),
              "Rename again and drop the identical-byte copy");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), accepted.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    AcceptedTip published = acceptedTip(notebook);
    assertThat(published.head(), equalTo(tip));
    assertThat(published.ancestry().subList(0, 3), contains(edit, firstRename, acceptedHead));
    assertFinalRootFileSet(notebook, published, finalTree);
    // The range's intermediate name was never a live Attachment: commits are not replayed.
    assertThat(committedRootAttachmentNames(notebook), not(hasItem("interim.json")));
  }

  @Test
  void nestedFileChangesBecomeExactlyTheirFinalSetAndDissolveEmptyFolders() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    List<PortableTreeEntry> nested =
        asCommitted(
            notebook,
            List.of(
                new PortableTreeEntry("physics/diagrams/force.png", DIAGRAM_BYTES),
                ofText("tools/cache/reference.json", REFERENCE_JSON)));
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(empty, NotebookGitProposalFile.asProposal(nested)));

    List<PortableTreeEntry> editedAndRenamed =
        asCommitted(
            notebook,
            List.of(
                new PortableTreeEntry("physics/diagrams/free-body.png", DIAGRAM_BYTES),
                ofText("tools/cache/reference.json", CHANGED_REFERENCE_JSON)));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    controller.publishNotebookGitProposal(
        notebook.getId(),
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(accepted, NotebookGitProposalFile.asProposal(editedAndRenamed)));

    assertThat(acceptedTip(notebook).content(), equalTo(editedAndRenamed));
    assertThat(committedAttachmentTree(notebook), equalTo(editedAndRenamed));

    List<PortableTreeEntry> referenceRemoved = List.of(editedAndRenamed.getFirst());
    accepted = reloadCommittedBinding(notebook.getId());
    controller.publishNotebookGitProposal(
        notebook.getId(),
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(accepted, NotebookGitProposalFile.asProposal(referenceRemoved)));

    assertThat(acceptedTip(notebook).content(), equalTo(referenceRemoved));
    assertThat(committedAttachmentTree(notebook), equalTo(referenceRemoved));
    assertThat(
        NotebookLiveProjectionTestReader.folderPaths(
            transactionManager, folderRepository, notebook.getId()),
        equalTo(List.of("physics", "physics/diagrams")));
  }

  private NotebookGitBinding publishBaseline(Notebook notebook) throws Exception {
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(empty, asProposed(notebook, BASELINE)));
    assertThat(acceptedTip(notebook).content(), equalTo(asCommitted(notebook, BASELINE)));
    return reloadCommittedBinding(notebook.getId());
  }

  /** The accepted tip and live projection hold exactly the final Attachment set. */
  private void assertFinalRootFileSet(
      Notebook notebook, AcceptedTip published, List<PortableTreeEntry> finalTree)
      throws IOException {
    List<PortableTreeEntry> committed = asCommitted(notebook, finalTree);
    assertThat(published.content(), equalTo(committed));
    assertThat(
        committedRootAttachments(notebook),
        equalTo(committed.stream().filter(NotebookGitControllerTestBase::isAttachment).toList()));

    List<String> surviving = finalTree.stream().map(PortableTreeEntry::path).toList();
    List<String> removed =
        BASELINE.stream()
            .map(PortableTreeEntry::path)
            .filter(name -> !surviving.contains(name))
            .toList();
    assertThat(
        published.exactTree().stream()
            .map(PortableTreeEntry::path)
            .filter(removed::contains)
            .toList(),
        is(empty()));
    assertThat(
        committedRootAttachmentNames(notebook).stream().filter(removed::contains).toList(),
        is(empty()));

    assertThat(
        NotebookLiveProjectionTestReader.noteTitles(
            transactionManager, noteRepository, notebook.getId()),
        contains("Root Note"));
    assertThat(
        NotebookLiveProjectionTestReader.memoryTrackerCount(entityManager, notebook.getId()),
        is(0L));
  }

  private List<PortableTreeEntry> committedRootAttachments(Notebook notebook) {
    return NotebookLiveProjectionTestReader.rootAttachments(
        transactionManager, notebookAttachmentRepository, notebook.getId());
  }

  private List<String> committedRootAttachmentNames(Notebook notebook) {
    return committedRootAttachments(notebook).stream().map(PortableTreeEntry::path).toList();
  }

  private List<PortableTreeEntry> committedAttachmentTree(Notebook notebook) {
    return NotebookLiveProjectionTestReader.attachmentTree(
        transactionManager, notebookAttachmentRepository, folderRepository, notebook.getId());
  }

  /** {@code tree} as a local LFS checkout stages it. */
  private List<NotebookGitProposalFile> asProposed(Notebook notebook, List<PortableTreeEntry> tree)
      throws IOException {
    return NotebookGitProposalFile.asProposal(asCommitted(notebook, tree));
  }

  private AcceptedTip acceptedTip(Notebook notebook) throws Exception {
    return GitBundleTestReader.fetchAcceptedTip(acceptedBundleBytes(notebook));
  }

  /** One local file-only range, named by the lifecycle operation it performs on the baseline. */
  private record FileOnlyChange(String name, List<PortableTreeEntry> finalTree) {
    @Override
    public String toString() {
      return name;
    }
  }
}
