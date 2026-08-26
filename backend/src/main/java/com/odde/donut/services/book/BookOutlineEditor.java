package com.odde.donut.services.book;

import static com.odde.donut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.BookBlockTitleLimits;
import com.odde.donut.entities.BookContentBlock;
import com.odde.donut.entities.repositories.BookContentBlockRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class BookOutlineEditor {

  private final BookContentBlockRepository bookContentBlockRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final ObjectMapper objectMapper;

  BookOutlineEditor(
      BookContentBlockRepository bookContentBlockRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      ObjectMapper objectMapper) {
    this.bookContentBlockRepository = bookContentBlockRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.objectMapper = objectMapper;
  }

  Book changeBlockDepth(Book book, BookBlock bookBlock, String direction) {
    if (!bookBlock.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    List<BookBlock> blocks = book.getBlocks();
    int idx = indexOfBlock(blocks, bookBlock);
    int currentDepth = bookBlock.getDepth();
    int subtreeEnd = subtreeEnd(blocks, idx, currentDepth);
    int delta;
    if ("INDENT".equals(direction)) {
      if (idx == 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot indent the first block");
      }
      int predecessorDepth = blocks.get(idx - 1).getDepth();
      if (currentDepth >= predecessorDepth + 1) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Block is already at maximum depth relative to predecessor");
      }
      delta = 1;
    } else {
      if (currentDepth == 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Block is already at minimum depth");
      }
      delta = -1;
    }
    for (int i = idx; i < subtreeEnd; i++) {
      BookBlock b = blocks.get(i);
      b.setDepth(b.getDepth() + delta);
      entityPersister.save(b);
    }
    entityPersister.flush();
    book.getBlocks().size();
    return book;
  }

  BookService.CancelBlockResult cancelBlock(Book book, BookBlock bookBlock) {
    if (!bookBlock.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    List<BookBlock> blocks = book.getBlocks();
    int idx = indexOfBlock(blocks, bookBlock);
    if (idx == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel the first block");
    }
    int predecessorBlockId = blocks.get(idx - 1).getId();
    BookBlock predecessor = blocks.get(idx - 1);

    List<BookContentBlock> predecessorContentBlocks =
        bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(predecessor.getId());
    List<BookContentBlock> cancelledContentBlocks =
        bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(bookBlock.getId());
    int offset = predecessorContentBlocks.size();
    if (cancelledContentBlocks.isEmpty()) {
      BookContentBlock titleCb = new BookContentBlock();
      titleCb.setBookBlock(predecessor);
      titleCb.setSiblingOrder(offset);
      titleCb.setType("text");
      try {
        titleCb.setRawData(
            objectMapper.writeValueAsString(
                Map.of("type", "text", "text", bookBlock.getStructuralTitle())));
      } catch (JsonProcessingException e) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize title content block");
      }
      entityPersister.save(titleCb);
    } else {
      for (int i = 0; i < cancelledContentBlocks.size(); i++) {
        BookContentBlock cb = cancelledContentBlocks.get(i);
        cb.setBookBlock(predecessor);
        cb.setSiblingOrder(offset + i);
        cb.setRawData(BookContentBlockPayloads.stripTextLevel(objectMapper, cb.getRawData()));
        entityPersister.save(cb);
      }
    }
    entityPersister.flush();

    int currentDepth = bookBlock.getDepth();
    int subtreeEnd = subtreeEnd(blocks, idx, currentDepth);
    for (int i = idx + 1; i < subtreeEnd; i++) {
      BookBlock b = blocks.get(i);
      b.setDepth(b.getDepth() - 1);
      entityPersister.save(b);
    }
    entityPersister.flush();

    blocks.remove(bookBlock);
    entityPersister.flush();

    book.getBlocks().size();
    return new BookService.CancelBlockResult(book, predecessorBlockId);
  }

  void splitAtContent(Book book, int fromBookContentBlockId, String structuralTitleOverride) {
    BookContentBlock pivot =
        bookContentBlockRepository
            .findById(fromBookContentBlockId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    BookBlock owner = pivot.getBookBlock();
    if (!owner.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    if (owner.getDepth() + 1 >= MAX_LAYOUT_DEPTH) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot increase nesting depth further");
    }

    List<BookBlock> blocks = book.getBlocks();
    int ownerIdx = indexOfBlock(blocks, owner);

    List<BookContentBlock> ordered =
        bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(owner.getId());
    int splitAt = -1;
    for (int i = 0; i < ordered.size(); i++) {
      if (ordered.get(i).getId().equals(fromBookContentBlockId)) {
        splitAt = i;
        break;
      }
    }
    if (splitAt < 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    if (splitAt == 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot split at the first content block");
    }

    BookBlock newBlock = new BookBlock();
    String trimmedOverride = BookContentBlockPayloads.trimToNull(structuralTitleOverride);
    newBlock.setStructuralTitle(
        trimmedOverride != null
            ? BookService.trimmedMax(trimmedOverride, BookBlockTitleLimits.STRUCTURAL_MAX_CHARS)
            : BookContentBlockPayloads.structuralTitleFromFirstMoved(
                objectMapper, ordered.get(splitAt)));
    newBlock.setDepth(owner.getDepth() + 1);
    book.getBlocks().add(ownerIdx + 1, newBlock);
    newBlock.setBook(book);

    for (int i = 0; i < splitAt; i++) {
      BookContentBlock cb = ordered.get(i);
      cb.setSiblingOrder(i);
      entityPersister.save(cb);
    }
    for (int i = splitAt; i < ordered.size(); i++) {
      BookContentBlock cb = ordered.get(i);
      cb.setBookBlock(newBlock);
      cb.setSiblingOrder(i - splitAt);
      cb.setRawData(BookContentBlockPayloads.stripTextLevel(objectMapper, cb.getRawData()));
      entityPersister.save(cb);
    }

    renumberLayoutSequences(book);
    book.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.save(book);
    entityPersister.flushAndClear();
  }

  private static int indexOfBlock(List<BookBlock> blocks, BookBlock bookBlock) {
    for (int i = 0; i < blocks.size(); i++) {
      if (blocks.get(i).getId().equals(bookBlock.getId())) {
        return i;
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
  }

  private static int subtreeEnd(List<BookBlock> blocks, int idx, int currentDepth) {
    int subtreeEnd = idx + 1;
    while (subtreeEnd < blocks.size() && blocks.get(subtreeEnd).getDepth() > currentDepth) {
      subtreeEnd++;
    }
    return subtreeEnd;
  }

  private void renumberLayoutSequences(Book book) {
    List<BookBlock> blks = book.getBlocks();
    for (int i = 0; i < blks.size(); i++) {
      BookBlock b = blks.get(i);
      b.setLayoutSequence(i);
      entityPersister.save(b);
    }
  }
}
