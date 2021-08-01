package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.entities.ReviewPoint;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

    public ClozeLinkTargetQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(null, reviewPoint);
    }

    @Override
    public String generateInstruction() {
        String clozeTitle = ClozeDescription.htmlClosedDescription().getClozeDescription(answerNote.getNoteContent().getNoteTitle(), link.getSourceNote().getTitle());
        return "<mark>" + clozeTitle + "</mark> " + link.getLinkTypeLabel() + ":";
    }

}