package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;

public interface QuizQuestionFactory {
    List<Note> generateFillingOptions();

    String generateInstruction();

    String generateMainTopic();

    Note generateAnswerNote();

    List<QuizQuestion.Option> toQuestionOptions(List<Note> notes);
}
