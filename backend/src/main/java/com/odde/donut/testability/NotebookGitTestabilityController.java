package com.odde.donut.testability;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;
  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired InjectNotesWorker injectNotesWorker;

  @Schema(name = "ResnapshotNotebookGitBindingRequest")
  @Getter
  @Setter
  static class ResnapshotNotebookGitBindingRequest {
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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
  }

  /**
   * Testability-only: simulates content changes landing after a notebook's real cutover by
   * replacing its accepted Git binding with a fresh snapshot of its current content. See {@link
   * NotebookGitCutoverService#resetHistory}.
   */
  @PostMapping("/resnapshot_notebook_git_binding_for_testability")
  @Transactional
  public String resnapshotNotebookGitBindingForTestability(
      @RequestBody ResnapshotNotebookGitBindingRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    notebookGitCutoverService.resetHistory(
        requireNotebook(request.getNotebookName()),
        testabilitySettings.getCurrentUTCTimestamp().toInstant());
    return "OK";
  }

  /**
   * Testability-only: stores a file's content as-is at a notebook path, creating missing folders,
   * then resnapshots the accepted Git binding so the file is part of the accepted tree.
   */
  @PostMapping("/put_notebook_file_for_testability")
  @Transactional
  public String putNotebookFileForTestability(@RequestBody PutNotebookFileRequest request) {
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
    attachment.setAcceptedGitContent(request.getContent().getBytes(StandardCharsets.UTF_8));
    notebookAttachmentRepository.save(attachment);
    notebookGitCutoverService.resetHistory(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant());
    return "OK";
  }

  /**
   * Testability-only: demotes a notebook to legacy RAW attachment storage and clears LFS attributes
   * so pull-based fixtures keep proving the raw journey after creation selects LFS.
   */
  @PostMapping("/force_raw_notebook_git_binding_for_testability")
  @Transactional
  public String forceRawNotebookGitBindingForTestability(
      @RequestBody ResnapshotNotebookGitBindingRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    Notebook notebook = requireNotebook(request.getNotebookName());
    NotebookGitBinding binding =
        notebookGitBindingRepository
            .findByNotebook_Id(notebook.getId())
            .orElseThrow(() -> new IllegalArgumentException("Notebook has no Git binding"));
    binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.RAW);
    notebookGitBindingRepository.save(binding);
    notebookGitCutoverService.resetHistory(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant(), List.of());
    return "OK";
  }

  private Notebook requireNotebook(String notebookName) {
    return notebookRepository
        .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(new DisplayName(notebookName))
        .orElseThrow(() -> new IllegalArgumentException("No notebook with name: " + notebookName));
  }
}
