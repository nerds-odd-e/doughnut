package com.odde.donut.services;

import com.odde.donut.entities.Answer;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AnswerRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.RetrievalConfig;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the learner's recall answer history in a rolling window ending at the call. */
@Service
public class RecallAnswerHistoryService {
  /** A fixed modest budget for a missed multiple-choice note's graph; get_note_graph asks more. */
  private static final int GRAPH_TOKEN_BUDGET = 1000;

  /** Answers per response, sized for an assistant to read. */
  private static final int PAGE_SIZE = 50;

  private final AnswerRepository answerRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final PortablePathAuthoring portablePathAuthoring;
  private final AuthorizationService authorizationService;
  private final FocusContextRetrievalService focusContextRetrievalService;

  public RecallAnswerHistoryService(
      AnswerRepository answerRepository,
      RecallPromptRepository recallPromptRepository,
      PortablePathAuthoring portablePathAuthoring,
      AuthorizationService authorizationService,
      FocusContextRetrievalService focusContextRetrievalService) {
    this.answerRepository = answerRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.portablePathAuthoring = portablePathAuthoring;
    this.authorizationService = authorizationService;
    this.focusContextRetrievalService = focusContextRetrievalService;
  }

  @Transactional(readOnly = true)
  public RecallAnswerHistory recentAttempts(User learner, Instant to, Duration lookback) {
    return continueFrom(learner, RecallAnswerCursor.firstPage(to.minus(lookback), to));
  }

  /** The page of the cursor's window after its position, with a cursor when more remain. */
  @Transactional(readOnly = true)
  public RecallAnswerHistory continueFrom(User learner, RecallAnswerCursor cursor) {
    List<Answer> found =
        answerRepository.findRecordedAnswersOfLearnerBefore(
            learner.getId(),
            Timestamp.from(cursor.from()),
            Timestamp.from(cursor.lastAnsweredAt()),
            cursor.lastAnswerId(),
            PageRequest.of(0, PAGE_SIZE + 1));
    List<Answer> answers = found.subList(0, Math.min(PAGE_SIZE, found.size()));
    Map<Answer, RecallPrompt> prompts =
        recallPromptRepository.findAllByAnswerIn(answers).stream()
            .collect(Collectors.toMap(RecallPrompt::getAnswer, Function.identity()));
    List<RecallAttempt> attempts = new ArrayList<>();
    for (Answer answer : answers) {
      if (prompts.containsKey(answer)) {
        attempts.add(attempt(answer, prompts.get(answer), learner));
      }
    }
    return new RecallAnswerHistory(
        cursor.from(), cursor.to(), attempts, nextCursor(cursor, found, answers));
  }

  private static String nextCursor(
      RecallAnswerCursor cursor, List<Answer> found, List<Answer> answers) {
    if (found.size() == answers.size()) {
      return null;
    }
    Answer last = answers.getLast();
    return new RecallAnswerCursor(
            cursor.from(), cursor.to(), last.getCreatedAt().toInstant(), last.getId())
        .encode();
  }

  private RecallAttempt attempt(Answer answer, RecallPrompt prompt, User learner) {
    Note note = prompt.requireMemoryTracker().getNote();
    Mcq mcq = prompt.getMcq();
    String unavailable = unavailableReason(note, learner);
    return new RecallAttempt(
        answer.getCreatedAt().toInstant(),
        answer.getCorrect(),
        answer.getOutcome(),
        answer.isExcludedAsAmbiguous()
            ? new RecallAttempt.AmbiguityExclusion(answer.getExclusionReason())
            : null,
        unavailable != null
            ? RecallAttempt.SourceNote.unavailable(unavailable)
            : sourceNote(note, learner, answer.getCorrect(), mcq != null),
        mcq == null ? null : multipleChoice(answer, mcq),
        mcq == null ? spelling(answer, prompt, unavailable) : null);
  }

  /** Why the learner can no longer read the note now, or null when they can. */
  private String unavailableReason(Note note, User learner) {
    if (note.isTrashed()) {
      return "The note is in the trash.";
    }
    if (!authorizationService.userMayReadNote(learner, note)) {
      return "The learner can no longer read this note's notebook.";
    }
    return null;
  }

  /** Evidence follows recorded correctness: a miss carries what the learner needs to see. */
  private RecallAttempt.SourceNote sourceNote(
      Note note, User learner, boolean correct, boolean multipleChoice) {
    String portablePath = portablePathAuthoring.qualifiedPortablePath(note, learner);
    if (correct) {
      return new RecallAttempt.SourceNote(note.getTitle(), portablePath, null, null, null);
    }
    if (multipleChoice) {
      return new RecallAttempt.SourceNote(
          note.getTitle(),
          portablePath,
          null,
          focusContextRetrievalService.retrieve(
              note, learner, RetrievalConfig.forGraphApi(GRAPH_TOKEN_BUDGET)),
          null);
    }
    return new RecallAttempt.SourceNote(
        note.getTitle(), portablePath, note.getContent(), null, null);
  }

  private static RecallAttempt.MultipleChoice multipleChoice(Answer answer, Mcq mcq) {
    return new RecallAttempt.MultipleChoice(
        mcq.getQuestionStem(),
        mcq.getResponseChoices(),
        mcq.getResponseChoices().get(answer.getChoiceIndex()),
        mcq.getResponseChoices().get(mcq.getCorrectAnswerIndex()),
        mcq.getValidationRationale());
  }

  /** The masked prompt is derived from the note, so it shares the note's availability. */
  private static RecallAttempt.Spelling spelling(
      Answer answer, RecallPrompt prompt, String unavailable) {
    if (unavailable != null) {
      return new RecallAttempt.Spelling(answer.getSpellingAnswer(), null, unavailable);
    }
    if (prompt.isNoteEditedSinceAnswer()) {
      return new RecallAttempt.Spelling(
          answer.getSpellingAnswer(),
          null,
          "The note was edited after this answer, so the prompt as shown cannot be recovered.");
    }
    return new RecallAttempt.Spelling(
        answer.getSpellingAnswer(), prompt.getSpellingQuestion().getStem(), null);
  }
}
