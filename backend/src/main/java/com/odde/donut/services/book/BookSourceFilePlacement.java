package com.odde.donut.services.book;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.NumberedNameSelection;
import com.odde.donut.services.notebookGit.NotebookGitPortablePath;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Inside an accepted web change: a Book's stored source file becomes a file at its notebook's root
 * named {@code <book name>.<format>} (or {@code book.<format>} when the book name is not a plain
 * filename), numbered before its extension until no entry at the root holds it (ignoring case), and
 * the Book reads from that path.
 */
@Service
public class BookSourceFilePlacement {
  private final NotebookAttachmentRepository attachmentRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;

  public BookSourceFilePlacement(
      NotebookAttachmentRepository attachmentRepository,
      FolderSiblingNameValidation folderSiblingNameValidation) {
    this.attachmentRepository = attachmentRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
  }

  public void place(Book book, byte[] pointer) {
    String extension = BookFormat.fromString(book.getFormat()).bookFileExtension();
    String preferred = book.getBookName() + extension;
    String filename =
        NumberedNameSelection.firstAvailableFilename(
            NotebookGitPortablePath.isPlainFilename(preferred) ? preferred : "book" + extension,
            candidate ->
                folderSiblingNameValidation
                    .entryHolding(book.getNotebook(), null, candidate, Set.of())
                    .isPresent());
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(book.getNotebook());
    attachment.setFilename(filename);
    attachment.setAcceptedGitContent(pointer);
    attachmentRepository.save(attachment);
    book.setSourceFilePath(filename);
  }
}
