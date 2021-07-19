package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.entities.ReviewPoint;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

    public ClozeLinkTargetQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(servant, reviewPoint);
    }

    @Override
    public String generateInstruction() {
        String clozeDescription = ClozeDescription.htmlClosedDescription().getClozeDescription(answerNote.getNoteContent().getNoteTitle(), link.getSourceNote().getTitle());
        return "<mark>" + clozeDescription + "</mark> " + link.getLinkTypeLabel() + ":";
    }

}