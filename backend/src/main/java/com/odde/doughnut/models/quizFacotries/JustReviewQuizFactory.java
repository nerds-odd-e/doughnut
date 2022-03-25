package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;

import java.util.List;

public record JustReviewQuizFactory(ReviewPoint reviewPoint) implements QuizQuestionFactory {

    @Override
    public boolean isValidQuestion() {
        return true;
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return reviewPoint.getNote();
    }

    @Override
    public List<Note> knownRightAnswers() {
        return List.of();
    }
}
