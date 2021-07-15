package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;

public class PictureTitleSelectionQuizFactory extends DefaultQuizFactory{
    public PictureTitleSelectionQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(servant, reviewPoint);
    }

    @Override
    public boolean isValidQuestion() {
        return true;
    }

}
