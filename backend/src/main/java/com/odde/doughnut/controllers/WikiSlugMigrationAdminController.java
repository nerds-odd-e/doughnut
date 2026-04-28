package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.WikiSlugMigrationBatchResult;
import com.odde.doughnut.controllers.dto.WikiSlugMigrationStatus;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.WikiSlugMigrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wiki-slug-migration")
class WikiSlugMigrationAdminController {

  private final AuthorizationService authorizationService;
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final WikiSlugMigrationService wikiSlugMigrationService;

  WikiSlugMigrationAdminController(
      AuthorizationService authorizationService,
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      WikiSlugMigrationService wikiSlugMigrationService) {
    this.authorizationService = authorizationService;
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.wikiSlugMigrationService = wikiSlugMigrationService;
  }

  @GetMapping("")
  public WikiSlugMigrationStatus getStatus() throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    WikiSlugMigrationStatus status = new WikiSlugMigrationStatus();
    status.setFoldersMissingSlug(folderRepository.countFoldersMissingSlug());
    status.setNotesMissingSlug(noteRepository.countNotesMissingSlug());
    return status;
  }

  @PostMapping("/batch/folders")
  public WikiSlugMigrationBatchResult batchMigrateFolders(
      @RequestParam(defaultValue = "100") int limit) throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return wikiSlugMigrationService.migrateFoldersBatch(limit);
  }

  @PostMapping("/batch/notes")
  public WikiSlugMigrationBatchResult batchMigrateNotes(
      @RequestParam(defaultValue = "100") int limit) throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return wikiSlugMigrationService.migrateNotesBatch(limit);
  }
}
