package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookAnchorRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.controllers.dto.BookBlockReadingRecordListItem;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockReadingRecord;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

  private final BookRepository bookRepository;
  private final BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  private final BookBlockReadingRecordRepository bookBlockReadingRecordRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final BookStorage bookStorage;
  private final ObjectMapper objectMapper;

  public BookService(
      BookRepository bookRepository,
      BookUserLastReadPositionRepository bookUserLastReadPositionRepository,
      BookBlockReadingRecordRepository bookBlockReadingRecordRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      BookStorage bookStorage,
      ObjectMapper objectMapper) {
    this.bookRepository = bookRepository;
    this.bookUserLastReadPositionRepository = bookUserLastReadPositionRepository;
    this.bookBlockReadingRecordRepository = bookBlockReadingRecordRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.bookStorage = bookStorage;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Book attachBookWithPdf(Notebook notebook, AttachBookRequest request, byte[] pdfBytes) {
    validateAttachRequest(request);
    assertNotebookHasNoBook(notebook);
    String ref = bookStorage.put(pdfBytes);
    return persistNewBook(notebook, request, ref);
  }

  private void assertNotebookHasNoBook(Notebook notebook) {
    if (bookRepository.findByNotebook_Id(notebook.getId()).isPresent()) {
      throw new ApiException(
          "This notebook already has a book attached",
          ApiError.ErrorType.RESOURCE_CONFLICT,
          "This notebook already has a book attached");
    }
  }

  private Book persistNewBook(Notebook notebook, AttachBookRequest request, String sourceFileRef) {
    var book = new Book();
    book.setNotebook(notebook);
    book.setBookName(trimmedMax(request.getBookName(), 512));
    book.setFormat(BOOK_FORMAT_PDF);
    book.setSourceFileRef(sourceFileRef);
    var now = testabilitySettings.getCurrentUTCTimestamp();
    book.setCreatedAt(now);
    book.setUpdatedAt(now);

    List<AttachBookLayoutNodeRequest> roots = request.getLayout().getRoots();
    List<Map.Entry<BookBlock, List<Map<String, Object>>>> pendingContentBlocks = new ArrayList<>();
    for (int i = 0; i < roots.size(); i++) {
      persistLayoutNode(book, null, roots.get(i), i, 1, pendingContentBlocks);
    }

    entityPersister.save(book);
    entityPersister.flush();

    for (var entry : pendingContentBlocks) {
      persistContentBlocks(entry.getKey(), entry.getValue());
    }

    return book;
  }

  @Transactional(readOnly = true)
  public Book getBookForNotebook(Notebook notebook) {
    return bookRepository
        .findByNotebook_Id(notebook.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
  }

  @Transactional(readOnly = true)
  public List<BookBlockReadingRecordListItem> listReadingRecordsForBook(
      Notebook notebook, User user) {
    Book book = getBookForNotebook(notebook);
    return bookBlockReadingRecordRepository
        .findAllByUser_IdAndBookBlock_Book_Id(user.getId(), book.getId())
        .stream()
        .map(
            row ->
                new BookBlockReadingRecordListItem(
                    row.getBookBlock().getId(), row.getStatus(), row.getCompletedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<BookUserLastReadPosition> getLastReadPosition(Notebook notebook, User user) {
    Book book = getBookForNotebook(notebook);
    return bookUserLastReadPositionRepository.findByUser_IdAndBook_Id(user.getId(), book.getId());
  }

  @Transactional
  public void upsertLastReadPosition(
      Notebook notebook, User user, BookLastReadPositionRequest request) {
    Book book = getBookForNotebook(notebook);
    bookUserLastReadPositionRepository
        .findByUser_IdAndBook_Id(user.getId(), book.getId())
        .map(
            existing -> {
              existing.setPageIndex(request.getPageIndex());
              existing.setNormalizedY(request.getNormalizedY());
              return entityPersister.save(existing);
            })
        .orElseGet(
            () -> {
              var row = new BookUserLastReadPosition();
              row.setUser(user);
              row.setBook(book);
              row.setPageIndex(request.getPageIndex());
              row.setNormalizedY(request.getNormalizedY());
              return entityPersister.save(row);
            });
    entityPersister.flush();
  }

  @Transactional
  public void upsertReadingRecord(
      Notebook notebook, User user, BookBlock bookBlock, String status) {
    if (!(BookBlockReadingRecord.STATUS_READ.equals(status)
        || BookBlockReadingRecord.STATUS_SKIMMED.equals(status)
        || BookBlockReadingRecord.STATUS_SKIPPED.equals(status))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reading record status");
    }
    Book book = getBookForNotebook(notebook);
    if (!bookBlock.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    bookBlockReadingRecordRepository
        .findByUser_IdAndBookBlock_Id(user.getId(), bookBlock.getId())
        .map(
            existing -> {
              existing.setStatus(status);
              existing.setCompletedAt(now);
              return entityPersister.save(existing);
            })
        .orElseGet(
            () -> {
              var row = new BookBlockReadingRecord();
              row.setUser(user);
              row.setBookBlock(bookBlock);
              row.setStatus(status);
              row.setCompletedAt(now);
              return entityPersister.save(row);
            });
    entityPersister.flush();
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
  public BookPdfFile getBookPdfFile(Notebook notebook) {
    Book book = getBookForNotebook(notebook);
    String ref = book.getSourceFileRef();
    byte[] bytes =
        bookStorage
            .get(ref)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    String attachmentFileName = sanitizeFileName(book.getBookName()) + ".pdf";
    return new BookPdfFile(bytes, attachmentFileName, etagForSourceRef(ref));
  }

  private static String etagForSourceRef(String ref) {
    return "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";
  }

  private static String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\/:*?\"<>|]", "_");
  }

  private void validateAttachRequest(AttachBookRequest request) {
    if (request.getLayout() == null) {
      throw new ApiException(
          "layout is required", ApiError.ErrorType.BINDING_ERROR, "layout is required");
    }
    if (!BOOK_FORMAT_PDF.equals(request.getFormat())) {
      throw new ApiException(
          "format must be \"pdf\"", ApiError.ErrorType.BINDING_ERROR, "format must be \"pdf\"");
    }
    List<AttachBookLayoutNodeRequest> roots = request.getLayout().getRoots();
    if (roots == null || roots.isEmpty()) {
      throw new ApiException(
          "layout.roots must be non-empty",
          ApiError.ErrorType.BINDING_ERROR,
          "layout.roots must be non-empty");
    }
    for (AttachBookLayoutNodeRequest root : roots) {
      validateLayoutNode(root, 1);
    }
  }

  private void validateLayoutNode(AttachBookLayoutNodeRequest node, int depth) {
    if (depth > MAX_LAYOUT_DEPTH) {
      throw new ApiException(
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH,
          ApiError.ErrorType.BINDING_ERROR,
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }
    String title = trimToNull(node.getTitle());
    if (title == null || title.isEmpty()) {
      throw new ApiException(
          "each node title must be non-empty",
          ApiError.ErrorType.BINDING_ERROR,
          "each node title must be non-empty");
    }
    if (title.length() > 512) {
      throw new ApiException(
          "title exceeds maximum length",
          ApiError.ErrorType.BINDING_ERROR,
          "title exceeds maximum length");
    }
    validateAnchor(node.getStartAnchor(), "startAnchor");
    List<AttachBookLayoutNodeRequest> children = node.getChildren();
    if (children != null) {
      for (AttachBookLayoutNodeRequest child : children) {
        validateLayoutNode(child, depth + 1);
      }
    }
  }

  private void validateAnchor(AttachBookAnchorRequest anchor, String label) {
    if (anchor == null) {
      throw new ApiException(
          label + " is required", ApiError.ErrorType.BINDING_ERROR, label + " is required");
    }
    String value = trimToNull(anchor.getValue());
    if (value == null || value.isEmpty()) {
      throw new ApiException(
          label + ".value must be non-empty",
          ApiError.ErrorType.BINDING_ERROR,
          label + ".value must be non-empty");
    }
  }

  private void persistLayoutNode(
      Book book,
      BookBlock parent,
      AttachBookLayoutNodeRequest node,
      int siblingIndex,
      int depth,
      List<Map.Entry<BookBlock, List<Map<String, Object>>>> pendingContentBlocks) {
    if (depth > MAX_LAYOUT_DEPTH) {
      throw new ApiException(
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH,
          ApiError.ErrorType.BINDING_ERROR,
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }

    BookBlock block = new BookBlock();
    block.setStructuralTitle(trimmedMax(node.getTitle(), 512));
    block.setStartAnchorValue(node.getStartAnchor().getValue().trim());
    block.setParent(parent);
    block.setSiblingOrder(siblingIndex);
    book.addBlock(block);

    List<AttachBookLayoutNodeRequest> children =
        node.getChildren() == null ? List.of() : node.getChildren();
    for (int j = 0; j < children.size(); j++) {
      persistLayoutNode(book, block, children.get(j), j, depth + 1, pendingContentBlocks);
    }

    List<Map<String, Object>> cbs = node.getContentBlocks();
    if (cbs != null && !cbs.isEmpty()) {
      pendingContentBlocks.add(new AbstractMap.SimpleEntry<>(block, cbs));
    }
  }

  private void persistContentBlocks(BookBlock block, List<Map<String, Object>> cbs) {
    for (int i = 0; i < cbs.size(); i++) {
      Map<String, Object> raw = cbs.get(i);
      BookContentBlock cb = new BookContentBlock();
      cb.setBookBlock(block);
      cb.setSiblingOrder(i);
      cb.setType(String.valueOf(raw.getOrDefault("type", "")));
      Object pi = raw.get("page_idx");
      cb.setPageIdx(pi instanceof Number n ? n.intValue() : null);
      try {
        cb.setRawData(objectMapper.writeValueAsString(raw));
      } catch (JsonProcessingException e) {
        throw new ApiException(
            "failed to serialize content block",
            ApiError.ErrorType.BINDING_ERROR,
            "failed to serialize content block");
      }
      entityPersister.save(cb);
    }
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String trimmedMax(String s, int max) {
    String t = s.trim();
    if (t.length() > max) {
      return t.substring(0, max);
    }
    return t;
  }
}
