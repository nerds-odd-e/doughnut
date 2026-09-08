package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotebookGitNoteCreationAtomicControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TITLE_ONLY_CONTENT = "---\ntype: Note\n---\n";

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void titleOnlyFirstRootNoteIsAcceptedAsCanonicalChildOfOldHead() throws Exception {
    Timestamp createdAt = Timestamp.from(Instant.parse("2026-09-08T01:02:03Z"));
    testabilitySettings.timeTravelTo(createdAt);
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    NoteRealm response = controller.createNoteAtNotebookRoot(notebook, titleOnly("First Note"));

    Note created = noteRepository.findById(response.getId()).orElseThrow();
    assertThat(created.getTitle(), is("First Note"));
    assertThat(created.getContent(), is(TITLE_ONLY_CONTENT));
    assertThat(created.getFolder(), nullValue());

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId newHead = GitBundleTestReader.fetchHead(repository, downloaded);
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(newHead);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), is(acceptedHead));
        assertThat(
            commit.getAuthorIdent().getName(), is(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
        assertThat(
            commit.getAuthorIdent().getEmailAddress(),
            is(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
        assertThat(commit.getFullMessage(), is("Add note: First Note"));
        assertThat(commit.getCommitTime(), is((int) createdAt.toInstant().getEpochSecond()));
      }
      assertThat(GitBundleTestReader.pathsIn(repository, newHead), contains("First Note.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, newHead, "First Note.md"),
          is(TITLE_ONLY_CONTENT));
    }
  }

  @Test
  void lateBindingSaveFailureRollsBackCreatedNoteCreatorAndAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    byte[] acceptedBundle = binding.getBundleBytes();
    String acceptedHead = binding.getAcceptedGitObjectId();
    Timestamp bindingUpdatedAt = binding.getUpdatedAt();
    long originalNoteCount =
        inCommittedTransaction(transactionManager, () -> countNotesForNotebook(notebook.getId()));
    long originalCreatorCount =
        inCommittedTransaction(
            transactionManager,
            () -> countRowsForNotebook("note_creator", "note_id", notebook.getId()));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.createNoteAtNotebookRoot(notebook, titleOnly("First Note")));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(countNotesForNotebook(notebook.getId()), is(originalNoteCount));
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
          assertThat(
              countRowsForNotebook("note_creator", "note_id", notebook.getId()),
              is(originalCreatorCount));
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }

  private static NoteCreationDTO titleOnly(String title) {
    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle(title);
    return dto;
  }

  private long countNotesForNotebook(Integer notebookId) {
    return ((Number)
            entityManager
                .createNativeQuery("SELECT COUNT(*) FROM note WHERE notebook_id = :notebookId")
                .setParameter("notebookId", notebookId)
                .getSingleResult())
        .longValue();
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
