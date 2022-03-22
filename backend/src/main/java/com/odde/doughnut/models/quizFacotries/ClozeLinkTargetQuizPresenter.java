package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;

public class ClozeLinkTargetQuizPresenter extends LinkTargetQuizPresenter {

    private final Note answerNote;

    public ClozeLinkTargetQuizPresenter(QuizQuestion quizQuestion) {
        super(quizQuestion);
        this.answerNote = link.getTargetNote();
    }

    @Override
    public String generateInstruction() {
        String clozeTitle = ClozeDescription.htmlClosedDescription().getClozeDescription(answerNote.getNoteTitle(), link.getSourceNote().getTitle());
        return "<mark>" + clozeTitle + "</mark> is " + link.getLinkTypeLabel() + ":";
    }
}