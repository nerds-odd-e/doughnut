package com.odde.donut.services.book;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookGit.NotebookRootFreeFilename;
import org.springframework.stereotype.Service;

/**
 * Inside an accepted web change: a Book's stored source file becomes a file at its notebook's root
 * named {@code <book name>.<format>} (or {@code book.<format>} when the book name is not a plain
 * filename) under the first free numbered form, and the Book reads from that path.
 */
@Service
public class BookSourceFilePlacement {
  private final NotebookAttachmentRepository attachmentRepository;
  private final NotebookRootFreeFilename rootFreeFilename;

  public BookSourceFilePlacement(
      NotebookAttachmentRepository attachmentRepository,
      NotebookRootFreeFilename rootFreeFilename) {
    this.attachmentRepository = attachmentRepository;
    this.rootFreeFilename = rootFreeFilename;
  }

  public void place(Book book, byte[] pointer) {
    String extension = BookFormat.fromString(book.getFormat()).bookFileExtension();
    String filename =
        rootFreeFilename.choose(
            book.getNotebook().getId(), book.getBookName() + extension, "book" + extension);
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(book.getNotebook());
    attachment.setFilename(filename);
    attachment.setAcceptedGitContent(pointer);
    attachmentRepository.save(attachment);
    book.setSourceFilePath(filename);
  }
}
