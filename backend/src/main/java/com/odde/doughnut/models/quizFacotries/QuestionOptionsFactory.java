package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;

import java.util.List;

public interface QuestionOptionsFactory {
    Note generateAnswerNote(QuizQuestionServant servant);
    List<Note> generateFillingOptions(QuizQuestionServant servant);
}
