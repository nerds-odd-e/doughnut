package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitConcurrentProjectionDriftControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_NOTE_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSAL_NOTE_CONTENT = "---\ntype: Note\n---\nproposed content";
  private static final String WEB_NOTE_CONTENT = "---\ntype: Note\n---\nweb content";
  private static final String FOLDER_README = "---\ntype: Readme\n---\nfolder readme";

  @Autowired TextContentController textContentController;
  @Autowired FolderRepository folderRepository;

  enum RacingWebChange {
    NOTE_CONTENT,
    README_BEARING_FOLDER_INSERTION
  }

  @ParameterizedTest
  @EnumSource(RacingWebChange.class)
  void rejectsWebChangeThatCommitsBeforeProjectionValidation(RacingWebChange webChange)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(ORIGINAL_NOTE_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("note.md", PROPOSAL_NOTE_CONTENT)));
    User owner = currentUser.getUser();
    CountDownLatch writeFlushed = new CountDownLatch(1);
    CountDownLatch releaseWriter = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<?> writer =
          executor.submit(
              NotebookGitConcurrentWriterTestSupport.inIsolatedRequest(
                  currentUser,
                  owner,
                  () -> {
                    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                    transaction.setPropagationBehavior(
                        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    transaction.executeWithoutResult(
                        ignored -> {
                          applyWebChange(webChange, notebook.getId(), note.getId());
                          entityManager.flush();
                          writeFlushed.countDown();
                          NotebookGitConcurrentWriterTestSupport.await(releaseWriter);
                        });
                    return null;
                  }));

      NotebookGitConcurrentWriterTestSupport.await(writeFlushed);
      Future<String> publishing =
          executor.submit(
              NotebookGitConcurrentWriterTestSupport.inIsolatedRequest(
                  currentUser,
                  owner,
                  () ->
                      controller.publishNotebookGitProposal(
                          notebook.getId(), binding.getAcceptedGitObjectId(), proposal)));

      NotebookGitConcurrentWriterTestSupport.assertQueued(publishing);
      releaseWriter.countDown();
      writer.get(10, TimeUnit.SECONDS);

      ExecutionException publishFailure =
          assertThrows(ExecutionException.class, () -> publishing.get(10, TimeUnit.SECONDS));
      ResponseStatusException rejection = (ResponseStatusException) publishFailure.getCause();
      assertThat(rejection.getStatusCode(), equalTo(HttpStatus.CONFLICT));
      String expectedReason =
          webChange == RacingWebChange.NOTE_CONTENT
              ? "expectedHead no longer matches"
              : "refresh the checkout before publishing";
      assertThat(rejection.getReason(), containsString(expectedReason));
      assertCommittedWebChange(webChange, notebook.getId(), note.getId());

      NotebookGitBinding bindingAfter = reloadCommittedBinding(notebook.getId());
      if (webChange == RacingWebChange.NOTE_CONTENT) {
        assertThat(
            bindingAfter.getAcceptedGitObjectId(), not(equalTo(binding.getAcceptedGitObjectId())));
        assertAcceptedBundleAdvancesFrom(binding, bindingAfter, note.getId());
      } else {
        assertThat(
            bindingAfter.getAcceptedGitObjectId(), equalTo(binding.getAcceptedGitObjectId()));
        assertThat(bindingAfter.getBundleBytes(), equalTo(binding.getBundleBytes()));
        assertThat(bindingAfter.getUpdatedAt(), equalTo(binding.getUpdatedAt()));
      }
    } finally {
      releaseWriter.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  private void applyWebChange(RacingWebChange webChange, Integer notebookId, Integer noteId) {
    try {
      Notebook notebook = notebookRepository.findById(notebookId).orElseThrow();
      switch (webChange) {
        case NOTE_CONTENT -> {
          NoteUpdateContentDTO update = new NoteUpdateContentDTO();
          update.setContent(WEB_NOTE_CONTENT);
          textContentController.updateNoteContent(
              noteRepository.findById(noteId).orElseThrow(), update);
        }
        case README_BEARING_FOLDER_INSERTION -> {
          FolderCreationRequest request = new FolderCreationRequest();
          request.setName("new folder");
          Folder folder = controller.createFolder(notebook, request);
          NoteUpdateContentDTO update = new NoteUpdateContentDTO();
          update.setContent(FOLDER_README);
          controller.updateFolderReadmeContent(notebook, folder, update);
        }
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private void assertCommittedWebChange(
      RacingWebChange webChange, Integer notebookId, Integer noteId) {
    switch (webChange) {
      case NOTE_CONTENT ->
          assertThat(
              noteRepository.findById(noteId).orElseThrow().getContent(),
              equalTo(WEB_NOTE_CONTENT));
      case README_BEARING_FOLDER_INSERTION -> {
        List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebookId);
        assertThat(folders, hasSize(1));
        assertThat(folders.getFirst().getReadmeContent(), equalTo(FOLDER_README));
      }
    }
  }

  private NotebookGitBinding reloadCommittedBinding(Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () -> notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow());
  }

  /**
   * Verifies the retained bundle after a content drift is one exact linear advance: the new head
   * matches the binding, its note.md agrees with the freshly loaded database projection, and its
   * sole parent is the prior accepted head.
   */
  private void assertAcceptedBundleAdvancesFrom(
      NotebookGitBinding before, NotebookGitBinding after, Integer noteId) throws Exception {
    String databaseContent =
        inCommittedTransaction(
            transactionManager, () -> noteRepository.findById(noteId).orElseThrow().getContent());
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, after.getBundleBytes());
      assertThat(head.getName(), is(after.getAcceptedGitObjectId()));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, head, "note.md"), is(databaseContent));
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(head);
        assertThat(commit.getParentCount(), is(1));
        assertThat(
            commit.getParent(0), equalTo(ObjectId.fromString(before.getAcceptedGitObjectId())));
      }
    }
  }
}
