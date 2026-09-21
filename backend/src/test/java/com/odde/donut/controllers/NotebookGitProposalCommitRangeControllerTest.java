package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies that a multi-commit proposal publishes its tip with the proposal's own history. */
class NotebookGitProposalCommitRangeControllerTest extends NotebookGitBundleControllerTestBase {

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
  void acceptsSeveralContentEditCommitsAheadOfAcceptedHeadWithOriginalHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("Topic").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ContentEditRange range =
        contentEditRangeOn(acceptedBundleBytes(notebook), acceptedHead, SECOND_EDIT);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.secondEdit().getName()));
    NoteRealm view = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(view.getId(), equalTo(note.getId()));
    assertThat(view.getNote().getContent(), equalTo(SECOND_EDIT));
    assertDownloadedHistoryIsRangeOn(notebook, range, acceptedHead);
  }

  @Test
  void acceptsContentEditRangeThatRestoresAcceptedTreeWithNewHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("Topic").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ContentEditRange range =
        contentEditRangeOn(acceptedBundleBytes(notebook), acceptedHead, ORIGINAL_CONTENT);

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
    assertDownloadedHistoryIsRangeOn(notebook, range, acceptedHead);
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
        contentEditRangeOn(acceptedWithMergeBelowBundle, acceptedWithMergeBelow, SECOND_EDIT);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), acceptedWithMergeBelow.getName(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.secondEdit().getName()));
  }

  /** The downloaded accepted history is the range's two edits, in order, on top of the base. */
  private void assertDownloadedHistoryIsRangeOn(
      Notebook notebook, ContentEditRange range, ObjectId baseHead) throws Exception {
    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(accepted)) {
      RevCommit tip = walk.parseCommit(GitBundleTestReader.fetchHead(accepted, downloaded));
      assertThat(tip.getId(), equalTo(range.secondEdit()));
      assertThat(tip.getParentCount(), equalTo(1));
      assertThat(tip.getParent(0), equalTo(range.firstEdit()));
      RevCommit middle = walk.parseCommit(tip.getParent(0));
      assertThat(middle.getParentCount(), equalTo(1));
      assertThat(middle.getParent(0), equalTo(baseHead));
    }
  }

  /** Edits {@code Topic.md} to {@link #FIRST_EDIT}, then to {@code secondContent}. */
  private ContentEditRange contentEditRangeOn(
      byte[] baseBundleBytes, ObjectId baseHead, String secondContent) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, baseBundleBytes);
      ObjectId firstEdit =
          commitOnTopOf(
              repository,
              List.of(baseHead),
              List.of(new NotebookGitProposalFile("Topic.md", FIRST_EDIT)),
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
