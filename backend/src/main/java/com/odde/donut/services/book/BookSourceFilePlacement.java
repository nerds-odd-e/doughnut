package com.odde.donut.services.book;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import org.springframework.stereotype.Service;

/**
 * Inside an accepted web change: a Book's stored source file becomes a file at its notebook's root
 * named {@code <book name>.<format>}, and the Book reads from that path.
 */
@Service
public class BookSourceFilePlacement {
  private final NotebookAttachmentRepository attachmentRepository;

  public BookSourceFilePlacement(NotebookAttachmentRepository attachmentRepository) {
    this.attachmentRepository = attachmentRepository;
  }

  public void place(Book book, byte[] pointer) {
    String filename =
        book.getBookName() + BookFormat.fromString(book.getFormat()).bookFileExtension();
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(book.getNotebook());
    attachment.setFilename(filename);
    attachment.setAcceptedGitContent(pointer);
    attachmentRepository.save(attachment);
    book.setSourceFilePath(filename);
  }
}
