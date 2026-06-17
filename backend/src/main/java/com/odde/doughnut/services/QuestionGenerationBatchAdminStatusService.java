package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchAdminStatusService {
  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final String openAiToken;
  private final Environment environment;

  public QuestionGenerationBatchAdminStatusService(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      @Value("${spring.openai.token}") String openAiToken,
      Environment environment) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
    this.openAiToken = openAiToken;
    this.environment = environment;
  }

  public QuestionGenerationBatchAdminStatusDTO getStatus() {
    QuestionGenerationBatchAdminStatusDTO dto = new QuestionGenerationBatchAdminStatusDTO();
    dto.setBatchCountsByStatus(
        toStatusCountMap(batchRepository.countByStatus(), QuestionGenerationBatchStatus.values()));
    dto.setRequestCountsByStatus(
        toStatusCountMap(
            batchRequestRepository.countByStatus(), QuestionGenerationBatchRequestStatus.values()));
    dto.setOpenAiTokenConfigured(openAiToken != null && !openAiToken.isBlank());
    dto.setSchedulerActive(Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equals));
    return dto;
  }

  private static <E extends Enum<E>> Map<String, Long> toStatusCountMap(
      List<Object[]> rows, E[] allStatuses) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (E status : allStatuses) {
      counts.put(status.name(), 0L);
    }
    for (Object[] row : rows) {
      @SuppressWarnings("unchecked")
      E status = (E) row[0];
      counts.put(status.name(), (Long) row[1]);
    }
    return counts;
  }
}
