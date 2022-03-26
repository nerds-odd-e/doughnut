package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;

import java.util.List;

public interface QuestionOptionsFactory {
    Note generateAnswerNote(QuizQuestionServant servant);
    List<Note> generateFillingOptions(QuizQuestionServant servant);
    int minimumOptionCount();

    default List<Note> generateOptions(QuizQuestionServant servant) {
        Note answerNote = generateAnswerNote(servant);
        if (answerNote == null) return null;
        List<Note> fillingOptions = generateFillingOptions(servant);
        if (minimumOptionCount() > fillingOptions.size() + 1) {
            return null;
        }
        fillingOptions.add(answerNote);
        return fillingOptions;
    }
}
