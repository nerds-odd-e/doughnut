package com.odde.donut.controllers;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/** Verifies that notebook Git proposals match the accepted head and its required ancestry. */
class NotebookGitProposalAncestryControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal.\n";
  private static final String FIRST_EDIT = "---\ntype: Note\n---\nFirst edit.\n";
  private static final String SECOND_EDIT = "---\ntype: Note\n---\nSecond edit.\n";
  private static final String TEMPORARY_INITIAL = "---\ntype: Note\n---\nTemporary initial.\n";
  private static final String TEMPORARY_EDITED = "---\ntype: Note\n---\nTemporary edited.\n";
  private static final String SURVIVING_FINAL = "---\ntype: Note\n---\nSurviving final.\n";

  @Autowired NoteController noteController;

  private record ContentEditRange(ObjectId firstEdit, ObjectId secondEdit, byte[] proposalBytes) {}

  private record TemporaryNoteRange(
      ObjectId afterAdd, ObjectId afterEditTemporary, ObjectId tip, byte[] proposalBytes) {}

  @Test
  void rejectsStaleExpectedHeadWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        "0000000000000000000000000000000000000000",
        singleParentChildBundleBytes(notebook),
        HttpStatus.CONFLICT);
  }

  @Test
  void rejectsProposalWithNoParentOrUnrelatedHistoryWithoutMutatingTheAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        validProposalBundleBytes(),
        HttpStatus.CONFLICT);
  }

  @Test
  void rejectsMergeCommitProposalWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        mergeCommitBundleBytes(notebook),
        HttpStatus.CONFLICT);
  }

  @Test
  void acceptsSeveralContentEditCommitsAheadOfAcceptedHeadWithOriginalHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("Topic").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ContentEditRange range = contentEditRangeOn(acceptedBundleBytes(notebook), acceptedHead);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.secondEdit().getName()));
    NoteRealm view = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(view.getId(), equalTo(note.getId()));
    assertThat(view.getNote().getContent(), equalTo(SECOND_EDIT));

    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(accepted)) {
      ObjectId head = GitBundleTestReader.fetchHead(accepted, downloaded);
      RevCommit tip = walk.parseCommit(head);
      assertThat(tip.getId(), equalTo(range.secondEdit()));
      assertThat(tip.getParentCount(), equalTo(1));
      assertThat(tip.getParent(0), equalTo(range.firstEdit()));
      RevCommit middle = walk.parseCommit(tip.getParent(0));
      assertThat(middle.getParentCount(), equalTo(1));
      assertThat(middle.getParent(0), equalTo(acceptedHead));
    }
  }

  @Test
  void acceptsContentEditRangeThatRestoresAcceptedTreeWithNewHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("Topic").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ContentEditRange range =
        editThenRestoreRangeOn(acceptedBundleBytes(notebook), acceptedHead, ORIGINAL_CONTENT);

    ObjectId acceptedTree;
    ObjectId restoredTree;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(proposal)) {
      GitBundleTestReader.fetchHead(proposal, range.proposalBytes());
      acceptedTree = walk.parseCommit(acceptedHead).getTree().getId();
      restoredTree = walk.parseCommit(range.secondEdit()).getTree().getId();
    }
    assertThat(restoredTree, equalTo(acceptedTree));
    assertThat(range.secondEdit().equals(acceptedHead), equalTo(false));

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.secondEdit().getName()));
    NoteRealm view = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(view.getId(), equalTo(note.getId()));
    assertThat(view.getNote().getContent(), equalTo(ORIGINAL_CONTENT));

    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(accepted)) {
      ObjectId head = GitBundleTestReader.fetchHead(accepted, downloaded);
      RevCommit tip = walk.parseCommit(head);
      assertThat(tip.getId(), equalTo(range.secondEdit()));
      assertThat(tip.getTree().getId(), equalTo(acceptedTree));
      assertThat(tip.getId().equals(acceptedHead), equalTo(false));
      assertThat(tip.getParentCount(), equalTo(1));
      assertThat(tip.getParent(0), equalTo(range.firstEdit()));
      RevCommit middle = walk.parseCommit(tip.getParent(0));
      assertThat(middle.getParentCount(), equalTo(1));
      assertThat(middle.getParent(0), equalTo(acceptedHead));
    }
  }

  @Test
  void publishesRangeThatAddsEditsAndRemovesTemporaryNoteLeavingOnlySurvivingEdit()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note surviving =
        makeMe.aNote().notebook(notebook).title("Surviving").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    TemporaryNoteRange range =
        temporaryNoteRemovedRangeOn(acceptedBundleBytes(notebook), acceptedHead);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.tip().getName()));
    List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(storedNotes, hasSize(1));
    assertThat(storedNotes.getFirst().getId(), equalTo(surviving.getId()));
    assertThat(storedNotes.getFirst().getTitle(), equalTo("Surviving"));
    assertThat(storedNotes.getFirst().getContent(), equalTo(SURVIVING_FINAL));

    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(accepted)) {
      ObjectId head = GitBundleTestReader.fetchHead(accepted, downloaded);
      RevCommit tip = walk.parseCommit(head);
      assertThat(tip.getId(), equalTo(range.tip()));
      assertThat(
          GitBundleTestReader.pathsIn(accepted, range.tip()), equalTo(List.of("Surviving.md")));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, range.tip(), "Surviving.md"),
          equalTo(SURVIVING_FINAL));
      assertThat(tip.getParentCount(), equalTo(1));
      assertThat(tip.getParent(0), equalTo(range.afterEditTemporary()));

      RevCommit editTemporary = walk.parseCommit(tip.getParent(0));
      assertThat(
          GitBundleTestReader.pathsIn(accepted, range.afterEditTemporary()),
          containsInAnyOrder("Surviving.md", "Temporary.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              accepted, range.afterEditTemporary(), "Temporary.md"),
          equalTo(TEMPORARY_EDITED));
      assertThat(editTemporary.getParentCount(), equalTo(1));
      assertThat(editTemporary.getParent(0), equalTo(range.afterAdd()));

      RevCommit addTemporary = walk.parseCommit(editTemporary.getParent(0));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, range.afterAdd(), "Temporary.md"),
          equalTo(TEMPORARY_INITIAL));
      assertThat(addTemporary.getParentCount(), equalTo(1));
      assertThat(addTemporary.getParent(0), equalTo(acceptedHead));
    }
  }

  @Test
  void acceptsSingleParentRangeWhenAMergeExistsBelowTheAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("Topic").content(ORIGINAL_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    ObjectId acceptedWithMergeBelow;
    byte[] acceptedWithMergeBelowBundle;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId priorAccepted =
          GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId otherParent =
          commitOnTopOf(repository, List.of(), "other.md", "other content", "Other root commit");
      acceptedWithMergeBelow =
          commitOnTopOf(
              repository,
              List.of(priorAccepted, otherParent),
              List.of(new NotebookGitProposalFile("Topic.md", ORIGINAL_CONTENT)),
              "Merge below accepted tip");
      acceptedWithMergeBelowBundle = bundleBytesForHead(repository, acceptedWithMergeBelow);
      seedAcceptedHistory(notebook, repository, acceptedWithMergeBelow);
    }

    ContentEditRange range =
        contentEditRangeOn(acceptedWithMergeBelowBundle, acceptedWithMergeBelow);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), acceptedWithMergeBelow.getName(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.secondEdit().getName()));
  }

  /** A well-formed, parentless root commit bundle unrelated to any notebook's accepted history. */
  private byte[] validProposalBundleBytes() {
    List<PortableTreeEntry> entries = List.of(ofText("README.md", "proposal"));
    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "Proposer", "proposer@example.com", "Proposal", Instant.now())) {
      return NotebookGitBundleWriter.write(repository).bundleBytes();
    }
  }

  /** A bundle whose {@code main} is a genuine single-parent child of the accepted head. */
  private byte[] singleParentChildBundleBytes(Notebook notebook) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead =
          GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId childCommit =
          commitOnTopOf(
              repository, List.of(acceptedHead), "proposal.md", "proposal content", "Proposal");
      return bundleBytesForHead(repository, childCommit);
    }
  }

  /**
   * A bundle whose {@code main} is a merge commit with two parents, one of which is the accepted
   * head - still rejected, because ancestry here requires a single-parent tip.
   */
  private byte[] mergeCommitBundleBytes(Notebook notebook) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead =
          GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId otherParent =
          commitOnTopOf(repository, List.of(), "other.md", "other content", "Other root commit");
      ObjectId mergeCommit =
          commitOnTopOf(
              repository,
              List.of(acceptedHead, otherParent),
              "merged.md",
              "merged content",
              "Merge commit");
      return bundleBytesForHead(repository, mergeCommit);
    }
  }

  private ContentEditRange contentEditRangeOn(byte[] baseBundleBytes, ObjectId baseHead)
      throws Exception {
    return contentEditRangeOn(baseBundleBytes, baseHead, FIRST_EDIT, SECOND_EDIT);
  }

  private ContentEditRange editThenRestoreRangeOn(
      byte[] baseBundleBytes, ObjectId baseHead, String restoredContent) throws Exception {
    return contentEditRangeOn(baseBundleBytes, baseHead, FIRST_EDIT, restoredContent);
  }

  private ContentEditRange contentEditRangeOn(
      byte[] baseBundleBytes, ObjectId baseHead, String firstContent, String secondContent)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, baseBundleBytes);
      ObjectId firstEdit =
          commitOnTopOf(
              repository,
              List.of(baseHead),
              List.of(new NotebookGitProposalFile("Topic.md", firstContent)),
              "First edit");
      ObjectId secondEdit =
          commitOnTopOf(
              repository,
              List.of(firstEdit),
              List.of(new NotebookGitProposalFile("Topic.md", secondContent)),
              "Second edit");
      return new ContentEditRange(
          firstEdit, secondEdit, bundleBytesForHead(repository, secondEdit));
    }
  }

  private TemporaryNoteRange temporaryNoteRemovedRangeOn(byte[] baseBundleBytes, ObjectId baseHead)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, baseBundleBytes);
      ObjectId afterAdd =
          commitOnTopOf(
              repository,
              List.of(baseHead),
              List.of(
                  new NotebookGitProposalFile("Surviving.md", ORIGINAL_CONTENT),
                  new NotebookGitProposalFile("Temporary.md", TEMPORARY_INITIAL)),
              "Add temporary note");
      ObjectId afterEditTemporary =
          commitOnTopOf(
              repository,
              List.of(afterAdd),
              List.of(
                  new NotebookGitProposalFile("Surviving.md", ORIGINAL_CONTENT),
                  new NotebookGitProposalFile("Temporary.md", TEMPORARY_EDITED)),
              "Edit temporary note");
      ObjectId tip =
          commitOnTopOf(
              repository,
              List.of(afterEditTemporary),
              List.of(new NotebookGitProposalFile("Surviving.md", SURVIVING_FINAL)),
              "Remove temporary and edit surviving");
      return new TemporaryNoteRange(
          afterAdd, afterEditTemporary, tip, bundleBytesForHead(repository, tip));
    }
  }
}
