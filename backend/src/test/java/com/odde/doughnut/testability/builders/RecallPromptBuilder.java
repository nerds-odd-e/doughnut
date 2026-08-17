package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class RecallPromptBuilder extends EntityBuilder<RecallPrompt> {
  private final McqBuilder mcqBuilder;
  private AnswerDTO answerDTO = null;
  private MemoryTracker memoryTracker = null;
  private String spellingAnswerText = null;
  private Timestamp answerTimestamp = null;

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
      Answer answer = Answer.buildAnswer(answerDTO, entity.getMcq(), entity.getAnswer());
      if (answerTimestamp != null) {
        answer.setCreatedAt(answerTimestamp);
      }
      entity.setAnswer(answer);
    } else if (spellingAnswerText != null) {
      Answer answer = new Answer();
      answer.setSpellingAnswer(spellingAnswerText);
      answer.setCorrect(true);
      if (answerTimestamp != null) {
        answer.setCreatedAt(answerTimestamp);
      }
      entity.setAnswer(answer);
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
    this.mcqBuilder.ofAIGeneratedQuestionForNote(note);
    return this;
  }

  public RecallPromptBuilder ofAIGeneratedQuestion(GeneratedMcq generatedMcq, Note note) {
    this.mcqBuilder.ofAIGeneratedQuestion(generatedMcq, note);
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
}
