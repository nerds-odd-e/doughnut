package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_CONTENT_LIST_ITEMS;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockTitleLimits;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public enum BookFormat {
  PDF {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockPdfContentLocators.pdfContentLocators(contentBlocks);
    }

    @Override
    public void validateAttachRequest(AttachBookRequest request) {
      List<Object> contentList = request.getContentList();
      boolean hasContentList = contentList != null && !contentList.isEmpty();
      AttachBookLayoutRequest layout = request.getLayout();
      List<AttachBookLayoutNodeRequest> layoutRoots =
          layout != null && layout.getRoots() != null ? layout.getRoots() : null;
      boolean hasLayoutRoots = layoutRoots != null && !layoutRoots.isEmpty();

      if (hasContentList && hasLayoutRoots) {
        throw new ApiException(
            "cannot send both layout.roots and contentList",
            ApiError.ErrorType.BINDING_ERROR,
            "cannot send both layout.roots and contentList");
      }
      if (!hasContentList && !hasLayoutRoots) {
        throw new ApiException(
            "exactly one of layout.roots or contentList is required",
            ApiError.ErrorType.BINDING_ERROR,
            "exactly one of layout.roots or contentList is required");
      }

      if (hasContentList) {
        if (contentList.size() > MAX_CONTENT_LIST_ITEMS) {
          throw new ApiException(
              "contentList exceeds maximum size of " + MAX_CONTENT_LIST_ITEMS,
              ApiError.ErrorType.BINDING_ERROR,
              "contentList exceeds maximum size of " + MAX_CONTENT_LIST_ITEMS);
        }
        AttachBookLayoutRequest built = MineruContentListLayoutBuilder.buildLayout(contentList);
        if (built.getRoots().isEmpty()) {
          throw new ApiException(
              "contentList produced no book layout blocks",
              ApiError.ErrorType.BINDING_ERROR,
              "contentList produced no book layout blocks");
        }
        request.setLayout(built);
        layoutRoots = built.getRoots();
      }

      for (AttachBookLayoutNodeRequest root : layoutRoots) {
        validateLayoutNode(root, 1);
      }
    }

    @Override
    public Book persistNewBook(BookService.PersistContext ctx) {
      return persistNewPdfBook(ctx);
    }

    @Override
    public MediaType bookFileMediaType() {
      return MediaType.APPLICATION_PDF;
    }

    @Override
    public String bookFileExtension() {
      return ".pdf";
    }

    @Override
    public void writeReadingPositionLocator(
        BookUserLastReadPosition row, ContentLocator locator, ObjectMapper objectMapper) {
      if (!(locator instanceof PdfLocator pdf)) {
        throw new ApiException(
            "PdfLocator_Full required for PDF reading-position locator",
            ApiError.ErrorType.BINDING_ERROR,
            "PdfLocator_Full required for PDF reading-position locator");
      }
      List<Double> bbox = pdf.bbox();
      if (bbox == null || bbox.size() != 4) {
        throw new ApiException(
            "PdfLocator_Full bbox must have four numbers",
            ApiError.ErrorType.BINDING_ERROR,
            "PdfLocator_Full bbox must have four numbers");
      }
      persistReadingPositionLocatorJson(row, objectMapper, locator);
    }
  },
  EPUB {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockEpubContentLocators.epubContentLocators(contentBlocks);
    }

    @Override
    public void validateAttachRequest(AttachBookRequest request) {
      List<Object> contentList = request.getContentList();
      boolean hasContentList = contentList != null && !contentList.isEmpty();
      AttachBookLayoutRequest layout = request.getLayout();
      List<AttachBookLayoutNodeRequest> layoutRoots =
          layout != null && layout.getRoots() != null ? layout.getRoots() : null;
      boolean hasLayoutRoots = layoutRoots != null && !layoutRoots.isEmpty();

      if (hasContentList || hasLayoutRoots) {
        throw new ApiException(
            "EPUB attach must not include layout or contentList",
            ApiError.ErrorType.BINDING_ERROR,
            "EPUB attach must not include layout or contentList");
      }
    }

    @Override
    public Book persistNewBook(BookService.PersistContext ctx) {
      return persistNewEpubBook(ctx);
    }

    @Override
    public MediaType bookFileMediaType() {
      return MediaType.parseMediaType("application/epub+zip");
    }

    @Override
    public String bookFileExtension() {
      return ".epub";
    }

    @Override
    public void writeReadingPositionLocator(
        BookUserLastReadPosition row, ContentLocator locator, ObjectMapper objectMapper) {
      if (!(locator instanceof EpubLocator epub)) {
        throw new ApiException(
            "EpubLocator_Full required for EPUB reading-position locator",
            ApiError.ErrorType.BINDING_ERROR,
            "EpubLocator_Full required for EPUB reading-position locator");
      }
      String href = trimToNull(epub.href());
      if (href == null) {
        throw new ApiException(
            "EpubLocator_Full href is required",
            ApiError.ErrorType.BINDING_ERROR,
            "EpubLocator_Full href is required");
      }
      String frag = epub.fragment();
      if (frag != null && frag.startsWith("#")) {
        frag = frag.substring(1);
      }
      frag = trimToNull(frag);
      persistReadingPositionLocatorJson(row, objectMapper, new EpubLocator(href, frag));
    }
  };

  public abstract List<ContentLocator> assembleContentLocators(
      List<BookContentBlock> contentBlocks);

  public abstract void validateAttachRequest(AttachBookRequest request);

  public abstract Book persistNewBook(BookService.PersistContext ctx);

  public abstract MediaType bookFileMediaType();

  public abstract String bookFileExtension();

  public abstract void writeReadingPositionLocator(
      BookUserLastReadPosition row, ContentLocator locator, ObjectMapper objectMapper);

  public static BookFormat forLocator(ContentLocator locator) {
    return switch (locator) {
      case EpubLocator e -> EPUB;
      case PdfLocator p -> PDF;
    };
  }

  public final ResponseEntity<Resource> streamFile(
      byte[] bytes, String baseName, String etag, CacheControl cacheControl) {
    return ResponseEntity.ok()
        .eTag(etag)
        .cacheControl(cacheControl)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + baseName + bookFileExtension() + "\"")
        .contentType(bookFileMediaType())
        .body(new ByteArrayResource(bytes));
  }

  public static BookFormat fromString(String format) {
    if (BookReadingWireConstants.BOOK_FORMAT_EPUB.equals(format)) {
      return EPUB;
    }
    return PDF;
  }

  private static void validateLayoutNode(AttachBookLayoutNodeRequest node, int depth) {
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
    if (title.length() > BookBlockTitleLimits.STRUCTURAL_MAX_CHARS) {
      throw new ApiException(
          "title exceeds maximum length",
          ApiError.ErrorType.BINDING_ERROR,
          "title exceeds maximum length");
    }
    List<AttachBookLayoutNodeRequest> children = node.getChildren();
    if (children != null) {
      for (AttachBookLayoutNodeRequest child : children) {
        validateLayoutNode(child, depth + 1);
      }
    }
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static void persistReadingPositionLocatorJson(
      BookUserLastReadPosition row, ObjectMapper objectMapper, ContentLocator locator) {
    try {
      row.setReadingPositionLocatorJson(objectMapper.writeValueAsString(locator));
    } catch (JsonProcessingException e) {
      throw new ApiException(
          "failed to serialize reading position locator",
          ApiError.ErrorType.BINDING_ERROR,
          "failed to serialize reading position locator");
    }
  }

  private static Book persistNewEpubBook(BookService.PersistContext ctx) {
    EntityPersister entityPersister = ctx.entityPersister();
    ObjectMapper objectMapper = ctx.objectMapper();
    TestabilitySettings testabilitySettings = ctx.testabilitySettings();
    AttachBookRequest request = ctx.request();
    byte[] epubBytes = ctx.fileBytes();

    var book = new Book();
    book.setNotebook(ctx.notebook());
    book.setBookName(BookService.trimmedMax(request.getBookName(), 512));
    book.setFormat(BOOK_FORMAT_EPUB);
    book.setSourceFileRef(ctx.sourceFileRef());
    var now = testabilitySettings.getCurrentUTCTimestamp();
    book.setCreatedAt(now);
    book.setUpdatedAt(now);

    List<EpubStructureExtractor.EpubLayoutBlock> layout =
        EpubStructureExtractor.extractEpubLayoutWithContent(epubBytes);
    for (int i = 0; i < layout.size(); i++) {
      EpubStructureExtractor.EpubLayoutBlock row = layout.get(i);
      BookBlock block = new BookBlock();
      block.setStructuralTitle(
          BookService.trimmedMax(row.title(), BookBlockTitleLimits.STRUCTURAL_MAX_CHARS));
      block.setDepth(row.depth());
      block.setLayoutSequence(i);
      List<Map<String, Object>> payloads = row.contentPayloads();
      for (int j = 0; j < payloads.size(); j++) {
        Map<String, Object> raw = payloads.get(j);
        BookContentBlock cb = new BookContentBlock();
        cb.setBookBlock(block);
        cb.setSiblingOrder(j);
        cb.setType(String.valueOf(raw.getOrDefault("type", "")));
        cb.setPageIdx(null);
        try {
          cb.setRawData(objectMapper.writeValueAsString(raw));
        } catch (JsonProcessingException e) {
          throw new ApiException(
              "failed to serialize content block",
              ApiError.ErrorType.BINDING_ERROR,
              "failed to serialize content block");
        }
        block.getContentBlocks().add(cb);
      }
      book.addBlock(block);
    }

    entityPersister.save(book);
    entityPersister.flush();
    return book;
  }

  private static Book persistNewPdfBook(BookService.PersistContext ctx) {
    EntityPersister entityPersister = ctx.entityPersister();
    AttachBookRequest request = ctx.request();
    TestabilitySettings testabilitySettings = ctx.testabilitySettings();

    var book = new Book();
    book.setNotebook(ctx.notebook());
    book.setBookName(BookService.trimmedMax(request.getBookName(), 512));
    book.setFormat(BOOK_FORMAT_PDF);
    book.setSourceFileRef(ctx.sourceFileRef());
    var now = testabilitySettings.getCurrentUTCTimestamp();
    book.setCreatedAt(now);
    book.setUpdatedAt(now);

    List<AttachBookLayoutNodeRequest> roots = request.getLayout().getRoots();
    IdentityHashMap<AttachBookLayoutNodeRequest, BookBlock> nodeToBlock = new IdentityHashMap<>();
    int[] seq = {0};
    for (AttachBookLayoutNodeRequest root : roots) {
      preorderAttachBlock(book, root, 1, seq, nodeToBlock);
    }

    entityPersister.save(book);
    entityPersister.flush();

    List<Map.Entry<BookBlock, List<Map<String, Object>>>> pendingContentBlocks = new ArrayList<>();
    for (AttachBookLayoutNodeRequest root : roots) {
      postorderCollectPending(root, nodeToBlock, pendingContentBlocks);
    }
    for (var entry : pendingContentBlocks) {
      persistContentBlocks(ctx, entry.getKey(), entry.getValue());
    }

    return book;
  }

  private static void preorderAttachBlock(
      Book book,
      AttachBookLayoutNodeRequest node,
      int level,
      int[] layoutSeq,
      IdentityHashMap<AttachBookLayoutNodeRequest, BookBlock> nodeToBlock) {
    if (level > MAX_LAYOUT_DEPTH) {
      throw new ApiException(
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH,
          ApiError.ErrorType.BINDING_ERROR,
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }

    BookBlock block = new BookBlock();
    block.setStructuralTitle(
        BookService.trimmedMax(node.getTitle(), BookBlockTitleLimits.STRUCTURAL_MAX_CHARS));
    block.setLayoutSequence(layoutSeq[0]++);
    block.setDepth(level - 1);
    nodeToBlock.put(node, block);
    book.addBlock(block);

    List<AttachBookLayoutNodeRequest> children =
        node.getChildren() == null ? List.of() : node.getChildren();
    for (AttachBookLayoutNodeRequest child : children) {
      preorderAttachBlock(book, child, level + 1, layoutSeq, nodeToBlock);
    }
  }

  private static void postorderCollectPending(
      AttachBookLayoutNodeRequest node,
      IdentityHashMap<AttachBookLayoutNodeRequest, BookBlock> nodeToBlock,
      List<Map.Entry<BookBlock, List<Map<String, Object>>>> pendingContentBlocks) {
    List<AttachBookLayoutNodeRequest> children =
        node.getChildren() == null ? List.of() : node.getChildren();
    for (AttachBookLayoutNodeRequest child : children) {
      postorderCollectPending(child, nodeToBlock, pendingContentBlocks);
    }
    List<Map<String, Object>> cbs = node.getContentBlocks();
    if (cbs != null && !cbs.isEmpty()) {
      pendingContentBlocks.add(new AbstractMap.SimpleEntry<>(nodeToBlock.get(node), cbs));
    }
  }

  private static void persistContentBlocks(
      BookService.PersistContext ctx, BookBlock block, List<Map<String, Object>> cbs) {
    EntityPersister entityPersister = ctx.entityPersister();
    ObjectMapper objectMapper = ctx.objectMapper();
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
}
