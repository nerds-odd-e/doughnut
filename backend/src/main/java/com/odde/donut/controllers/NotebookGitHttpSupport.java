package com.odde.donut.controllers;

import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookGit.NotebookGitBundleDownloadService;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitProposalImporter;
import com.odde.donut.services.notebookGit.NotebookGitProposalPublisher;
import com.odde.donut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Accepted-history Git HTTP for notebooks (bundle download, publish, history reset). */
abstract class NotebookGitHttpSupport {
  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitBundleDownloadService notebookGitBundleDownloadService;
  private final NotebookGitProposalPublisher notebookGitProposalPublisher;
  private final NotebookGitCutoverService notebookGitCutoverService;

  NotebookGitHttpSupport(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      NotebookGitBundleDownloadService notebookGitBundleDownloadService,
      NotebookGitProposalPublisher notebookGitProposalPublisher,
      NotebookGitCutoverService notebookGitCutoverService) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.notebookGitBundleDownloadService = notebookGitBundleDownloadService;
    this.notebookGitProposalPublisher = notebookGitProposalPublisher;
    this.notebookGitCutoverService = notebookGitCutoverService;
  }

  @Operation(
      operationId = "resetNotebookGitHistory",
      summary = "Restart the notebook's Git history from its current content")
  @PostMapping("/{notebook}/reset-git-history")
  @Transactional
  public void resetNotebookGitHistory(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebookGitCutoverService.resetHistory(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant());
  }

  @Operation(
      operationId = "downloadNotebookGitBundle",
      summary = "Download the notebook's accepted Git bundle")
  @GetMapping(value = "/{notebook}/git-bundle", produces = "application/x-git-bundle")
  public ResponseEntity<byte[]> downloadNotebookGitBundle(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    byte[] bundleBytes = notebookGitBundleDownloadService.select(notebook.getId());
    String filename = "notebook-" + notebook.getId() + ".bundle";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.valueOf("application/x-git-bundle"))
        .body(bundleBytes);
  }

  @Operation(
      operationId = "publishNotebookGitProposal",
      summary = "Submit a proposal Git bundle to publish onto the notebook's accepted main")
  @PostMapping(value = "/{notebook}/git-bundle", consumes = "application/x-git-bundle")
  public String publishNotebookGitProposal(
      @PathVariable("notebook") @Schema(type = "integer") Integer notebookId,
      @RequestParam("expectedHead") String expectedHead,
      @RequestBody byte[] bundleBytes)
      throws UnexpectedNoAccessRightException {
    NotebookGitProposalImporter.ImportedProposal proposal =
        NotebookGitProposalImporter.importMainHead(bundleBytes);
    try {
      return notebookGitProposalPublisher.publish(notebookId, expectedHead, proposal);
    } finally {
      proposal.repository().close();
    }
  }

  protected AuthorizationService authorizationService() {
    return authorizationService;
  }

  protected TestabilitySettings testabilitySettings() {
    return testabilitySettings;
  }
}
