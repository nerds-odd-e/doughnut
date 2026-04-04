package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1;
import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.odde.doughnut.controllers.dto.AttachBookAnchorRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookAnchor;
import com.odde.doughnut.entities.BookRange;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

  private final BookRepository bookRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final BookPdfStorage bookPdfStorage;

  public BookService(
      BookRepository bookRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      BookPdfStorage bookPdfStorage) {
    this.bookRepository = bookRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.bookPdfStorage = bookPdfStorage;
  }

  @Transactional
  public Book attachBook(Notebook notebook, AttachBookRequest request) {
    validateAttachRequest(request);
    assertNotebookHasNoBook(notebook);
    return persistNewBook(notebook, request, null);
  }

  @Transactional
  public Book attachBookWithPdf(Notebook notebook, AttachBookRequest request, byte[] pdfBytes) {
    validateAttachRequest(request);
    assertNotebookHasNoBook(notebook);
    String ref = bookPdfStorage.put(pdfBytes);
    return persistNewBook(notebook, request, ref);
  }

  private void assertNotebookHasNoBook(Notebook notebook) {
    if (bookRepository.findByNotebook_Id(notebook.getId()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This notebook already has a book attached");
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
  public BookPdfFile getBookPdfFile(Notebook notebook) {
    Book book = getBookForNotebook(notebook);
    String ref = book.getSourceFileRef();
    if (ref == null || ref.isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    byte[] bytes =
        bookPdfStorage
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layout is required");
    }
    if (!BOOK_FORMAT_PDF.equals(request.getFormat())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format must be \"pdf\"");
    }
    List<AttachBookLayoutNodeRequest> roots = request.getLayout().getRoots();
    if (roots == null || roots.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layout.roots must be non-empty");
    }
    for (AttachBookLayoutNodeRequest root : roots) {
      validateLayoutNode(root, 1);
    }
  }

  private void validateLayoutNode(AttachBookLayoutNodeRequest node, int depth) {
    if (depth > MAX_LAYOUT_DEPTH) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }
    String title = trimToNull(node.getTitle());
    if (title == null || title.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "each node title must be non-empty");
    }
    if (title.length() > 512) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title exceeds maximum length");
    }
    validateAnchor(node.getStartAnchor(), "startAnchor");
    validateAnchor(node.getEndAnchor(), "endAnchor");
    List<AttachBookLayoutNodeRequest> children = node.getChildren();
    if (children != null) {
      for (AttachBookLayoutNodeRequest child : children) {
        validateLayoutNode(child, depth + 1);
      }
    }
  }

  private void validateAnchor(AttachBookAnchorRequest anchor, String label) {
    if (anchor == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
    }
    String format = trimToNull(anchor.getAnchorFormat());
    if (format == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, label + ".anchorFormat is required");
    }
    if (!ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1.equals(format)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          label + ".anchorFormat must be \"" + ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 + "\"");
    }
    String value = trimToNull(anchor.getValue());
    if (value == null || value.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + ".value must be non-empty");
    }
  }

  private void persistLayoutNode(
      Book book, BookRange parent, AttachBookLayoutNodeRequest node, int siblingIndex, int depth) {
    if (depth > MAX_LAYOUT_DEPTH) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }

    BookAnchor start = new BookAnchor();
    start.setAnchorFormat(ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1);
    start.setValue(node.getStartAnchor().getValue().trim());

    BookAnchor end = new BookAnchor();
    end.setAnchorFormat(ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1);
    end.setValue(node.getEndAnchor().getValue().trim());

    BookRange range = new BookRange();
    range.setStructuralTitle(trimmedMax(node.getTitle(), 512));
    range.setStartAnchor(start);
    range.setEndAnchor(end);
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
