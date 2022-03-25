package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;

public class JustReviewQuizFactory extends ClozeDescriptonQuizFactory {
    public JustReviewQuizFactory(ReviewPoint reviewPoint) {
        super(reviewPoint);
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        return new ArrayList<>();
    }

    @Override
    public boolean isValidQuestion() {
        return true;
    }
}
