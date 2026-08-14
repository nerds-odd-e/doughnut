package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.FrontmatterOverlaps;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerOutcome;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/** Grades spelling answers and creates spelling recall prompts for a memory tracker. */
final class SpellingRecallGrading {
  private final EntityPersister entityPersister;
  private final RecallPromptRepository recallPromptRepository;
  private final WikiLinkResolver wikiLinkResolver;
  private final MemoryTrackerService memoryTrackerService;

  SpellingRecallGrading(
      EntityPersister entityPersister,
      RecallPromptRepository recallPromptRepository,
      WikiLinkResolver wikiLinkResolver,
      MemoryTrackerService memoryTrackerService) {
    this.entityPersister = entityPersister;
    this.recallPromptRepository = recallPromptRepository;
    this.wikiLinkResolver = wikiLinkResolver;
    this.memoryTrackerService = memoryTrackerService;
  }

  RecallPrompt getSpellingQuestion(MemoryTracker memoryTracker) {
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

  RecallPrompt answerSpelling(
      MemoryTracker memoryTracker,
      AnswerSpellingDTO answerSpellingDTO,
      Timestamp currentUTCTimestamp) {
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setMemoryTracker(memoryTracker);
    recallPrompt.setQuestionType(QuestionType.SPELLING);
    Answer answer = new Answer();
    answer.setSpellingAnswer(answerSpellingDTO.getSpellingAnswer());
    answer.setCorrect(memoryTracker.getNote().matchAnswer(answerSpellingDTO.getSpellingAnswer()));
    answer.setThinkingTimeMs(answerSpellingDTO.getThinkingTimeMs());
    recallPrompt.setAnswer(answer);
    memoryTrackerService.markAsRecalled(
        currentUTCTimestamp,
        answer.getCorrect(),
        memoryTracker,
        answerSpellingDTO.getThinkingTimeMs());
    return recallPrompt;
  }

  MemoryTrackerService.SpellingAnswerResult answerSpelling(
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
    MemoryTracker memoryTracker = recallPrompt.requireMemoryTracker();
    String spellingAnswer = answerSpellingDTO.getSpellingAnswer();
    Note note = memoryTracker.getNote();
    Boolean correct = note.matchAnswer(spellingAnswer);

    Answer answer = new Answer();
    answer.setSpellingAnswer(spellingAnswer);
    answer.setCorrect(correct);
    answer.setThinkingTimeMs(answerSpellingDTO.getThinkingTimeMs());
    recallPrompt.setAnswer(answer);
    recallPrompt = entityPersister.save(recallPrompt);

    if (Boolean.TRUE.equals(correct) && isNonDistinguishingOverlap(note, spellingAnswer, user)) {
      Answer gradedAnswer = recallPrompt.getAnswer();
      gradedAnswer.setCorrect(false);
      gradedAnswer.setOutcome(AnswerOutcome.OVERLAP);
      entityPersister.save(recallPrompt);
      return new MemoryTrackerService.SpellingAnswerResult(recallPrompt, List.of());
    }

    List<Note> matches = List.of();
    if (!correct && spellingAnswer != null && !spellingAnswer.isBlank()) {
      matches = wikiLinkResolver.findAllAccidentalMatches(spellingAnswer, note, user);
      if (!matches.isEmpty()) {
        Answer gradedAnswer = recallPrompt.getAnswer();
        gradedAnswer.setMatchedNoteId(matches.getFirst().getId().longValue());
        gradedAnswer.setOutcome(AnswerOutcome.ACCIDENTAL_MATCH);
        if (matches.size() == 1) {
          memoryTrackerService
              .findConfusionAdjustmentTracker(user, matches.getFirst())
              .ifPresent(
                  target -> {
                    memoryTrackerService.applyConfusionAdjustment(target);
                    gradedAnswer.setConfusionAdjustedMemoryTracker(target);
                  });
        }
      }
    }

    memoryTrackerService.markAsRecalled(
        currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
    return new MemoryTrackerService.SpellingAnswerResult(recallPrompt, matches);
  }

  private boolean isNonDistinguishingOverlap(Note reviewedNote, String spellingAnswer, User user) {
    for (String token :
        FrontmatterOverlaps.overlapWikiLinkTokensFromNoteContent(reviewedNote.getContent())) {
      Matcher matcher = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(token);
      if (!matcher.matches()) {
        continue;
      }
      String inner = matcher.group(1).trim();
      Optional<Note> target = wikiLinkResolver.resolveWikiLinkToken(inner, reviewedNote, user);
      if (target.isEmpty()) {
        continue;
      }
      Note other = target.get();
      if (other.getId().equals(reviewedNote.getId())) {
        continue;
      }
      if (other.matchAnswer(spellingAnswer)) {
        return true;
      }
    }
    return false;
  }
}
