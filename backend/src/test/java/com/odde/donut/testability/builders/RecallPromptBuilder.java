package com.odde.donut.testability.builders;

import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.entities.Answer;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionType;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;

public class RecallPromptBuilder extends EntityBuilder<RecallPrompt> {
  private final McqBuilder mcqBuilder;
  private AnswerDTO answerDTO = null;
  private MemoryTracker memoryTracker = null;
  private String spellingAnswerText = null;
  private Timestamp answerTimestamp = null;
  private AnswerOutcome answerOutcome = null;

  public RecallPromptBuilder(MakeMe makeMe, RecallPrompt recallPrompt) {
    super(makeMe, recallPrompt);
    mcqBuilder = new McqBuilder(makeMe);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      entity = new RecallPrompt();
      if (entity.getQuestionType() != QuestionType.SPELLING) {
        Mcq mcq = mcqBuilder.please(needPersist);
        entity.setMcq(mcq);
        entity.setQuestionType(QuestionType.MCQ);
      }
    }
    if (answerDTO != null && entity.getQuestionType() != QuestionType.SPELLING) {
      Answer answer =
          Answer.buildAnswer(
              answerDTO, entity.getMcq(), entity.getAnswer(), resolvedAnswerTimestamp());
      entity.setAnswer(answer);
    } else if (spellingAnswerText != null) {
      Answer answer = new Answer();
      answer.setSpellingAnswer(spellingAnswerText);
      answer.setCorrect(true);
      answer.setCreatedAt(resolvedAnswerTimestamp());
      entity.setAnswer(answer);
    }
    if (entity.getAnswer() != null && answerOutcome != null) {
      entity.getAnswer().setOutcome(answerOutcome);
      entity.getAnswer().setCorrect(false);
    }
    if (entity.getMemoryTracker() == null && memoryTracker == null) {
      throw new IllegalStateException("call forMemoryTracker() before please()");
    }
    entity.setMemoryTracker(memoryTracker);
  }

  public RecallPromptBuilder forMemoryTracker(MemoryTracker memoryTracker) {
    this.memoryTracker = memoryTracker;
    return this;
  }

  public RecallPromptBuilder withMcqForNote(Note note) {
    this.mcqBuilder.forNote(note);
    return this;
  }

  public RecallPromptBuilder withMcq(Mcq mcq) {
    this.mcqBuilder.mcq(mcq);
    return this;
  }

  public RecallPromptBuilder answer(AnswerDTO answerDTO) {
    this.answerDTO = answerDTO;
    return this;
  }

  public RecallPromptBuilder answerChoiceIndex(int index) {
    AnswerDTO dto = new AnswerDTO();
    dto.setChoiceIndex(index);
    return answer(dto);
  }

  public RecallPromptBuilder spelling() {
    if (entity == null) {
      entity = new RecallPrompt();
    }
    entity.setQuestionType(QuestionType.SPELLING);
    entity.setMcq(null);
    return this;
  }

  public RecallPromptBuilder contested() {
    this.mcqBuilder.contested();
    return this;
  }

  public RecallPromptBuilder answerSpelling(String spellingAnswer) {
    this.spellingAnswerText = spellingAnswer;
    return this;
  }

  public RecallPromptBuilder answerTimestamp(Timestamp timestamp) {
    this.answerTimestamp = timestamp;
    return this;
  }

  private Timestamp resolvedAnswerTimestamp() {
    return answerTimestamp != null ? answerTimestamp : new Timestamp(System.currentTimeMillis());
  }

  public RecallPromptBuilder overlap() {
    return spellingAnswerWithOutcome(AnswerOutcome.OVERLAP);
  }

  public RecallPromptBuilder accidentalMatch() {
    return spellingAnswerWithOutcome(AnswerOutcome.ACCIDENTAL_MATCH);
  }

  private RecallPromptBuilder spellingAnswerWithOutcome(AnswerOutcome outcome) {
    spelling();
    if (spellingAnswerText == null) {
      spellingAnswerText = "x";
    }
    this.answerOutcome = outcome;
    return this;
  }
}
