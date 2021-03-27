package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;

public interface QuizQuestionFactory {
    List<NoteEntity> generateFillingOptions();

    String generateInstruction();

    String generateMainTopic();

    NoteEntity generateAnswerNote();

    List<QuizQuestion.Option> toQuestionOptions(List<NoteEntity> noteEntities);
}
