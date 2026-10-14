package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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

class NotebookGitConcurrentProjectionDriftControllerTest extends NotebookGitControllerTestBase {

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
      assertThat(
          rejection.getReason(), containsString("The notebook changed since this publish started"));
      assertCommittedWebChange(webChange, notebook.getId(), note.getId());

      NotebookGitBinding bindingAfter = reloadCommittedBinding(notebook.getId());
      assertThat(
          bindingAfter.getAcceptedGitObjectId(), not(equalTo(binding.getAcceptedGitObjectId())));
      assertAcceptedBundleAdvancesFrom(webChange, notebook, binding, bindingAfter, note.getId());
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
          Folder folder = folderController.createFolder(notebook, request);
          NoteUpdateContentDTO update = new NoteUpdateContentDTO();
          update.setContent(FOLDER_README);
          folderController.updateFolderReadmeContent(notebook, folder, update);
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

  /**
   * Verifies the accepted bundle after a content drift is one exact linear advance: the new head
   * matches the binding, its note.md agrees with the freshly loaded database projection, and its
   * sole parent is the prior accepted head.
   */
  private void assertAcceptedBundleAdvancesFrom(
      RacingWebChange webChange,
      Notebook notebook,
      NotebookGitBinding before,
      NotebookGitBinding after,
      Integer noteId)
      throws Exception {
    String databaseContent =
        inCommittedTransaction(
            transactionManager, () -> noteRepository.findById(noteId).orElseThrow().getContent());
    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, downloaded);
      assertThat(head.getName(), is(after.getAcceptedGitObjectId()));
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(head);
        RevCommit priorAccepted =
            revWalk.parseCommit(ObjectId.fromString(before.getAcceptedGitObjectId()));
        assertThat(revWalk.isMergedInto(priorAccepted, commit), is(true));
        if (webChange == RacingWebChange.NOTE_CONTENT) {
          assertThat(
              NotebookGitProposalBlobText.readUtf8(repository, head, "note.md"),
              is(databaseContent));
          assertThat(commit.getParentCount(), is(1));
          assertThat(commit.getParent(0), equalTo(priorAccepted));
        } else {
          assertThat(
              GitBundleTestReader.pathsIn(repository, head),
              containsInAnyOrder("note.md", "new folder/README.md"));
          assertThat(commit.getParentCount(), is(1));
          RevCommit folderCreation = revWalk.parseCommit(commit.getParent(0));
          assertThat(folderCreation.getParentCount(), is(1));
          assertThat(folderCreation.getParent(0), equalTo(priorAccepted));
        }
      }
    }
  }
}
