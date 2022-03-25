package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;

import java.util.ArrayList;
import java.util.List;

public interface QuizQuestionFactory {
    Note generateAnswerNote(QuizQuestionServant servant);

    default List<Note> generateFillingOptions(QuizQuestionServant servant) {
        return new ArrayList<>();
    }

    default boolean isValidQuestion() {
        return true;
    }

    default int minimumFillingOptionCount() {
        return 0;
    }

    default int minimumViceReviewPointCount() {
        return 0;
    }

    default List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
        return null;
    }

    default List<Note> knownRightAnswers() {
        return null;
    }

    default List<Note> allWrongAnswers() {
        return null;
    }

    default Link getCategoryLink() { return null; }
}
