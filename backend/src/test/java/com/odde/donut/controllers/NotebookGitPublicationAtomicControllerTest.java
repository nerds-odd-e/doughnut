package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NotebookGitPublicationAtomicControllerTest extends NotebookGitControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\n[[new reference]]";

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackMixedNotesAndAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String existingContent = "---\ntype: Note\n---\n[[original reference]]";
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(existingContent).please();
    authorReferencingContentCommitted(existing, existingContent);
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    var acceptedHistoryBefore = acceptedHistory(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    Timestamp bindingUpdatedAt = binding.getUpdatedAt();
    Timestamp noteUpdatedAt =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(existing.getId()).orElseThrow().getUpdatedAt());
    List<AuthoredNoteReferenceRow> originalReferences =
        inCommittedTransaction(transactionManager, () -> rowsFor(entityManager, existing));
    assertThat(originalReferences, hasSize(1));
    long originalCreatorCount =
        inCommittedTransaction(
            transactionManager,
            () -> countRowsForNotebook("note_creator", "note_id", notebook.getId()));
    long originalReferenceCount =
        inCommittedTransaction(
            transactionManager,
            () ->
                countRowsForNotebook(
                    "authored_note_reference", "source_note_id", notebook.getId()));
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Created Note.md", PROPOSED_CONTENT),
                new NotebookGitProposalFile("Existing.md", PROPOSED_CONTENT),
                new NotebookGitProposalFile("Second Note.md", PROPOSED_CONTENT)));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, proposal));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(1));
          Note reloadedNote = noteRepository.findById(existing.getId()).orElseThrow();
          assertThat(reloadedNote.getContent(), is(existingContent));
          assertThat(reloadedNote.getUpdatedAt(), is(noteUpdatedAt));
          List<AuthoredNoteReferenceRow> remainingReferences = rowsFor(entityManager, reloadedNote);
          assertThat(remainingReferences, equalTo(originalReferences));
          assertThat(
              remainingReferences.stream()
                  .map(AuthoredNoteReferenceRow::toDomainReference)
                  .toList(),
              equalTo(
                  originalReferences.stream()
                      .map(AuthoredNoteReferenceRow::toDomainReference)
                      .toList()));
          assertThat(
              countRowsForNotebook("note_creator", "note_id", notebook.getId()),
              is(originalCreatorCount));
          assertThat(
              countRowsForNotebook("authored_note_reference", "source_note_id", notebook.getId()),
              is(originalReferenceCount));
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
  }

  @Test
  void lateBindingSaveFailureRollsBackTheProjectedRootFilesAndAcceptedBinding() throws Exception {
    String referenceJson = "{\"schema\": \"donut\"}\n";
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", ACCEPTED_CONTENT),
                new NotebookGitProposalFile("reference.json", referenceJson))));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    String acceptedHead = accepted.getAcceptedGitObjectId();
    var acceptedHistoryBefore = acceptedHistory(notebook);
    byte[] renameAndEdit =
        proposalBundleBytes(
            accepted,
            List.of(
                new NotebookGitProposalFile("Root Note.md", PROPOSED_CONTENT),
                new NotebookGitProposalFile("guide.json", referenceJson)));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook.getId(), acceptedHead, renameAndEdit));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(
              notebookAttachmentRepository
                  .findPortableTreeRowsByNotebookId(notebook.getId())
                  .stream()
                  .map(row -> new PortableTreeEntry(row.filename(), row.acceptedGitContent()))
                  .toList(),
              equalTo(List.of(PortableTreeEntry.ofText("reference.json", referenceJson))));
          NotebookGitBinding reloaded =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloaded.getAcceptedGitObjectId(), is(acceptedHead));
        });
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
  }

  private long countRowsForNotebook(String table, String noteColumn, Integer notebookId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM "
                        + table
                        + " child JOIN note n ON child."
                        + noteColumn
                        + " = n.id WHERE n.notebook_id = :notebookId")
                .setParameter("notebookId", notebookId)
                .getSingleResult())
        .longValue();
  }
}
