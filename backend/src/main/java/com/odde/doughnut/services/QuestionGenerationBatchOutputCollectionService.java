package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.batches.Batch;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchOutputCollectionService {
  private static final Logger logger =
      LoggerFactory.getLogger(QuestionGenerationBatchOutputCollectionService.class);

  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final OpenAiApiHandler openAiApiHandler;
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  public QuestionGenerationBatchOutputCollectionService(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      OpenAiApiHandler openAiApiHandler) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
    this.openAiApiHandler = openAiApiHandler;
  }

  public void collectOutputForCompletedBatches(Timestamp collectedAt) {
    List<QuestionGenerationBatch> completedBatches =
        batchRepository.findByStatusAndOutputCollectedAtIsNull(
            QuestionGenerationBatchStatus.COMPLETED);
    logger.info(
        "Collecting OpenAI output for {} completed question generation batches",
        completedBatches.size());

    int collectedCount = 0;
    int failedCount = 0;

    for (QuestionGenerationBatch batch : completedBatches) {
      try {
        collectOutputForBatch(batch, collectedAt);
        collectedCount++;
      } catch (RuntimeException e) {
        failedCount++;
        logger.warn(
            "Failed to collect OpenAI output for question generation batch {}", batch.getId(), e);
      }
    }

    logger.info(
        "Question generation batch output collection finished: {} collected, {} failed",
        collectedCount,
        failedCount);
  }

  private void collectOutputForBatch(QuestionGenerationBatch batch, Timestamp collectedAt) {
    if (batch.getOpenaiOutputFileId() == null) {
      Batch openAiBatch = openAiApiHandler.retrieveBatch(batch.getOpenaiBatchId());
      batch.setOpenaiOutputFileId(openAiBatch.outputFileId().orElse(null));
      batch.setOpenaiErrorFileId(openAiBatch.errorFileId().orElse(null));
    }

    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(batch.getId());
    Map<String, QuestionGenerationBatchRequest> requestsByCustomId = new HashMap<>();
    for (QuestionGenerationBatchRequest request : requests) {
      requestsByCustomId.put(request.getCustomId(), request);
    }

    Optional.ofNullable(batch.getOpenaiOutputFileId())
        .ifPresent(fileId -> applyOutputFileLines(fileId, requestsByCustomId));
    Optional.ofNullable(batch.getOpenaiErrorFileId())
        .ifPresent(fileId -> applyErrorFileLines(fileId, requestsByCustomId));

    for (QuestionGenerationBatchRequest request : requests) {
      if (request.getStatus() == QuestionGenerationBatchRequestStatus.PENDING) {
        request.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
        request.setErrorDetail("missing batch output line");
      }
    }
    batchRequestRepository.saveAll(requests);

    batch.setOutputCollectedAt(collectedAt);
    batchRepository.saveAndFlush(batch);
  }

  private void applyOutputFileLines(
      String fileId, Map<String, QuestionGenerationBatchRequest> requestsByCustomId) {
    for (String line : splitJsonl(openAiApiHandler.downloadFileContent(fileId))) {
      applyOutputLine(line, requestsByCustomId);
    }
  }

  private void applyErrorFileLines(
      String fileId, Map<String, QuestionGenerationBatchRequest> requestsByCustomId) {
    for (String line : splitJsonl(openAiApiHandler.downloadFileContent(fileId))) {
      applyErrorLine(line, requestsByCustomId);
    }
  }

  private void applyOutputLine(
      String line, Map<String, QuestionGenerationBatchRequest> requestsByCustomId) {
    Optional<OutputLine> parsed = parseOutputLine(line);
    if (parsed.isEmpty()) {
      return;
    }
    OutputLine outputLine = parsed.get();
    QuestionGenerationBatchRequest request = requestsByCustomId.get(outputLine.customId());
    if (request == null) {
      logger.warn(
          "Ignoring OpenAI batch output line with unknown custom_id {}", outputLine.customId());
      return;
    }
    if (outputLine.success()) {
      request.setStatus(QuestionGenerationBatchRequestStatus.OUTPUT_READY);
      request.setRawSuccessPayload(outputLine.rawLine());
      request.setRawErrorPayload(null);
      request.setErrorDetail(null);
    } else {
      request.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
      request.setRawSuccessPayload(null);
      request.setRawErrorPayload(outputLine.rawLine());
      request.setErrorDetail(outputLine.errorDetail());
    }
  }

  private void applyErrorLine(
      String line, Map<String, QuestionGenerationBatchRequest> requestsByCustomId) {
    Optional<ErrorLine> parsed = parseErrorLine(line);
    if (parsed.isEmpty()) {
      return;
    }
    ErrorLine errorLine = parsed.get();
    QuestionGenerationBatchRequest request = requestsByCustomId.get(errorLine.customId());
    if (request == null) {
      logger.warn(
          "Ignoring OpenAI batch error line with unknown custom_id {}", errorLine.customId());
      return;
    }
    request.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
    request.setRawSuccessPayload(null);
    request.setRawErrorPayload(errorLine.rawLine());
    request.setErrorDetail(errorLine.errorDetail());
  }

  private Optional<OutputLine> parseOutputLine(String line) {
    try {
      JsonNode root = objectMapper.readTree(line);
      JsonNode customIdNode = root.get("custom_id");
      if (customIdNode == null || customIdNode.isNull() || customIdNode.asText().isBlank()) {
        logger.warn("Ignoring OpenAI batch output line with missing custom_id");
        return Optional.empty();
      }
      String customId = customIdNode.asText();
      JsonNode errorNode = root.get("error");
      if (errorNode != null && !errorNode.isNull()) {
        return Optional.of(new OutputLine(customId, false, line, summarizeErrorNode(errorNode)));
      }
      JsonNode responseNode = root.get("response");
      if (responseNode == null || responseNode.isNull()) {
        return Optional.of(
            new OutputLine(customId, false, line, "missing response in batch output line"));
      }
      int statusCode = responseNode.path("status_code").asInt(0);
      if (statusCode < 200 || statusCode >= 300) {
        return Optional.of(
            new OutputLine(
                customId, false, line, "batch output response status_code " + statusCode));
      }
      return Optional.of(new OutputLine(customId, true, line, null));
    } catch (JsonProcessingException | RuntimeException e) {
      logger.warn("Ignoring malformed OpenAI batch output line", e);
      return Optional.empty();
    }
  }

  private Optional<ErrorLine> parseErrorLine(String line) {
    try {
      JsonNode root = objectMapper.readTree(line);
      JsonNode customIdNode = root.get("custom_id");
      if (customIdNode == null || customIdNode.isNull() || customIdNode.asText().isBlank()) {
        logger.warn("Ignoring OpenAI batch error line with missing custom_id");
        return Optional.empty();
      }
      String customId = customIdNode.asText();
      JsonNode errorNode = root.get("error");
      return Optional.of(new ErrorLine(customId, line, summarizeErrorNode(errorNode)));
    } catch (JsonProcessingException | RuntimeException e) {
      logger.warn("Ignoring malformed OpenAI batch error line", e);
      return Optional.empty();
    }
  }

  private String summarizeErrorNode(JsonNode errorNode) {
    if (errorNode == null || errorNode.isNull()) {
      return "batch error line without error object";
    }
    JsonNode messageNode = errorNode.get("message");
    if (messageNode != null && !messageNode.isNull() && !messageNode.asText().isBlank()) {
      return messageNode.asText();
    }
    return errorNode.toString();
  }

  private List<String> splitJsonl(String content) {
    if (content == null || content.isBlank()) {
      return List.of();
    }
    return content.lines().filter(line -> !line.isBlank()).toList();
  }

  private record OutputLine(String customId, boolean success, String rawLine, String errorDetail) {}

  private record ErrorLine(String customId, String rawLine, String errorDetail) {}
}
