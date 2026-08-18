package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.controllers.dto.ThresholdExceededResult;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.doughnut.entities.repositories.ConversationRepository;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.RecallLogRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemoryTrackerService {
  private static final int FREQUENT_FAILURE_THRESHOLD = 5;
  private static final int FREQUENT_FAILURE_PERIOD_DAYS = 14;

  private final EntityPersister entityPersister;
  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final RecallLogRepository recallLogRepository;
  private final ConversationRepository conversationRepository;
  private final MemoryTrackerAssimilation assimilation;
  private final SpellingRecallGrading spellingRecallGrading;

  public MemoryTrackerService(
      EntityPersister entityPersister,
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository,
      RecallPromptRepository recallPromptRepository,
      RecallLogRepository recallLogRepository,
      ConversationRepository conversationRepository,
      WikiLinkResolver wikiLinkResolver,
      AssimilationSequenceSkipRepository skipRepository) {
    this.entityPersister = entityPersister;
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.recallLogRepository = recallLogRepository;
    this.conversationRepository = conversationRepository;
    this.assimilation =
        new MemoryTrackerAssimilation(entityPersister, userService, this, skipRepository);
    this.spellingRecallGrading =
        new SpellingRecallGrading(entityPersister, recallPromptRepository, wikiLinkResolver, this);
  }

  public List<MemoryTracker> findLast100ByUser(Integer userId) {
    return memoryTrackerRepository.findLast100ByUser(userId);
  }

  public List<MemoryTracker> findLast100RecalledByUser(Integer userId) {
    return memoryTrackerRepository.findLast100RecalledByUser(userId);
  }

  public List<MemoryTracker> assimilate(
      AssimilationRequestDTO request, User currentUser, Timestamp currentTime) {
    return assimilation.assimilate(request, currentUser, currentTime);
  }

  public void updateStability(MemoryTracker memoryTracker, float adjustment) {
    memoryTracker.setStability(memoryTracker.getStability() + adjustment);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    entityPersister.save(memoryTracker);
  }

  public boolean updateMemoryTrackerAfterAnsweringQuestion(
      Timestamp currentUTCTimestamp, Boolean correct, RecallPrompt recallPrompt) {
    MemoryTracker memoryTracker = recallPrompt.requireMemoryTracker();
    Answer answer = recallPrompt.getAnswer();
    return markAsRecalled(currentUTCTimestamp, correct, memoryTracker, answer);
  }

  public boolean markAsRecalled(
      Timestamp currentUTCTimestamp, Boolean correct, MemoryTracker memoryTracker, Answer answer) {
    persistRecallLog(
        memoryTracker,
        currentUTCTimestamp,
        correct ? ProductOutcome.GOOD : ProductOutcome.AGAIN,
        answer);
    memoryTracker.markAsRecalled(currentUTCTimestamp, correct);
    entityPersister.save(memoryTracker);

    if (!correct) {
      return isThresholdExceeded(memoryTracker, currentUTCTimestamp);
    }
    return false;
  }

  void persistRecallLog(
      MemoryTracker memoryTracker,
      Timestamp recordedAt,
      ProductOutcome productOutcome,
      Answer answer) {
    RecallLog recallLog = new RecallLog();
    recallLog.setMemoryTracker(memoryTracker);
    recallLog.setRecordedAt(recordedAt);
    recallLog.setElapsedHours((int) memoryTracker.elapsedHoursUntil(recordedAt));
    recallLog.setProductOutcome(productOutcome);
    recallLog.setAnswer(answer);
    entityPersister.save(recallLog);
    memoryTracker.addRecallLog(recallLog);
    if (answer != null) {
      answer.addRecallLog(recallLog);
    }
  }

  public Optional<MemoryTracker> findConfusionAdjustmentTracker(User user, Note note) {
    List<MemoryTracker> activeNoteLevel =
        userService.getMemoryTrackersFor(user, note).stream()
            .filter(MemoryTracker::isActive)
            .filter(MemoryTracker::isNoteLevelTracker)
            .toList();
    return activeNoteLevel.stream()
        .filter(MemoryTracker::isSpelling)
        .findFirst()
        .or(() -> activeNoteLevel.stream().filter(MemoryTracker::isUnderstanding).findFirst());
  }

  public void applyConfusionAdjustment(MemoryTracker tracker, Timestamp currentUTCTimestamp) {
    tracker.adjustForConfusion(currentUTCTimestamp);
    entityPersister.save(tracker);
  }

  public void delete(MemoryTracker memoryTracker) {
    entityPersister.remove(memoryTracker);
  }

  public void updatePropertyKey(MemoryTracker memoryTracker, String newPropertyKey) {
    if (memoryTracker.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory tracker is deleted");
    }
    if (memoryTracker.isNoteLevelTracker()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot rename note-level memory tracker");
    }
    if (newPropertyKey == null || newPropertyKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Property key must not be blank");
    }
    if (newPropertyKey.equals(memoryTracker.getPropertyKey())) {
      return;
    }
    boolean conflict =
        userService.getMemoryTrackersFor(memoryTracker.getUser(), memoryTracker.getNote()).stream()
            .filter(MemoryTracker::isActive)
            .filter(mt -> !mt.isSpelling())
            .filter(mt -> !mt.getId().equals(memoryTracker.getId()))
            .anyMatch(mt -> newPropertyKey.equals(mt.getPropertyKey()));
    if (conflict) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A property memory tracker for \"" + newPropertyKey + "\" already exists on this note.");
    }
    memoryTracker.setPropertyKey(newPropertyKey);
    entityPersister.save(memoryTracker);
  }

  public RecallPrompt answerSpelling(
      MemoryTracker memoryTracker,
      AnswerSpellingDTO answerSpellingDTO,
      User user,
      Timestamp currentUTCTimestamp) {
    return spellingRecallGrading.answerSpelling(
        memoryTracker, answerSpellingDTO, currentUTCTimestamp);
  }

  public List<RecallPrompt> getAllRecallPrompts(MemoryTracker memoryTracker) {
    return recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(memoryTracker.getId());
  }

  public List<RecallLog> getRecallLogs(MemoryTracker memoryTracker) {
    return recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(
        memoryTracker.getId());
  }

  public void deleteUnansweredRecallPrompts(MemoryTracker memoryTracker) {
    List<RecallPrompt> unansweredPrompts =
        recallPromptRepository.findAllUnansweredByMemoryTrackerId(memoryTracker.getId());
    if (!unansweredPrompts.isEmpty()) {
      conversationRepository
          .findBySubjectRecallPromptIn(unansweredPrompts)
          .forEach(
              conversation -> {
                conversation.getSubject().setRecallPrompt(null);
                conversationRepository.save(conversation);
              });
    }
    unansweredPrompts.forEach(entityPersister::remove);
  }

  public RecallPrompt getSpellingQuestion(MemoryTracker memoryTracker) {
    return spellingRecallGrading.getSpellingQuestion(memoryTracker);
  }

  public record SpellingAnswerResult(RecallPrompt recallPrompt, List<Note> matchedNotes) {}

  public SpellingAnswerResult answerSpelling(
      RecallPrompt recallPrompt,
      AnswerSpellingDTO answerSpellingDTO,
      User user,
      Timestamp currentUTCTimestamp) {
    return spellingRecallGrading.answerSpelling(
        recallPrompt, answerSpellingDTO, user, currentUTCTimestamp);
  }

  public ThresholdExceededResult getThresholdExceededResult(
      MemoryTracker memoryTracker, Timestamp currentTime) {
    Timestamp since =
        new Timestamp(
            currentTime.getTime() - (long) FREQUENT_FAILURE_PERIOD_DAYS * 24 * 60 * 60 * 1000);
    int wrongCount =
        recallLogRepository.countAgainOutcomesSinceForMemoryTracker(memoryTracker.getId(), since);
    return new ThresholdExceededResult(
        wrongCount >= FREQUENT_FAILURE_THRESHOLD,
        wrongCount,
        FREQUENT_FAILURE_THRESHOLD,
        FREQUENT_FAILURE_PERIOD_DAYS);
  }

  public boolean isThresholdExceeded(MemoryTracker memoryTracker, Timestamp currentTime) {
    return getThresholdExceededResult(memoryTracker, currentTime).thresholdExceeded();
  }
}
