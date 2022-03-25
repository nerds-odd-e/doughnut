package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;

import java.util.List;

public abstract class ClozeDescriptonQuizFactory implements QuizQuestionFactory {
    protected final ReviewPoint reviewPoint;
    protected final Note answerNote;

    public ClozeDescriptonQuizFactory(ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.answerNote = getAnswerNote();
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return answerNote;
    }

    @Override
    public List<Note> knownRightAnswers() {
        return List.of(answerNote);
    }

    private Note getAnswerNote() {
        return reviewPoint.getNote();
    }

}