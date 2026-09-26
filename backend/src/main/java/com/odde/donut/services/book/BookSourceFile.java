package com.odde.donut.services.book;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Where a Book's source file is read from: the notebook-root file its path names. */
@Service
public class BookSourceFile {
  private final NotebookAttachmentRepository notebookAttachmentRepository;
  private final NotebookAttachmentFile notebookAttachmentFile;

  public BookSourceFile(
      NotebookAttachmentRepository notebookAttachmentRepository,
      NotebookAttachmentFile notebookAttachmentFile) {
    this.notebookAttachmentRepository = notebookAttachmentRepository;
    this.notebookAttachmentFile = notebookAttachmentFile;
  }

  NotebookBookFile read(Book book) {
    NotebookAttachment attachment =
        notebookAttachmentRepository
            .findByNotebook_IdAndFolderIsNullAndFilename(
                book.getNotebookId(), book.getSourceFilePath())
            .orElseThrow(BookSourceFile::notFound);
    return NotebookBookFile.of(
        book, notebookAttachmentFile.bytes(attachment), attachment.getAcceptedGitContent());
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
  }
}
