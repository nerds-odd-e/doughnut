package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1;
import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookAnchorRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookAnchor;
import com.odde.doughnut.entities.BookRange;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

  private final BookRepository bookRepository;
  private final BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final BookStorage bookStorage;

  public BookService(
      BookRepository bookRepository,
      BookUserLastReadPositionRepository bookUserLastReadPositionRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      BookStorage bookStorage) {
    this.bookRepository = bookRepository;
    this.bookUserLastReadPositionRepository = bookUserLastReadPositionRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.bookStorage = bookStorage;
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
    for (int i = 0; i < roots.size(); i++) {
      persistLayoutNode(book, null, roots.get(i), i, 1);
    }

    entityPersister.save(book);
    entityPersister.flush();
    return book;
  }

  @Transactional(readOnly = true)
  public Book getBookForNotebook(Notebook notebook) {
    return bookRepository
        .findByNotebook_Id(notebook.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
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
  public void deleteBookForNotebook(Notebook notebook) {
    Book book =
        bookRepository
            .findByNotebook_Id(notebook.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    String ref = book.getSourceFileRef();
    bookUserLastReadPositionRepository.deleteByBook_Id(book.getId());
    bookRepository.delete(book);
    if (ref != null && !ref.isBlank()) {
      bookStorage.delete(ref);
    }
  }

  @Transactional(readOnly = true)
  public BookPdfFile getBookPdfFile(Notebook notebook) {
    Book book = getBookForNotebook(notebook);
    String ref = book.getSourceFileRef();
    if (ref == null || ref.isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    byte[] bytes =
        bookStorage
            .get(ref)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    String attachmentFileName = sanitizeFileName(book.getBookName()) + ".pdf";
    return new BookPdfFile(bytes, attachmentFileName);
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
    String format = trimToNull(anchor.getAnchorFormat());
    if (format == null) {
      throw new ApiException(
          label + ".anchorFormat is required",
          ApiError.ErrorType.BINDING_ERROR,
          label + ".anchorFormat is required");
    }
    if (!ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1.equals(format)) {
      throw new ApiException(
          label + ".anchorFormat must be \"" + ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 + "\"",
          ApiError.ErrorType.BINDING_ERROR,
          label + ".anchorFormat must be \"" + ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 + "\"");
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
      Book book, BookRange parent, AttachBookLayoutNodeRequest node, int siblingIndex, int depth) {
    if (depth > MAX_LAYOUT_DEPTH) {
      throw new ApiException(
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH,
          ApiError.ErrorType.BINDING_ERROR,
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }

    BookAnchor start = new BookAnchor();
    start.setAnchorFormat(ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1);
    start.setValue(node.getStartAnchor().getValue().trim());

    BookRange range = new BookRange();
    range.setStructuralTitle(trimmedMax(node.getTitle(), 512));
    range.setStartAnchor(start);
    range.setParent(parent);
    range.setSiblingOrder(siblingIndex);
    book.addRange(range);

    List<AttachBookLayoutNodeRequest> children =
        node.getChildren() == null ? List.of() : node.getChildren();
    for (int j = 0; j < children.size(); j++) {
      persistLayoutNode(book, range, children.get(j), j, depth + 1);
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
