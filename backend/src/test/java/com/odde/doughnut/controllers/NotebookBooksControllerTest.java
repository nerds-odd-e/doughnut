package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion.BlockDepthSuggestion;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockReadingRecord;
import com.odde.doughnut.entities.BookBlockTitleLimits;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookContentBlockRepository;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import com.odde.doughnut.services.book.BookStorage;
import com.odde.doughnut.services.book.PdfLocator;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksControllerTest extends ControllerTestBase {

  private static final byte[] STUB_PDF_BYTES = new byte[] {1};

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialOpenAiClient;

  @Autowired NotebookBooksController controller;
  @Autowired BookRepository bookRepository;
  @Autowired BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  @Autowired BookBlockReadingRecordRepository bookBlockReadingRecordRepository;
  @Autowired BookContentBlockRepository bookContentBlockRepository;
  @Autowired BookStorage bookStorage;
  @Autowired ObjectMapper objectMapper;

  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialOpenAiClient);
  }

  private Notebook myNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private Notebook notebookWithBook() {
    return makeMe
        .aNotebook()
        .creatorAndOwner(currentUser.getUser())
        .withBook("Linear Algebra")
        .please();
  }

  private Book bookOf(Notebook nb) {
    return bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
  }

  private void setSourceFileRef(Notebook nb, String ref) {
    Book book = bookOf(nb);
    book.setSourceFileRef(ref);
    makeMe.entityPersister.save(book);
    makeMe.entityPersister.flush();
  }

  private Notebook otherUsersNotebook() {
    return makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
  }

  private Notebook otherUsersNotebookWithBook() {
    User other = makeMe.aUser().please();
    return makeMe.aNotebook().creatorAndOwner(other).withBook("Linear Algebra").please();
  }

  private static MultipartFile pdfFile(byte[] content) {
    return new MockMultipartFile("file", "book.pdf", "application/pdf", content);
  }

  private static MultipartFile epubFile(byte[] content) {
    return new MockMultipartFile("file", "book.epub", "application/epub+zip", content);
  }

  private static byte[] readFixtureEpubValidMinimal() throws Exception {
    Path epubPath =
        Path.of("..", "e2e_test", "fixtures", "book_reading", "epub_valid_minimal.epub");
    return Files.readAllBytes(epubPath);
  }

  private static byte[] readFixtureEpubInvalidDrmEncryptionXml() throws Exception {
    Path epubPath =
        Path.of(
            "..", "e2e_test", "fixtures", "book_reading", "epub_invalid_drm_encryption_xml.epub");
    return Files.readAllBytes(epubPath);
  }

  private static AttachBookRequest epubAttachRequest(String bookName) {
    AttachBookRequest r = new AttachBookRequest();
    r.setBookName(bookName);
    r.setFormat(BookReadingWireConstants.BOOK_FORMAT_EPUB);
    return r;
  }

  private static ServletWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  private static List<BookBlock> blocksByLayoutOrder(Book book) {
    return book.getBlocks().stream()
        .sorted(Comparator.comparingInt(BookBlock::getLayoutSequence))
        .toList();
  }

  private static List<BookBlock> rootBlocksSorted(Book book) {
    return blocksByLayoutOrder(book).stream().filter(b -> b.getDepth() == 0).toList();
  }

  private static List<BookBlock> childrenOf(Book book, BookBlock parent) {
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

  private static AttachBookLayoutNodeRequest node(
      String title, AttachBookLayoutNodeRequest... kids) {
    AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
    n.setTitle(title);
    if (kids != null && kids.length > 0) {
      n.setChildren(new ArrayList<>(List.of(kids)));
    }
    return n;
  }

  private static Map<String, Object> headingBlock(
      String text, int textLevel, int pageIdx, List<Double> bbox) {
    Map<String, Object> h = new LinkedHashMap<>();
    h.put("type", "text");
    h.put("text_level", textLevel);
    h.put("text", text);
    h.put("page_idx", pageIdx);
    h.put("bbox", new ArrayList<>(bbox));
    return h;
  }

  private static AttachBookRequest attachRequest(AttachBookLayoutNodeRequest... roots) {
    AttachBookLayoutRequest layout = new AttachBookLayoutRequest();
    layout.setRoots(new ArrayList<>(List.of(roots)));
    AttachBookRequest r = new AttachBookRequest();
    r.setBookName("Linear Algebra");
    r.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
    r.setLayout(layout);
    return r;
  }

  private static BookLastReadPositionRequest lastReadBody(int pageIndex, int normalizedY) {
    BookLastReadPositionRequest r = new BookLastReadPositionRequest();
    r.setPageIndex(pageIndex);
    r.setNormalizedY(normalizedY);
    return r;
  }

  private static BookLastReadPositionRequest lastReadBody(
      int pageIndex, int normalizedY, int selectedBookBlockId) {
    BookLastReadPositionRequest r = lastReadBody(pageIndex, normalizedY);
    r.setSelectedBookBlockId(selectedBookBlockId);
    return r;
  }

  private static BookLastReadPositionRequest lastReadEpubBody(String epubLocator) {
    BookLastReadPositionRequest r = new BookLastReadPositionRequest();
    r.setEpubLocator(epubLocator);
    return r;
  }

  @Nested
  class AttachBook {
    @Test
    void persistsNestedOutlineAndReturnsBookWithBlocks() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest ch1 = node("Section 1.1");
      AttachBookLayoutNodeRequest ch2 = node("Section 1.2");
      AttachBookLayoutNodeRequest root = node("Chapter 1", ch1, ch2);
      AttachBookRequest req = attachRequest(root);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};

      ResponseEntity<Book> res = controller.attachBook(nb, req, pdfFile(pdfBytes));

      assertThat(res.getStatusCode(), equalTo(HttpStatus.CREATED));
      assertThat(res.getBody(), notNullValue());
      Book created = res.getBody();
      assertThat(created.getId(), notNullValue());
      assertThat(created.getBookName(), equalTo("Linear Algebra"));
      assertThat(created.getSourceFileRef(), notNullValue());
      assertThat(created.getSourceFileRef().isBlank(), equalTo(false));
      assertThat(created.getBlocks(), hasSize(3));

      BookBlock outRoot = rootBlocksSorted(created).getFirst();
      assertThat(outRoot.getStructuralTitle(), equalTo("Chapter 1"));
      assertThat(outRoot.getId(), notNullValue());
      List<BookBlock> children = childrenOf(created, outRoot);
      assertThat(children, hasSize(2));
      assertThat(children.getFirst().getStructuralTitle(), equalTo("Section 1.1"));
      assertThat(children.get(1).getStructuralTitle(), equalTo("Section 1.2"));

      Book detail = controller.getBook(nb);
      assertThat(detail.getBlocks(), hasSize(3));
      BookBlock detailRoot = rootBlocksSorted(detail).getFirst();
      assertThat(detailRoot.getId(), equalTo(outRoot.getId()));
      assertThat(childrenOf(detail, detailRoot), hasSize(2));

      ResponseEntity<byte[]> fileRes = controller.getBookFile(webRequest(), nb);
      assertThat(fileRes.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(fileRes.getBody(), equalTo(pdfBytes));
    }

    @Test
    void getBookFullViewJsonExposesDepthAndPreorderMatchesLayoutSequence() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest ch1 = node("Section 1.1");
      AttachBookLayoutNodeRequest ch2 = node("Section 1.2");
      AttachBookLayoutNodeRequest root = node("Chapter 1", ch1, ch2);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};
      controller.attachBook(nb, attachRequest(root), pdfFile(pdfBytes));

      Book detail = controller.getBook(nb);
      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detail);
      JsonNode tree = objectMapper.readTree(json);
      JsonNode blocks = tree.get("blocks");
      assertThat(blocks.size(), equalTo(3));
      assertThat(blocks.get(0).get("depth").asInt(), equalTo(0));
      assertThat(blocks.get(1).get("depth").asInt(), equalTo(1));
      assertThat(blocks.get(2).get("depth").asInt(), equalTo(1));
      assertThat(blocks.get(0).get("title").asText(), equalTo("Chapter 1"));
      assertThat(blocks.get(1).get("title").asText(), equalTo("Section 1.1"));
      assertThat(blocks.get(2).get("title").asText(), equalTo("Section 1.2"));

      List<BookBlock> byLayoutSeq =
          detail.getBlocks().stream()
              .sorted(Comparator.comparingInt(BookBlock::getLayoutSequence))
              .toList();
      for (int i = 0; i < 3; i++) {
        assertThat(blocks.get(i).get("id").asInt(), equalTo(byLayoutSeq.get(i).getId()));
      }
      for (int i = 0; i < 3; i++) {
        JsonNode cbs = blocks.get(i).get("contentBlocks");
        assertThat(cbs.isArray(), equalTo(true));
        assertThat(cbs.size(), equalTo(0));
      }
    }

    @Test
    void contentListAttachSkipsMineruLevelsAndDerivesWireTree() throws Exception {
      Notebook nb = myNotebook();
      List<Object> contentList = new ArrayList<>();
      contentList.add(headingBlock("Part A", 1, 0, List.of(10.0, 20.0, 100.0, 40.0)));
      contentList.add(headingBlock("Deep section", 3, 0, List.of(10.0, 50.0, 100.0, 70.0)));
      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("MinerU book");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      req.setContentList(contentList);

      ResponseEntity<Book> res = controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
      assertThat(created, notNullValue());
      assertThat(created.getBlocks(), hasSize(2));
      BookBlock root = rootBlocksSorted(created).getFirst();
      assertThat(root.getStructuralTitle(), equalTo("Part A"));
      assertThat(root.getDepth(), equalTo(0));
      List<BookBlock> children = childrenOf(created, root);
      assertThat(children, hasSize(1));
      assertThat(children.getFirst().getStructuralTitle(), equalTo("Deep section"));
      assertThat(children.getFirst().getDepth(), equalTo(1));
    }

    @Test
    void rejectsSecondAttachForSameNotebook() throws Exception {
      Notebook nb = notebookWithBook();
      ApiException ex =
          assertThrows(
              ApiException.class,
              () ->
                  controller.attachBook(
                      nb, attachRequest(node("Second")), pdfFile(STUB_PDF_BYTES)));
      assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    }

    @Test
    void rejectsUnauthorizedNotebook() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.attachBook(otherNb, attachRequest(node("A")), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsEpubWhenMetaInfEncryptionXmlPresent() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubInvalidDrmEncryptionXml();
      AttachBookRequest req = epubAttachRequest("DRM EPUB");
      ApiException ex =
          assertThrows(
              ApiException.class, () -> controller.attachBook(nb, req, epubFile(epubBytes)));
      assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
      assertThat(ex.getErrorBody().getMessage(), containsString("encrypted or DRM-protected"));
    }

    @Test
    void persistsEpubAttachWithFormatAndStorageRef() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      AttachBookRequest req = epubAttachRequest("Minimal EPUB");

      ResponseEntity<Book> res = controller.attachBook(nb, req, epubFile(epubBytes));

      assertThat(res.getStatusCode(), equalTo(HttpStatus.CREATED));
      assertThat(res.getBody(), notNullValue());
      Book created = res.getBody();
      assertThat(created.getFormat(), equalTo(BookReadingWireConstants.BOOK_FORMAT_EPUB));
      assertThat(created.getBookName(), equalTo("Minimal EPUB"));
      assertThat(created.getSourceFileRef(), notNullValue());
      assertThat(created.getSourceFileRef().isBlank(), equalTo(false));
      assertThat(created.getBlocks(), hasSize(5));
      List<BookBlock> createdPreorder = blocksByLayoutOrder(created);
      assertThat(createdPreorder.get(0).getStructuralTitle(), equalTo("Part One"));
      assertThat(createdPreorder.get(0).getDepth(), equalTo(0));
      assertThat(createdPreorder.get(1).getStructuralTitle(), equalTo("Chapter Alpha"));
      assertThat(createdPreorder.get(1).getDepth(), equalTo(1));
      assertThat(createdPreorder.get(2).getStructuralTitle(), equalTo("Chapter Beta"));
      assertThat(createdPreorder.get(2).getDepth(), equalTo(0));
      assertThat(createdPreorder.get(3).getStructuralTitle(), equalTo("Section Beta-One"));
      assertThat(createdPreorder.get(3).getDepth(), equalTo(1));
      assertThat(createdPreorder.get(4).getStructuralTitle(), equalTo("Section Beta-Two"));
      assertThat(createdPreorder.get(4).getDepth(), equalTo(1));

      Book detail = controller.getBook(nb);
      assertThat(detail.getFormat(), equalTo(BookReadingWireConstants.BOOK_FORMAT_EPUB));
      assertThat(detail.getBlocks(), hasSize(5));
      List<BookBlock> detailPreorder = blocksByLayoutOrder(detail);
      assertThat(detailPreorder.get(0).getStructuralTitle(), equalTo("Part One"));
      assertThat(detailPreorder.get(0).getDepth(), equalTo(0));
      assertThat(detailPreorder.get(1).getStructuralTitle(), equalTo("Chapter Alpha"));
      assertThat(detailPreorder.get(1).getDepth(), equalTo(1));
      assertThat(detailPreorder.get(2).getStructuralTitle(), equalTo("Chapter Beta"));
      assertThat(detailPreorder.get(2).getDepth(), equalTo(0));
      assertThat(detailPreorder.get(3).getStructuralTitle(), equalTo("Section Beta-One"));
      assertThat(detailPreorder.get(3).getDepth(), equalTo(1));
      assertThat(detailPreorder.get(4).getStructuralTitle(), equalTo("Section Beta-Two"));
      assertThat(detailPreorder.get(4).getDepth(), equalTo(1));

      BookBlock partOne = detailPreorder.get(0);
      assertThat(partOne.getEpubStartHref(), equalTo("OEBPS/chapter1.xhtml"));
      assertThat(partOne.getContentBlocks(), hasSize(1));
      assertThat(partOne.getContentBlocks().getFirst().getType(), equalTo("text"));
      JsonNode partOneRaw =
          objectMapper.readTree(partOne.getContentBlocks().getFirst().getRawData());
      assertThat(partOneRaw.get("href").asText(), equalTo("OEBPS/chapter1.xhtml"));
      assertThat(partOneRaw.get("fragment").asText(), equalTo(""));
      assertThat(partOneRaw.get("text").asText(), equalTo("Opening paragraph for part one."));

      BookBlock chapterAlpha = detailPreorder.get(1);
      assertThat(chapterAlpha.getEpubStartHref(), equalTo("OEBPS/chapter2.xhtml"));
      assertThat(chapterAlpha.getContentBlocks(), hasSize(2));
      assertThat(chapterAlpha.getContentBlocks().get(0).getType(), equalTo("text"));
      assertThat(chapterAlpha.getContentBlocks().get(1).getType(), equalTo("image"));
      JsonNode alphaTextRaw =
          objectMapper.readTree(chapterAlpha.getContentBlocks().get(0).getRawData());
      assertThat(alphaTextRaw.get("href").asText(), equalTo("OEBPS/chapter2.xhtml"));
      assertThat(alphaTextRaw.get("fragment").asText(), equalTo(""));
      assertThat(alphaTextRaw.get("text").asText(), equalTo("Body text with an illustration."));
      JsonNode alphaImgRaw =
          objectMapper.readTree(chapterAlpha.getContentBlocks().get(1).getRawData());
      assertThat(alphaImgRaw.get("href").asText(), equalTo("OEBPS/chapter2.xhtml"));
      assertThat(alphaImgRaw.get("src").asText(), equalTo("figure.png"));

      BookBlock chapterBeta = detailPreorder.get(2);
      assertThat(chapterBeta.getEpubStartHref(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(chapterBeta.getContentBlocks(), hasSize(2));
      assertThat(chapterBeta.getContentBlocks().get(0).getType(), equalTo("text"));
      assertThat(chapterBeta.getContentBlocks().get(1).getType(), equalTo("table"));
      JsonNode betaTableRaw =
          objectMapper.readTree(chapterBeta.getContentBlocks().get(1).getRawData());
      assertThat(betaTableRaw.get("href").asText(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(betaTableRaw.get("fragment").asText(), equalTo("#beta-table"));
      assertThat(betaTableRaw.get("text").asText(), equalTo("Cell One"));

      BookBlock sectionBetaOne = detailPreorder.get(3);
      assertThat(
          sectionBetaOne.getEpubStartHref(), equalTo("OEBPS/chapter3.xhtml#section-beta-one"));
      assertThat(sectionBetaOne.getContentBlocks(), hasSize(1));
      assertThat(sectionBetaOne.getContentBlocks().getFirst().getType(), equalTo("text"));
      JsonNode sectionBetaOneRaw =
          objectMapper.readTree(sectionBetaOne.getContentBlocks().getFirst().getRawData());
      assertThat(sectionBetaOneRaw.get("href").asText(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(sectionBetaOneRaw.get("fragment").asText(), equalTo("#section-beta-one"));
      assertThat(
          sectionBetaOneRaw.get("text").asText(), equalTo("Unique content in section beta-one."));

      BookBlock sectionBetaTwo = detailPreorder.get(4);
      assertThat(
          sectionBetaTwo.getEpubStartHref(), equalTo("OEBPS/chapter3.xhtml#section-beta-two"));
      assertThat(sectionBetaTwo.getContentBlocks(), hasSize(1));
      assertThat(sectionBetaTwo.getContentBlocks().getFirst().getType(), equalTo("text"));
      JsonNode sectionBetaTwoRaw =
          objectMapper.readTree(sectionBetaTwo.getContentBlocks().getFirst().getRawData());
      assertThat(sectionBetaTwoRaw.get("href").asText(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(sectionBetaTwoRaw.get("fragment").asText(), equalTo("#section-beta-two"));
      assertThat(
          sectionBetaTwoRaw.get("text").asText(), equalTo("Unique content in section beta-two."));
    }

    @Test
    void rejectsEpubWhenLayoutIncluded() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_EPUB);
      ApiException ex =
          assertThrows(
              ApiException.class, () -> controller.attachBook(nb, req, epubFile(STUB_PDF_BYTES)));
      assertThat(ex.getMessage(), equalTo("EPUB attach must not include layout or contentList"));
    }

    @Test
    void rejectsUnknownBookFormat() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      req.setFormat("doc");
      ApiException ex =
          assertThrows(
              ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
      assertThat(ex.getMessage(), equalTo("format must be \"pdf\" or \"epub\""));
    }

    @Test
    void rejectsEmptyRoots() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest();
      req.getLayout().setRoots(new ArrayList<>());
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsExcessiveDepth() {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest deep = node("leaf");
      for (int i = 0; i < BookReadingWireConstants.MAX_LAYOUT_DEPTH; i++) {
        deep = node("d" + i, deep);
      }
      AttachBookLayoutNodeRequest root = deep;
      assertThrows(
          ApiException.class,
          () -> controller.attachBook(nb, attachRequest(root), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsEmptyFile() throws Exception {
      Notebook nb = myNotebook();
      MultipartFile empty =
          new MockMultipartFile("file", "book.pdf", "application/pdf", new byte[0]);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, attachRequest(node("A")), empty));
    }

    @Test
    void persistsContentBlocksForEachBlock() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      bodyItem.put("page_idx", 1);
      AttachBookLayoutNodeRequest n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));

      ResponseEntity<Book> res =
          controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
      BookBlock block = rootBlocksSorted(created).getFirst();
      List<BookContentBlock> cbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(block.getId());
      assertThat(cbs, hasSize(2));
      assertThat(cbs.get(0).getSiblingOrder(), equalTo(0));
      assertThat(cbs.get(0).getType(), equalTo("text"));
      assertThat(cbs.get(1).getSiblingOrder(), equalTo(1));
      assertThat(cbs.get(1).getType(), equalTo("text"));
      assertThat(cbs.get(1).getPageIdx(), equalTo(1));

      Book detail = controller.getBook(nb);
      assertThat(
          detail.getBlocks().stream().map(BookBlock::getId).toList(), hasItem(block.getId()));
      BookBlock detailChapter =
          detail.getBlocks().stream()
              .filter(b -> b.getId().equals(block.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(detailChapter.getContentBlocks(), hasSize(2));

      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detail);
      JsonNode blocks = objectMapper.readTree(json).get("blocks");
      JsonNode chapter = null;
      for (JsonNode b : blocks) {
        if (b.get("id").asInt() == block.getId()) {
          chapter = b;
          break;
        }
      }
      assertThat(chapter, notNullValue());
      JsonNode wireCbs = chapter.get("contentBlocks");
      assertThat(wireCbs.isArray(), equalTo(true));
      assertThat(wireCbs.size(), equalTo(2));
      assertThat(wireCbs.get(0).get("id").asInt(), equalTo(cbs.get(0).getId()));
      assertThat(wireCbs.get(1).get("id").asInt(), equalTo(cbs.get(1).getId()));
      assertThat(wireCbs.get(0).get("type").asText(), equalTo("text"));
      assertThat(wireCbs.get(1).get("type").asText(), equalTo("text"));
      assertThat(wireCbs.get(1).get("pageIdx").asInt(), equalTo(1));
      assertThat(wireCbs.get(0).get("raw"), nullValue());
      assertThat(wireCbs.get(1).get("raw"), nullValue());

      Book detailAgain = controller.getBook(nb);
      String jsonAgain =
          objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detailAgain);
      JsonNode blocksAgain = objectMapper.readTree(jsonAgain).get("blocks");
      JsonNode chapterAgain = null;
      for (JsonNode b : blocksAgain) {
        if (b.get("id").asInt() == block.getId()) {
          chapterAgain = b;
          break;
        }
      }
      assertThat(chapterAgain, notNullValue());
      assertThat(
          chapterAgain.get("contentBlocks").get(0).get("id").asInt(), equalTo(cbs.get(0).getId()));
      assertThat(
          chapterAgain.get("contentBlocks").get(1).get("id").asInt(), equalTo(cbs.get(1).getId()));
    }

    @Test
    void persistsContentBlocksUnderSyntheticBeginningRoot() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> anchorBlock = new LinkedHashMap<>();
      anchorBlock.put("type", "beginning_anchor");
      anchorBlock.put("page_idx", 0);
      anchorBlock.put("bbox", new ArrayList<>(List.of(10.0, 70.0, 200.0, 100.0)));
      Map<String, Object> orphan = new LinkedHashMap<>();
      orphan.put("type", "text");
      orphan.put("text", "Preface paragraph");
      orphan.put("page_idx", 0);

      AttachBookLayoutNodeRequest beginning = node("*beginning*");
      beginning.setContentBlocks(new ArrayList<>(List.of(anchorBlock, orphan)));
      AttachBookLayoutNodeRequest chapter = node("Chapter 1");

      ResponseEntity<Book> res =
          controller.attachBook(nb, attachRequest(beginning, chapter), pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
      assertThat(created, notNullValue());
      List<BookBlock> roots = rootBlocksSorted(created);
      assertThat(roots, hasSize(2));
      assertThat(roots.getFirst().getStructuralTitle(), equalTo("*beginning*"));
      assertThat(roots.get(1).getStructuralTitle(), equalTo("Chapter 1"));

      List<BookContentBlock> beginningCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              roots.getFirst().getId());
      assertThat(beginningCbs, hasSize(2));
      assertThat(beginningCbs.getFirst().getSiblingOrder(), equalTo(0));
      assertThat(beginningCbs.getFirst().getType(), equalTo("beginning_anchor"));
      assertThat(beginningCbs.get(1).getSiblingOrder(), equalTo(1));
      assertThat(beginningCbs.get(1).getType(), equalTo("text"));
      assertThat(beginningCbs.get(1).getPageIdx(), equalTo(0));
    }

    @Test
    void persistsFromContentListBuildsBeginningAndChapter() throws Exception {
      Notebook nb = myNotebook();
      List<Object> cl = new ArrayList<>();
      Map<String, Object> orphan = new LinkedHashMap<>();
      orphan.put("type", "text");
      orphan.put("text", "Orphan body");
      orphan.put("bbox", new ArrayList<>(List.of(10, 100, 200, 130)));
      orphan.put("page_idx", 0);
      cl.add(orphan);
      cl.add(headingBlock("Chapter One", 2, 1, List.of(1.0, 200.0, 300.0, 240.0)));

      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("Linear Algebra");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      req.setContentList(cl);

      ResponseEntity<Book> res = controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
      assertThat(created, notNullValue());
      List<BookBlock> roots = rootBlocksSorted(created);
      assertThat(roots, hasSize(2));
      assertThat(roots.getFirst().getStructuralTitle(), equalTo("*beginning*"));
      assertThat(roots.get(1).getStructuralTitle(), equalTo("Chapter One"));

      List<BookContentBlock> beginningCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              roots.getFirst().getId());
      assertThat(beginningCbs, hasSize(2));
      assertThat(beginningCbs.getFirst().getType(), equalTo("beginning_anchor"));
      assertThat(beginningCbs.get(1).getRawData(), containsString("Orphan body"));
    }

    @Test
    void rejectsBothLayoutRootsAndContentList() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      List<Object> cl = new ArrayList<>();
      Map<String, Object> noise = new LinkedHashMap<>();
      noise.put("type", "text");
      noise.put("text", "only body");
      noise.put("page_idx", 0);
      noise.put("bbox", new ArrayList<>(List.of(0.0, 0.0, 1.0, 1.0)));
      cl.add(noise);
      req.setContentList(cl);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsNeitherLayoutNorContentList() {
      Notebook nb = myNotebook();
      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("X");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsContentListThatProducesNoBlocks() {
      Notebook nb = myNotebook();
      List<Object> cl = new ArrayList<>();
      Map<String, Object> pn = new LinkedHashMap<>();
      pn.put("type", "page_number");
      pn.put("text", "1");
      pn.put("page_idx", 0);
      cl.add(pn);
      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("Book");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      req.setContentList(cl);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }
  }

  @Nested
  class GetBook {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
    }

    @Test
    void getBookReturnsBookWithNonBlankSourceFileRef() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      String ref = controller.getBook(nb).getSourceFileRef();
      assertThat(ref, notNullValue());
      assertThat(ref.isBlank(), equalTo(false));
    }

    @Test
    void doesNotReturnAnotherNotebooksBook() {
      notebookWithBook();
      Notebook nb2 = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb2));
    }

    @Test
    void allBboxesDerivesStartAnchorFromFirstContentBlock() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Headed Section");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Headed Section", 1, 1, List.of(5.0, 10.0, 200.0, 50.0)))));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getAllBboxes(), hasSize(1));
      assertThat(block.getAllBboxes().getFirst().pageIndex(), equalTo(1));
      assertThat(block.getAllBboxes().getFirst().bbox(), equalTo(List.of(5.0, 10.0, 200.0, 50.0)));
    }

    @Test
    void allBboxesIncludesHeadingBboxThenBodyBboxes() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Bbox");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Bbox", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getAllBboxes(), hasSize(2));
      assertThat(block.getAllBboxes().getFirst().pageIndex(), equalTo(2));
      assertThat(block.getAllBboxes().getFirst().bbox(), equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      assertThat(block.getAllBboxes().get(1).pageIndex(), equalTo(2));
      assertThat(block.getAllBboxes().get(1).bbox(), equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void allBboxesSkipsHeaderFooterPageChromeAndStructuralHeadingsInBodyBlocks() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("type", "header");
      header.put("text", "Running title");
      header.put("page_idx", 2);
      header.put("bbox", new ArrayList<>(List.of(1.0, 1.0, 50.0, 10.0)));
      Map<String, Object> footer = new LinkedHashMap<>();
      footer.put("type", "footer");
      footer.put("text", "copyright");
      footer.put("page_idx", 2);
      footer.put("bbox", new ArrayList<>(List.of(1.0, 500.0, 50.0, 510.0)));
      Map<String, Object> pageNum = new LinkedHashMap<>();
      pageNum.put("type", "page_number");
      pageNum.put("text", "7");
      pageNum.put("page_idx", 2);
      pageNum.put("bbox", new ArrayList<>(List.of(400.0, 500.0, 410.0, 510.0)));
      Map<String, Object> subHeading = new LinkedHashMap<>();
      subHeading.put("type", "text");
      subHeading.put("text_level", 2);
      subHeading.put("text", "2.1 Section");
      subHeading.put("page_idx", 2);
      subHeading.put("bbox", new ArrayList<>(List.of(15.0, 30.0, 200.0, 45.0)));
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Noise");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Noise", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  header,
                  footer,
                  pageNum,
                  subHeading,
                  bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getAllBboxes(), hasSize(2));
      assertThat(block.getAllBboxes().getFirst().bbox(), equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      assertThat(block.getAllBboxes().get(1).bbox(), equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void contentLocatorsForPdfMatchAllBboxes() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Bbox");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Bbox", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getAllBboxes(), hasSize(2));
      assertThat(block.getContentLocators(), hasSize(2));
      for (int i = 0; i < 2; i++) {
        assertThat(block.getContentLocators().get(i), instanceOf(PdfLocator.class));
        PdfLocator loc = (PdfLocator) block.getContentLocators().get(i);
        assertThat(loc.pageIndex(), equalTo(block.getAllBboxes().get(i).pageIndex()));
        assertThat(loc.bbox(), equalTo(block.getAllBboxes().get(i).bbox()));
      }
    }

    @Test
    void fullViewJsonHasEmptyContentLocatorsForEveryEpubBlock() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      controller.attachBook(nb, epubAttachRequest("Minimal EPUB"), epubFile(epubBytes));
      makeMe.entityPersister.flushAndClear();

      Book detail = controller.getBook(nb);
      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detail);
      JsonNode tree = objectMapper.readTree(json);
      JsonNode blocks = tree.get("blocks");
      assertThat(blocks.isArray(), equalTo(true));
      for (JsonNode b : blocks) {
        JsonNode locs = b.get("contentLocators");
        assertThat(locs.isArray(), equalTo(true));
        assertThat(locs.size(), equalTo(0));
      }
    }
  }

  @Nested
  class GetBookFile {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getBookFile(webRequest(), otherNb));
    }

    @Test
    void returnsPdfWhenSourceFileRefPointsAtBlob() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes, "pdf");
      setSourceFileRef(nb, ref);
      String expectedEtag =
          "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      ResponseEntity<byte[]> res = controller.getBookFile(webRequest(), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(pdfBytes));
      assertThat(res.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
      assertThat(res.getHeaders().getETag(), equalTo(expectedEtag));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("inline; filename=\"Linear Algebra.pdf\""));
      assertThat(res.getHeaders().getCacheControl(), containsString("private"));
      assertThat(res.getHeaders().getCacheControl(), containsString("max-age="));
    }

    @Test
    void returnsEpubZipWhenBookFormatIsEpub() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      controller.attachBook(nb, epubAttachRequest("Minimal EPUB"), epubFile(epubBytes));
      makeMe.entityPersister.flushAndClear();
      String ref = bookOf(nb).getSourceFileRef();
      String expectedEtag =
          "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      ResponseEntity<byte[]> res = controller.getBookFile(webRequest(), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(epubBytes));
      assertThat(
          res.getHeaders().getContentType(),
          equalTo(MediaType.parseMediaType("application/epub+zip")));
      assertThat(res.getHeaders().getETag(), equalTo(expectedEtag));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("inline; filename=\"Minimal EPUB.epub\""));
      assertThat(res.getHeaders().getCacheControl(), containsString("private"));
      assertThat(res.getHeaders().getCacheControl(), containsString("max-age="));
    }

    @Test
    void returns304WhenIfNoneMatchMatchesEtag() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes, "pdf");
      setSourceFileRef(nb, ref);
      String etag = "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      MockHttpServletRequest req = new MockHttpServletRequest();
      req.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
      ResponseEntity<byte[]> res = controller.getBookFile(new ServletWebRequest(req), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NOT_MODIFIED));
      assertThat(res.getBody(), nullValue());
      assertThat(res.getHeaders().getETag(), equalTo(etag));
    }

    @Test
    void returns404WhenSourceFileRefIsNotNumeric() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, "not-an-id");
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void returns404WhenSourceFileRefBlobMissing() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, String.valueOf(Integer.MAX_VALUE));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }
  }

  @Nested
  class DeleteBook {
    @Test
    void removesBookRowAndStoredBytes() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      String ref = bookOf(nb).getSourceFileRef();

      controller.deleteBook(nb);

      assertThat(bookRepository.findByNotebook_Id(nb.getId()).isEmpty(), equalTo(true));
      assertThat(bookStorage.get(ref).isEmpty(), equalTo(true));
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void removesEpubBookRowAndStoredBytes() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      controller.attachBook(nb, epubAttachRequest("Minimal EPUB"), epubFile(epubBytes));
      String ref = bookOf(nb).getSourceFileRef();

      controller.deleteBook(nb);

      assertThat(bookRepository.findByNotebook_Id(nb.getId()).isEmpty(), equalTo(true));
      assertThat(bookStorage.get(ref).isEmpty(), equalTo(true));
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.deleteBook(nb));
    }

    @Test
    void rejectsUnauthorizedNotebook() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.deleteBook(otherNb));
    }
  }

  @Nested
  class PatchReadingPosition {
    @Test
    void persistsSnapshotForCurrentUserAndBook() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(nb, lastReadBody(2, 750));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getPageIndex(), equalTo(2));
      assertThat(stored.getNormalizedY(), equalTo(750));
    }

    @Test
    void secondPatchUpdatesSameRow() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(nb, lastReadBody(0, 100));
      controller.patchReadingPosition(nb, lastReadBody(5, 0));

      assertThat(bookUserLastReadPositionRepository.count(), equalTo(1L));
      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getPageIndex(), equalTo(5));
      assertThat(stored.getNormalizedY(), equalTo(0));
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nb, lastReadBody(0, 0)));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.patchReadingPosition(otherNb, lastReadBody(0, 0)));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nb, lastReadBody(0, 0)));
    }

    @Test
    void removesPositionWhenBookDeleted() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      int bookId = bookOf(nb).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 500));

      controller.deleteBook(nb);

      assertThat(
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookId)
              .isEmpty(),
          equalTo(true));
    }

    @Test
    void persistsSelectedBookBlockId() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest ch1 = node("Section 1.1");
      AttachBookLayoutNodeRequest root = node("Chapter 1", ch1);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};
      controller.attachBook(nb, attachRequest(root), pdfFile(pdfBytes));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();

      controller.patchReadingPosition(nb, lastReadBody(3, 420, secondBlockId));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getPageIndex(), equalTo(3));
      assertThat(stored.getNormalizedY(), equalTo(420));
      assertThat(stored.getSelectedBookBlockId(), equalTo(secondBlockId));
    }

    @Test
    void patchRejectsBlockIdFromAnotherNotebookBook() throws Exception {
      Notebook nbA = myNotebook();
      Notebook nbB = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};
      controller.attachBook(nbA, attachRequest(node("A")), pdfFile(pdfBytes));
      controller.attachBook(nbB, attachRequest(node("B")), pdfFile(pdfBytes));
      int blockFromA = blocksByLayoutOrder(bookOf(nbA)).getFirst().getId();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nbB, lastReadBody(0, 100, blockFromA)));
    }

    @Test
    void patchWithoutSelectedBookBlockIdLeavesStoredBlockUnchanged()
        throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      int blockId = blocksByLayoutOrder(bookOf(nb)).getFirst().getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 500, blockId));
      controller.patchReadingPosition(nb, lastReadBody(2, 600));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getPageIndex(), equalTo(2));
      assertThat(stored.getNormalizedY(), equalTo(600));
      assertThat(stored.getSelectedBookBlockId(), equalTo(blockId));
    }

    @Test
    void persistsEpubLocatorAndClearsPdfFields() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(
          nb, lastReadEpubBody("OEBPS/chapter2.xhtml#section-beta-two"));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getEpubLocator(), equalTo("OEBPS/chapter2.xhtml#section-beta-two"));
      assertThat(stored.getPageIndex(), nullValue());
      assertThat(stored.getNormalizedY(), nullValue());
    }

    @Test
    void rejectsPatchWithoutPdfFieldsOrEpubLocator() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      BookLastReadPositionRequest empty = new BookLastReadPositionRequest();

      ApiException ex =
          assertThrows(ApiException.class, () -> controller.patchReadingPosition(nb, empty));
      assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    }
  }

  @Nested
  class GetReadingPosition {
    @Test
    void returnsSavedSnapshotAfterPatch() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      controller.patchReadingPosition(nb, lastReadBody(3, 420));

      ResponseEntity<BookUserLastReadPosition> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(res.getBody().getPageIndex(), equalTo(3));
      assertThat(res.getBody().getNormalizedY(), equalTo(420));
    }

    @Test
    void returnsSelectedBookBlockIdAfterPatch() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest ch1 = node("Section 1.1");
      AttachBookLayoutNodeRequest root = node("Chapter 1", ch1);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};
      controller.attachBook(nb, attachRequest(root), pdfFile(pdfBytes));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 200, secondBlockId));

      ResponseEntity<BookUserLastReadPosition> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(res.getBody().getSelectedBookBlockId(), equalTo(secondBlockId));
    }

    @Test
    void responseBodyExposesOnlySelectedBookBlockIdNotTheEntity() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest ch1 = node("Section 1.1");
      AttachBookLayoutNodeRequest root = node("Chapter 1", ch1);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};
      controller.attachBook(nb, attachRequest(root), pdfFile(pdfBytes));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 200, secondBlockId));

      ResponseEntity<BookUserLastReadPosition> res = controller.getReadingPosition(nb);

      JsonNode json = objectMapper.valueToTree(res.getBody());
      assertThat(json.get("selectedBookBlockId").asInt(), equalTo(secondBlockId));
      assertThat(
          "selectedBookBlock entity must not appear in JSON (causes 500 via Hibernate proxy)",
          json.has("selectedBookBlock"),
          is(false));
    }

    @Test
    void returnsSavedEpubLocatorAfterPatch() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      controller.patchReadingPosition(
          nb, lastReadEpubBody("OEBPS/chapter2.xhtml#section-beta-two"));

      ResponseEntity<BookUserLastReadPosition> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(res.getBody().getEpubLocator(), equalTo("OEBPS/chapter2.xhtml#section-beta-two"));
      assertThat(res.getBody().getPageIndex(), nullValue());
      assertThat(res.getBody().getNormalizedY(), nullValue());
    }

    @Test
    void returns204WhenNoSnapshotStored() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      ResponseEntity<BookUserLastReadPosition> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
      assertThat(res.getBody(), nullValue());
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getReadingPosition(nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getReadingPosition(otherNb));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.getReadingPosition(nb));
    }
  }

  @Nested
  class GetBookReadingRecords {
    @Test
    void returnsRecordForMarkedRange() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      controller.putBlockReadingRecord(nb, range, null);

      var list = controller.getBookReadingRecords(nb);
      assertThat(list, hasSize(1));
      assertThat(list.getFirst().getBookBlockId(), equalTo(range.getId()));
      assertThat(list.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          list.getFirst().getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returnsOnlyMarkedRangesAmongSiblings() throws Exception {
      Notebook nb = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      controller.attachBook(nb, attachRequest(node("2.1"), node("2.2")), pdfFile(pdfBytes));
      Book book = bookOf(nb);
      List<BookBlock> roots = rootBlocksSorted(book);
      BookBlock first = roots.getFirst();
      controller.putBlockReadingRecord(nb, first, null);

      var list = controller.getBookReadingRecords(nb);
      assertThat(list, hasSize(1));
      assertThat(list.getFirst().getBookBlockId(), equalTo(first.getId()));
    }

    @Test
    void doesNotIncludeAnotherUsersRecords() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      User other = makeMe.aUser().please();
      var otherRow = new BookBlockReadingRecord();
      otherRow.setUser(other);
      otherRow.setBookBlock(range);
      otherRow.setStatus(BookBlockReadingRecord.STATUS_READ);
      otherRow.setCompletedAt(testabilitySettings.getCurrentUTCTimestamp());
      makeMe.entityPersister.save(otherRow);
      makeMe.entityPersister.flush();

      assertThat(controller.getBookReadingRecords(nb), empty());
    }

    @Test
    void returnsEmptyWhenNoRecords() throws Exception {
      Notebook nb = notebookWithBook();
      assertThat(controller.getBookReadingRecords(nb), empty());
    }
  }

  @Nested
  class PutBlockReadingRecord {
    @Test
    void persistsReadRecordForCurrentUserAndRange() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();

      var returned = controller.putBlockReadingRecord(nb, range, null);
      assertThat(returned, hasSize(1));
      assertThat(returned.getFirst().getBookBlockId(), equalTo(range.getId()));
      assertThat(returned.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          returned.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));

      var stored =
          bookBlockReadingRecordRepository
              .findByUser_IdAndBookBlock_Id(currentUser.getUser().getId(), range.getId())
              .orElseThrow();
      assertThat(stored.getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(stored.getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returns404WhenNotebookHasNoBook() {
      Notebook nbEmpty = myNotebook();
      Notebook nbWith = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nbWith)).getFirst();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.putBlockReadingRecord(nbEmpty, range, null));
    }

    @Test
    void returns404WhenRangeBelongsToAnotherNotebooksBook() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock otherRange = rootBlocksSorted(bookOf(otherNb)).getFirst();
      Notebook myNb = notebookWithBook();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.putBlockReadingRecord(myNb, otherRange, null));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.putBlockReadingRecord(otherNb, range, null));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      currentUser.setUser(null);

      assertThrows(
          ResponseStatusException.class, () -> controller.putBlockReadingRecord(nb, range, null));
    }

    @Test
    void persistsSkimmedAndSkippedRejectsBadStatus() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      controller.attachBook(nb, attachRequest(node("Block A"), node("Block B")), pdfFile(pdfBytes));
      List<BookBlock> roots = rootBlocksSorted(bookOf(nb));
      assertThat(roots, hasSize(2));
      BookBlock first = roots.getFirst();
      BookBlock second = roots.get(1);

      var skimBody = new BookBlockReadingRecordPutRequest();
      skimBody.setStatus(BookBlockReadingRecord.STATUS_SKIMMED);
      var afterSkim = controller.putBlockReadingRecord(nb, first, skimBody);
      assertThat(afterSkim, hasSize(1));
      assertThat(afterSkim.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_SKIMMED));

      var skipBody = new BookBlockReadingRecordPutRequest();
      skipBody.setStatus(BookBlockReadingRecord.STATUS_SKIPPED);
      var afterSkip = controller.putBlockReadingRecord(nb, second, skipBody);
      assertThat(afterSkip, hasSize(2));
      assertThat(
          afterSkip.stream()
              .filter(i -> i.getBookBlockId().equals(first.getId()))
              .findFirst()
              .orElseThrow()
              .getStatus(),
          equalTo(BookBlockReadingRecord.STATUS_SKIMMED));
      assertThat(
          afterSkip.stream()
              .filter(i -> i.getBookBlockId().equals(second.getId()))
              .findFirst()
              .orElseThrow()
              .getStatus(),
          equalTo(BookBlockReadingRecord.STATUS_SKIPPED));

      var bad = new BookBlockReadingRecordPutRequest();
      bad.setStatus("NOT_A_STATUS");
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.putBlockReadingRecord(nb, first, bad));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Invalid reading record status"));
    }

    @Test
    void secondPutKeepsOneRowOverwritesStatusAndUpdatesCompletedAt()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(0, 10).please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();

      var skim = new BookBlockReadingRecordPutRequest();
      skim.setStatus(BookBlockReadingRecord.STATUS_SKIMMED);
      assertThat(controller.putBlockReadingRecord(nb, range, skim), hasSize(1));

      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 11).please());
      var second = controller.putBlockReadingRecord(nb, range, null);

      assertThat(bookBlockReadingRecordRepository.count(), equalTo(1L));
      assertThat(second.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          second.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));
      var stored =
          bookBlockReadingRecordRepository
              .findByUser_IdAndBookBlock_Id(currentUser.getUser().getId(), range.getId())
              .orElseThrow();
      assertThat(stored.getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(stored.getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }
  }

  @Nested
  class ChangeBookBlockDepth {

    private Notebook nb;
    private byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};

    private BookBlockDepthRequest indent() {
      var r = new BookBlockDepthRequest();
      r.setDirection("INDENT");
      return r;
    }

    private BookBlockDepthRequest outdent() {
      var r = new BookBlockDepthRequest();
      r.setDirection("OUTDENT");
      return r;
    }

    @BeforeEach
    void setup() throws Exception {
      nb = myNotebook();
      // Layout: A(0), B(0), C(1), D(0)
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(pdfBytes));
    }

    private List<BookBlock> blocks() {
      return bookOf(nb).getBlocks();
    }

    private BookBlock blockByTitle(String title) {
      return blocks().stream()
          .filter(b -> b.getStructuralTitle().equals(title))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Block not found: " + title));
    }

    private int depthOfMut(List<BookBlockMutationResponse> blocks, String title) {
      return blocks.stream()
          .filter(b -> b.getTitle().equals(title))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Block not found: " + title))
          .getDepth();
    }

    @Test
    void indentIncreasesDepthByOne() throws Exception {
      BookBlock b = blockByTitle("B");
      assertThat(b.getDepth(), equalTo(0));

      BookMutationResponse result = controller.changeBookBlockDepth(nb, b, indent());

      int newDepth =
          result.getBlocks().stream()
              .filter(blk -> blk.getId() == b.getId())
              .findFirst()
              .orElseThrow()
              .getDepth();
      assertThat(newDepth, equalTo(1));
    }

    @Test
    void indentReturnsFullBookWithAllBlocks() throws Exception {
      BookBlock b = blockByTitle("B");

      BookMutationResponse result = controller.changeBookBlockDepth(nb, b, indent());

      assertThat(result.getBlocks(), hasSize(4));
    }

    @Test
    void indentMovesDescendantsWithHead() throws Exception {
      // Layout: A(0), B(0), C(1), D(0) — indent B should move B to 1 and C to 2
      BookBlock b = blockByTitle("B");

      BookMutationResponse result = controller.changeBookBlockDepth(nb, b, indent());

      List<BookBlockMutationResponse> resultBlocks = result.getBlocks();
      assertThat(depthOfMut(resultBlocks, "A"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "B"), equalTo(1));
      assertThat(depthOfMut(resultBlocks, "C"), equalTo(2));
      assertThat(depthOfMut(resultBlocks, "D"), equalTo(0));
    }

    @Test
    void outdentMovesDescendantsWithHead() throws Exception {
      // Layout: A(0), B(0), C(1), D(0) — outdent C brings it to 0 (leaf, no descendants)
      // To test subtree outdent: attach a deeper layout X(0), Y(1), Z(2), W(0)
      Notebook nb2 = myNotebook();
      controller.attachBook(
          nb2, attachRequest(node("X", node("Y", node("Z"))), node("W")), pdfFile(pdfBytes));
      BookBlock y =
          bookOf(nb2).getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("Y"))
              .findFirst()
              .orElseThrow();

      BookMutationResponse result = controller.changeBookBlockDepth(nb2, y, outdent());

      List<BookBlockMutationResponse> resultBlocks = result.getBlocks();
      assertThat(depthOfMut(resultBlocks, "X"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Y"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Z"), equalTo(1));
      assertThat(depthOfMut(resultBlocks, "W"), equalTo(0));
    }

    @Test
    void indentFirstBlockThrows() {
      BookBlock a = blockByTitle("A");

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, a, indent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Cannot indent the first block"));
    }

    @Test
    void indentWhenAlreadyAtMaxDepthRelativeToPredecessorThrows() {
      // C is at depth 1, its predecessor B is at depth 0; C is already at B.depth+1
      BookBlock c = blockByTitle("C");

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, c, indent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(
          ex.getReason(), equalTo("Block is already at maximum depth relative to predecessor"));
    }

    @Test
    void outdentDecreasesDepthByOne() throws Exception {
      BookBlock c = blockByTitle("C");
      assertThat(c.getDepth(), equalTo(1));

      BookMutationResponse result = controller.changeBookBlockDepth(nb, c, outdent());

      int newDepth =
          result.getBlocks().stream()
              .filter(blk -> blk.getId() == c.getId())
              .findFirst()
              .orElseThrow()
              .getDepth();
      assertThat(newDepth, equalTo(0));
    }

    @Test
    void outdentBlockAtDepthZeroThrows() {
      BookBlock a = blockByTitle("A");

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, a, outdent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Block is already at minimum depth"));
    }

    @Test
    void outdentMovesEntireSubtreeKeepingInvariantValid() throws Exception {
      // X(0), Y(1), Z(2) — outdenting Y moves Y+Z together → X(0), Y(0), Z(1), valid tree
      Notebook nb2 = myNotebook();
      controller.attachBook(nb2, attachRequest(node("X", node("Y", node("Z")))), pdfFile(pdfBytes));
      BookBlock y =
          bookOf(nb2).getBlocks().stream()
              .filter(blk -> blk.getStructuralTitle().equals("Y"))
              .findFirst()
              .orElseThrow();

      BookMutationResponse result = controller.changeBookBlockDepth(nb2, y, outdent());

      List<BookBlockMutationResponse> resultBlocks = result.getBlocks();
      assertThat(depthOfMut(resultBlocks, "X"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Y"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Z"), equalTo(1));
    }

    @Test
    void changeBookBlockDepthJsonOmitsAllBboxesOnEveryBlock() throws Exception {
      BookBlock b = blockByTitle("B");
      BookMutationResponse wire = controller.changeBookBlockDepth(nb, b, indent());
      String json = objectMapper.writeValueAsString(wire);
      JsonNode tree = objectMapper.readTree(json);
      JsonNode blocksNode = tree.get("blocks");
      assertThat(blocksNode.size(), equalTo(4));
      for (JsonNode block : blocksNode) {
        assertThat(block.has("allBboxes"), equalTo(false));
      }
    }

    @Test
    void blockFromAnotherNotebooksBookThrows() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock otherBlock = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.changeBookBlockDepth(nb, otherBlock, indent()));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock block = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.changeBookBlockDepth(otherNb, block, indent()));
    }
  }

  @Nested
  class CreateBookBlockFromContent {

    private static CreateBookBlockFromContentRequest splitRequest(int contentBlockId) {
      return splitRequest(contentBlockId, null);
    }

    private static CreateBookBlockFromContentRequest splitRequest(
        int contentBlockId, String structuralTitle) {
      var req = new CreateBookBlockFromContentRequest();
      req.setFromBookContentBlockId(contentBlockId);
      req.setStructuralTitle(structuralTitle);
      return req;
    }

    @Test
    void createsChildBlockAndMovesTailContent() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      bodyItem.put("page_idx", 1);
      AttachBookLayoutNodeRequest n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));

      ResponseEntity<Book> attachRes =
          controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      assertThat(attachRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book created = attachRes.getBody();
      assertThat(created, notNullValue());
      BookBlock chapter =
          rootBlocksSorted(created).stream()
              .filter(b -> b.getStructuralTitle().equals("Chapter 1"))
              .findFirst()
              .orElseThrow();
      List<BookContentBlock> cbsBefore =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(chapter.getId());
      assertThat(cbsBefore, hasSize(2));
      int secondId = cbsBefore.get(1).getId();

      ResponseEntity<Book> splitRes =
          controller.createBookBlockFromContent(nb, splitRequest(secondId));
      assertThat(splitRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book after = splitRes.getBody();
      assertThat(after, notNullValue());
      assertThat(after.getBlocks(), hasSize(2));

      List<BookBlock> ordered = blocksByLayoutOrder(after);
      assertThat(ordered.get(0).getStructuralTitle(), equalTo("Chapter 1"));
      assertThat(ordered.get(1).getStructuralTitle(), equalTo("Some body text"));
      assertThat(ordered.get(0).getDepth(), equalTo(0));
      assertThat(ordered.get(1).getDepth(), equalTo(1));

      List<BookContentBlock> ownerCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              ordered.get(0).getId());
      List<BookContentBlock> childCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              ordered.get(1).getId());
      assertThat(ownerCbs, hasSize(1));
      assertThat(childCbs, hasSize(1));
      assertThat(childCbs.get(0).getId(), equalTo(secondId));

      for (int i = 0; i < ordered.size(); i++) {
        assertThat(ordered.get(i).getLayoutSequence(), equalTo(i));
      }

      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(after);
      JsonNode tree = objectMapper.readTree(json);
      JsonNode blocksNode = tree.get("blocks");
      assertThat(blocksNode.size(), equalTo(2));
      assertThat(blocksNode.get(0).get("contentBlocks").size(), equalTo(1));
      assertThat(blocksNode.get(1).get("contentBlocks").size(), equalTo(1));
      assertThat(
          blocksNode.get(1).get("contentBlocks").get(0).get("id").asInt(), equalTo(secondId));
    }

    @Test
    void createsChildBlockUsingStructuralTitleOverride() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "W".repeat(550));
      bodyItem.put("page_idx", 1);
      AttachBookLayoutNodeRequest n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));

      ResponseEntity<Book> attachRes =
          controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      assertThat(attachRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book created = attachRes.getBody();
      assertThat(created, notNullValue());
      BookBlock chapter =
          rootBlocksSorted(created).stream()
              .filter(b -> b.getStructuralTitle().equals("Chapter 1"))
              .findFirst()
              .orElseThrow();
      List<BookContentBlock> cbsBefore =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(chapter.getId());
      int secondId = cbsBefore.get(1).getId();

      ResponseEntity<Book> splitRes =
          controller.createBookBlockFromContent(nb, splitRequest(secondId, "My custom title"));
      assertThat(splitRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book after = splitRes.getBody();
      assertThat(after, notNullValue());
      List<BookBlock> ordered = blocksByLayoutOrder(after);
      assertThat(ordered.get(1).getStructuralTitle(), equalTo("My custom title"));
    }

    @Test
    void createBookBlockFromContentRequestRejectsStructuralTitleOverMax() {
      var req = new CreateBookBlockFromContentRequest();
      req.setFromBookContentBlockId(1);
      req.setStructuralTitle("x".repeat(BookBlockTitleLimits.STRUCTURAL_MAX_CHARS + 1));
      try (var factory = Validation.buildDefaultValidatorFactory()) {
        assertThat(factory.getValidator().validate(req), not(empty()));
      }
    }

    @Test
    void unknownContentBlockIdThrows404() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(nb, attachRequest(node("A")), pdfFile(STUB_PDF_BYTES));
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nb, splitRequest(Integer.MAX_VALUE)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void contentBlockFromAnotherNotebookBookThrows404() throws Exception {
      Notebook nbA = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "tail");
      AttachBookLayoutNodeRequest rootA = node("A");
      rootA.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("A", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));
      controller.attachBook(nbA, attachRequest(rootA), pdfFile(STUB_PDF_BYTES));
      BookBlock blockA = rootBlocksSorted(bookOf(nbA)).getFirst();
      int foreignContentId =
          bookContentBlockRepository
              .findAllByBookBlock_IdOrderBySiblingOrder(blockA.getId())
              .get(1)
              .getId();

      Notebook nbB = myNotebook();
      controller.attachBook(nbB, attachRequest(node("B")), pdfFile(STUB_PDF_BYTES));

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nbB, splitRequest(foreignContentId)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void splitAtFirstContentBlockThrows400() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      AttachBookLayoutNodeRequest n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      BookBlock chapter = rootBlocksSorted(bookOf(nb)).getFirst();
      int firstId =
          bookContentBlockRepository
              .findAllByBookBlock_IdOrderBySiblingOrder(chapter.getId())
              .getFirst()
              .getId();

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nb, splitRequest(firstId)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenChildWouldExceedMaxLayoutDepth() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem1 = new LinkedHashMap<>();
      bodyItem1.put("type", "text");
      bodyItem1.put("text", "a");
      Map<String, Object> bodyItem2 = new LinkedHashMap<>();
      bodyItem2.put("type", "text");
      bodyItem2.put("text", "b");
      AttachBookLayoutNodeRequest leaf = node("Leaf");
      leaf.setContentBlocks(new ArrayList<>(List.of(bodyItem1, bodyItem2)));
      AttachBookLayoutNodeRequest cur = leaf;
      for (int i = 0; i < 63; i++) {
        cur = node("B" + i, cur);
      }
      controller.attachBook(nb, attachRequest(cur), pdfFile(STUB_PDF_BYTES));
      Book book = bookOf(nb);
      BookBlock deepestLeaf =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("Leaf"))
              .findFirst()
              .orElseThrow();
      assertThat(deepestLeaf.getDepth(), equalTo(63));
      List<BookContentBlock> cbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(deepestLeaf.getId());
      int secondId = cbs.get(1).getId();

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nb, splitRequest(secondId)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() throws Exception {
      User owner = makeMe.aUser().please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(owner).please();
      currentUser.setUser(owner);
      AttachBookLayoutNodeRequest root = node("R");
      Map<String, Object> b1 = new LinkedHashMap<>();
      b1.put("type", "text");
      b1.put("text", "a");
      Map<String, Object> b2 = new LinkedHashMap<>();
      b2.put("type", "text");
      b2.put("text", "b");
      root.setContentBlocks(new ArrayList<>(List.of(b1, b2)));
      controller.attachBook(otherNb, attachRequest(root), pdfFile(STUB_PDF_BYTES));
      BookBlock blk = rootBlocksSorted(bookOf(otherNb)).getFirst();
      int cid =
          bookContentBlockRepository
              .findAllByBookBlock_IdOrderBySiblingOrder(blk.getId())
              .get(1)
              .getId();

      currentUser.setUser(makeMe.aUser().please());

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createBookBlockFromContent(otherNb, splitRequest(cid)));
    }
  }

  @Nested
  class SuggestBookLayoutReorganization {

    private Notebook nb;
    private final byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};

    @BeforeEach
    void setupBook() throws Exception {
      nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(pdfBytes));
    }

    private BookLayoutReorganizationSuggestion suggestionRenestingBAndC() {
      Book book = bookOf(nb);
      var suggestion = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth(
            switch (b.getStructuralTitle()) {
              case "A" -> 0;
              case "B" -> 1;
              case "C" -> 2;
              case "D" -> 0;
              default -> throw new IllegalStateException();
            });
        items.add(e);
      }
      suggestion.setBlocks(items);
      return suggestion;
    }

    @Test
    void returnsAiSuggestionWithValidatedDepths() throws Exception {
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(suggestionRenestingBAndC());

      BookLayoutReorganizationSuggestion result = controller.suggestBookLayoutReorganization(nb);

      assertThat(result.getBlocks(), hasSize(4));
      Map<String, Integer> byTitle = new LinkedHashMap<>();
      Book book = bookOf(nb);
      for (BookBlock blk : book.getBlocks()) {
        int id = blk.getId();
        int depth =
            result.getBlocks().stream()
                .filter(s -> s.getId().equals(id))
                .map(BlockDepthSuggestion::getDepth)
                .findFirst()
                .orElseThrow();
        byTitle.put(blk.getStructuralTitle(), depth);
      }
      assertThat(byTitle.get("A"), equalTo(0));
      assertThat(byTitle.get("B"), equalTo(1));
      assertThat(byTitle.get("C"), equalTo(2));
      assertThat(byTitle.get("D"), equalTo(0));
    }

    @Test
    void rejectsInvalidPreorderDepthsFromAi() throws Exception {
      Book book = bookOf(nb);
      var bad = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth("A".equals(b.getStructuralTitle()) ? 0 : 2);
        items.add(e);
      }
      bad.setBlocks(items);
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(bad);

      var ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.suggestBookLayoutReorganization(nb));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenAiOmitsABlockId() throws Exception {
      Book book = bookOf(nb);
      var bad = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        if ("C".equals(b.getStructuralTitle())) {
          continue;
        }
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth(0);
        items.add(e);
      }
      bad.setBlocks(items);
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(bad);

      var ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.suggestBookLayoutReorganization(nb));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenAiReturnsEmptyCompletion() {
      openAIChatCompletionMock.mockNullChatCompletion();

      assertThrows(
          OpenAIServiceErrorException.class, () -> controller.suggestBookLayoutReorganization(nb));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.suggestBookLayoutReorganization(otherNb));
    }
  }

  @Nested
  class ApplyBookLayoutReorganization {

    private Notebook nb;
    private final byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};

    @BeforeEach
    void setupBook() throws Exception {
      nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(pdfBytes));
    }

    private BookLayoutReorganizationSuggestion flatSuggestion() {
      Book book = bookOf(nb);
      var suggestion = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth(
            switch (b.getStructuralTitle()) {
              case "A" -> 0;
              case "B" -> 1;
              case "C" -> 2;
              case "D" -> 0;
              default -> throw new IllegalStateException();
            });
        items.add(e);
      }
      suggestion.setBlocks(items);
      return suggestion;
    }

    @Test
    void appliesDepthChangesAndReturnsMutation() throws Exception {
      BookMutationResponse result = controller.applyBookLayoutReorganization(nb, flatSuggestion());

      assertThat(result.getBlocks(), hasSize(4));
      Map<String, Integer> byTitle = new LinkedHashMap<>();
      for (var row : result.getBlocks()) {
        byTitle.put(row.getTitle(), row.getDepth());
      }
      assertThat(byTitle.get("A"), equalTo(0));
      assertThat(byTitle.get("B"), equalTo(1));
      assertThat(byTitle.get("C"), equalTo(2));
      assertThat(byTitle.get("D"), equalTo(0));
    }

    @Test
    void rejectsInvalidPreorderDepths() {
      Book book = bookOf(nb);
      var bad = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth("A".equals(b.getStructuralTitle()) ? 0 : 2);
        items.add(e);
      }
      bad.setBlocks(items);

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.applyBookLayoutReorganization(nb, bad));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.applyBookLayoutReorganization(otherNb, flatSuggestion()));
    }
  }

  @Nested
  class CancelBookBlock {

    @Test
    void titleBecomesContentBlockOfPredecessorWhenCancelledBlockHasNoContent() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(nb, attachRequest(node("A"), node("B")), pdfFile(STUB_PDF_BYTES));
      Book book = bookOf(nb);
      BookBlock blockB =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("B"))
              .findFirst()
              .orElseThrow();
      BookBlock blockA =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("A"))
              .findFirst()
              .orElseThrow();

      BookMutationResponse cancelWire = controller.cancelBookBlock(nb, blockB);
      String cancelJson = objectMapper.writeValueAsString(cancelWire);
      JsonNode cancelTree = objectMapper.readTree(cancelJson);
      int aId = blockA.getId();
      for (JsonNode row : cancelTree.get("blocks")) {
        if (row.get("id").asInt() == aId) {
          assertThat(row.has("allBboxes"), equalTo(true));
        } else {
          assertThat(row.has("allBboxes"), equalTo(false));
        }
      }

      List<BookContentBlock> aCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(blockA.getId());
      assertThat(aCbs, hasSize(1));
      JsonNode raw = objectMapper.readTree(aCbs.getFirst().getRawData());
      assertThat(raw.get("text").asText(), equalTo("B"));
    }

    @Test
    void titleBecomesFirstContentBlockOfPredecessorWhenCancelledBlockHasContent() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body of B");
      bodyItem.put("page_idx", 1);
      AttachBookLayoutNodeRequest nodeB = node("B");
      nodeB.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("B", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));
      controller.attachBook(nb, attachRequest(node("A"), nodeB), pdfFile(STUB_PDF_BYTES));
      Book book = bookOf(nb);
      BookBlock blockB =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("B"))
              .findFirst()
              .orElseThrow();
      BookBlock blockA =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("A"))
              .findFirst()
              .orElseThrow();

      controller.cancelBookBlock(nb, blockB);

      List<BookContentBlock> aCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(blockA.getId());
      assertThat(aCbs, hasSize(2));
      JsonNode headingRaw = objectMapper.readTree(aCbs.get(0).getRawData());
      assertThat(headingRaw.get("text").asText(), equalTo("B"));
      assertThat(headingRaw.has("text_level"), equalTo(false));
      JsonNode bodyRaw = objectMapper.readTree(aCbs.get(1).getRawData());
      assertThat(bodyRaw.get("text").asText(), equalTo("Body of B"));
    }
  }
}
