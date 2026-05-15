package com.odde.doughnut.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookContentBlockRepository;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import com.odde.doughnut.services.book.BookStorage;
import com.odde.doughnut.services.book.EpubLocator;
import com.odde.doughnut.services.book.PdfLocator;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

abstract class NotebookBooksControllerTestBase extends ControllerTestBase {

  static final byte[] STUB_PDF_BYTES = new byte[] {1};

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialOpenAiClient;

  @Autowired NotebookBooksController controller;
  @Autowired BookRepository bookRepository;
  @Autowired BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  @Autowired BookBlockReadingRecordRepository bookBlockReadingRecordRepository;
  @Autowired BookContentBlockRepository bookContentBlockRepository;
  @Autowired BookStorage bookStorage;
  @Autowired ObjectMapper objectMapper;
  @Autowired EntityManager entityManager;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialOpenAiClient);
  }

  Notebook myNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  Notebook notebookWithBook() {
    return makeMe
        .aNotebook()
        .creatorAndOwner(currentUser.getUser())
        .withBook("Linear Algebra")
        .please();
  }

  Book bookOf(Notebook nb) {
    return bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
  }

  void setSourceFileRef(Notebook nb, String ref) {
    Book book = bookOf(nb);
    book.setSourceFileRef(ref);
    makeMe.entityPersister.save(book);
    makeMe.entityPersister.flush();
  }

  Notebook otherUsersNotebook() {
    return makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
  }

  Notebook otherUsersNotebookWithBook() {
    User other = makeMe.aUser().please();
    return makeMe.aNotebook().creatorAndOwner(other).withBook("Linear Algebra").please();
  }

  static MultipartFile pdfFile(byte[] content) {
    return new MockMultipartFile("file", "book.pdf", "application/pdf", content);
  }

  static MultipartFile epubFile(byte[] content) {
    return new MockMultipartFile("file", "book.epub", "application/epub+zip", content);
  }

  static byte[] readFixtureEpubValidMinimal() throws Exception {
    Path epubPath =
        Path.of("..", "e2e_test", "fixtures", "book_reading", "epub_valid_minimal.epub");
    return Files.readAllBytes(epubPath);
  }

  static byte[] readFixtureEpubInvalidDrmEncryptionXml() throws Exception {
    Path epubPath =
        Path.of(
            "..", "e2e_test", "fixtures", "book_reading", "epub_invalid_drm_encryption_xml.epub");
    return Files.readAllBytes(epubPath);
  }

  static AttachBookRequest epubAttachRequest(String bookName) {
    AttachBookRequest r = new AttachBookRequest();
    r.setBookName(bookName);
    r.setFormat(BookReadingWireConstants.BOOK_FORMAT_EPUB);
    return r;
  }

  static ServletWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  static List<BookBlock> blocksByLayoutOrder(Book book) {
    return book.getBlocks().stream()
        .sorted(Comparator.comparingInt(BookBlock::getLayoutSequence))
        .toList();
  }

  static List<BookBlock> rootBlocksSorted(Book book) {
    return blocksByLayoutOrder(book).stream().filter(b -> b.getDepth() == 0).toList();
  }

  static List<BookBlock> childrenOf(Book book, BookBlock parent) {
    List<BookBlock> ordered = blocksByLayoutOrder(book);
    int p = -1;
    for (int i = 0; i < ordered.size(); i++) {
      if (ordered.get(i).getId().equals(parent.getId())) {
        p = i;
        break;
      }
    }
    if (p < 0) {
      return List.of();
    }
    int parentDepth = parent.getDepth();
    List<BookBlock> out = new ArrayList<>();
    int i = p + 1;
    while (i < ordered.size() && ordered.get(i).getDepth() > parentDepth) {
      BookBlock candidate = ordered.get(i);
      if (candidate.getDepth() == parentDepth + 1) {
        out.add(candidate);
        int subtreeRootDepth = candidate.getDepth();
        i++;
        while (i < ordered.size() && ordered.get(i).getDepth() > subtreeRootDepth) {
          i++;
        }
      } else {
        i++;
      }
    }
    return out;
  }

  static AttachBookLayoutNodeRequest node(String title, AttachBookLayoutNodeRequest... kids) {
    AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
    n.setTitle(title);
    if (kids != null && kids.length > 0) {
      n.setChildren(new ArrayList<>(List.of(kids)));
    }
    return n;
  }

  static Map<String, Object> headingBlock(
      String text, int textLevel, int pageIdx, List<Double> bbox) {
    Map<String, Object> h = new LinkedHashMap<>();
    h.put("type", "text");
    h.put("text_level", textLevel);
    h.put("text", text);
    h.put("page_idx", pageIdx);
    h.put("bbox", new ArrayList<>(bbox));
    return h;
  }

  static AttachBookRequest attachRequest(AttachBookLayoutNodeRequest... roots) {
    AttachBookLayoutRequest layout = new AttachBookLayoutRequest();
    layout.setRoots(new ArrayList<>(List.of(roots)));
    AttachBookRequest r = new AttachBookRequest();
    r.setBookName("Linear Algebra");
    r.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
    r.setLayout(layout);
    return r;
  }

  static BookLastReadPositionRequest lastReadBody(int pageIndex, int normalizedY) {
    BookLastReadPositionRequest r = new BookLastReadPositionRequest();
    r.setLocator(
        new PdfLocator(pageIndex, List.of(0.0, (double) normalizedY, 100.0, 600.0), null, null));
    return r;
  }

  static BookLastReadPositionRequest lastReadBody(
      int pageIndex, int normalizedY, int selectedBookBlockId) {
    BookLastReadPositionRequest r = lastReadBody(pageIndex, normalizedY);
    r.setSelectedBookBlockId(selectedBookBlockId);
    return r;
  }

  static BookLastReadPositionRequest lastReadEpubBody(String epubLocatorWire) {
    String t = epubLocatorWire.trim();
    int hash = t.indexOf('#');
    String hrefPart = hash < 0 ? t : t.substring(0, hash);
    String fragPart = hash < 0 ? null : t.substring(hash + 1);
    if (fragPart != null) {
      fragPart = fragPart.trim();
      if (fragPart.startsWith("#")) {
        fragPart = fragPart.substring(1).trim();
      }
      if (fragPart.isEmpty()) {
        fragPart = null;
      }
    }
    hrefPart = hrefPart.trim();
    BookLastReadPositionRequest r = new BookLastReadPositionRequest();
    r.setLocator(new EpubLocator(hrefPart.isEmpty() ? null : hrefPart, fragPart));
    return r;
  }
}
