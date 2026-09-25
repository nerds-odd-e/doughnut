package com.odde.donut.services.book;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentFile;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Where a Book's source file is read from: the notebook-root file its path names, or the old Book
 * storage while it has no path.
 */
@Service
public class BookSourceFile {
  private final BookStorage bookStorage;
  private final NotebookAttachmentRepository notebookAttachmentRepository;
  private final NotebookAttachmentFile notebookAttachmentFile;

  public BookSourceFile(
      BookStorage bookStorage,
      NotebookAttachmentRepository notebookAttachmentRepository,
      NotebookAttachmentFile notebookAttachmentFile) {
    this.bookStorage = bookStorage;
    this.notebookAttachmentRepository = notebookAttachmentRepository;
    this.notebookAttachmentFile = notebookAttachmentFile;
  }

  NotebookBookFile read(Book book) {
    String path = book.getSourceFilePath();
    if (path == null) {
      String ref = book.getSourceFileRef();
      return NotebookBookFile.of(
          book,
          bookStorage.get(ref).orElseThrow(BookSourceFile::notFound),
          ref.getBytes(StandardCharsets.UTF_8));
    }
    NotebookAttachment attachment =
        notebookAttachmentRepository
            .findByNotebook_IdAndFolderIsNullAndFilename(book.getNotebookId(), path)
            .orElseThrow(BookSourceFile::notFound);
    return NotebookBookFile.of(
        book, notebookAttachmentFile.bytes(attachment), attachment.getAcceptedGitContent());
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
  }
}
