package com.odde.donut.services.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.AttachBookRequest;
import com.odde.donut.controllers.dto.BookBlockReadingRecordListItem;
import com.odde.donut.controllers.dto.BookLastReadPositionRequest;
import com.odde.donut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.donut.controllers.dto.BookUserLastReadPositionResponse;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.donut.entities.repositories.BookBlockRepository;
import com.odde.donut.entities.repositories.BookContentBlockRepository;
import com.odde.donut.entities.repositories.BookRepository;
import com.odde.donut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
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

  private final BookRepository bookRepository;
  private final BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  private final BookStorage bookStorage;
  private final EntityPersister entityPersister;
  private final BookLayoutReorganizer layoutReorganizer;
  private final BookReadingProgress readingProgress;
  private final BookOutlineEditor outlineEditor;
  private final BookSourceFile bookSourceFile;
  private final AttachBookService attachBookService;

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
      GlobalSettingsService globalSettingsService,
      BookSourceFile bookSourceFile,
      AttachBookService attachBookService) {
    this.bookSourceFile = bookSourceFile;
    this.attachBookService = attachBookService;
    this.bookRepository = bookRepository;
    this.bookUserLastReadPositionRepository = bookUserLastReadPositionRepository;
    this.bookStorage = bookStorage;
    this.entityPersister = entityPersister;
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

  public Book attachBook(Notebook notebook, AttachBookRequest request, byte[] fileBytes)
      throws IOException, UnexpectedNoAccessRightException {
    return attachBookService.attach(notebook, request, fileBytes);
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
    return bookSourceFile.read(book);
  }

  public ResponseEntity<byte[]> streamBookFile(NotebookBookFile file, CacheControl cacheControl) {
    return file.stream(cacheControl);
  }

  static String trimmedMax(String s, int max) {
    String t = s.trim();
    if (t.length() > max) {
      return t.substring(0, max);
    }
    return t;
  }
}
