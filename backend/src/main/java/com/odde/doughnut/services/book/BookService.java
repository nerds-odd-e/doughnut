package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.controllers.dto.BookBlockReadingRecordListItem;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion.BlockDepthSuggestion;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockReadingRecord;
import com.odde.doughnut.entities.BookBlockTitleLimits;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookBlockRepository;
import com.odde.doughnut.entities.repositories.BookContentBlockRepository;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.TestabilitySettings;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
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
  private final BookBlockRepository bookBlockRepository;
  private final BookContentBlockRepository bookContentBlockRepository;
  private final BookBlockReadingRecordRepository bookBlockReadingRecordRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final BookStorage bookStorage;
  private final ObjectMapper objectMapper;
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;

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
    this.bookBlockRepository = bookBlockRepository;
    this.bookContentBlockRepository = bookContentBlockRepository;
    this.bookBlockReadingRecordRepository = bookBlockReadingRecordRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.bookStorage = bookStorage;
    this.objectMapper = objectMapper;
    this.openAiApiHandler = openAiApiHandler;
    this.globalSettingsService = globalSettingsService;
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
    Book book = requireBook(notebook);
    List<BookBlock> ordered = book.getBlocks();
    if (ordered.isEmpty()) {
      var empty = new BookLayoutReorganizationSuggestion();
      empty.setBlocks(List.of());
      return empty;
    }
    String userJson;
    try {
      userJson = objectMapper.writeValueAsString(layoutBlocksPayload(ordered));
    } catch (JsonProcessingException e) {
      throw new ApiException(
          "failed to serialize book blocks",
          ApiError.ErrorType.BINDING_ERROR,
          "failed to serialize book blocks");
    }
    InstructionAndSchema tool = AiToolFactory.bookLayoutReorganizationAiTool();
    String model = globalSettingsService.globalSettingEvaluation().getValue();
    OpenAIChatRequestBuilder builder =
        new OpenAIChatRequestBuilder().model(model).addUserMessage(userJson);
    JsonNode jsonNode =
        openAiApiHandler
            .requestAndGetJsonSchemaResult(tool, builder)
            .orElseThrow(
                () ->
                    new OpenAIServiceErrorException(
                        "AI did not return a layout reorganization suggestion",
                        HttpStatus.BAD_GATEWAY));
    BookLayoutReorganizationSuggestion suggestion;
    try {
      suggestion =
          objectMapper
              .copy()
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .treeToValue(jsonNode, BookLayoutReorganizationSuggestion.class);
    } catch (JsonProcessingException e) {
      throw new OpenAIServiceErrorException(
          "AI returned malformed layout suggestion", HttpStatus.BAD_GATEWAY);
    }
    validateSuggestedLayout(ordered, suggestion);
    return suggestion;
  }

  private static List<Map<String, Object>> layoutBlocksPayload(List<BookBlock> ordered) {
    List<Map<String, Object>> payload = new ArrayList<>(ordered.size());
    for (BookBlock b : ordered) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", b.getId());
      row.put("title", b.getStructuralTitle());
      row.put("depth", b.getDepth());
      payload.add(row);
    }
    return payload;
  }

  private static void validateSuggestedLayout(
      List<BookBlock> ordered, BookLayoutReorganizationSuggestion suggestion) {
    List<BlockDepthSuggestion> items = suggestion.getBlocks();
    if (items == null || items.size() != ordered.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AI suggestion");
    }
    Set<Integer> inputIds = new HashSet<>();
    for (BookBlock b : ordered) {
      inputIds.add(b.getId());
    }
    Map<Integer, Integer> idToDepth = new HashMap<>();
    for (BlockDepthSuggestion e : items) {
      if (e.getId() == null || e.getDepth() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AI suggestion");
      }
      if (idToDepth.put(e.getId(), e.getDepth()) != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AI suggestion");
      }
    }
    if (!inputIds.equals(idToDepth.keySet())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AI suggestion");
    }
    int[] depths = new int[ordered.size()];
    for (int i = 0; i < ordered.size(); i++) {
      depths[i] = idToDepth.get(ordered.get(i).getId());
    }
    validatePreorderDepths(depths);
  }

  private static void validatePreorderDepths(int[] depths) {
    if (depths.length == 0) {
      return;
    }
    if (depths[0] != 0 || depths[0] > MAX_LAYOUT_DEPTH) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Suggested depths do not form a valid outline");
    }
    for (int i = 1; i < depths.length; i++) {
      int d = depths[i];
      if (d < 0 || d > MAX_LAYOUT_DEPTH || d > depths[i - 1] + 1) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Suggested depths do not form a valid outline");
      }
    }
  }

  @Transactional
  public Book applyLayoutReorganization(
      Notebook notebook, BookLayoutReorganizationSuggestion suggestion) {
    Book book = requireBook(notebook);
    List<BookBlock> ordered = book.getBlocks();
    validateSuggestedLayout(ordered, suggestion);
    Map<Integer, Integer> idToDepth =
        suggestion.getBlocks().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    BlockDepthSuggestion::getId, BlockDepthSuggestion::getDepth));
    for (BookBlock b : ordered) {
      b.setDepth(idToDepth.get(b.getId()));
      entityPersister.save(b);
    }
    entityPersister.flush();
    book.getBlocks().size();
    return book;
  }

  private Book requireBook(Notebook notebook) {
    return bookRepository
        .findByNotebook_Id(notebook.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
  }

  @Transactional(readOnly = true)
  public List<BookBlockReadingRecordListItem> listReadingRecordsForBook(
      Notebook notebook, User user) {
    Book book = requireBook(notebook);
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
    Book book = requireBook(notebook);
    return bookUserLastReadPositionRepository.findByUser_IdAndBook_Id(user.getId(), book.getId());
  }

  @Transactional
  public void upsertLastReadPosition(
      Notebook notebook, User user, BookLastReadPositionRequest request) {
    Book book = requireBook(notebook);
    final boolean hasEpubLocator = request.getEpubLocator() != null;
    final boolean hasPdfPosition =
        request.getPageIndex() != null && request.getNormalizedY() != null;
    if (!hasEpubLocator && !hasPdfPosition) {
      throw new ApiException(
          "reading-position payload requires either pageIndex+normalizedY or epubLocator",
          ApiError.ErrorType.BINDING_ERROR,
          "reading-position payload requires either pageIndex+normalizedY or epubLocator");
    }
    final Optional<BookBlock> selectedBlockPatch =
        request.getSelectedBookBlockId() == null
            ? Optional.empty()
            : Optional.of(resolveBookBlockForBook(request.getSelectedBookBlockId(), book));
    bookUserLastReadPositionRepository
        .findByUser_IdAndBook_Id(user.getId(), book.getId())
        .map(
            existing -> {
              applyReadingPositionFields(existing, request, hasEpubLocator);
              selectedBlockPatch.ifPresent(existing::setSelectedBookBlock);
              return entityPersister.save(existing);
            })
        .orElseGet(
            () -> {
              var row = new BookUserLastReadPosition();
              row.setUser(user);
              row.setBook(book);
              applyReadingPositionFields(row, request, hasEpubLocator);
              selectedBlockPatch.ifPresent(row::setSelectedBookBlock);
              return entityPersister.save(row);
            });
    entityPersister.flush();
  }

  private static void applyReadingPositionFields(
      BookUserLastReadPosition row, BookLastReadPositionRequest request, boolean hasEpubLocator) {
    if (hasEpubLocator) {
      row.setEpubLocator(request.getEpubLocator());
      row.setPageIndex(null);
      row.setNormalizedY(null);
    } else {
      row.setPageIndex(request.getPageIndex());
      row.setNormalizedY(request.getNormalizedY());
    }
  }

  private BookBlock resolveBookBlockForBook(int bookBlockId, Book book) {
    BookBlock bb =
        bookBlockRepository
            .findById(bookBlockId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    if (!bb.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    return bb;
  }

  @Transactional
  public void upsertReadingRecord(
      Notebook notebook, User user, BookBlock bookBlock, String status) {
    if (!(BookBlockReadingRecord.STATUS_READ.equals(status)
        || BookBlockReadingRecord.STATUS_SKIMMED.equals(status)
        || BookBlockReadingRecord.STATUS_SKIPPED.equals(status))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reading record status");
    }
    Book book = requireBook(notebook);
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
  public Book changeBlockDepth(Notebook notebook, BookBlock bookBlock, String direction) {
    Book book = requireBook(notebook);
    if (!bookBlock.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    List<BookBlock> blocks = book.getBlocks();
    int idx = -1;
    for (int i = 0; i < blocks.size(); i++) {
      if (blocks.get(i).getId().equals(bookBlock.getId())) {
        idx = i;
        break;
      }
    }
    if (idx < 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    int currentDepth = bookBlock.getDepth();
    // Collect the subtree: target + contiguous run of blocks deeper than target
    int subtreeEnd = idx + 1;
    while (subtreeEnd < blocks.size() && blocks.get(subtreeEnd).getDepth() > currentDepth) {
      subtreeEnd++;
    }
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

  @Transactional
  public CancelBlockResult cancelBlock(Notebook notebook, BookBlock bookBlock) {
    Book book = requireBook(notebook);
    if (!bookBlock.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    List<BookBlock> blocks = book.getBlocks();
    int idx = -1;
    for (int i = 0; i < blocks.size(); i++) {
      if (blocks.get(i).getId().equals(bookBlock.getId())) {
        idx = i;
        break;
      }
    }
    if (idx < 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
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
        cb.setRawData(stripTextLevel(cb.getRawData()));
        entityPersister.save(cb);
      }
    }
    entityPersister.flush();

    int currentDepth = bookBlock.getDepth();
    int subtreeEnd = idx + 1;
    while (subtreeEnd < blocks.size() && blocks.get(subtreeEnd).getDepth() > currentDepth) {
      subtreeEnd++;
    }
    for (int i = idx + 1; i < subtreeEnd; i++) {
      BookBlock b = blocks.get(i);
      b.setDepth(b.getDepth() - 1);
      entityPersister.save(b);
    }
    entityPersister.flush();

    blocks.remove(bookBlock);
    entityPersister.flush();

    book.getBlocks().size();
    return new CancelBlockResult(book, predecessorBlockId);
  }

  @Transactional
  public Book createBookBlockFromContent(
      Notebook notebook, int fromBookContentBlockId, String structuralTitleOverride) {
    Book book = requireBook(notebook);
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
    int ownerIdx = -1;
    for (int i = 0; i < blocks.size(); i++) {
      if (blocks.get(i).getId().equals(owner.getId())) {
        ownerIdx = i;
        break;
      }
    }
    if (ownerIdx < 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }

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
    String trimmedOverride = trimToNull(structuralTitleOverride);
    newBlock.setStructuralTitle(
        trimmedOverride != null
            ? trimmedMax(trimmedOverride, BookBlockTitleLimits.STRUCTURAL_MAX_CHARS)
            : structuralTitleFromFirstMovedContent(ordered.get(splitAt)));
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
      cb.setRawData(stripTextLevel(cb.getRawData()));
      entityPersister.save(cb);
    }

    renumberLayoutSequences(book);
    book.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.save(book);
    entityPersister.flushAndClear();

    return getBookForNotebook(notebook);
  }

  private void renumberLayoutSequences(Book book) {
    List<BookBlock> blks = book.getBlocks();
    for (int i = 0; i < blks.size(); i++) {
      BookBlock b = blks.get(i);
      b.setLayoutSequence(i);
      entityPersister.save(b);
    }
  }

  private String structuralTitleFromFirstMovedContent(BookContentBlock firstMoved) {
    String raw = firstMoved.getRawData();
    if (raw == null || raw.isBlank()) {
      return "Untitled";
    }
    try {
      JsonNode n = objectMapper.readTree(raw);
      JsonNode text = n.get("text");
      if (text != null && text.isTextual()) {
        String t = trimmedMax(text.asText(), BookBlockTitleLimits.STRUCTURAL_MAX_CHARS);
        if (!t.isEmpty()) {
          return t;
        }
      }
    } catch (JsonProcessingException e) {
      return "Untitled";
    }
    return "Untitled";
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

  private static final MediaType APPLICATION_EPUB_ZIP =
      MediaType.parseMediaType("application/epub+zip");

  @Transactional(readOnly = true)
  public NotebookBookFile getNotebookBookFile(Notebook notebook) {
    Book book = requireBook(notebook);
    String ref = book.getSourceFileRef();
    byte[] bytes =
        bookStorage
            .get(ref)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    String format = book.getFormat();
    String baseName = sanitizeFileName(book.getBookName());
    if (BOOK_FORMAT_PDF.equals(format)) {
      return new NotebookBookFile(
          bytes, baseName + ".pdf", etagForSourceRef(ref), MediaType.APPLICATION_PDF);
    }
    if (BOOK_FORMAT_EPUB.equals(format)) {
      return new NotebookBookFile(
          bytes, baseName + ".epub", etagForSourceRef(ref), APPLICATION_EPUB_ZIP);
    }
    throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR, "unsupported book format: " + format);
  }

  private static String etagForSourceRef(String ref) {
    return "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";
  }

  private static String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\/:*?\"<>|]", "_");
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

  private String stripTextLevel(String rawData) {
    if (rawData == null) {
      return rawData;
    }
    try {
      com.fasterxml.jackson.databind.node.ObjectNode node =
          (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(rawData);
      node.remove("text_level");
      return objectMapper.writeValueAsString(node);
    } catch (Exception e) {
      return rawData;
    }
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  static String trimmedMax(String s, int max) {
    String t = s.trim();
    if (t.length() > max) {
      return t.substring(0, max);
    }
    return t;
  }
}
