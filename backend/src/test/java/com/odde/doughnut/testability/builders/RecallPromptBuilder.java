package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;

public class RecallPromptBuilder extends EntityBuilder<RecallPrompt> {
  private final PredefinedQuestionBuilder predefinedQuestionBuilder;
  private AnswerDTO answerDTO = null;
  private MemoryTracker memoryTracker = null;

  public RecallPromptBuilder(MakeMe makeMe, RecallPrompt recallPrompt) {
    super(makeMe, recallPrompt);
    predefinedQuestionBuilder = new PredefinedQuestionBuilder(makeMe);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      entity = new RecallPrompt();
      entity.setPredefinedQuestion(predefinedQuestionBuilder.please(needPersist));
    }
    if (answerDTO != null) {
      entity.buildAnswer(answerDTO);
    }
    // Set MemoryTracker if not already set
    if (entity.getMemoryTracker() == null && memoryTracker == null) {
      Note note = entity.getPredefinedQuestion().getNote();
      if (note != null && note.getCreator() != null) {
        // Find existing memory tracker for this note and user, or create one
        List<MemoryTracker> trackers =
            makeMe
                .entityPersister
                .createQuery(
                    "SELECT mt FROM MemoryTracker mt WHERE mt.note = :note AND mt.user = :user",
                    MemoryTracker.class)
                .setParameter("note", note)
                .setParameter("user", note.getCreator())
                .getResultList();
        if (!trackers.isEmpty()) {
          memoryTracker = trackers.get(0);
        } else {
          memoryTracker = makeMe.aMemoryTrackerFor(note).by(note.getCreator()).please(needPersist);
        }
      }
    }
    if (memoryTracker != null) {
      entity.setMemoryTracker(memoryTracker);
    }
  }

  public RecallPromptBuilder forMemoryTracker(MemoryTracker memoryTracker) {
    this.memoryTracker = memoryTracker;
    return this;
  }

  public RecallPromptBuilder approvedQuestionOf(Note note) {
    this.predefinedQuestionBuilder.approvedQuestionOf(note);
    return this;
  }

  public RecallPromptBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.predefinedQuestionBuilder.ofAIGeneratedQuestion(mcqWithAnswer, note);
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
}
