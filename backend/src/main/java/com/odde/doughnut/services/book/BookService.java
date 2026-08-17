package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.controllers.dto.BookBlockReadingRecordListItem;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.doughnut.controllers.dto.BookUserLastReadPositionResponse;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookBlockRepository;
import com.odde.doughnut.entities.repositories.BookContentBlockRepository;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import java.util.Optional;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

  public record CancelBlockResult(Book book, int predecessorBlockId) {}

  public record PersistContext(
      Notebook notebook,
      AttachBookRequest request,
      String sourceFileRef,
      byte[] fileBytes,
      EntityPersister entityPersister,
      ObjectMapper objectMapper,
      TestabilitySettings testabilitySettings) {}

  private final BookRepository bookRepository;
  private final BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  private final BookStorage bookStorage;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final ObjectMapper objectMapper;
  private final BookLayoutReorganizer layoutReorganizer;
  private final BookReadingProgress readingProgress;
  private final BookOutlineEditor outlineEditor;

  public BookService(
      BookRepository bookRepository,
      BookUserLastReadPositionRepository bookUserLastReadPositionRepository,
      BookBlockRepository bookBlockRepository,
      BookContentBlockRepository bookContentBlockRepository,
      BookBlockReadingRecordRepository bookBlockReadingRecordRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      BookStorage bookStorage,
      ObjectMapper objectMapper,
      OpenAiApiHandler openAiApiHandler,
      GlobalSettingsService globalSettingsService) {
    this.bookRepository = bookRepository;
    this.bookUserLastReadPositionRepository = bookUserLastReadPositionRepository;
    this.bookStorage = bookStorage;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.objectMapper = objectMapper;
    this.layoutReorganizer =
        new BookLayoutReorganizer(
            objectMapper, openAiApiHandler, globalSettingsService, entityPersister);
    this.readingProgress =
        new BookReadingProgress(
            bookUserLastReadPositionRepository,
            bookBlockRepository,
            bookBlockReadingRecordRepository,
            entityPersister,
            testabilitySettings,
            objectMapper);
    this.outlineEditor =
        new BookOutlineEditor(
            bookContentBlockRepository, entityPersister, testabilitySettings, objectMapper);
  }

  @Transactional
  public Book attachBook(Notebook notebook, AttachBookRequest request, byte[] fileBytes) {
    validateAttachRequest(request);
    assertNotebookHasNoBook(notebook);
    if (BOOK_FORMAT_EPUB.equals(request.getFormat())) {
      EpubAttachValidator.validateAttachableEpub(fileBytes);
    }
    String ref = bookStorage.put(fileBytes, request.getFormat());
    var ctx =
        new PersistContext(
            notebook, request, ref, fileBytes, entityPersister, objectMapper, testabilitySettings);
    return BookFormat.fromString(request.getFormat()).persistNewBook(ctx);
  }

  private void assertNotebookHasNoBook(Notebook notebook) {
    if (bookRepository.findByNotebook_Id(notebook.getId()).isPresent()) {
      throw new ApiException(
          "This notebook already has a book attached",
          ApiError.ErrorType.RESOURCE_CONFLICT,
          "This notebook already has a book attached");
    }
  }

  @Transactional(readOnly = true)
  public Book getBookForNotebook(Notebook notebook) {
    Book book = requireBook(notebook);
    List<BookBlock> blocks = book.getBlocks();
    blocks.size();
    for (BookBlock block : blocks) {
      entityPersister.refresh(block);
      block.getContentBlocks().size();
    }
    return book;
  }

  @Transactional(readOnly = true)
  public BookLayoutReorganizationSuggestion suggestLayoutReorganization(Notebook notebook) {
    return layoutReorganizer.suggest(requireBook(notebook));
  }

  @Transactional
  public Book applyLayoutReorganization(
      Notebook notebook, BookLayoutReorganizationSuggestion suggestion) {
    return layoutReorganizer.apply(requireBook(notebook), suggestion);
  }

  private Book requireBook(Notebook notebook) {
    return bookRepository
        .findByNotebook_Id(notebook.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
  }

  @Transactional(readOnly = true)
  public List<BookBlockReadingRecordListItem> listReadingRecordsForBook(
      Notebook notebook, User user) {
    return readingProgress.list(requireBook(notebook), user);
  }

  @Transactional(readOnly = true)
  public Optional<BookUserLastReadPositionResponse> getLastReadPosition(
      Notebook notebook, User user) {
    return readingProgress.getLastReadPosition(requireBook(notebook), user);
  }

  @Transactional
  public void upsertLastReadPosition(
      Notebook notebook, User user, BookLastReadPositionRequest request) {
    readingProgress.upsertLastReadPosition(requireBook(notebook), user, request);
  }

  @Transactional
  public void upsertReadingRecord(
      Notebook notebook, User user, BookBlock bookBlock, String status) {
    readingProgress.upsertReadingRecord(requireBook(notebook), user, bookBlock, status);
  }

  @Transactional
  public Book changeBlockDepth(Notebook notebook, BookBlock bookBlock, String direction) {
    return outlineEditor.changeBlockDepth(requireBook(notebook), bookBlock, direction);
  }

  @Transactional
  public CancelBlockResult cancelBlock(Notebook notebook, BookBlock bookBlock) {
    return outlineEditor.cancelBlock(requireBook(notebook), bookBlock);
  }

  @Transactional
  public Book createBookBlockFromContent(
      Notebook notebook, int fromBookContentBlockId, String structuralTitleOverride) {
    outlineEditor.splitAtContent(
        requireBook(notebook), fromBookContentBlockId, structuralTitleOverride);
    return getBookForNotebook(notebook);
  }

  @Transactional
  public void deleteBookForNotebook(Notebook notebook) {
    Book book =
        bookRepository
            .findByNotebook_Id(notebook.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    String ref = book.getSourceFileRef();
    bookUserLastReadPositionRepository.deleteByBook_Id(book.getId());
    bookRepository.delete(book);
    bookStorage.delete(ref);
  }

  @Transactional(readOnly = true)
  public NotebookBookFile getNotebookBookFile(Notebook notebook) {
    return notebookBookFileFromBook(requireBook(notebook));
  }

  @Transactional(readOnly = true)
  public NotebookBookFile notebookBookFileFromBook(Book book) {
    return NotebookBookFile.fromBook(book, bookStorage);
  }

  public ResponseEntity<byte[]> streamBookFile(NotebookBookFile file, CacheControl cacheControl) {
    return file.stream(cacheControl);
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

  static String trimmedMax(String s, int max) {
    String t = s.trim();
    if (t.length() > max) {
      return t.substring(0, max);
    }
    return t;
  }
}
