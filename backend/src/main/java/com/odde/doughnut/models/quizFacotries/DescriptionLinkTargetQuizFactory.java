package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.entities.ReviewPoint;
import org.apache.logging.log4j.util.Strings;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory {

    public DescriptionLinkTargetQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(servant, reviewPoint);
    }

    @Override
    public String generateInstruction() {
        String clozeDescription = ClozeDescription.htmlClosedDescription().getClozeDescription(answerNote.getNoteContent().getNoteTitle(), getSourceDescription());
        return "<p>The following descriptions " + link.getLinkTypeLabel() + ":</p>" + "<pre>" + clozeDescription + "</pre> " ;
    }

    private String getSourceDescription() {
        return link.getSourceNote().getNoteContent().getDescription();
    }

    @Override
    public boolean isValidQuestion() {
        return super.isValidQuestion() && Strings.isNotEmpty(getSourceDescription());
    }

}