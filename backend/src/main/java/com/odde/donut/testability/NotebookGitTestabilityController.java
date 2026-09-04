package com.odde.donut.testability;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;

  @Schema(name = "ResnapshotNotebookGitBindingRequest")
  @Getter
  @Setter
  static class ResnapshotNotebookGitBindingRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String notebookName;
  }

  /**
   * Testability-only: simulates content changes landing after a notebook's real cutover by
   * replacing its accepted Git binding with a fresh snapshot of its current content. See {@link
   * NotebookGitCutoverService#resnapshotForTestability}.
   */
  @PostMapping("/resnapshot_notebook_git_binding_for_testability")
  @Transactional
  public String resnapshotNotebookGitBindingForTestability(
      @RequestBody ResnapshotNotebookGitBindingRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    Notebook notebook =
        notebookRepository
            .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(
                new DisplayName(request.getNotebookName()))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No notebook with name: " + request.getNotebookName()));
    notebookGitCutoverService.resnapshotForTestability(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant());
    return "OK";
  }
}
