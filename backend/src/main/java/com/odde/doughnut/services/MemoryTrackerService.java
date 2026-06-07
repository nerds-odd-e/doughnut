package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.ConversationRepository;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemoryTrackerService {
  private static final int WRONG_ANSWER_THRESHOLD = 5;
  private static final int WRONG_ANSWER_PERIOD_DAYS = 14;

  private final EntityPersister entityPersister;
  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final ConversationRepository conversationRepository;

  public MemoryTrackerService(
      EntityPersister entityPersister,
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository,
      RecallPromptRepository recallPromptRepository,
      ConversationRepository conversationRepository) {
    this.entityPersister = entityPersister;
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.conversationRepository = conversationRepository;
  }

  public List<MemoryTracker> findLast100ByUser(Integer userId) {
    return memoryTrackerRepository.findLast100ByUser(userId);
  }

  public List<MemoryTracker> findLast100RecalledByUser(Integer userId) {
    return memoryTrackerRepository.findLast100RecalledByUser(userId);
  }

  public List<MemoryTracker> assimilate(
      AssimilationRequestDTO request, User currentUser, Timestamp currentTime) {
    Note note = entityPersister.find(Note.class, request.noteId);
    List<MemoryTracker> existingTrackers = userService.getMemoryTrackersFor(currentUser, note);
    boolean skipMemoryTracking =
        request.skipMemoryTracking != null ? request.skipMemoryTracking : false;

    if (request.propertyKey != null && !request.propertyKey.isEmpty()) {
      boolean propertyTrackerExists =
          existingTrackers.stream().anyMatch(mt -> request.propertyKey.equals(mt.getPropertyKey()));
      if (propertyTrackerExists) {
        return List.of();
      }
      MemoryTracker propertyTracker =
          createPropertyMemoryTracker(
              note, currentUser, currentTime, skipMemoryTracking, request.propertyKey);
      return List.of(propertyTracker);
    }

    List<MemoryTracker> existingNoteLevelTrackers =
        existingTrackers.stream().filter(MemoryTrackerService::isNoteLevelTracker).toList();

    boolean addSpellingOnly =
        !existingNoteLevelTrackers.isEmpty()
            && Boolean.TRUE.equals(note.getRecallSetting().getRememberSpelling())
            && existingNoteLevelTrackers.stream()
                .noneMatch(mt -> Boolean.TRUE.equals(mt.getSpelling()));

    if (addSpellingOnly) {
      MemoryTracker spellingTracker =
          createMemoryTracker(note, currentUser, currentTime, skipMemoryTracking, true);
      return List.of(spellingTracker);
    }

    if (!existingNoteLevelTrackers.isEmpty()) {
      return List.of();
    }

    MemoryTracker memoryTracker =
        createMemoryTracker(note, currentUser, currentTime, skipMemoryTracking, false);
    List<MemoryTracker> trackers = new ArrayList<>();
    trackers.add(memoryTracker);

    if (Boolean.TRUE.equals(note.getRecallSetting().getRememberSpelling())) {
      MemoryTracker spellingTracker =
          createMemoryTracker(note, currentUser, currentTime, skipMemoryTracking, true);
      trackers.add(spellingTracker);
    }

    return trackers;
  }

  private static boolean isNoteLevelTracker(MemoryTracker memoryTracker) {
    String propertyKey = memoryTracker.getPropertyKey();
    return propertyKey == null || propertyKey.isEmpty();
  }

  private MemoryTracker createMemoryTracker(
      Note note,
      User currentUser,
      Timestamp currentTime,
      boolean skipMemoryTracking,
      boolean isSpelling) {
    MemoryTracker memoryTracker = MemoryTracker.buildMemoryTrackerForNote(note);
    memoryTracker.setRemovedFromTracking(skipMemoryTracking);
    memoryTracker.setSpelling(isSpelling);
    memoryTracker.setUser(currentUser);
    memoryTracker.setAssimilatedAt(currentTime);
    memoryTracker.setLastRecalledAt(currentTime);
    updateForgettingCurve(memoryTracker, 0.0f);
    return memoryTracker;
  }

  private MemoryTracker createPropertyMemoryTracker(
      Note note,
      User currentUser,
      Timestamp currentTime,
      boolean skipMemoryTracking,
      String propertyKey) {
    MemoryTracker memoryTracker = MemoryTracker.buildMemoryTrackerForProperty(note, propertyKey);
    memoryTracker.setRemovedFromTracking(skipMemoryTracking);
    memoryTracker.setSpelling(false);
    memoryTracker.setUser(currentUser);
    memoryTracker.setAssimilatedAt(currentTime);
    memoryTracker.setLastRecalledAt(currentTime);
    updateForgettingCurve(memoryTracker, 0.0f);
    return memoryTracker;
  }

  public void updateForgettingCurve(MemoryTracker memoryTracker, float adjustment) {
    memoryTracker.setForgettingCurveIndex(memoryTracker.getForgettingCurveIndex() + adjustment);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    entityPersister.save(memoryTracker);
  }

  public boolean updateMemoryTrackerAfterAnsweringQuestion(
      Timestamp currentUTCTimestamp, Boolean correct, RecallPrompt recallPrompt) {
    MemoryTracker memoryTracker = recallPrompt.getMemoryTracker();
    if (memoryTracker == null) {
      return false;
    }
    Integer thinkingTimeMs =
        recallPrompt.getAnswer() != null ? recallPrompt.getAnswer().getThinkingTimeMs() : null;
    return markAsRecalled(currentUTCTimestamp, correct, memoryTracker, thinkingTimeMs);
  }

  public boolean markAsRecalled(
      Timestamp currentUTCTimestamp,
      Boolean correct,
      MemoryTracker memoryTracker,
      Integer thinkingTimeMs) {
    memoryTracker.markAsRecalled(currentUTCTimestamp, correct, thinkingTimeMs);
    entityPersister.save(memoryTracker);

    if (!correct) {
      return hasExceededWrongAnswerThreshold(
          memoryTracker.getNote(),
          currentUTCTimestamp,
          WRONG_ANSWER_PERIOD_DAYS,
          WRONG_ANSWER_THRESHOLD);
    }
    return false;
  }

  public void softDelete(MemoryTracker memoryTracker) {
    memoryTracker.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    entityPersister.save(memoryTracker);
  }

  public void updatePropertyKey(MemoryTracker memoryTracker, String newPropertyKey) {
    if (memoryTracker.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory tracker is deleted");
    }
    if (isNoteLevelTracker(memoryTracker)) {
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
            .filter(mt -> !Boolean.TRUE.equals(mt.getSpelling()))
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
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setMemoryTracker(memoryTracker);
    recallPrompt.setQuestionType(QuestionType.SPELLING);
    Answer answer = new Answer();
    answer.setSpellingAnswer(answerSpellingDTO.getSpellingAnswer());
    answer.setCorrect(memoryTracker.getNote().matchAnswer(answerSpellingDTO.getSpellingAnswer()));
    answer.setThinkingTimeMs(answerSpellingDTO.getThinkingTimeMs());
    recallPrompt.setAnswer(answer);
    markAsRecalled(
        currentUTCTimestamp,
        answer.getCorrect(),
        memoryTracker,
        answerSpellingDTO.getThinkingTimeMs());
    return recallPrompt;
  }

  public List<RecallPrompt> getAllRecallPrompts(MemoryTracker memoryTracker) {
    return recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(memoryTracker.getId());
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
    // First check if there's an existing unanswered recall prompt for this memory tracker
    RecallPrompt existingPrompt =
        recallPromptRepository.findUnansweredByMemoryTracker(memoryTracker.getId()).orElse(null);
    if (existingPrompt != null && existingPrompt.getQuestionType() == QuestionType.SPELLING) {
      return existingPrompt;
    }

    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setMemoryTracker(memoryTracker);
    recallPrompt.setQuestionType(QuestionType.SPELLING);
    return entityPersister.save(recallPrompt);
  }

  public RecallPrompt answerSpelling(
      RecallPrompt recallPrompt,
      AnswerSpellingDTO answerSpellingDTO,
      User user,
      Timestamp currentUTCTimestamp) {
    if (recallPrompt.getQuestionType() != QuestionType.SPELLING) {
      throw new IllegalArgumentException("Recall prompt must be of type SPELLING");
    }
    if (recallPrompt.getAnswer() != null) {
      throw new IllegalArgumentException("Recall prompt is already answered");
    }
    MemoryTracker memoryTracker = recallPrompt.getMemoryTracker();
    String spellingAnswer = answerSpellingDTO.getSpellingAnswer();
    Note note = memoryTracker.getNote();
    Boolean correct = note.matchAnswer(spellingAnswer);

    Answer answer = new Answer();
    answer.setSpellingAnswer(spellingAnswer);
    answer.setCorrect(correct);
    answer.setThinkingTimeMs(answerSpellingDTO.getThinkingTimeMs());
    recallPrompt.setAnswer(answer);
    entityPersister.save(recallPrompt);

    markAsRecalled(
        currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
    return recallPrompt;
  }

  public boolean hasExceededWrongAnswerThreshold(
      Note note, Timestamp currentTime, int periodDays, int threshold) {
    Timestamp since =
        new Timestamp(currentTime.getTime() - (long) periodDays * 24 * 60 * 60 * 1000);
    int wrongCount = recallPromptRepository.countWrongAnswersSince(note.getId(), since);
    return wrongCount >= threshold;
  }

  public boolean isThresholdExceeded(MemoryTracker memoryTracker, Timestamp currentTime) {
    return hasExceededWrongAnswerThreshold(
        memoryTracker.getNote(), currentTime, WRONG_ANSWER_PERIOD_DAYS, WRONG_ANSWER_THRESHOLD);
  }
}
