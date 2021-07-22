package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.List;
import java.util.Map;

public interface QuizQuestionFactory {
    List<Note> generateFillingOptions();

    String generateInstruction();

    String generateMainTopic();

    Note generateAnswerNote();

    List<QuizQuestion.Option> toQuestionOptions(List<Note> notes);

    Map<Link.LinkType, LinkViewed> generateHintLinks();

    default boolean isValidQuestion() {
        return true;
    }

    default List<ReviewPoint> getViceReviewPoints() {
        return null;
    }
}
