package com.odde.donut.testability;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.Base64;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"e2e", "test"})
@RequestMapping("/api/testability")
class NotebookGitTestabilityController {

  @Autowired NotebookRepository notebookRepository;
  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired InjectNotesWorker injectNotesWorker;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;

  @Schema(name = "NotebookNameRequest")
  @Getter
  @Setter
  static class NotebookNameRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String notebookName;
  }

  @Schema(name = "PutNotebookFileRequest")
  @Getter
  @Setter
  static class PutNotebookFileRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String notebookName;

    /** Slash-separated notebook path; missing folders are created. */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String path;

    /** The file's exact bytes, base64-encoded. */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentBase64;
  }

  /**
   * Testability-only: simulates content changes landing after a notebook's real cutover by
   * replacing its accepted Git binding with a fresh snapshot of its current content. See {@link
   * NotebookGitCutoverService#resetHistory}.
   */
  @PostMapping("/resnapshot_notebook_git_binding_for_testability")
  @Transactional
  public String resnapshotNotebookGitBindingForTestability(
      @RequestBody NotebookNameRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    notebookGitCutoverService.resetHistory(
        requireNotebook(request.getNotebookName()),
        testabilitySettings.getCurrentUTCTimestamp().toInstant());
    return "OK";
  }

  /**
   * Testability-only: stores a file at a notebook path as the product does (payload in the content
   * store, pointer in the accepted tree), creating missing folders, then resnapshots the accepted
   * Git binding so the file is part of the accepted tree.
   */
  @PostMapping("/put_notebook_file_for_testability")
  @Transactional
  public String putNotebookFileForTestability(@RequestBody PutNotebookFileRequest request)
      throws IOException {
    Notebook notebook = requireNotebook(request.getNotebookName());
    int slash = request.getPath().lastIndexOf('/');
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(notebook);
    if (slash >= 0) {
      attachment.setFolder(
          injectNotesWorker.resolveOrCreateFolderPath(
              notebook,
              request.getPath().substring(0, slash),
              testabilitySettings.getCurrentUTCTimestamp()));
    }
    attachment.setFilename(request.getPath().substring(slash + 1));
    attachment.setAcceptedGitContent(
        notebookAttachmentContent.storeAsLfsPointer(
            notebook.getId(), Base64.getDecoder().decode(request.getContentBase64())));
    notebookAttachmentRepository.save(attachment);
    notebookGitCutoverService.resetHistory(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant());
    return "OK";
  }

  private Notebook requireNotebook(String notebookName) {
    return notebookRepository
        .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(new DisplayName(notebookName))
        .orElseThrow(() -> new IllegalArgumentException("No notebook with name: " + notebookName));
  }
}
