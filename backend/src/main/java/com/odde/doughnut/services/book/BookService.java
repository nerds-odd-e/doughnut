package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_CONTENT_LIST_ITEMS;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutRequest;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

  public record CancelBlockResult(Book book, int predecessorBlockId) {}

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
      persistContentBlocks(entry.getKey(), entry.getValue());
    }

    return book;
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
    final Optional<BookBlock> selectedBlockPatch =
        request.getSelectedBookBlockId() == null
            ? Optional.empty()
            : Optional.of(resolveBookBlockForBook(request.getSelectedBookBlockId(), book));
    bookUserLastReadPositionRepository
        .findByUser_IdAndBook_Id(user.getId(), book.getId())
        .map(
            existing -> {
              existing.setPageIndex(request.getPageIndex());
              existing.setNormalizedY(request.getNormalizedY());
              selectedBlockPatch.ifPresent(existing::setSelectedBookBlock);
              return entityPersister.save(existing);
            })
        .orElseGet(
            () -> {
              var row = new BookUserLastReadPosition();
              row.setUser(user);
              row.setBook(book);
              row.setPageIndex(request.getPageIndex());
              row.setNormalizedY(request.getNormalizedY());
              selectedBlockPatch.ifPresent(row::setSelectedBookBlock);
              return entityPersister.save(row);
            });
    entityPersister.flush();
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

  @Transactional(readOnly = true)
  public BookPdfFile getBookPdfFile(Notebook notebook) {
    Book book = requireBook(notebook);
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
    if (!BOOK_FORMAT_PDF.equals(request.getFormat())) {
      throw new ApiException(
          "format must be \"pdf\"", ApiError.ErrorType.BINDING_ERROR, "format must be \"pdf\"");
    }

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

  private void preorderAttachBlock(
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
        trimmedMax(node.getTitle(), BookBlockTitleLimits.STRUCTURAL_MAX_CHARS));
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

  private static String trimmedMax(String s, int max) {
    String t = s.trim();
    if (t.length() > max) {
      return t.substring(0, max);
    }
    return t;
  }
}
