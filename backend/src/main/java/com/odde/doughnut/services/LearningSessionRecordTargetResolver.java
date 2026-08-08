package com.odde.doughnut.services;

import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LearningSessionRecordTargetResolver {

  public record LearningSessionRecordTarget(LearningSession session, boolean isAmend) {}

  private final LearningSessionRepository learningSessionRepository;

  @Autowired
  public LearningSessionRecordTargetResolver(LearningSessionRepository learningSessionRepository) {
    this.learningSessionRepository = learningSessionRepository;
  }

  public LearningSessionRecordTarget resolve(
      User user, Notebook notebook, Integer learningSessionId) {
    List<LearningSession> awaitingSessions =
        learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
            user.getId(), notebook.getId(), LearningSessionStatus.AWAITING_REPORT);

    if (learningSessionId != null) {
      LearningSession session =
          learningSessionRepository
              .findById(learningSessionId)
              .filter(s -> s.getUser().getId().equals(user.getId()))
              .filter(s -> s.getNotebook().getId().equals(notebook.getId()))
              .filter(s -> s.getStatus() == LearningSessionStatus.RECORDED)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "No recorded learning session found for amend."));
      return new LearningSessionRecordTarget(session, true);
    }
    if (!awaitingSessions.isEmpty()) {
      return new LearningSessionRecordTarget(awaitingSessions.getFirst(), false);
    }
    List<LearningSession> recordedSessions =
        learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
            user.getId(), notebook.getId(), LearningSessionStatus.RECORDED);
    if (recordedSessions.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "No learning session to record or amend for this notebook.");
    }
    LearningSession session =
        recordedSessions.stream()
            .sorted(
                Comparator.comparing(
                        LearningSession::getRecordedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(LearningSession::getId, Comparator.reverseOrder()))
            .findFirst()
            .orElseThrow();
    return new LearningSessionRecordTarget(session, true);
  }
}
