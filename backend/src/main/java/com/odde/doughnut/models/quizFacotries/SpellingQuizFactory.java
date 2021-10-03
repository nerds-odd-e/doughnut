package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;

public class SpellingQuizFactory extends ClozeDescriptonQuizFactory {
    public SpellingQuizFactory(ReviewPoint reviewPoint) {
        super(reviewPoint);
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        return new ArrayList<>();
    }

    @Override
    public boolean isValidQuestion() {
        Note note = reviewPoint.getNote();
        if (!Strings.isEmpty(note.getNoteContent().getDescription())) {
            ReviewSetting reviewSetting = note.getMasterReviewSetting();
            return reviewSetting != null && reviewSetting.getRememberSpelling();
        }
        return false;
    }
}
