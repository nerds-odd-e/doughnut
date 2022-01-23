package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.entities.ReviewPoint;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

    public ClozeLinkTargetQuizFactory(ReviewPoint reviewPoint) {
        super(reviewPoint);
    }

    @Override
    public String generateInstruction() {
        String clozeTitle = ClozeDescription.htmlClosedDescription().getClozeDescription(answerNote.getNoteTitle(), link.getSourceNote().getTitle());
        return "<mark>" + clozeTitle + "</mark> is " + link.getLinkTypeLabel() + ":";
    }
}