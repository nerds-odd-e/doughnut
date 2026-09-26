package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.testability.TestabilitySettings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deleting a file on the web removes its row as one accepted change. Its LFS object stays: earlier
 * history still points at it.
 */
@Service
public class WebAttachmentDeleteService {
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final NotebookRepository notebookRepository;
  private final NotebookAttachmentRepository attachmentRepository;
  private final AuthorizationService authorizationService;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public WebAttachmentDeleteService(
      AcceptedWebChangeService acceptedWebChangeService,
      NotebookRepository notebookRepository,
      NotebookAttachmentRepository attachmentRepository,
      AuthorizationService authorizationService,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.notebookRepository = notebookRepository;
    this.attachmentRepository = attachmentRepository;
    this.authorizationService = authorizationService;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  public void delete(Notebook notebook, NotebookAttachment attachment)
      throws UnexpectedNoAccessRightException {
    Integer attachmentId = attachment.getId();
    acceptedWebChangeService.apply(
        notebook.getId(),
        () -> {
          Notebook liveNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
          NotebookAttachment liveAttachment =
              attachmentRepository
                  .findById(attachmentId)
                  .orElseThrow(
                      () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found."));
          authorizationService.assertAuthorization(liveNotebook);
          liveAttachment.requireInNotebook(liveNotebook);
          String path = NotebookGitPortablePath.ofAttachment(liveAttachment);
          entityPersister.remove(liveAttachment);
          return path;
        },
        path -> "Delete file: " + path,
        testabilitySettings.getCurrentUTCTimestamp());
  }
}
