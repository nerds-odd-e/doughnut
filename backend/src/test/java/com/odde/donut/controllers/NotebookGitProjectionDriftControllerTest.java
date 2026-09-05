package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** Verifies that a Git proposal cannot overwrite Portable content changed through the web. */
class NotebookGitProjectionDriftControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nproposed content";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb content";
  private static final String FOLDER_README = "---\ntype: Readme\n---\nfolder readme";

  @Autowired TextContentController textContentController;
  @Autowired RelationController relationController;
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
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("note.md", PROPOSED_CONTENT)));
    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    CountDownLatch writeFlushed = new CountDownLatch(1);
    CountDownLatch releaseWriter = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<?> writer =
          executor.submit(
              withRequestContext(
                  requestAttributes,
                  () -> {
                    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                    transaction.setPropagationBehavior(
                        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    transaction.executeWithoutResult(
                        ignored -> {
                          applyWebChange(webChange, notebook.getId(), note.getId());
                          entityManager.flush();
                          writeFlushed.countDown();
                          await(releaseWriter);
                        });
                    return null;
                  }));

      if (!writeFlushed.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("Web writer did not flush");
      }
      Future<String> publishing =
          executor.submit(
              withRequestContext(
                  requestAttributes,
                  () ->
                      controller.publishNotebookGitProposal(
                          notebook.getId(), binding.getAcceptedGitObjectId(), proposal)));

      assertThrows(TimeoutException.class, () -> publishing.get(250, TimeUnit.MILLISECONDS));
      releaseWriter.countDown();
      writer.get(10, TimeUnit.SECONDS);

      ExecutionException publishFailure =
          assertThrows(ExecutionException.class, () -> publishing.get(10, TimeUnit.SECONDS));
      ResponseStatusException rejection = (ResponseStatusException) publishFailure.getCause();
      assertThat(rejection.getStatusCode(), equalTo(HttpStatus.CONFLICT));
      assertThat(rejection.getReason(), containsString("web changes cannot yet be synchronized"));
      assertCommittedWebChange(webChange, notebook.getId(), note.getId());

      NotebookGitBinding bindingAfter = reloadCommittedBinding(notebook.getId());
      assertThat(bindingAfter.getAcceptedGitObjectId(), equalTo(binding.getAcceptedGitObjectId()));
      assertThat(bindingAfter.getBundleBytes(), equalTo(binding.getBundleBytes()));
      assertThat(bindingAfter.getUpdatedAt(), equalTo(binding.getUpdatedAt()));
    } finally {
      releaseWriter.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  void rejectsWhenWebContentHasDriftedFromAcceptedMain() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent(WEB_CONTENT);
    textContentController.updateNoteContent(note, update);

    ResponseStatusException exception = submitCurrentParentProposal(notebook, binding);

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(exception.getReason(), containsString("web changes cannot yet be synchronized"));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(),
        equalTo(update.getContent()));
  }

  @Test
  void rejectsWhenWebStructureHasDriftedFromAcceptedMain() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("folder").please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    relationController.moveNoteToFolder(note, folder);

    ResponseStatusException exception = submitCurrentParentProposal(notebook, binding);

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(exception.getReason(), containsString("web changes cannot yet be synchronized"));
  }

  private ResponseStatusException submitCurrentParentProposal(
      Notebook notebook, NotebookGitBinding binding) throws Exception {
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("note.md", PROPOSED_CONTENT)));
    return assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.CONFLICT);
  }

  private void applyWebChange(RacingWebChange webChange, Integer notebookId, Integer noteId) {
    try {
      Notebook notebook = notebookRepository.findById(notebookId).orElseThrow();
      switch (webChange) {
        case NOTE_CONTENT -> {
          NoteUpdateContentDTO update = new NoteUpdateContentDTO();
          update.setContent(WEB_CONTENT);
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
              noteRepository.findById(noteId).orElseThrow().getContent(), equalTo(WEB_CONTENT));
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

  private static <T> java.util.concurrent.Callable<T> withRequestContext(
      RequestAttributes requestAttributes, ThrowingSupplier<T> task) {
    return () -> {
      RequestContextHolder.setRequestAttributes(requestAttributes);
      try {
        return task.get();
      } finally {
        RequestContextHolder.resetRequestAttributes();
      }
    };
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for test coordination");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    }
  }
}
