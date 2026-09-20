package com.odde.donut.controllers;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A local file-only range publishes exactly its final root-file set, with no Markdown edit and no
 * Attachment rename correspondence. Edit, rename and removal share that one rule, so a name the
 * range left behind is absent from both the accepted tip and the live projection, and a
 * multi-commit range lands as its final set instead of being replayed as live mutations.
 */
class NotebookGitRootAttachmentLocalChangeControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String NOTE_MARKDOWN = "---\ntype: Note\n---\naccepted content";
  private static final String REFERENCE_JSON = "{\"schema\": \"donut\"}\n";
  private static final String CHANGED_REFERENCE_JSON = "{\"schema\": \"changed\"}\n";
  // Deliberately not valid UTF-8, so nothing may decode these bytes on the way in or out.
  private static final byte[] DIAGRAM_BYTES = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

  private static final PortableTreeEntry NOTE = ofText("Root Note.md", NOTE_MARKDOWN);
  private static final PortableTreeEntry REFERENCE = ofText("reference.json", REFERENCE_JSON);
  private static final PortableTreeEntry DIAGRAM =
      new PortableTreeEntry("Diagram.png", DIAGRAM_BYTES);
  // Identical bytes under a second name: a no-op-byte guard must not let one stand in for the
  // other.
  private static final PortableTreeEntry TWIN = new PortableTreeEntry("copy.png", DIAGRAM_BYTES);

  /** Git tree order is byte-wise by path, so capitals sort before lowercase. */
  private static final List<PortableTreeEntry> BASELINE = List.of(DIAGRAM, NOTE, TWIN, REFERENCE);

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;

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
        proposalBundleBytes(accepted, NotebookGitProposalFile.asProposal(change.finalTree())));

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
      GitBundleTestReader.fetchHead(repository, accepted.getBundleBytes());
      firstRename =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              NotebookGitProposalFile.asProposal(
                  List.of(DIAGRAM, NOTE, TWIN, ofText("interim.json", REFERENCE_JSON))),
              "Rename the reference file");
      edit =
          commitOnTopOf(
              repository,
              List.of(firstRename),
              NotebookGitProposalFile.asProposal(
                  List.of(DIAGRAM, NOTE, TWIN, ofText("interim.json", CHANGED_REFERENCE_JSON))),
              "Edit the renamed file");
      tip =
          commitOnTopOf(
              repository,
              List.of(edit),
              NotebookGitProposalFile.asProposal(finalTree),
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

  /** The accepted starting point: one note beside three root files, two of them byte-identical. */
  private NotebookGitBinding publishBaseline(Notebook notebook) throws Exception {
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(empty, NotebookGitProposalFile.asProposal(BASELINE)));
    assertThat(acceptedTip(notebook).entries(), equalTo(BASELINE));
    return reloadCommittedBinding(notebook.getId());
  }

  /**
   * The accepted tip and the live projection both hold exactly {@code finalTree}; every baseline
   * name the range left behind is gone from both; and the Attachments created no note identity or
   * learning data of their own.
   */
  private void assertFinalRootFileSet(
      Notebook notebook, AcceptedTip published, List<PortableTreeEntry> finalTree) {
    assertThat(published.entries(), equalTo(finalTree));
    assertThat(
        committedRootAttachments(notebook),
        equalTo(finalTree.stream().filter(entry -> !entry.path().endsWith(".md")).toList()));

    List<String> surviving = finalTree.stream().map(PortableTreeEntry::path).toList();
    List<String> removed =
        BASELINE.stream()
            .map(PortableTreeEntry::path)
            .filter(name -> !surviving.contains(name))
            .toList();
    assertThat(
        published.entries().stream()
            .map(PortableTreeEntry::path)
            .filter(removed::contains)
            .toList(),
        is(empty()));
    assertThat(
        committedRootAttachmentNames(notebook).stream().filter(removed::contains).toList(),
        is(empty()));

    assertThat(committedNoteTitles(notebook), contains("Root Note"));
    assertThat(countMemoryTrackersForNotebook(notebook.getId()), is(0L));
  }

  private List<PortableTreeEntry> committedRootAttachments(Notebook notebook) {
    return NotebookLiveProjectionTestReader.rootAttachments(
        transactionManager, notebookAttachmentRepository, notebook.getId());
  }

  private List<String> committedRootAttachmentNames(Notebook notebook) {
    return committedRootAttachments(notebook).stream().map(PortableTreeEntry::path).toList();
  }

  private List<String> committedNoteTitles(Notebook notebook) {
    return NotebookLiveProjectionTestReader.noteTitles(
        transactionManager, noteRepository, notebook.getId());
  }

  private long countMemoryTrackersForNotebook(Integer notebookId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM memory_tracker mt "
                        + "JOIN note n ON mt.note_id = n.id WHERE n.notebook_id = :notebookId")
                .setParameter("notebookId", notebookId)
                .getSingleResult())
        .longValue();
  }

  private AcceptedTip acceptedTip(Notebook notebook) throws Exception {
    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit head = revWalk.parseCommit(GitBundleTestReader.fetchHead(repository, downloaded));
      List<ObjectId> ancestry = new ArrayList<>();
      for (RevCommit walked = head; walked.getParentCount() > 0; ) {
        walked = revWalk.parseCommit(walked.getParent(0));
        ancestry.add(walked.getId());
      }
      return new AcceptedTip(
          head.getId(), ancestry, GitBundleTestReader.readTreeEntries(repository, head));
    }
  }

  private record AcceptedTip(
      ObjectId head, List<ObjectId> ancestry, List<PortableTreeEntry> entries) {}

  /** One local file-only range, named by the lifecycle operation it performs on the baseline. */
  private record FileOnlyChange(String name, List<PortableTreeEntry> finalTree) {
    @Override
    public String toString() {
      return name;
    }
  }
}
