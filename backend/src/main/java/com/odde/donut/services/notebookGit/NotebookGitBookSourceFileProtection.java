package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.repositories.BookRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Keeps the notebook file a Book reads from in place: a proposal may not delete, rename or change
 * it, and the web may not delete it. A rename removes the old path, so it needs no separate rule.
 */
@Component
class NotebookGitBookSourceFileProtection {

  private final BookRepository bookRepository;

  NotebookGitBookSourceFileProtection(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  void refuseChanging(
      Integer notebookId, List<NotebookGitProposalTreeShape.InspectedRegularFile> files) {
    refuseChanging(
        notebookId,
        files.stream()
            .filter(file -> !Objects.equals(file.acceptedBlobId(), file.proposedBlobId()))
            .map(NotebookGitProposalTreeShape.InspectedRegularFile::path));
  }

  void refuseChanging(Integer notebookId, String path) {
    refuseChanging(notebookId, Stream.of(path));
  }

  private void refuseChanging(Integer notebookId, Stream<String> paths) {
    bookRepository
        .findByNotebook_Id(notebookId)
        .ifPresent(
            book ->
                paths
                    .filter(path -> path.equals(book.getSourceFilePath()))
                    .findFirst()
                    .ifPresent(
                        path -> {
                          throw new ResponseStatusException(
                              HttpStatus.CONFLICT,
                              "\""
                                  + path
                                  + "\" is the source file of the Book \""
                                  + book.getBookName()
                                  + "\". Remove the Book on the web before deleting, renaming or"
                                  + " changing it.");
                        }));
  }
}
