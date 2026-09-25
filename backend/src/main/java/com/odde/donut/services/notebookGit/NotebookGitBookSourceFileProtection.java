package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Book;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Keeps the notebook file a Book reads from in place: a proposal may not delete, rename or change
 * it. A rename removes the old path, so it needs no separate rule.
 */
final class NotebookGitBookSourceFileProtection {

  private NotebookGitBookSourceFileProtection() {}

  static void refuseChanging(
      Book book, List<NotebookGitProposalTreeShape.InspectedRegularFile> files) {
    String path = book.getSourceFilePath();
    if (files.stream()
        .anyMatch(
            file ->
                file.path().equals(path)
                    && !Objects.equals(file.acceptedBlobId(), file.proposedBlobId()))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "\""
              + path
              + "\" is the source file of the Book \""
              + book.getBookName()
              + "\". Remove the Book on the web before deleting, renaming or changing it.");
    }
  }
}
