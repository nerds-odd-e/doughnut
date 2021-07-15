package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;
import org.apache.logging.log4j.util.Strings;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
    public PictureTitleSelectionQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(servant, reviewPoint);
    }

    @Override
    public boolean isValidQuestion() {
        return !Strings.isEmpty(reviewPoint.getNote().getNotePicture());
    }

}
