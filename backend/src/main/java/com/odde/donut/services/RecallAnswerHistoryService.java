package com.odde.donut.services;

import com.odde.donut.entities.Answer;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AnswerRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the learner's recall answer history in a rolling window ending at the call. */
@Service
public class RecallAnswerHistoryService {
  private final AnswerRepository answerRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final PortablePathAuthoring portablePathAuthoring;

  public RecallAnswerHistoryService(
      AnswerRepository answerRepository,
      RecallPromptRepository recallPromptRepository,
      PortablePathAuthoring portablePathAuthoring) {
    this.answerRepository = answerRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.portablePathAuthoring = portablePathAuthoring;
  }

  @Transactional(readOnly = true)
  public RecallAnswerHistory recentAttempts(User learner, Instant to, Duration lookback) {
    Instant from = to.minus(lookback);
    List<Answer> answers =
        answerRepository.findRecordedAnswersOfLearnerBetween(
            learner.getId(), Timestamp.from(from), Timestamp.from(to));
    Map<Answer, RecallPrompt> prompts =
        recallPromptRepository.findAllByAnswerIn(answers).stream()
            .collect(Collectors.toMap(RecallPrompt::getAnswer, Function.identity()));
    List<RecallAttempt> attempts =
        answers.stream()
            .filter(prompts::containsKey)
            .map(answer -> attempt(answer, prompts.get(answer), learner))
            .toList();
    return new RecallAnswerHistory(from, to, attempts);
  }

  private RecallAttempt attempt(Answer answer, RecallPrompt prompt, User learner) {
    Note note = prompt.requireMemoryTracker().getNote();
    Mcq mcq = prompt.getMcq();
    return new RecallAttempt(
        answer.getCreatedAt().toInstant(),
        answer.getCorrect(),
        answer.getOutcome(),
        answer.isExcludedAsAmbiguous()
            ? new RecallAttempt.AmbiguityExclusion(answer.getExclusionReason())
            : null,
        new RecallAttempt.SourceNote(
            note.getTitle(), portablePathAuthoring.qualifiedPortablePath(note, learner)),
        mcq == null ? null : multipleChoice(answer, mcq),
        mcq == null ? spelling(answer, prompt) : null);
  }

  private static RecallAttempt.MultipleChoice multipleChoice(Answer answer, Mcq mcq) {
    return new RecallAttempt.MultipleChoice(
        mcq.getQuestionStem(),
        mcq.getResponseChoices(),
        mcq.getResponseChoices().get(answer.getChoiceIndex()),
        mcq.getResponseChoices().get(mcq.getCorrectAnswerIndex()),
        mcq.getValidationRationale());
  }

  private static RecallAttempt.Spelling spelling(Answer answer, RecallPrompt prompt) {
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
