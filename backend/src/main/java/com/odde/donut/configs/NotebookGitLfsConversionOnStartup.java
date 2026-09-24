package com.odde.donut.configs;

import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookGit.NotebookGitLfsConversionService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * After Flyway's migration on startup, converts every notebook still on raw storage to LFS. A
 * notebook whose conversion fails stays raw and is logged; the others still convert.
 */
@Configuration
@Profile({"!test"})
public class NotebookGitLfsConversionOnStartup {
  private static final Logger logger =
      LoggerFactory.getLogger(NotebookGitLfsConversionOnStartup.class);
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
        .forEach(this::convertLeavingFailuresRaw);
  }

  private void convertLeavingFailuresRaw(Integer notebookId) {
    try {
      conversionService.convert(notebookId, Instant.now());
    } catch (RuntimeException e) {
      logger.error("Notebook {} stays on raw storage: LFS conversion failed", notebookId, e);
    }
  }
}
