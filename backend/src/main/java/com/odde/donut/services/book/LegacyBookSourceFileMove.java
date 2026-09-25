package com.odde.donut.services.book;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.repositories.BookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Book still reading from the old Book storage gets its source file at its notebook's root, as
 * attaching places it, and reads from there. The bytes are stored first, then the file and the
 * Book's path are accepted as one web change. The old copy stays as the backup.
 */
@Service
public class LegacyBookSourceFileMove {
  private final EntityPersister entityPersister;
  private final BookRepository bookRepository;
  private final BookStorage bookStorage;
  private final NotebookAttachmentContent attachmentContent;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final BookSourceFilePlacement bookSourceFilePlacement;
  private final TestabilitySettings testabilitySettings;

  public LegacyBookSourceFileMove(
      EntityPersister entityPersister,
      BookRepository bookRepository,
      BookStorage bookStorage,
      NotebookAttachmentContent attachmentContent,
      AcceptedWebChangeService acceptedWebChangeService,
      BookSourceFilePlacement bookSourceFilePlacement,
      TestabilitySettings testabilitySettings) {
    this.entityPersister = entityPersister;
    this.bookRepository = bookRepository;
    this.bookStorage = bookStorage;
    this.attachmentContent = attachmentContent;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.bookSourceFilePlacement = bookSourceFilePlacement;
    this.testabilitySettings = testabilitySettings;
  }

  /** The notebooks whose Book has no path in the notebook yet. */
  public List<Integer> notebookIdsWithUnmovedBooks() {
    return entityPersister
        .createQuery(
            "SELECT b.notebook.id FROM Book b WHERE b.sourceFilePath IS NULL ORDER BY b.notebook.id",
            Integer.class)
        .getResultList();
  }

  @Transactional(rollbackFor = Exception.class)
  public void move(Integer notebookId) throws IOException, UnexpectedNoAccessRightException {
    Book book = bookRepository.findByNotebook_Id(notebookId).orElseThrow();
    byte[] pointer =
        attachmentContent.storeAsLfsPointer(
            notebookId, bookStorage.get(book.getSourceFileRef()).orElseThrow());
    acceptedWebChangeService.apply(
        notebookId,
        () -> {
          bookSourceFilePlacement.place(book, pointer);
          return null;
        },
        ignored -> "Move the book's source file into the notebook",
        testabilitySettings.getCurrentUTCTimestamp());
  }
}
