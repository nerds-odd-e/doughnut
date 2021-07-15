package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import org.apache.logging.log4j.util.Strings;

public class SpellingQuizFactory extends DefaultQuizFactory{
    public SpellingQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(servant, reviewPoint);
    }

    @Override
    public boolean isValidQuestion() {
        Note note = reviewPoint.getNote();
        if (!Strings.isEmpty(note.getNoteContent().getDescription())) {
            ReviewSetting reviewSetting = note.getMasterReviewSetting();
            if (reviewSetting != null && reviewSetting.getRememberSpelling()) {
                return true;
            }
        }
        return false;
    }
}
