package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion.BlockDepthSuggestion;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class BookLayoutReorganizer {

  private final ObjectMapper objectMapper;
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;
  private final EntityPersister entityPersister;

  BookLayoutReorganizer(
      ObjectMapper objectMapper,
      OpenAiApiHandler openAiApiHandler,
      GlobalSettingsService globalSettingsService,
      EntityPersister entityPersister) {
    this.objectMapper = objectMapper;
    this.openAiApiHandler = openAiApiHandler;
    this.globalSettingsService = globalSettingsService;
    this.entityPersister = entityPersister;
  }

  BookLayoutReorganizationSuggestion suggest(Book book) {
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
    StructuredResponseCreateParams<BookLayoutReorganizationSuggestion> params =
        new OpenAIResponseRequestBuilder<>(BookLayoutReorganizationSuggestion.class)
            .model(model)
            .addInstruction(tool.getMessageBody())
            .addUserMessage(userJson)
            .build();
    BookLayoutReorganizationSuggestion suggestion =
        openAiApiHandler
            .requestAndGetStructuredResponseResult(params)
            .orElseThrow(
                () ->
                    new OpenAIServiceErrorException(
                        "AI did not return a book layout reorganization suggestion",
                        HttpStatus.BAD_GATEWAY));
    validateSuggestedLayout(ordered, suggestion);
    return suggestion;
  }

  Book apply(Book book, BookLayoutReorganizationSuggestion suggestion) {
    List<BookBlock> ordered = book.getBlocks();
    validateSuggestedLayout(ordered, suggestion);
    Map<Integer, Integer> idToDepth =
        suggestion.getBlocks().stream()
            .collect(Collectors.toMap(BlockDepthSuggestion::getId, BlockDepthSuggestion::getDepth));
    for (BookBlock b : ordered) {
      b.setDepth(idToDepth.get(b.getId()));
      entityPersister.save(b);
    }
    entityPersister.flush();
    book.getBlocks().size();
    return book;
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
}
