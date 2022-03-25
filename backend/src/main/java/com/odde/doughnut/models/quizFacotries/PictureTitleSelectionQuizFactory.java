package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;
import org.apache.logging.log4j.util.Strings;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
    public PictureTitleSelectionQuizFactory(ReviewPoint reviewPoint) {
        super(reviewPoint);
    }

    @Override
    public boolean isValidQuestion() {
        return reviewPoint.getNote().getPictureWithMask().isPresent();
    }

}
