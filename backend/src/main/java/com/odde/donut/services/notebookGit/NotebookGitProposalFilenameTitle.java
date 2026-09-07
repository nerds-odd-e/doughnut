package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.DisplayName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates a Portable Markdown path's filename-derived note title for Git proposal publication.
 * Rejects Bean Validation failures on {@link NoteUpdateTitleDTO} and titles {@link DisplayName}
 * would normalize, with HTTP 400 {@code Invalid note title at path "<path>": <reason>}.
 */
@Service
public class NotebookGitProposalFilenameTitle {

  private final Validator validator;

  public NotebookGitProposalFilenameTitle(Validator validator) {
    this.validator = validator;
  }

  public String requireValid(String path) {
    String filename = path.substring(path.lastIndexOf('/') + 1);
    String title = filename.substring(0, filename.length() - ".md".length());
    NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
    titleDto.setNewTitle(title);
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(titleDto);
    if (!violations.isEmpty()) {
      String reason =
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .sorted()
              .findFirst()
              .orElseThrow();
      throw invalid(path, reason);
    }
    String normalizedTitle = new DisplayName(title).value();
    if (!normalizedTitle.equals(title)) {
      throw invalid(path, "filename title would be normalized to \"" + normalizedTitle + "\"");
    }
    return title;
  }

  private static ResponseStatusException invalid(String path, String reason) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid note title at path \"" + path + "\": " + reason);
  }
}
