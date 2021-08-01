package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.List;
import java.util.Map;

public interface QuizQuestionFactory {
    List<Note> generateFillingOptions(QuizQuestionServant servant);

    String generateInstruction();

    String generateMainTopic();

    Note generateAnswerNote(QuizQuestionServant servant);

    Map<Link.LinkType, LinkViewed> generateHintLinks();

    default QuizQuestion.OptionCreator optionCreator() {
        return new QuizQuestion.TitleOptionCreator();
    }

    default boolean isValidQuestion() {
        return true;
    }

    default int minimumFillingOptionCount() {
        return 0;
    }

    default List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
        return null;
    }

    default List<Note> generateScope() {
        return null;
    }
}
