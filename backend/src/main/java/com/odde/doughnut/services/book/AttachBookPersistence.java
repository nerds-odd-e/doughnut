package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockTitleLimits;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class AttachBookPersistence {

  private AttachBookPersistence() {}

  static Book persistNewEpubBook(BookService.PersistContext ctx) {
    EntityPersister entityPersister = ctx.entityPersister();
    ObjectMapper objectMapper = ctx.objectMapper();
    byte[] epubBytes = ctx.fileBytes();

    var book = newBook(ctx, BOOK_FORMAT_EPUB);

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
        BookContentBlock cb = newContentBlock(block, j, raw, objectMapper, false);
        block.getContentBlocks().add(cb);
      }
      book.addBlock(block);
    }

    entityPersister.save(book);
    entityPersister.flush();
    return book;
  }

  static Book persistNewPdfBook(BookService.PersistContext ctx) {
    EntityPersister entityPersister = ctx.entityPersister();

    var book = newBook(ctx, BOOK_FORMAT_PDF);

    List<AttachBookLayoutNodeRequest> roots = ctx.request().getLayout().getRoots();
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

  private static Book newBook(BookService.PersistContext ctx, String format) {
    AttachBookRequest request = ctx.request();
    TestabilitySettings testabilitySettings = ctx.testabilitySettings();
    var book = new Book();
    book.setNotebook(ctx.notebook());
    book.setBookName(BookService.trimmedMax(request.getBookName(), 512));
    book.setFormat(format);
    book.setSourceFileRef(ctx.sourceFileRef());
    var now = testabilitySettings.getCurrentUTCTimestamp();
    book.setCreatedAt(now);
    book.setUpdatedAt(now);
    return book;
  }

  private static void preorderAttachBlock(
      Book book,
      AttachBookLayoutNodeRequest node,
      int level,
      int[] layoutSeq,
      IdentityHashMap<AttachBookLayoutNodeRequest, BookBlock> nodeToBlock) {
    AttachBookLayoutValidator.rejectIfExceedsMaxDepth(level);

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
      entityPersister.save(newContentBlock(block, i, cbs.get(i), objectMapper, true));
    }
  }

  private static BookContentBlock newContentBlock(
      BookBlock block,
      int siblingOrder,
      Map<String, Object> raw,
      ObjectMapper objectMapper,
      boolean includePageIdx) {
    BookContentBlock cb = new BookContentBlock();
    cb.setBookBlock(block);
    cb.setSiblingOrder(siblingOrder);
    cb.setType(String.valueOf(raw.getOrDefault("type", "")));
    if (includePageIdx) {
      Object pi = raw.get("page_idx");
      cb.setPageIdx(pi instanceof Number n ? n.intValue() : null);
    } else {
      cb.setPageIdx(null);
    }
    try {
      cb.setRawData(objectMapper.writeValueAsString(raw));
    } catch (JsonProcessingException e) {
      throw new ApiException(
          "failed to serialize content block",
          ApiError.ErrorType.BINDING_ERROR,
          "failed to serialize content block");
    }
    return cb;
  }
}
