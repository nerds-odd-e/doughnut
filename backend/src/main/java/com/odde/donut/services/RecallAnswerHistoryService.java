package com.odde.donut.services;

import com.odde.donut.entities.Answer;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallLog;
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
  private static final Duration DEFAULT_LOOKBACK = Duration.ofDays(7);

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
  public RecallAnswerHistory recentAttempts(User learner, Instant to) {
    Instant from = to.minus(DEFAULT_LOOKBACK);
    List<Answer> answers =
        answerRepository.findRecordedAnswersOfLearnerBetween(
            learner.getId(), Timestamp.from(from), Timestamp.from(to));
    Map<Answer, RecallPrompt> prompts =
        recallPromptRepository.findAllByAnswerIn(answers).stream()
            .collect(Collectors.toMap(RecallPrompt::getAnswer, Function.identity()));
    List<RecallAttempt> attempts =
        answers.stream()
            .filter(answer -> prompts.containsKey(answer) && prompts.get(answer).getMcq() != null)
            .map(answer -> attempt(answer, prompts.get(answer).getMcq(), learner))
            .toList();
    return new RecallAnswerHistory(from, to, attempts);
  }

  private RecallAttempt attempt(Answer answer, Mcq mcq, User learner) {
    Note note =
        answer.getRecallLogs().stream()
            .filter(log -> !log.isConfusion())
            .map(RecallLog::getMemoryTracker)
            .findFirst()
            .orElseThrow()
            .getNote();
    return new RecallAttempt(
        answer.getCreatedAt().toInstant(),
        answer.getCorrect(),
        new RecallAttempt.SourceNote(
            note.getTitle(), portablePathAuthoring.qualifiedPortablePath(note, learner)),
        new RecallAttempt.MultipleChoice(
            mcq.getQuestionStem(),
            mcq.getResponseChoices(),
            mcq.getResponseChoices().get(answer.getChoiceIndex()),
            mcq.getResponseChoices().get(mcq.getCorrectAnswerIndex()),
            mcq.getValidationRationale()));
  }
}
