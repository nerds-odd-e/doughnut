package com.odde.donut.configs;

import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookGit.NotebookGitLfsConversionService;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/** After Flyway's migration on startup, converts every notebook still on raw storage to LFS. */
@Configuration
@Profile({"!test"})
public class NotebookGitLfsConversionOnStartup {
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookGitLfsConversionService conversionService;

  public NotebookGitLfsConversionOnStartup(
      NotebookGitBindingRepository bindingRepository,
      NotebookGitLfsConversionService conversionService) {
    this.bindingRepository = bindingRepository;
    this.conversionService = conversionService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void convertRawNotebooks() {
    bindingRepository
        .findNotebookIdsByAttachmentRepresentation(NotebookGitAttachmentRepresentation.RAW)
        .forEach(notebookId -> conversionService.convert(notebookId, Instant.now()));
  }
}
