package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookAttachment.InMemoryNotebookAttachmentContent;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A publish reads stored file content only for the attachments its commits change. */
class NotebookGitPublishCostControllerTest extends NotebookGitAttachmentSizeAdmissionTestSupport {

  private Notebook notebook;
  private List<NotebookGitProposalFile> acceptedFiles;
  private InMemoryNotebookAttachmentContent contentStore;

  @BeforeEach
  void notebookWithFilesAndALongAcceptedHistory() throws Exception {
    notebook = createGitBackedNotebook();
    makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    byte[] legacy =
        storeFolderAttachmentAndSnapshot(
                notebook, null, "legacy.bin", filledBytes(LIMIT + 1, (byte) 0x41))
            .getAcceptedGitContent();
    acceptedFiles = new ArrayList<>(List.of(new NotebookGitProposalFile("legacy.bin", legacy)));
    for (int i = 0; i < 3; i++) {
      acceptedFiles.add(
          new NotebookGitProposalFile(
              "file-" + i + ".bin", pointerFor(notebook, filledBytes(512, (byte) i))));
    }
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      for (int i = 0; i < 10; i++) {
        head = localCommitOnTopOf(repository, head, withNote("Note " + i), "Note " + i);
      }
      controller.publishNotebookGitProposal(
          notebook.getId(), binding.getAcceptedGitObjectId(), bundleBytesForHead(repository, head));
    }
    contentStore = (InMemoryNotebookAttachmentContent) notebookAttachmentContent;
  }

  @Test
  void noteEditReadsNoStoredContent() throws Exception {
    contentStore.resetAccessCounts();

    publish(withNote("Edited"));

    assertThat(contentStore.getCalls(), equalTo(0L));
  }

  @Test
  void newFilesAreEachReadOnce() throws Exception {
    List<NotebookGitProposalFile> added = new ArrayList<>(withNote("Added files"));
    for (int i = 0; i < 3; i++) {
      added.add(
          new NotebookGitProposalFile(
              "new-" + i + ".bin", pointerFor(notebook, filledBytes(256, (byte) (0x60 + i)))));
    }
    contentStore.resetAccessCounts();

    publish(added);

    assertThat(contentStore.getCalls(), equalTo(3L));
  }

  @Test
  void oneChangedFileIsReadOnce() throws Exception {
    byte[] changed = pointerFor(notebook, filledBytes(1024, (byte) 0x70));
    List<NotebookGitProposalFile> files =
        withNote("Changed file").stream()
            .map(
                file ->
                    file.path().equals("file-0.bin")
                        ? new NotebookGitProposalFile("file-0.bin", changed)
                        : file)
            .toList();
    contentStore.resetAccessCounts();

    publish(files);

    assertThat(contentStore.getCalls(), equalTo(1L));
  }

  @Test
  void newOversizedFileIsRefusedBeforeAnyContentRead() throws Exception {
    byte[] oversized = pointerFor(notebook, filledBytes(LIMIT + 1, (byte) 0x4F));
    List<NotebookGitProposalFile> files =
        Stream.concat(
                withNote("Oversized").stream(),
                Stream.of(new NotebookGitProposalFile("huge.bin", oversized)))
            .toList();
    contentStore.resetAccessCounts();

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(),
            proposalBundleBytes(reloadCommittedBinding(notebook.getId()), files),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "huge.bin", LIMIT + 1);
    assertThat(contentStore.getCalls(), equalTo(0L));
  }

  private List<NotebookGitProposalFile> withNote(String body) {
    return Stream.concat(
            Stream.of(
                new NotebookGitProposalFile(
                    "Root Note.md", "---\ntype: Note\n---\n" + body + "\n")),
            acceptedFiles.stream())
        .toList();
  }

  private void publish(List<NotebookGitProposalFile> files) throws Exception {
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBundleBytes(binding, files));
  }
}
