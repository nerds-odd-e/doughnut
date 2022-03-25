package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;

import java.util.List;

public class PictureSelectionQuizFactory implements QuizQuestionFactory {
    private final Note answerNote;

    public PictureSelectionQuizFactory(ReviewPoint reviewPoint) {
        this.answerNote = reviewPoint.getNote();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        return servant.choose5FromCohort(answerNote, n -> n.getNoteAccessories().hasPicture() && !n.equals(answerNote));
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return answerNote;
    }

    @Override
    public boolean isValidQuestion() {
        return answerNote.getPictureWithMask().isPresent();
    }

    @Override
    public int minimumFillingOptionCount() {
        return 1;
    }

    @Override
    public List<Note> knownRightAnswers() {
        return List.of(answerNote);
    }

}