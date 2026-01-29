package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.controllers.dto.SpellingResultDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemoryTrackerService {
  private final EntityPersister entityPersister;
  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final RecallPromptRepository recallPromptRepository;

  public MemoryTrackerService(
      EntityPersister entityPersister,
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository,
      RecallPromptRepository recallPromptRepository) {
    this.entityPersister = entityPersister;
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.recallPromptRepository = recallPromptRepository;
  }

  public List<MemoryTracker> findLast100ByUser(Integer userId) {
    return memoryTrackerRepository.findLast100ByUser(userId);
  }

  public List<MemoryTracker> findLast100ReviewedByUser(Integer userId) {
    return memoryTrackerRepository.findLast100ReviewedByUser(userId);
  }

  public List<MemoryTracker> assimilate(
      InitialInfo initialInfo, User currentUser, Timestamp currentTime) {
    Note note = entityPersister.find(Note.class, initialInfo.noteId);

    MemoryTracker memoryTracker =
        createMemoryTracker(
            note,
            currentUser,
            currentTime,
            initialInfo.skipMemoryTracking != null ? initialInfo.skipMemoryTracking : false,
            false);

    List<MemoryTracker> trackers = new ArrayList<>();
    trackers.add(memoryTracker);

    if (note.getRecallSetting().getRememberSpelling()) {
      MemoryTracker spellingTracker =
          createMemoryTracker(
              note,
              currentUser,
              currentTime,
              initialInfo.skipMemoryTracking != null ? initialInfo.skipMemoryTracking : false,
              true);
      trackers.add(spellingTracker);
    }

    return trackers;
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

  public void updateForgettingCurve(MemoryTracker memoryTracker, float adjustment) {
    memoryTracker.setForgettingCurveIndex(memoryTracker.getForgettingCurveIndex() + adjustment);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    entityPersister.save(memoryTracker);
  }

  public void updateMemoryTrackerAfterAnsweringQuestion(
      User user, Timestamp currentUTCTimestamp, Boolean correct, RecallPrompt recallPrompt) {
    List<MemoryTracker> memoryTrackers =
        userService.getMemoryTrackersFor(user, recallPrompt.getPredefinedQuestion().getNote());
    Integer thinkingTimeMs =
        recallPrompt.getAnswer() != null ? recallPrompt.getAnswer().getThinkingTimeMs() : null;
    memoryTrackers.stream()
        .filter(
            tracker -> {
              Boolean trackerSpelling = tracker.getSpelling();
              return !Boolean.TRUE.equals(trackerSpelling);
            })
        .findFirst()
        .ifPresent(
            memoryTracker ->
                markAsRepeated(currentUTCTimestamp, correct, memoryTracker, thinkingTimeMs));
  }

  public void markAsRepeated(
      Timestamp currentUTCTimestamp,
      Boolean correct,
      MemoryTracker memoryTracker,
      Integer thinkingTimeMs) {
    memoryTracker.markAsRepeated(currentUTCTimestamp, correct, thinkingTimeMs);
    entityPersister.save(memoryTracker);
  }

  public SpellingResultDTO answerSpelling(
      MemoryTracker memoryTracker,
      AnswerSpellingDTO answerSpellingDTO,
      User user,
      Timestamp currentUTCTimestamp) {
    String spellingAnswer = answerSpellingDTO.getSpellingAnswer();
    Note note = memoryTracker.getNote();
    Boolean correct = note.matchAnswer(spellingAnswer);
    markAsRepeated(
        currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
    return new SpellingResultDTO(note, spellingAnswer, correct, memoryTracker.getId());
  }

  public List<RecallPrompt> getAllRecallPrompts(MemoryTracker memoryTracker) {
    return recallPromptRepository.findAllByMemoryTrackerIdOrderByIdDesc(memoryTracker.getId());
  }

  public void deleteUnansweredRecallPrompts(MemoryTracker memoryTracker) {
    List<RecallPrompt> unansweredPrompts =
        recallPromptRepository.findAllUnansweredByMemoryTrackerId(memoryTracker.getId());
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

  public SpellingResultDTO answerSpelling(
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

    markAsRepeated(
        currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
    return new SpellingResultDTO(note, spellingAnswer, correct, memoryTracker.getId());
  }

  public boolean hasExceededWrongAnswerThreshold(
      Note note, Timestamp currentTime, int periodDays, int threshold) {
    Timestamp since =
        new Timestamp(currentTime.getTime() - (long) periodDays * 24 * 60 * 60 * 1000);
    int wrongCount = recallPromptRepository.countWrongAnswersSince(note.getId(), since);
    return wrongCount >= threshold;
  }
}
