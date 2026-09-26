package com.odde.donut.services.book;

import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.AttachBookRequest;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.BookRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attaching a Book to a notebook: the request is validated, the file is stored first, then the Book
 * and its file at the notebook root are accepted as one web change.
 */
@Service
public class AttachBookService {

  public record PersistContext(
      AttachBookRequest request,
      byte[] fileBytes,
      EntityPersister entityPersister,
      ObjectMapper objectMapper) {}

  private final BookRepository bookRepository;
  private final EntityPersister entityPersister;
  private final ObjectMapper objectMapper;
  private final TestabilitySettings testabilitySettings;
  private final NotebookAttachmentContent attachmentContent;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final BookSourceFilePlacement bookSourceFilePlacement;

  public AttachBookService(
      BookRepository bookRepository,
      EntityPersister entityPersister,
      ObjectMapper objectMapper,
      TestabilitySettings testabilitySettings,
      NotebookAttachmentContent attachmentContent,
      AcceptedWebChangeService acceptedWebChangeService,
      BookSourceFilePlacement bookSourceFilePlacement) {
    this.bookRepository = bookRepository;
    this.entityPersister = entityPersister;
    this.objectMapper = objectMapper;
    this.testabilitySettings = testabilitySettings;
    this.attachmentContent = attachmentContent;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.bookSourceFilePlacement = bookSourceFilePlacement;
  }

  @Transactional
  public Book attach(Notebook notebook, AttachBookRequest request, byte[] fileBytes)
      throws IOException, UnexpectedNoAccessRightException {
    validateAttachRequest(request);
    assertNotebookHasNoBook(notebook);
    if (BOOK_FORMAT_EPUB.equals(request.getFormat())) {
      EpubAttachValidator.validateAttachableEpub(fileBytes);
    }
    byte[] pointer = attachmentContent.storeAsLfsPointer(notebook.getId(), fileBytes);
    var ctx = new PersistContext(request, fileBytes, entityPersister, objectMapper);
    return acceptedWebChangeService.apply(
        notebook.getId(),
        () -> {
          Book book = newBook(notebook, request);
          bookSourceFilePlacement.place(book, pointer);
          BookFormat.fromString(request.getFormat()).persistNewBook(ctx, book);
          return book;
        },
        book -> "Attach book: " + book.getBookName(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  private Book newBook(Notebook notebook, AttachBookRequest request) {
    var book = new Book();
    book.setNotebook(notebook);
    book.setBookName(BookService.trimmedMax(request.getBookName(), 512));
    book.setFormat(request.getFormat());
    var now = testabilitySettings.getCurrentUTCTimestamp();
    book.setCreatedAt(now);
    book.setUpdatedAt(now);
    return book;
  }

  private void assertNotebookHasNoBook(Notebook notebook) {
    if (bookRepository.findByNotebook_Id(notebook.getId()).isPresent()) {
      throw new ApiException(
          "This notebook already has a book attached",
          ApiError.ErrorType.RESOURCE_CONFLICT,
          "This notebook already has a book attached");
    }
  }

  private void validateAttachRequest(AttachBookRequest request) {
    String format = request.getFormat();
    if (!BOOK_FORMAT_PDF.equals(format) && !BOOK_FORMAT_EPUB.equals(format)) {
      throw new ApiException(
          "format must be \"pdf\" or \"epub\"",
          ApiError.ErrorType.BINDING_ERROR,
          "format must be \"pdf\" or \"epub\"");
    }
    BookFormat.fromString(format).validateAttachRequest(request);
  }
}
